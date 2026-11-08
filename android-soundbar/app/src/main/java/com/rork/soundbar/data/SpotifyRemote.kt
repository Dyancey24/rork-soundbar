package com.rork.soundbar.data

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** The song Spotify's own player is pouring right now, seen through App Remote. */
data class LiveTrack(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val positionMs: Long,
    val isPlaying: Boolean,
    /** When Spotify reported this, so the needle can keep moving between reports. */
    val readAtMillis: Long = System.currentTimeMillis()
) {
    /** Position now, with the clock carrying the needle between Spotify's reports. */
    fun positionNowMs(): Long {
        if (!isPlaying) return positionMs
        return (positionMs + (System.currentTimeMillis() - readAtMillis)).coerceAtMost(durationMs)
    }
}

/**
 * The quiet conductor's door into the Spotify app on the same phone. While it
 * is bound, Soundbar starts the real music, pauses it, skips it, and watches
 * what is actually playing — and the reader never leaves the house. Spotify's
 * app does the streaming; Soundbar simply conducts.
 *
 * Nothing here touches the Web API account: App Remote asks the installed
 * Spotify app directly (app-control only), so it works alongside the PKCE
 * session, not through it.
 */
class SpotifyRemote private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _live = MutableStateFlow<LiveTrack?>(null)
    val live: StateFlow<LiveTrack?> = _live.asStateFlow()

    @Volatile
    private var remote: SpotifyAppRemote? = null
    private val connecting = AtomicBoolean(false)

    /** Callers waiting on the next successful binding. */
    private val waiting = ArrayList<() -> Unit>()
    private val failing = ArrayList<() -> Unit>()

    val isConnected: Boolean get() = remote?.isConnected == true

    /** True when the Spotify app itself lives on this phone. */
    fun isSpotifyInstalled(): Boolean = SpotifyAppRemote.isSpotifyInstalled(appContext)

    /**
     * Knocks on Spotify's door if nobody has yet; the moment it opens — or if
     * it was open all along — [onReady] runs. When the door never opens (no
     * Spotify app, an old version, a refusal), [onError] runs instead and the
     * caller's Web API pour carries the music.
     */
    fun whenConnected(onReady: () -> Unit, onError: () -> Unit = {}) {
        val existing = remote
        if (existing?.isConnected == true) {
            onReady()
            return
        }
        if (!isSpotifyInstalled()) {
            onError()
            return
        }
        synchronized(waiting) { waiting.add(onReady) }
        synchronized(failing) { failing.add(onError) }
        if (connecting.compareAndSet(false, true)) {
            val params = ConnectionParams.Builder(SpotifyConfig.CLIENT_ID)
                .setRedirectUri(SpotifyConfig.REDIRECT_URI)
                .build()
            SpotifyAppRemote.connect(appContext, params, object : Connector.ConnectionListener {
                override fun onConnected(connected: SpotifyAppRemote) {
                    remote = connected
                    connecting.set(false)
                    watchPlayer(connected)
                    flush(waiting)
                }

                override fun onFailure(error: Throwable) {
                    Log.w(TAG, "App Remote connection failed: ${error.message}")
                    connecting.set(false)
                    remote = null
                    flush(failing)
                }
            })
        }
    }

    private fun flush(queue: ArrayList<() -> Unit>) {
        val toRun = synchronized(queue) {
            val list = queue.toList()
            queue.clear()
            list
        }
        toRun.forEach { it() }
    }

    /** Streams the player state into [live], and keeps the needle honest between reports. */
    private fun watchPlayer(appRemote: SpotifyAppRemote) {
        appRemote.playerApi.subscribeToPlayerState().apply {
            setEventCallback { state ->
                _live.value = state.toLive()
                scope.launch {
                    // Spotify reports only on changes; a one-second heartbeat
                    // walks the progress line until the next report arrives.
                    while (isActive && remote?.isConnected == true) {
                        delay(1000)
                        val current = _live.value ?: break
                        if (current.isPlaying) {
                            _live.value = current.copy(positionMs = current.positionNowMs())
                        }
                    }
                }
            }
            setErrorCallback { error ->
                Log.w(TAG, "Player watch ended: ${error?.message}")
                remote = null
                _live.value = null
            }
        }
    }

    private fun PlayerState.toLive(): LiveTrack? {
        val track = track ?: return null
        return LiveTrack(
            title = track.name.orEmpty(),
            artist = track.artist?.name ?: track.artists?.firstOrNull()?.name.orEmpty(),
            artworkUrl = track.imageUri?.raw?.let(::artworkUrl),
            durationMs = track.duration,
            positionMs = playbackPosition,
            isPlaying = !isPaused
        )
    }

    /** Spotify image handles read "spotify:image:…" — the web wants the CDN host. */
    private fun artworkUrl(raw: String): String {
        if (raw.startsWith("http")) return raw
        return if (raw.startsWith("spotify:image:")) {
            "https://i.scdn.co/image/${raw.substringAfterLast(':')}"
        } else {
            raw
        }
    }

    // region commands

    /**
     * Starts the blend on this phone: the first URI plays at once, the rest
     * line up behind it so Spotify's own queue carries the dish end to end.
     * [onDone] says whether the first song actually left the ground.
     */
    fun play(uris: List<String>, onDone: (Boolean) -> Unit) {
        val playerApi = remote?.playerApi
        val first = uris.firstOrNull()
        if (playerApi == null || first == null) {
            onDone(false)
            return
        }
        playerApi.play(first).setResultCallback({
            uris.drop(1).forEach { playerApi.queue(it) }
            onDone(true)
        }).setErrorCallback {
            onDone(false)
        }
    }

    fun resume() {
        remote?.playerApi?.resume()
    }

    fun pause() {
        remote?.playerApi?.pause()
    }

    fun skipNext() {
        remote?.playerApi?.skipNext()
    }

    fun skipPrevious() {
        remote?.playerApi?.skipPrevious()
    }

    // endregion

    companion object {
        private const val TAG = "SpotifyRemote"

        @Volatile
        private var instance: SpotifyRemote? = null

        fun get(context: Context): SpotifyRemote =
            instance ?: synchronized(this) {
                instance ?: SpotifyRemote(context.applicationContext).also { instance = it }
            }
    }
}
