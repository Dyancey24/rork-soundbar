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
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom

/** Where the house stands with the reader's Spotify account. */
sealed interface SpotifyConnection {
    data object Disconnected : SpotifyConnection
    data object Connecting : SpotifyConnection
    data class Connected(val displayName: String) : SpotifyConnection
    data class Failed(val message: String) : SpotifyConnection
}

/** How a pour into Spotify's own player ended. */
enum class SpotifyQueueResult { QUEUED, NO_DEVICE, NOT_PREMIUM, NEEDS_CONNECT, FAILED }

/**
 * Connects the reader's Spotify account with a PKCE browser handshake (no
 * secret involved), keeps the tokens in encrypted preferences, and lines a
 * blend's tracks up in Spotify's own player queue: every track is matched to
 * its Spotify URI, then handed to /me/player/play on an awake device.
 */
class SpotifyManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<SpotifyConnection>(SpotifyConnection.Disconnected)
    val state: StateFlow<SpotifyConnection> = _state.asStateFlow()

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "soundbar_spotify",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val http = HttpClient(Android)

    /** Track title + artist → Spotify URI, so repeated pours skip the search. */
    private val uriCache = HashMap<String, String>()

    private var pendingState: String? = null

    /** True while the code-for-token exchange is in flight. */
    @Volatile
    private var exchanging = false

    private var connectStartedAt = 0L

    init {
        if (prefs.getString(KEY_REFRESH, null) != null) {
            _state.value = SpotifyConnection.Connected(
                displayName = prefs.getString(KEY_NAME, null) ?: "Spotify"
            )
        }
    }

    /** Opens the Spotify consent screen in a browser tab and remembers the PKCE verifier. */
    fun startConnect() {
        if (!SpotifyConfig.isConfigured) {
            _state.value = SpotifyConnection.Failed("Spotify is not configured yet.")
            return
        }
        // A fresh press always starts a fresh handshake: a reader who backed
        // out of the browser last time must never be stuck at "Connecting".
        if (exchanging) return
        val verifier = codeVerifier()
        val state = newToken(16)
        pendingState = state
        prefs.edit()
            .putString(KEY_VERIFIER, verifier)
            .putString(KEY_PENDING_STATE, state)
            .apply()
        _state.value = SpotifyConnection.Connecting
        connectStartedAt = System.currentTimeMillis()

        val authorize = Uri.parse(SpotifyConfig.AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", SpotifyConfig.CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", SpotifyConfig.REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge(verifier))
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", SpotifyConfig.SCOPES)
            .build()
        // Launched from the application context, the browser needs its own
        // task — without the flag Android refuses the launch outright, which
        // is why the door used to stay silently shut. Custom Tabs first, the
        // plain browser as backup.
        try {
            val tabs = CustomTabsIntent.Builder().build()
            tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            tabs.launchUrl(appContext, authorize)
        } catch (error: Exception) {
            try {
                appContext.startActivity(
                    Intent(Intent.ACTION_VIEW, authorize).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (inner: Exception) {
                Log.w(TAG, "Could not open the Spotify consent screen")
                _state.value = SpotifyConnection.Failed("No browser would open — install or enable one and try again.")
            }
        }
    }

    /**
     * The reader came back to the app without finishing in the browser. A
     * handshake left hanging falls back to disconnected so the button works
     * again; a real callback arriving afterwards still completes.
     */
    fun abandonIfStillConnecting() {
        val waited = System.currentTimeMillis() - connectStartedAt
        if (_state.value is SpotifyConnection.Connecting && !exchanging && waited > 1500) {
            _state.value = SpotifyConnection.Disconnected
        }
    }

    /** Clears a shown failure once the reader has seen it. */
    fun acknowledgeFailure() {
        if (_state.value is SpotifyConnection.Failed) {
            _state.value = if (prefs.getString(KEY_REFRESH, null) != null) {
                SpotifyConnection.Connected(prefs.getString(KEY_NAME, null) ?: "Spotify")
            } else {
                SpotifyConnection.Disconnected
            }
        }
    }

    /** Receives the browser hop back into the house and finishes the handshake. */
    fun handleCallback(data: Uri) {
        val error = data.getQueryParameter("error")
        if (error != null) {
            pendingState = null
            _state.value = SpotifyConnection.Disconnected
            return
        }
        val code = data.getQueryParameter("code") ?: return
        val state = data.getQueryParameter("state")
        // The expected state survives the app being closed while the browser
        // was open, so a cold-started return still matches.
        val expected = pendingState ?: prefs.getString(KEY_PENDING_STATE, null)
        pendingState = null
        prefs.edit().remove(KEY_PENDING_STATE).apply()
        if (state == null || state != expected) {
            _state.value = SpotifyConnection.Failed("Spotify's reply didn't match — try once more.")
            return
        }
        val verifier = prefs.getString(KEY_VERIFIER, null)
        if (verifier == null) {
            _state.value = SpotifyConnection.Failed("Spotify's reply didn't match — try once more.")
            return
        }
        _state.value = SpotifyConnection.Connecting
        exchanging = true
        scope.launch {
            try {
                val form = "grant_type=authorization_code" +
                    "&code=${Uri.encode(code)}" +
                    "&redirect_uri=${Uri.encode(SpotifyConfig.REDIRECT_URI)}" +
                    "&client_id=${Uri.encode(SpotifyConfig.CLIENT_ID)}" +
                    "&code_verifier=${Uri.encode(verifier)}"
                val response = http.post(SpotifyConfig.TOKEN_URL) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(form)
                }
                val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                if (!response.status.isSuccess()) {
                    Log.w(TAG, "Token exchange failed: ${payload["error"]?.jsonPrimitive?.contentOrNull}")
                    _state.value = SpotifyConnection.Failed("Spotify didn't open the door — try once more.")
                    return@launch
                }
                storeSession(payload)
                prefs.edit().remove(KEY_VERIFIER).apply()
                val name = whoAmI()
                prefs.edit().putString(KEY_NAME, name).apply()
                _state.value = SpotifyConnection.Connected(displayName = name)
            } catch (error: Exception) {
                Log.w(TAG, "Spotify handshake failed")
                _state.value = SpotifyConnection.Failed("Couldn't reach Spotify — check your connection.")
            } finally {
                exchanging = false
            }
        }
    }

    /** Forgets the reader's Spotify tokens; the connection starts over. */
    fun disconnect() {
        scope.launch {
            prefs.edit().clear().apply()
            uriCache.clear()
            _state.value = SpotifyConnection.Disconnected
        }
    }

    /**
     * Lines the whole blend up in Spotify's player: resolves each track's URI,
     * wakes the active device, and starts the lineup so Spotify's own queue
     * plays the dish end to end.
     */
    suspend fun queueAndPlay(tracks: List<Track>, startIndex: Int = 0): SpotifyQueueResult =
        try {
            pour(tracks, startIndex)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // A dropped network mid-pour reads as a failed pour, never a crash
            // that leaves the play buttons wedged.
            Log.w(TAG, "Spotify pour failed")
            SpotifyQueueResult.FAILED
        }

    private suspend fun pour(tracks: List<Track>, startIndex: Int): SpotifyQueueResult {
        val token = accessToken() ?: return SpotifyQueueResult.NEEDS_CONNECT

        val uris = mutableListOf<String>()
        var skipped = 0
        for ((index, track) in tracks.withIndex()) {
            // The blend pours from the reader's chosen starting song on —
            // earlier songs stay behind, so the dish starts where it was asked.
            if (index < startIndex) continue
            val uri = resolveUri(token, track)
            if (uri != null) uris.add(uri) else skipped++
        }
        if (uris.isEmpty()) return SpotifyQueueResult.FAILED

        val deviceId = awakeDevice(token) ?: return SpotifyQueueResult.NO_DEVICE
        val body = buildJsonObject {
            put("uris", Json.parseToJsonElement(uris.joinToString(",", "[", "]") { "\"$it\"" }))
            put("position_ms", 0)
        }
        val response = http.put("${SpotifyConfig.API_URL}/v1/me/player/play?device_id=$deviceId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        return when {
            response.status.isSuccess() -> SpotifyQueueResult.QUEUED
            response.status.value == 401 -> {
                // The access token aged out mid-pour; one refresh and retry.
                val fresh = accessToken(forceRefresh = true)
                    ?: return SpotifyQueueResult.NEEDS_CONNECT
                val retry = http.put("${SpotifyConfig.API_URL}/v1/me/player/play?device_id=$deviceId") {
                    header(HttpHeaders.Authorization, "Bearer $fresh")
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
                if (retry.status.isSuccess()) SpotifyQueueResult.QUEUED else SpotifyQueueResult.FAILED
            }
            response.status.value == 403 -> SpotifyQueueResult.NOT_PREMIUM
            response.status.value == 404 -> SpotifyQueueResult.NO_DEVICE
            else -> SpotifyQueueResult.FAILED
        }
    }

    /**
     * Pauses or resumes Spotify's own player, so the pour button's
     * pause/resume reaches the real music. Failures stay quiet — the
     * reader can always use Spotify's controls.
     */
    suspend fun setPlaying(play: Boolean) {
        val token = accessToken() ?: return
        try {
            http.put("${SpotifyConfig.API_URL}/v1/me/player/${if (play) "play" else "pause"}") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Spotify ${if (play) "resume" else "pause"} failed")
        }
    }

    /** A device Spotify is already playing on, else any awake device. */
    private suspend fun awakeDevice(token: String): String? {
        val response = http.get("${SpotifyConfig.API_URL}/v1/me/player/devices") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) return null
        val devices = Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["devices"]?.jsonArray ?: return null
        val ids = devices.mapNotNull { device ->
            val entry = device.jsonObject
            val id = entry["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val isActive = entry["is_active"]?.jsonPrimitive?.contentOrNull == "true"
            id to isActive
        }
        return ids.firstOrNull { it.second }?.first ?: ids.firstOrNull()?.first
    }

    /**
     * Matches a shelf of tracks to Spotify URIs, in pour order — the shared
     * resolution behind the Web API pour and the App Remote pour alike.
     * Returns null only when the search itself was refused.
     */
    suspend fun resolveUris(tracks: List<Track>, startIndex: Int = 0): List<String>? {
        val token = accessToken() ?: return null
        val uris = mutableListOf<String>()
        for ((index, track) in tracks.withIndex()) {
            // The blend pours from the chosen starting song on — earlier
            // songs stay behind, so the dish starts where it was asked.
            if (index < startIndex) continue
            val uri = resolveUri(token, track)
            if (uri != null) uris.add(uri)
        }
        return uris
    }

    /** Matches a shelf track to its Spotify URI by title and artist search. */
    private suspend fun resolveUri(token: String, track: Track): String? {
        val key = "${track.title}|${track.artist}"
        uriCache[key]?.let { return it }

        val query = "track:\"${track.title}\" artist:\"${track.artist}\""
        val response = http.get(
            "${SpotifyConfig.API_URL}/v1/search?type=track&limit=1&q=${Uri.encode(query)}"
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) return null
        val items = Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["tracks"]?.jsonObject?.get("items")?.jsonArray
            ?: return null
        val uri = items.firstOrNull()?.jsonObject?.get("uri")?.jsonPrimitive?.contentOrNull
        if (uri != null) uriCache[key] = uri
        return uri
    }

    /**
     * The access token, refreshed whenever it is near its end. Returns null
     * when there is no refresh token left to lean on.
     */
    private suspend fun accessToken(forceRefresh: Boolean = false): String? {
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES, 0)
        if (!forceRefresh && System.currentTimeMillis() < expiresAt) {
            return prefs.getString(KEY_ACCESS, null)
        }
        val form = "grant_type=refresh_token" +
            "&refresh_token=${Uri.encode(refresh)}" +
            "&client_id=${Uri.encode(SpotifyConfig.CLIENT_ID)}"
        return try {
            val response = http.post(SpotifyConfig.TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(form)
            }
            if (!response.status.isSuccess()) return null
            val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val access = payload["access_token"]?.jsonPrimitive?.contentOrNull ?: return null
            val expiresIn = payload["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L
            prefs.edit()
                .putString(KEY_ACCESS, access)
                .putLong(KEY_EXPIRES, System.currentTimeMillis() + (expiresIn - 60) * 1000)
                .apply()
            // Spotify occasionally issues a new refresh token with the renewal.
            payload["refresh_token"]?.jsonPrimitive?.contentOrNull?.let { fresh ->
                prefs.edit().putString(KEY_REFRESH, fresh).apply()
            }
            access
        } catch (error: Exception) {
            Log.w(TAG, "Token refresh failed")
            null
        }
    }

    /** Stores the fresh session; the refresh token is the part that matters. */
    private suspend fun storeSession(payload: JsonObject) {
        val access = payload["access_token"]?.jsonPrimitive?.contentOrNull ?: return
        val refresh = payload["refresh_token"]?.jsonPrimitive?.contentOrNull
        val expiresIn = payload["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putLong(KEY_EXPIRES, System.currentTimeMillis() + (expiresIn - 60) * 1000)
            .apply()
        if (refresh != null) prefs.edit().putString(KEY_REFRESH, refresh).apply()
    }

    /**
     * Asks Spotify whether its player is actively pouring right now. Any
     * doubt — no token, a dead network, a silent player — reads as false, so
     * the listening ledger never earns on faith.
     */
    suspend fun isPlayerActive(): Boolean {
        val token = accessToken() ?: return false
        return try {
            val response = http.get(
                "${SpotifyConfig.API_URL}/v1/me/player/currently-playing?additional_types=track"
            ) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) return false
            Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["is_playing"]?.jsonPrimitive?.booleanOrNull == true
        } catch (error: Exception) {
            Log.w(TAG, "Player check failed")
            false
        }
    }

    private suspend fun whoAmI(): String {
        val token = prefs.getString(KEY_ACCESS, null) ?: return "Spotify"
        return try {
            val response = http.get("${SpotifyConfig.API_URL}/v1/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) return "Spotify"
            Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["display_name"]?.jsonPrimitive?.contentOrNull ?: "Spotify"
        } catch (error: Exception) {
            "Spotify"
        }
    }

    private fun codeVerifier(): String = newToken(64)

    /** S256 challenge: base64url of the SHA-256 digest, padding stripped. */
    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun newToken(length: Int): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val random = SecureRandom()
        val builder = StringBuilder(length)
        repeat(length) { builder.append(alphabet[random.nextInt(alphabet.length)]) }
        return builder.toString()
    }

    companion object {
        private const val TAG = "SpotifyManager"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES = "expires_at"
        private const val KEY_NAME = "display_name"
        private const val KEY_VERIFIER = "pkce_verifier"
        private const val KEY_PENDING_STATE = "pkce_state"

        @Volatile
        private var instance: SpotifyManager? = null

        fun get(context: Context): SpotifyManager =
            instance ?: synchronized(this) {
                instance ?: SpotifyManager(context.applicationContext).also { instance = it }
            }
    }
}
