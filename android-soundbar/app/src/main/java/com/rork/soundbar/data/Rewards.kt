package com.rork.soundbar.data

import java.util.Calendar

/**
 * The rewards ledger's arithmetic: songs and albums pay points, and genre
 * badges climb ten levels each — 1000 credited songs to max a badge — with
 * early levels coming quickly and the deep levels saved for the devoted.
 * Every level lifts the payout multiplier, the steps growing past level 5.
 * The boost is floored at x1.5 and capped at x25.0; holding the founder badge
 * raises the ceiling to x50.0.
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

    /** Credited songs of one genre it takes to max a badge. */
    const val MAX_LEVEL_SONGS = 1000

    /**
     * Credited songs of one genre required to stand at each level, indexed by
     * level: quick gains early (a level-up every few songs), then real stretch
     * after level 5 so the last levels stay worth chasing. Level 10 = 1000.
     */
    private val LEVEL_SONGS = intArrayOf(0, 1, 10, 25, 50, 100, 200, 350, 550, 750, 1000)

    /** The multiplier floor — the smallest boost the board ever offers. */
    const val MIN_MULTIPLIER = 1.5

    /** The genre-board cap — no collection, however full, boosts past this. */
    const val MAX_MULTIPLIER = 25.0

    /** The multiplier step each badge level from 2 to 5 adds. */
    const val EARLY_LEVEL_STEP = 0.02

    /** The larger multiplier step each badge level past 5 adds. */
    const val LATE_LEVEL_STEP = 0.16

    /** The flat bonus the founder badge adds, on top of the genre board. */
    const val FOUNDER_BADGE_BONUS = 25.0

    /** While the founder badge is held, the payout ceiling rises to this. */
    const val FOUNDER_MAX_MULTIPLIER = 50.0

    /** The flat bonus every ordinary event badge pays, on top of the genre board. */
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
    fun songsForLevel(level: Int): Int =
        LEVEL_SONGS[level.coerceIn(0, MAX_LEVEL)]

    /** The badge level a genre sits at after [songs] credited songs of it. */
    fun level(songs: Int): Int {
        if (songs <= 0) return 0
        var current = MAX_LEVEL
        while (current > 1 && songs < songsForLevel(current)) current--
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
        val start = songsForLevel(current)
        val span = songsForLevel(current + 1) - start
        return ((songs - start).toFloat() / span).coerceIn(0f, 1f)
    }

    /**
     * The multiplier one badge at [level] contributes: small steps through the
     * early levels, then the larger late steps past level 5. A maxed badge is
     * worth +0.88.
     */
    fun badgeBonus(level: Int): Double {
        if (level <= 1) return 0.0
        val early = minOf(level, 5) - 1
        val late = (level - 5).coerceIn(0, MAX_LEVEL - 5)
        return early * EARLY_LEVEL_STEP + late * LATE_LEVEL_STEP
    }

    /** The summed bonus of every earned badge at its current level. */
    fun totalBonus(earnedBadges: Set<String>, songsByGenre: Map<String, Int>): Double =
        earnedBadges.sumOf { badge -> badgeBonus(maxOf(1, level(songsByGenre[badge] ?: 0))) }

    /**
     * Total genre-badge levels in force: every earned badge counts as at least
     * level one, and further levels come from the credited songs of its genre.
     */
    fun totalLevels(earnedBadges: Set<String>, songsByGenre: Map<String, Int>): Int =
        earnedBadges.sumOf { badge -> maxOf(1, level(songsByGenre[badge] ?: 0)) }

    /**
     * The payout multiplier in force: the badge board's summed bonus, the
     * ordinary event badges' flat bonuses, and the founder badge's large bonus
     * — which also lifts the ceiling, to x50 while it is held.
     */
    fun multiplier(
        earnedBadges: Set<String>,
        songsByGenre: Map<String, Int>,
        eventBadges: Set<String>
    ): Double {
        val founderHeld = eventBadges.contains(FOUNDER_BADGE)
        val eventBonus = eventBadges.count { it != FOUNDER_BADGE } * EVENT_BADGE_BONUS
        val founderBonus = if (founderHeld) FOUNDER_BADGE_BONUS else 0.0
        val cap = if (founderHeld) FOUNDER_MAX_MULTIPLIER else MAX_MULTIPLIER
        return minOf(cap, MIN_MULTIPLIER + totalBonus(earnedBadges, songsByGenre) + eventBonus + founderBonus)
    }

    /** A base payout run through [multiplier], rounded to whole points. */
    fun payout(basePoints: Int, multiplier: Double): Long = Math.round(basePoints * multiplier)

    /** True while the founder badge can still be earned. */
    fun isFounderWindowOpen(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis <= founderDeadlineMillis
}
