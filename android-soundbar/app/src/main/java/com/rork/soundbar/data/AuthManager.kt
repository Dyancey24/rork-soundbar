package com.rork.soundbar.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom

/** Who is signed in — the account the shelf is kept for. */
data class AuthUser(
    val id: String,
    val name: String,
    val email: String?,
    /** True once the address has tapped the confirmation link. */
    val emailVerified: Boolean = false,
    /** Epoch millis when the requested deletion executes; null while the account stays. */
    val deletionScheduledAt: Long? = null
)

/** Where the app stands with the door. */
sealed interface AuthState {
    data object SignedOut : AuthState
    /** A door is open; [provider] is null while an email sign-in runs. */
    data class InProgress(val provider: AuthProvider?) : AuthState
    /** Signed up, but the account needs the confirmation link from the inbox first. */
    data class EmailConfirmationPending(val email: String) : AuthState
    /** The confirmation link came home — the address is verified; a sign-in finishes the visit. */
    data object EmailConfirmed : AuthState
    data class Failed(val message: String) : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

/** The sign-in doors the house offers. */
enum class AuthProvider(val wireId: String, val displayName: String) {
    GOOGLE("google", "Google"),
    APPLE("apple", "Apple")
}

/**
 * Owns the sign-in session against Supabase Auth: email + password accounts
 * sign up and sign in straight over the auth REST API, while Google/Apple go
 * through the browser with a PKCE exchange and return to the app's deep link.
 * Tokens live in encrypted preferences and the session refreshes on demand.
 */
class AuthManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "soundbar_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val http = HttpClient(Android)

    private var pendingProvider: AuthProvider? = null
    private var pendingVerifier: String? = null

    init {
        val id = prefs.getString(KEY_USER_ID, null)
        if (id != null) {
            _state.value = AuthState.SignedIn(
                AuthUser(
                    id = id,
                    name = prefs.getString(KEY_USER_NAME, null) ?: "Guest",
                    email = prefs.getString(KEY_USER_EMAIL, null),
                    emailVerified = prefs.getBoolean(KEY_USER_VERIFIED, false),
                    deletionScheduledAt = prefs.getString(KEY_DELETION_AT, null)?.toLongOrNull()
                )
            )
        }
    }

