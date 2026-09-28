package com.rork.soundbar.data

/**
 * Rork Auth's public client values — provisioned by Rork and safe to embed.
 * The callback scheme doubles as the manifest deep link the browser returns to.
 */
object AuthConfig {
    const val APP_KEY = "rpk_jlhna1b2zsp5zlwkx0o86hilh0xng47z"
    const val AUTH_URL = "https://api.rork.com"
    const val CALLBACK_SCHEME = "rork-wm7dwkmg7ky73dvhk1cpw"
    const val CALLBACK_HOST = "auth"
    const val CALLBACK_PATH = "/callback"

    /** The cloud sync backend (Cloudflare Worker behind EXPO_PUBLIC_RORK_FUNCTIONS_URL). */
    const val FUNCTIONS_URL = "https://sound-bites-backend.rork.app"
}
