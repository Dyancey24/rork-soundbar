package com.rork.soundbar.data

/**
 * The rewards ledger's arithmetic: songs and albums pay points, and every
 * genre badge a listener earns lifts the multiplier for every payout after it.
 */
object Rewards {

    /** Points for hearing one song past the credit line. */
    const val SONG_POINTS = 10

    /** Points for hearing every song on one album. */
    const val ALBUM_POINTS = 25

    /** The bonus step each genre badge adds to the multiplier. */
    const val BADGE_BONUS = 0.10

    /** A song only pays once it has been heard this far, so skipping past never farms points. */
    const val SONG_CREDIT_SECONDS = 30

    /** The payout multiplier in force with [badges] genre badges earned. */
    fun multiplier(badges: Int): Double = 1.0 + badges * BADGE_BONUS

    /** A base payout run through the badge multiplier, rounded to whole points. */
    fun payout(basePoints: Int, badges: Int): Long = Math.round(basePoints * multiplier(badges))
}