    /** Creates an email account. Confirmed accounts sign straight in. */
    fun signUpWithEmail(email: String, password: String) {
        if (_state.value is AuthState.InProgress) return
        val trimmed = email.trim()
        _state.value = AuthState.InProgress(provider = null)
        scope.launch {
            try {
                val body = buildJsonObject {
                    put("email", trimmed)
                    put("password", password)
                    // The confirmation link hands the reader back into the app.
                    put("email_redirect_to", callbackUrl())
                }.toString()
                val response = http.post("${AuthConfig.SUPABASE_URL}/auth/v1/signup") {
                    apiKeyHeaders()
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                if (!response.status.isSuccess()) {
                    _state.value = AuthState.Failed(signUpError(payload))
                    return@launch
                }
                val session = payload["access_token"]?.jsonPrimitive?.contentOrNull
                if (session.isNullOrBlank()) {
                    // Confirmation required — the account exists but waits for the inbox link.
                    _state.value = AuthState.EmailConfirmationPending(trimmed)
                    return@launch
                }
                signInFromSession(payload)
            } catch (error: Exception) {
                Log.w(TAG, "Email sign-up failed: ${error.message}", error)
                _state.value = AuthState.Failed("Couldn't reach the door — check your connection and try again.")
            }
        }
    }

    /** Signs an existing email account in. */
    fun signInWithEmail(email: String, password: String) {
        if (_state.value is AuthState.InProgress) return
        _state.value = AuthState.InProgress(provider = null)
        scope.launch {
            try {
                val body = buildJsonObject {
                    put("email", email.trim())
                    put("password", password)
                }.toString()
                val response = http.post("${AuthConfig.SUPABASE_URL}/auth/v1/token?grant_type=password") {
                    apiKeyHeaders()
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                if (!response.status.isSuccess()) {
                    _state.value = AuthState.Failed(signInError(payload))
                    return@launch
                }
                signInFromSession(payload)
            } catch (error: Exception) {
                Log.w(TAG, "Email sign-in failed: ${error.message}", error)
                _state.value = AuthState.Failed("Couldn't reach the door — check your connection and try again.")
            }
        }
    }

    /** Opens the OAuth door for the chosen provider in a browser tab. */
    fun signIn(provider: AuthProvider) {
        if (_state.value is AuthState.InProgress) return
        val verifier = newToken()
        pendingProvider = provider
        pendingVerifier = verifier
        _state.value = AuthState.InProgress(provider)
        val authorizeUrl = Uri.parse("${AuthConfig.SUPABASE_URL}/auth/v1/authorize").buildUpon()
            .appendQueryParameter("provider", provider.wireId)
            .appendQueryParameter("redirect_to", callbackUrl())
            .appendQueryParameter("code_challenge", challengeFor(verifier))
            .appendQueryParameter("code_challenge_method", "s256")
            .appendQueryParameter("apikey", AuthConfig.SUPABASE_ANON_KEY)
            .build()
        withMain { openBrowser(authorizeUrl.toString()) }
    }

    /**
     * Catches the redirect back into the app — either an OAuth handshake
     * trading its code, or the email-confirmation link carrying the fresh
     * session home on its back.
     */
    fun handleCallback(uri: Uri) {
        val params = callbackParams(uri)

        // The browser reports a failure — a cancelled consent or a stale link.
        params["error"]?.let {
            val wasHandshake = _state.value is AuthState.InProgress
            pendingProvider = null
            pendingVerifier = null
            _state.value = if (wasHandshake) {
                AuthState.Failed("Sign-in was cancelled.")
            } else {
                AuthState.Failed("That confirmation link has expired — sign in to get a fresh one.")
            }
            return
        }

        // The email's confirmation link arrives holding the session itself.
        val access = params["access_token"]
        val refresh = params["refresh_token"]
        if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) {
            pendingProvider = null
            pendingVerifier = null
            _state.value = AuthState.InProgress(provider = null)
            scope.launch { exchangeEmailSession(access, refresh) }
            return
        }

        val code = params["code"] ?: return
        val verifier = pendingVerifier
        if (verifier == null) {
            // A confirmation code without a handshake: the address is verified,
            // but the session must come from a proper sign-in.
            _state.value = AuthState.EmailConfirmed
            return
        }
        if (_state.value !is AuthState.InProgress) return
        scope.launch {
            try {
                val body = buildJsonObject {
                    put("auth_code", code)
                    put("code_verifier", verifier)
                }.toString()
                val response = http.post("${AuthConfig.SUPABASE_URL}/auth/v1/token?grant_type=pkce") {
                    apiKeyHeaders()
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                if (!response.status.isSuccess()) {
                    _state.value = AuthState.Failed("Sign-in couldn't be completed. Please try again.")
                    return@launch
                }
                signInFromSession(payload)
            } catch (error: Exception) {
                Log.w(TAG, "Token exchange failed: ${error.message}", error)
                _state.value = AuthState.Failed("Sign-in couldn't be completed. Please try again.")
            } finally {
                pendingProvider = null
                pendingVerifier = null
            }
        }
    }

    /** Builds the session from the tokens an email-confirmation link brought home. */
    private suspend fun exchangeEmailSession(accessToken: String, refreshToken: String) {
        try {
            val response = http.get("${AuthConfig.SUPABASE_URL}/auth/v1/user") {
                header("apikey", AuthConfig.SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $accessToken")
            }
            if (!response.status.isSuccess()) throw IllegalStateException("user lookup failed")
            val user = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            signInFromSession(buildJsonObject {
                put("access_token", accessToken)
                put("refresh_token", refreshToken)
                put("user", user)
            })
        } catch (error: Exception) {
            Log.w(TAG, "Confirmation session failed: ${error.message}", error)
            // The address is verified regardless — a sign-in completes the visit.
            _state.value = AuthState.EmailConfirmed
        }
    }

    /** The deep link's fragment and query flattened into one map. */
    private fun callbackParams(uri: Uri): Map<String, String> {
        val params = mutableMapOf<String, String>()
        uri.fragment?.split('&')?.forEach { pair ->
            val index = pair.indexOf('=')
            if (index > 0) {
                params[pair.take(index)] = Uri.decode(pair.substring(index + 1))
            }
        }
        listOf("error", "error_description", "code", "access_token", "refresh_token").forEach { key ->
            uri.getQueryParameter(key)?.let { params[key] = it }
        }
        return params
    }

    /** The current bearer token, if a signed-in session holds one. */
    fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Trades the stored refresh token for a fresh access token when the old
     * one expired. True when a usable token is back in place.
     */
    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return false
        return try {
            val body = buildJsonObject {
                put("refresh_token", refreshToken)
            }.toString()
            val response = http.post("${AuthConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token") {
                apiKeyHeaders()
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) return false
            val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val access = payload.str("access_token") ?: return false
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, access)
                .putString(KEY_REFRESH_TOKEN, payload.str("refresh_token") ?: refreshToken)
                .apply()
            true
        } catch (error: Exception) {
            Log.w(TAG, "Token refresh failed")
            false
        }
    }

    /**
     * Schedules the account for deletion. The house holds everything for a
     * 48-hour grace — signing back in and undoing keeps it for good.
     */
    fun requestAccountDeletion() {
        val current = (_state.value as? AuthState.SignedIn)?.user ?: return
        val scheduledAt = System.currentTimeMillis() + GRACE_PERIOD_MS
        scope.launch {
            try {
                updateUserMetadata(buildJsonObject {
                    put("deletion_scheduled_at", scheduledAt)
                })
                updateSignedInUser(current.copy(deletionScheduledAt = scheduledAt))
            } catch (error: Exception) {
                Log.w(TAG, "Deletion request failed: ${error.message}", error)
            }
        }
    }

    /** Second thoughts: takes back a pending deletion request. */
    fun cancelAccountDeletion() {
        val current = (_state.value as? AuthState.SignedIn)?.user ?: return
        if (current.deletionScheduledAt == null) return
        scope.launch {
            try {
                updateUserMetadata(buildJsonObject {
                    put("deletion_scheduled_at", null)
                })
                updateSignedInUser(current.copy(deletionScheduledAt = null))
            } catch (error: Exception) {
                Log.w(TAG, "Deletion cancel failed: ${error.message}", error)
            }
        }
    }

    /** Clears the session; the shelf itself keeps waiting on this device. */
    fun signOut() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (token != null) {
            scope.launch {
                // Best effort — the local session is cleared regardless.
                runCatching {
                    http.post("${AuthConfig.SUPABASE_URL}/auth/v1/logout") {
                        apiKeyHeaders()
                        setBody("")
                    }
                }
            }
        }
        prefs.edit().clear().apply()
        pendingProvider = null
        pendingVerifier = null
        _state.value = AuthState.SignedOut
    }

