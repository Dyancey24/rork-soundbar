package com.rork.soundbar.data

/**
 * Spotify Web API client values. The client ID is public by design — the app
 * is a PKCE public client, so no secret ever ships. Scopes cover exactly what
 * queuing a blend needs: see the player and line tracks up in it.
 */
object SpotifyConfig {
    const val CLIENT_ID = ""
    const val REDIRECT_SCHEME = "com.rork.soundbar"
    const val REDIRECT_HOST = "spotify"
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST/callback"

    const val ACCOUNTS_URL = "https://accounts.spotify.com"
    const val API_URL = "https://api.spotify.com"
    const val AUTHORIZE_URL = "$ACCOUNTS_URL/authorize"
    const val TOKEN_URL = "$ACCOUNTS_URL/api/token"

    const val SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing"

    val isConfigured: Boolean get() = CLIENT_ID.isNotBlank()
}
