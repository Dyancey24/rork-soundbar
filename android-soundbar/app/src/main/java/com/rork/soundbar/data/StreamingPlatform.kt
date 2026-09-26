package com.rork.soundbar.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * The streaming house the user's playlists open in. A play press hands the
 * blend to the chosen app; when it is not installed, the same search opens
 * in the browser instead.
 */
enum class StreamingPlatform(
    val id: String,
    val displayName: String,
    private val appPackage: String?
) {
    SPOTIFY("spotify", "Spotify", "com.spotify.music"),
    APPLE_MUSIC("apple", "Apple Music", "com.apple.android.music");

    /**
     * Opens the blend's music in this platform, searched by the track that
     * will start pouring — or by the blend's name when it has no tracks.
     */
    fun launch(context: Context, blend: Blend, trackIndex: Int = 0) {
        val track = blend.tracks.getOrNull(trackIndex)
        val query = track?.let { "${it.artist} ${it.title}" } ?: blend.name
        val encoded = Uri.encode(query)

        val appIntent = when (this) {
            SPOTIFY -> Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$encoded"))
            APPLE_MUSIC -> Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL + encoded)).setPackage(appPackage)
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL + encoded))
        val intent = if (canStart(context, appIntent)) appIntent else webIntent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            // The app did not answer after all; fall back to the plain web link.
            try {
                context.startActivity(webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (inner: Exception) {
                Log.w(TAG, "No streaming platform could open the blend")
            }
        }
    }

    private fun canStart(context: Context, intent: Intent): Boolean =
        try {
            intent.resolveActivity(context.packageManager) != null
        } catch (error: Exception) {
            false
        }

    companion object {
        private const val TAG = "StreamingPlatform"
        private const val WEB_URL = "https://music.apple.com/us/search?term="

        fun fromId(id: String): StreamingPlatform =
            entries.firstOrNull { it.id == id } ?: SPOTIFY
    }
}
