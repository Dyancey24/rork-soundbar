package com.rork.soundbar.data

import java.util.Calendar

/**
 * The rewards ledger's arithmetic: songs and albums pay points, and genre
 * badges climb ten levels each, every level lifting the payout multiplier a
 * step. The boost is floored at x1.5 and hard-capped at x5.0 so the large
 * badge board stays balanced; limited event badges pay a flat bonus on top.
 */
object Rewards {

    /** Points for hearing one song past the credit line. */
    const val SONG_POINTS = 10

    /** Points for hearing every song on one album. */
    const val ALBUM_POINTS = 25

    /** A song only pays once it has been heard this far, so skipping past never farms points. */
    const val SONG_CREDIT_SECONDS = 30

    /** Genre badges climb to this level and no further. */
    const val MAX_LEVEL = 10

    /** Credited songs of a genre needed for the first level-up. */
    const val SONGS_PER_LEVEL = 5

    /**
     * Every level-up asks this many songs more than the one before, so a
     * badge's early levels come quickly and its last levels are earned.
     */
    const val LEVEL_GROWTH = 1

    /** The multiplier floor — the smallest boost the board ever offers. */
    const val MIN_MULTIPLIER = 1.5

    /** The multiplier cap — no collection, however full, boosts past this. */
    const val MAX_MULTIPLIER = 5.0

    /** The multiplier step each genre-badge level adds. */
    const val LEVEL_STEP = 0.0125

    /** The flat bonus every event badge pays, on top of the genre board. */
    const val EVENT_BADGE_BONUS = 0.25

    /** The one event badge live at launch — for early adopters only. */
    const val FOUNDER_BADGE = "founder"

    /** The house opened its doors on this day; the founder window runs three months. */
    private val LAUNCH_MILLIS: Long = Calendar.getInstance().run {
        clear()
        set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0)
        timeInMillis
    }

    /** Last moment the founder badge can still be earned — three months after launch. */
    val founderDeadlineMillis: Long = LAUNCH_MILLIS + 92L * 86_400_000L

    /** Credited songs of a genre required to stand at [level] (level 1 costs only its first song). */
    fun songsForLevel(level: Int): Int {
        if (level <= 1) return 0
        val steps = minOf(level, MAX_LEVEL) - 1
        return steps * SONGS_PER_LEVEL + LEVEL_GROWTH * steps * (steps - 1) / 2
    }

    /** The badge level a genre sits at after [songs] credited songs of it. */
    fun level(songs: Int): Int {
        if (songs <= 0) return 0
        var current = 1
        while (current < MAX_LEVEL && songs >= songsForLevel(current + 1)) current++
        return current
    }

    /**
     * How full a badge's progress ring is, 0..1, on its way to the next level.
     * A locked badge sits empty; a maxed badge reads as a complete circle.
     */
    fun levelProgress(songs: Int): Float {
        val current = level(songs)
        if (current <= 0) return 0f
        if (current >= MAX_LEVEL) return 1f
        val span = songsForLevel(current + 1) - songsForLevel(current)
        val intoLevel = songs - songsForLevel(current)
        return (intoLevel.toFloat() / span).coerceIn(0f, 1f)
    }

    /**
     * Total genre-badge levels in force: every earned badge counts as at least
     * level one, and further levels come from the credited songs of its genre.
     */
    fun totalLevels(earnedBadges: Set<String>, songsByGenre: Map<String, Int>): Int =
        earnedBadges.sumOf { badge -> maxOf(1, level(songsByGenre[badge] ?: 0)) }

    /** The payout multiplier for [genreLevels] badge levels and [eventBonus] event bonuses. */
    fun multiplier(genreLevels: Int, eventBonus: Double = 0.0): Double =
        minOf(MAX_MULTIPLIER, MIN_MULTIPLIER + genreLevels * LEVEL_STEP + eventBonus)

    /** A base payout run through [multiplier], rounded to whole points. */
    fun payout(basePoints: Int, multiplier: Double): Long = Math.round(basePoints * multiplier)

    /** True while the founder badge can still be earned. */
    fun isFounderWindowOpen(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis <= founderDeadlineMillis
}
