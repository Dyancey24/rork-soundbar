package com.rork.soundbar.data

/**
 * Public client values — safe to embed. Sessions are issued by Supabase Auth
 * (email + password, or Google/Apple OAuth over PKCE); the callback scheme
 * doubles as the manifest deep link the browser returns to.
 */
object AuthConfig {
    const val SUPABASE_URL = "https://lqttmgwoslgksvenpjjl.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_1BrAevm9i1dNsh2HpxuhIw_03y6Llct"
    const val CALLBACK_SCHEME = "rork-wm7dwkmg7ky73dvhk1cpw"
    const val CALLBACK_HOST = "auth"
    const val CALLBACK_PATH = "/callback"

    /** The cloud sync backend (Cloudflare Worker behind EXPO_PUBLIC_RORK_FUNCTIONS_URL). */
    const val FUNCTIONS_URL = "https://sound-bites-backend.rork.app"
}
