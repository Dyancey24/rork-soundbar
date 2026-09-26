package com.rork.soundbar.data

import com.rork.soundbar.ui.theme.Concept
import kotlin.math.absoluteValue

/**
 * The house reads a drinker's taste and crafts their signature blend: every
 * saved playlist and every play is weighted, the top base spirit becomes the
 * backbone, and the two strongest modifiers get poured in. The recipe is fully
 * deterministic — the same taste always serves the same dish — and recrafts
 * itself whenever the collection changes.
 */
object SignatureCraft {

    /** Stable id so notes, play counts, and history follow the recipe as it evolves. */
    const val ID = "signature"

    private val barNames = listOf(
        "The Regular", "The House Pour", "Last Call", "Your Usual", "The Nightly"
    )

    private val kitchenNames = listOf(
        "The Daily Special", "House Special", "The Standing Order", "Chef's Tasting", "Today's Plate"
    )

    /**
     * Weighs the shelf by how often each blend was played (plays + presence,
     * bases counting double), then pours the top base and its two strongest
     * partners into one recipe. Returns null when there is nothing to taste yet.
     */
    fun craft(shelf: List<Blend>, playCounts: Map<String, Int>, concept: Concept): Blend? {
        if (shelf.isEmpty()) return null

        val weights = mutableMapOf<String, Int>()
        shelf.forEach { blend ->
            val weight = (playCounts[blend.id] ?: 0) + 1
            weights[blend.baseGenreId] = (weights[blend.baseGenreId] ?: 0) + weight * 2
            blend.ingredients.forEach { ingredient ->
                weights[ingredient.genreId] = (weights[ingredient.genreId] ?: 0) + weight
            }
        }
        val ranked = weights.entries.sortedByDescending { it.value }

        val baseIds = GenreCatalog.bases.map { it.id }.toSet()
        val base = ranked.firstOrNull { baseIds.contains(it.key) }?.key ?: return null
        val mixers = ranked.filter { it.key != base && !baseIds.contains(it.key) }.take(2)
        val ingredients = mixers.mapIndexed { index, entry ->
            Ingredient(entry.key, if (index == 0) 2 else 1)
        }

        val kitchen = concept == Concept.KITCHEN
        val names = if (kitchen) kitchenNames else barNames
        val verb = if (kitchen) "cooked" else "poured"
        val seed = (base + ingredients.joinToString("|") { it.genreId }).hashCode().absoluteValue
        val flavorNote = mixers.firstOrNull()?.key
            ?.let { " — heavy on the ${GenreCatalog.name(it).lowercase()}" }
            .orEmpty()
        val plural = if (shelf.size == 1) "" else "s"

        return BlendBar.mix(base, ingredients, ID, concept).copy(
            name = names[seed % names.size],
            tagline = "Your taste, $verb from ${shelf.size} saved blend$plural$flavorNote."
        )
    }
}
