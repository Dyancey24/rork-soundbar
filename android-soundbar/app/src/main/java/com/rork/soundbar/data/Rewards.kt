package com.rork.soundbar.data

import java.util.Calendar

/**
 * The rewards ledger's arithmetic: songs and albums pay points, and genre
 * badges climb ten levels each — 1000 credited songs from level 1 to level
 * 10 — with early levels coming quickly and the deep levels saved for the
 * devoted. A badge's boost grows with its listening progress up to x25.0
 * (2500%) when maxed, every earned badge's bonus stacks on a x1.5 floor,
 * and the founder badge is scaled to match at x50.0.
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

    /** The total boost a genre badge offers at level 10 — 2500%. */
    const val GENRE_BADGE_MAX_BONUS = 25.0

    /** The founder badge's boost, scaled to sit above the genre maximum. */
    const val FOUNDER_BADGE_BONUS = 50.0

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
     * The total boost one badge at [level] contributes, on its way to
     * [GENRE_BADGE_MAX_BONUS] at level 10. It tracks the level thresholds, so
     * the quick early levels pay small steps and the stretched levels past 5
     * pay the large ones: Lv 5 is worth +2.5, Lv 6 +5.0, and Lv 10 the full
     * +25.0. Bonuses of earned badges stack.
     */
    fun badgeBonus(level: Int): Double =
        GENRE_BADGE_MAX_BONUS * songsForLevel(level) / MAX_LEVEL_SONGS

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
     * The payout multiplier in force: the x1.5 floor, every earned badge's
     * bonus (up to x25 each), the ordinary event badges' flat bonuses, and the
     * founder badge's x50 — the whole stack, uncapped by design.
     */
    fun multiplier(
        earnedBadges: Set<String>,
        songsByGenre: Map<String, Int>,
        eventBadges: Set<String>
    ): Double {
        val founderBonus = if (eventBadges.contains(FOUNDER_BADGE)) FOUNDER_BADGE_BONUS else 0.0
        val eventBonus = eventBadges.count { it != FOUNDER_BADGE } * EVENT_BADGE_BONUS
        return MIN_MULTIPLIER + totalBonus(earnedBadges, songsByGenre) + eventBonus + founderBonus
    }

    /** A base payout run through [multiplier], rounded to whole points. */
    fun payout(basePoints: Int, multiplier: Double): Long = Math.round(basePoints * multiplier)

    /** True while the founder badge can still be earned. */
    fun isFounderWindowOpen(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis <= founderDeadlineMillis
}
