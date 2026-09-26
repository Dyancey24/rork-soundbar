package com.rork.soundbar.data

import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * A finishing touch dropped onto a served blend: a splash of a new genre, a
 * twist of one more artist, or a single extra song. Garnishes live on the blend
 * itself, so they ride along to the shelf, the cookbook, and every replay.
 */
@Serializable
data class Garnish(
    val kind: String,
    val genreId: String,
    val track: Track? = null
) {
    companion object {
        const val GENRE = "genre"
        const val ARTIST = "artist"
        const val SONG = "song"
    }
}

/**
 * The garnish tray the house offers once a drink is poured. Suggestions are
 * drawn deterministically from the blend's pairings — the same recipe always
 * gets the same tray — and empty out as garnishes are used up.
 */
object GarnishBar {

    /**
     * Two genres, two artists, and two songs the blend doesn't have yet.
     * Genres lean on the base's curated pairings first, then the wider shelf.
     */
    fun options(blend: Blend): List<Garnish> {
        val random = Random(blend.id.hashCode().absoluteValue)
        val spentGenres = inBlendGenres(blend) + blend.garnishes.map { it.genreId }.toSet()
        val spentTitles = blend.tracks.map { it.title }.toSet() +
            blend.garnishes.mapNotNull { it.track?.title }.toSet()
        val spentArtists = blend.tracks.map { it.artist.lowercase() }.toSet()

        val candidateGenres = (GenreCatalog.find(blend.baseGenreId).pairings +
            GenreCatalog.mixers.map { it.id })
            .distinct()
            .filter { it !in spentGenres }

        val trackPool = candidateGenres
            .flatMap { TrackLibrary.forGenre(it) }
            .filter { it.title !in spentTitles }
            .distinctBy { it.title }

        val genreOptions = candidateGenres.shuffled(random).take(2)
            .map { Garnish(Garnish.GENRE, it) }

        val artistOptions = trackPool
            .filter { it.artist.lowercase() !in spentArtists }
            .distinctBy { it.artist.lowercase() }
            .shuffled(random).take(2)
            .map { Garnish(Garnish.ARTIST, it.genreId, it) }
        val artistTitles = artistOptions.mapNotNull { it.track?.title }.toSet()

        val songOptions = trackPool
            .filter { it.title !in artistTitles }
            .shuffled(random).take(2)
            .map { Garnish(Garnish.SONG, it.genreId, it) }

        return genreOptions + artistOptions + songOptions
    }

    /** Drops the garnish onto the blend; new tracks land at the end of the list. */
    fun apply(blend: Blend, garnish: Garnish): Blend {
        val added = when (garnish.kind) {
            Garnish.GENRE -> TrackLibrary.forGenre(garnish.genreId)
                .filter { track -> blend.tracks.none { it.title == track.title } }
                .distinctBy { it.title }
                .shuffled(Random((blend.id + garnish.genreId).hashCode().absoluteValue))
                .take(2)
            else -> listOfNotNull(garnish.track)
        }
        if (added.isEmpty()) return blend
        return blend.copy(tracks = blend.tracks + added, garnishes = blend.garnishes + garnish)
    }

    private fun inBlendGenres(blend: Blend): Set<String> = blend.genreIds.toSet()
}
