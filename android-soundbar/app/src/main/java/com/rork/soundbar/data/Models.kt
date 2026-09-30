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
 * A recipe collected from a guest nearby. Carries the playlist alone — the
 * exchange never transmits a name, account, or anything else about the sender.
 */
@Serializable
data class GuestCard(
    val blend: Blend,
    val receivedAtMillis: Long
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
    val seeded: Boolean = false,
    /** Millis stamp of the newest write; the cloud keeps the latest. */
    val updatedAt: Long = 0L
)
