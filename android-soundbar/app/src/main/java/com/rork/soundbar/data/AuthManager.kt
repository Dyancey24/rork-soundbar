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
import io.ktor.client.request.post
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
    val email: String?
)

/** Where the app stands with the door. */
sealed interface AuthState {
    data object SignedOut : AuthState
    data class InProgress(val provider: AuthProvider) : AuthState
    data class Failed(val message: String) : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

/** The sign-in doors the house offers. */
enum class AuthProvider(val wireId: String, val displayName: String) {
    GOOGLE("google", "Google"),
    APPLE("apple", "Apple")
}

/**
 * Owns the sign-in session: opens the OAuth door in a browser tab, catches the
 * redirect back into the app, trades the code for tokens over PKCE, and
 * remembers the user between launches. Tokens live in encrypted preferences.
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
    private var pendingState: String? = null

    init {
        val id = prefs.getString(KEY_USER_ID, null)
        if (id != null) {
            _state.value = AuthState.SignedIn(
                AuthUser(
                    id = id,
                    name = prefs.getString(KEY_USER_NAME, null) ?: "Guest",
                    email = prefs.getString(KEY_USER_EMAIL, null)
                )
            )
        }
    }

    /** Opens the OAuth door for the chosen provider in a browser tab. */
    fun signIn(provider: AuthProvider) {
        if (_state.value is AuthState.InProgress) return
        val verifier = newToken()
        pendingProvider = provider
        pendingVerifier = verifier
        pendingState = newToken(length = 16)
        _state.value = AuthState.InProgress(provider)
        scope.launch {
            try {
                val body = buildJsonObject {
                    put("app_key", AuthConfig.APP_KEY)
                    put("provider", provider.wireId)
                    put("code_challenge", challengeFor(verifier))
                    put("target", "rn")
                    put("env", "native")
                }.toString()
                val response = http.post("${AuthConfig.AUTH_URL}/oauth/initiate") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val authUrl = Json.parseToJsonElement(response.bodyAsText())
                    .jsonObject["auth_url"]?.jsonPrimitive?.contentOrNull
                if (authUrl.isNullOrBlank()) throw IllegalStateException("no auth_url")
                withContext(Dispatchers.Main) { openBrowser(authUrl) }
            } catch (error: Exception) {
                Log.w(TAG, "Sign-in could not start")
                _state.value = AuthState.Failed(
                    "The door wouldn't open — check your connection and try again."
                )
            }
        }
    }

    /** Catches the redirect back into the app and trades the code for tokens. */
    fun handleCallback(uri: Uri) {
        if (_state.value !is AuthState.InProgress) return
        val provider = pendingProvider ?: return
        uri.getQueryParameter("error")?.let {
            _state.value = AuthState.Failed("Sign-in was cancelled.")
            return
        }
        val code = uri.getQueryParameter("code")
        val returnedState = uri.getQueryParameter("state")
        val verifier = pendingVerifier
        if (code.isNullOrBlank() || verifier == null || returnedState != pendingState) {
            _state.value = AuthState.Failed("Sign-in couldn't be verified. Please try again.")
            return
        }
        scope.launch {
            try {
                val body = buildJsonObject {
                    put("app_key", AuthConfig.APP_KEY)
                    put("code", code)
                    put("code_verifier", verifier)
                }.toString()
                val response = http.post("${AuthConfig.AUTH_URL}/oauth/token") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val user = payload["user"] as? JsonObject
                    ?: throw IllegalStateException("no user in token response")
                val signedIn = AuthUser(
                    id = user.str("id") ?: user.str("sub") ?: "",
                    name = user.str("name")
                        ?: user.str("email")?.substringBefore('@')
                        ?: provider.displayName + " guest",
                    email = user.str("email")
                )
                prefs.edit()
                    .putString(KEY_USER_ID, signedIn.id)
                    .putString(KEY_USER_NAME, signedIn.name)
                    .putString(KEY_USER_EMAIL, signedIn.email)
                    .putString(KEY_ACCESS_TOKEN, payload.str("access_token"))
                    .putString(KEY_REFRESH_TOKEN, payload.str("refresh_token"))
                    .apply()
                pendingProvider = null
                pendingVerifier = null
                pendingState = null
                _state.value = AuthState.SignedIn(signedIn)
            } catch (error: Exception) {
                Log.w(TAG, "Token exchange failed")
                _state.value = AuthState.Failed("Sign-in couldn't be completed. Please try again.")
            }
        }
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
                put("app_key", AuthConfig.APP_KEY)
                put("refresh_token", refreshToken)
            }.toString()
            val response = http.post("${AuthConfig.AUTH_URL}/oauth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) return false
            val token = Json.parseToJsonElement(response.bodyAsText())
                .jsonObject.str("access_token") ?: return false
            prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
            true
        } catch (error: Exception) {
            Log.w(TAG, "Token refresh failed")
            false
        }
    }

    /** Clears the session; the shelf itself keeps waiting on this device. */
    fun signOut() {
        prefs.edit().clear().apply()
        pendingProvider = null
        pendingVerifier = null
        pendingState = null
        _state.value = AuthState.SignedOut
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
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        @Volatile
        private var instance: AuthManager? = null

        fun get(context: Context): AuthManager = instance ?: synchronized(this) {
            instance ?: AuthManager(context).also { instance = it }
        }
    }
}