    /** Updates the signed-in user's metadata over the auth API. */
    private suspend fun updateUserMetadata(data: JsonObject) {
        val token = accessToken() ?: throw IllegalStateException("not signed in")
        val response = http.put("${AuthConfig.SUPABASE_URL}/auth/v1/user") {
            apiKeyHeaders()
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("data", data) }.toString())
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("metadata update failed: ${'$'}{response.status.value}")
        }
    }

    /** Republishes a refreshed account to the session and the encrypted store. */
    private fun updateSignedInUser(user: AuthUser) {
        prefs.edit()
            .putString(KEY_DELETION_AT, user.deletionScheduledAt?.toString())
            .apply()
        _state.value = AuthState.SignedIn(user)
    }

    private fun signInFromSession(payload: JsonObject) {
        val user = payload["user"] as? JsonObject
            ?: throw IllegalStateException("no user in session response")
        val meta = user["user_metadata"] as? JsonObject
        val email = user.str("email")
        val signedIn = AuthUser(
            id = user.str("id") ?: "",
            name = meta?.str("name")
                ?: meta?.str("full_name")
                ?: meta?.str("user_name")
                ?: email?.substringBefore('@')
                ?: "Guest",
            email = email,
            emailVerified = user["email_confirmed_at"] != null || user["confirmed_at"] != null,
            deletionScheduledAt = meta?.str("deletion_scheduled_at")?.toLongOrNull()
        )
        prefs.edit()
            .putString(KEY_USER_ID, signedIn.id)
            .putString(KEY_USER_NAME, signedIn.name)
            .putString(KEY_USER_EMAIL, signedIn.email)
            .putBoolean(KEY_USER_VERIFIED, signedIn.emailVerified)
            .putString(KEY_DELETION_AT, signedIn.deletionScheduledAt?.toString())
            .putString(KEY_ACCESS_TOKEN, payload.str("access_token") ?: "")
            .putString(KEY_REFRESH_TOKEN, payload.str("refresh_token"))
            .apply()
        _state.value = AuthState.SignedIn(signedIn)
    }

    private fun signUpError(payload: JsonObject): String = when (payload.str("error_code")) {
        "user_already_exists", "email_exists" -> "That email already has an account — sign in instead."
        "weak_password" -> "Choose a longer password — at least 6 characters."
        "validation_failed", "email_address_invalid" -> "That email doesn't look right — check it and try again."
        "over_email_send_rate_limit" -> "Too many confirmation emails just now — give it a few minutes and try again."
        else -> "Sign-up couldn't be completed. Please try again."
    }

    private fun signInError(payload: JsonObject): String = when (payload.str("error_code")) {
        "email_not_confirmed" -> "Confirm your email first — tap the link we sent you."
        "invalid_credentials", "invalid_grant" -> "Wrong email or password — try again."
        else -> "Wrong email or password — try again."
    }

    private fun callbackUrl(): String =
        "${AuthConfig.CALLBACK_SCHEME}://${AuthConfig.CALLBACK_HOST}${AuthConfig.CALLBACK_PATH}"

    private fun io.ktor.client.request.HttpRequestBuilder.apiKeyHeaders() {
        headers.append("apikey", AuthConfig.SUPABASE_ANON_KEY)
    }

    private fun withMain(block: () -> Unit) {
        scope.launch { withContext(Dispatchers.Main) { block() } }
    }

    private fun openBrowser(url: String) {
        try {
            val tabs = CustomTabsIntent.Builder().build()
            tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            tabs.launchUrl(appContext, Uri.parse(url))
        } catch (error: Exception) {
            try {
                appContext.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (inner: Exception) {
                Log.w(TAG, "No browser available for sign-in")
                _state.value = AuthState.Failed("No browser is available to sign in with.")
            }
        }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun newToken(length: Int = 32): String =
        Base64.encodeToString(
            ByteArray(length).also { SecureRandom().nextBytes(it) },
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

    private fun challengeFor(verifier: String): String =
        Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

    companion object {
        private const val TAG = "AuthManager"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_VERIFIED = "user_verified"
        private const val KEY_DELETION_AT = "deletion_at"

        /** How long a requested deletion waits before it goes through. */
        private const val GRACE_PERIOD_MS = 48L * 60L * 60L * 1000L
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        @Volatile
        private var instance: AuthManager? = null

        fun get(context: Context): AuthManager = instance ?: synchronized(this) {
            instance ?: AuthManager(context).also { instance = it }
        }
    }
}
