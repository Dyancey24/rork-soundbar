package com.rork.soundbar.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** A music genre, treated throughout the app as a drinkable ingredient. */
data class Genre(
    val id: String,
    val name: String,
    val flavor: String,
    val liquid: Color,
    val isBase: Boolean,
    val pairings: List<String> = emptyList(),
    /** 1 hushed to 5 electric — powers the tray's intensity sort. */
    val energy: Int = 3
)

/** How much of a genre goes into a blend. Parts drive both copy and the glass layers. */
@Serializable
data class Ingredient(
    val genreId: String,
    val parts: Int
)

@Serializable
data class Track(
    val title: String,
    val artist: String,
    val seconds: Int,
    val genreId: String
) {
    val duration: String
        get() = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Serializable
data class Blend(
    val id: String,
    val name: String,
    val tagline: String,
    val baseGenreId: String,
    val ingredients: List<Ingredient>,
    val imageUrl: String,
    val tracks: List<Track>,
    val garnishes: List<Garnish> = emptyList()
) {
    val totalSeconds: Int get() = tracks.sumOf { it.seconds }

    val runtimeLabel: String
        get() {
            val minutes = totalSeconds / 60
            return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
        }

    val servingLabel: String get() = "${tracks.size} tracks · $runtimeLabel"

    /** Every genre in the blend, base first — used for chips and taste stats. */
    val genreIds: List<String> get() = listOf(baseGenreId) + ingredients.map { it.genreId }
}

/**
 * A recipe collected from a guest nearby. The playlist always crosses the air;
 * the sender's name and mark only come along when that guest has opted into a
 * public profile — otherwise the card stays anonymous.
 */
@Serializable
data class GuestCard(
    val blend: Blend,
    val receivedAtMillis: Long,
    val senderName: String? = null,
    val senderAvatar: Avatar? = null
)

/**
 * The reader's avatar art: one of the house's preset marks drawn in one of the
 * fixed colour pairs. Both sides of the exchange know the same catalogue, so a
 * glyph id plus a palette index is all that ever needs to travel or persist.
 */
@Serializable
data class Avatar(
    val glyph: String,
    val palette: Int
) {
    /** Compact wire form, e.g. "vinyl:3" — what the cloud leaderboard stores. */
    val code: String get() = "$glyph:$palette"

    companion object {
        fun fromCode(code: String?): Avatar? {
            if (code == null) return null
            val parts = code.split(":")
            val palette = parts.getOrNull(1)?.toIntOrNull() ?: return null
            val glyph = parts.getOrNull(0) ?: return null
            return Avatar(glyph, palette)
        }
    }
}

/**
 * The reader's public-facing self: a chosen username and avatar mark, plus the
 * opt-in switch. Everything here stays on the device unless [Profile.isPublic]
 * is on — only then do the name and mark ride the walk-by playlist and the
 * friends leaderboard.
 */
@Serializable
data class Profile(
    val username: String = "",
    val avatar: Avatar? = null,
    val isPublic: Boolean = false
)

/** Persisted shelf state so a user's collection survives app restarts. */
@Serializable
data class ShelfState(
    val blends: List<Blend> = emptyList(),
    val playCounts: Map<String, Int> = emptyMap(),
    val listenedSeconds: Long = 0L,
    val mixDays: List<Long> = emptyList(),
    val concept: String = "bar",
    val notes: Map<String, String> = emptyMap(),
    val lastPlayed: Map<String, Long> = emptyMap(),
    val signatureId: String? = null,
    val signatureBlend: Blend? = null,
    val guestRecipes: List<GuestCard> = emptyList(),
    val sharingEnabled: Boolean = false,
    /** The streaming house play presses open; a StreamingPlatform id. */
    val platform: String = "spotify",
    /** Rewards ledger: points earned, songs heard, albums finished, genre badges minted. */
    val points: Long = 0L,
    val listenedTracks: Set<String> = emptySet(),
    val completedAlbums: Set<String> = emptySet(),
    val genreBadges: Set<String> = emptySet(),
    /** Credited songs per genre — drives each badge's level. */
    val genreSongs: Map<String, Int> = emptyMap(),
    /** Limited, exclusive event badges — ids from Rewards. */
    val eventBadges: Set<String> = emptySet(),
    /** The reader's username, avatar mark, and the public-profile opt-in. */
    val profile: Profile = Profile(),
    val seeded: Boolean = false,
    /** Millis stamp of the newest write; the cloud keeps the latest. */
    val updatedAt: Long = 0L
)
