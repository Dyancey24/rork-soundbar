package com.rork.soundbar.data

/**
 * The house's paperwork, published at the backend's web address — the privacy
 * policy and house rules a reader can also find from the store listing. The
 * deletion page is here too, for the shelf of links in one place.
 */
object LegalLinks {
    private const val BASE = "https://sound-bites-backend.rork.app"
    const val PRIVACY_URL = "$BASE/privacy"
    const val TERMS_URL = "$BASE/terms"
    const val DELETION_URL = "$BASE/account-deletion"
}
