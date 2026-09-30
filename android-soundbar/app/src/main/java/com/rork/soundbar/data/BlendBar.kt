package com.rork.soundbar.data

import com.rork.soundbar.ui.theme.Concept
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Curated house blends plus the recipe engine that turns a user's mix into a
 * served dish: a name, a tagline, artwork, and a tracklist poured in proportion.
 * Every blend exists in both house concepts — a cocktail at the bar, a plate in
 * the kitchen — with concept-specific naming, copy, and photography.
 */
object BlendBar {

    object Art {
        const val SMOKY = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/77c35f95-10be-46d3-85d6-076113b119be.png"
        const val BITTER = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/be601653-a209-4f86-bb6c-f5920a5920ce.png"
        const val TIRAMISU = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/59a94f8c-7bb8-4e9b-be08-83e22f44cb11.png"
        const val ESPRESSO = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/65357430-2153-427b-b238-df2a26fc11b6.png"
        const val MARTINI = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/6f93ecdc-d5fa-4cca-88ee-92611e2f2a80.png"
        const val CARAMEL = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/236d7c2b-8539-4e6d-a612-f053d81c9331.png"
        const val PEACH = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/0977a76f-9876-4e0f-a790-2f2386633d26.png"
        const val HOUSE_SPECIAL = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/68d6c567-fb7c-4de6-aebd-0dc8e2b1f816.png"

        const val K_PIZZA = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/b2662829-c027-4982-ba32-263c6d62b517.png"
        const val K_RADICCHIO = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/933870aa-d041-491b-a7b7-f0f9272e6723.png"
        const val K_TIRAMISU = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/5bf28121-bdff-415b-a9d3-a15dd83b6309.png"
        const val K_AFFOGATO = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/514f6ff4-7633-4d59-bc60-560d6554e653.png"
        const val K_CEVICHE = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/2ff8f8af-3676-476e-9674-390faf5b5690.png"
        const val K_TART = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/54d491b7-5ad0-4e9d-bce5-a2ad79a385e3.png"
        const val K_GALETTE = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/cdae3adf-0710-4a89-a491-d18cf84fa410.png"
        const val K_PASTA = "https://wm7dwkmg7ky73dvhk1cpw.rork.app/~assets/img/3972c8eb-9ac1-466c-8149-684687b4362a.png"

        val barPool: List<String> = listOf(SMOKY, BITTER, TIRAMISU, ESPRESSO, MARTINI, CARAMEL, PEACH, HOUSE_SPECIAL)
        val kitchenPool: List<String> = listOf(K_PIZZA, K_RADICCHIO, K_TIRAMISU, K_AFFOGATO, K_CEVICHE, K_TART, K_GALETTE, K_PASTA)

        fun pool(concept: Concept): List<String> = if (concept == Concept.KITCHEN) kitchenPool else barPool
    }

    private val serveNouns: Map<String, List<String>> = mapOf(
        "jazz" to listOf("Neapolitan", "Manhattan", "Nightcap", "Sazerac"),
        "house" to listOf("Spritz", "Highball", "Fizz", "Cooler"),
        "hiphop" to listOf("Boulevardier", "Sour", "Smash", "Old Fashioned"),
        "country" to listOf("Julep", "Cobbler", "Cider", "Toddy"),
        "metal" to listOf("Boilermaker", "Char", "Blackstrap", "Cask"),
        "soul" to listOf("Flip", "Cream", "Praline", "Velvet"),
        "funk" to listOf("Punch", "Daiquiri", "Swizzle", "Grog"),
        "classical" to listOf("Consommé", "Sorbet", "Aperitif", "Coupe")
    )

    private val dishNouns: Map<String, List<String>> = mapOf(
        "jazz" to listOf("Risotto", "Bisque", "Napoleon", "Consommé"),
        "house" to listOf("Poke", "Platter", "Bruschetta", "Bowl"),
        "hiphop" to listOf("Smash", "Slider", "Chowder", "Melt"),
        "country" to listOf("Cobbler", "Skillet", "Hash", "Stew"),
        "metal" to listOf("Char", "Skewer", "Rib", "Rind"),
        "soul" to listOf("Pot Pie", "Gumbo", "Custard", "Bread Pudding"),
        "funk" to listOf("Jambalaya", "Taco", "Fritter", "Salsa"),
        "classical" to listOf("Terrine", "Soufflé", "Velouté", "Timbale")
    )

    private val serveAdjectives: Map<String, String> = mapOf(
        "dreampop" to "Velvet",
        "bossanova" to "Sun-warmed",
        "triphop" to "Midnight",
        "gospel" to "Golden",
        "ambient" to "Slow",
        "shoegaze" to "Hazy",
        "postpunk" to "Iron",
        "disco" to "Glitter",
        "lofi" to "Dusty",
        "indiefolk" to "Orchard",
        "rnb" to "Silk",
        "afrobeat" to "Ember",
        "synthwave" to "Neon",
        "reggae" to "Tidal",
        "dnb" to "Static",
        "psychrock" to "Kaleido",
        "neosoul" to "Plush",
        "garage" to "Rust",
        "bluegrass" to "Bright",
        "coldwave" to "Frostbitten",
        "soul" to "Smoky",
        "funk" to "Bitter",
        "jazz" to "Blue",
        "house" to "Marble",
        "hiphop" to "Concrete",
        "country" to "Prairie",
        "metal" to "Scorched",
        "classical" to "Marble"
    )

    private val barTaglines: List<String> = listOf(
        "Poured slow, best taken by candlelight.",
        "Bittersweet on the first sip, warm on the last.",
        "For the hour when the room finally quiets down.",
        "Sharp edges, soft finish.",
        "Built for a long night and a short walk home.",
        "Rich, smoky, and just a little reckless.",
        "Layered deep — let it settle before you drink."
    )

    private val kitchenTaglines: List<String> = listOf(
        "Baked slow, best shared while it's still warm.",
        "Sweet on the first bite, savory on the last.",
        "For the long Sunday table that never wants to end.",
        "Sharp edges, soft center.",
        "Made for a big table and a second helping.",
        "Rich, roasted, and just a little reckless.",
        "Layered deep — let it rest before you serve."
    )

    /** Deterministic pseudo-random so the same recipe always serves the same dish. */
    private fun seedOf(baseGenreId: String, ingredients: List<Ingredient>): Int {
        val key = baseGenreId + ingredients.joinToString("|") { "${it.genreId}:${it.parts}" }
        return key.hashCode().absoluteValue
    }

    /**
     * Pours a blend from a base genre and its modifiers. Track counts follow the
     * dose each ingredient was given, so a generous pour really is more of it.
     */
    fun mix(
        baseGenreId: String,
        ingredients: List<Ingredient>,
        id: String? = null,
        concept: Concept = Concept.BAR
    ): Blend {
        val seed = seedOf(baseGenreId, ingredients)
        val random = Random(seed)
        val nouns = (if (concept == Concept.KITCHEN) dishNouns else serveNouns)[baseGenreId] ?: serveNouns.getValue("jazz")
        val noun = nouns[seed % nouns.size]
        val topIngredient = ingredients.maxByOrNull { it.parts }?.genreId
        val adjective = serveAdjectives[topIngredient] ?: serveAdjectives[baseGenreId] ?: "House"
        val name = when {
            ingredients.isEmpty() && concept == Concept.KITCHEN -> "$noun, Simple"
            ingredients.isEmpty() -> "$noun, Neat"
            else -> "$adjective $noun"
        }

        val baseParts = 4
        val totalParts = baseParts + ingredients.sumOf { it.parts }
        val servingSize = 14 + ingredients.size * 4

        val tracks = mutableListOf<Track>()
        fun pourFrom(genreId: String, parts: Int) {
            val share = (servingSize * parts / totalParts.toFloat()).toInt().coerceAtLeast(1)
            val pool = TrackLibrary.forGenre(genreId).shuffled(random)
            repeat(share) { index -> tracks += pool[index % pool.size] }
        }
        pourFrom(baseGenreId, baseParts)
        ingredients.forEach { pourFrom(it.genreId, it.parts) }

        // Lead with the base, then interleave so the blend never plays in blocks.
        val distinct = tracks.distinctBy { it.title + it.artist }
        val lead = distinct.filter { it.genreId == baseGenreId }
        val rest = distinct.filter { it.genreId != baseGenreId }.shuffled(random)
        val ordered = mutableListOf<Track>()
        val leadQueue = ArrayDeque(lead)
        val restQueue = ArrayDeque(rest)
        while (leadQueue.isNotEmpty() || restQueue.isNotEmpty()) {
            leadQueue.removeFirstOrNull()?.let { ordered += it }
            restQueue.removeFirstOrNull()?.let { ordered += it }
            restQueue.removeFirstOrNull()?.let { ordered += it }
        }

        val taglines = if (concept == Concept.KITCHEN) kitchenTaglines else barTaglines
        return Blend(
            id = id ?: "mix-${System.currentTimeMillis()}",
            name = name,
            tagline = taglines[seed % taglines.size],
            baseGenreId = baseGenreId,
            ingredients = ingredients,
            imageUrl = Art.pool(concept)[seed % Art.pool(concept).size],
            tracks = ordered
        )
    }

    /** One house recipe, defined once, served in both concepts. */
    private data class HouseSpec(
        val id: String,
        val base: String,
        val ingredients: List<Ingredient>,
        val barName: String,
        val kitchenName: String,
        val barTagline: String,
        val kitchenTagline: String,
        val barArt: String,
        val kitchenArt: String
    )

    private fun buildHouse(spec: HouseSpec, concept: Concept): Blend {
        val kitchen = concept == Concept.KITCHEN
        return mix(spec.base, spec.ingredients, spec.id, concept).copy(
            name = if (kitchen) spec.kitchenName else spec.barName,
            tagline = if (kitchen) spec.kitchenTagline else spec.barTagline,
            imageUrl = if (kitchen) spec.kitchenArt else spec.barArt
        )
    }

    private val tonightSpecs = listOf(
        HouseSpec(
            "house-smoky-neapolitan", "jazz",
            listOf(Ingredient("dreampop", 1), Ingredient("soul", 2)),
            "Smoky Neapolitan", "Smoky Margherita",
            "Poured slow, best taken by candlelight.", "Baked slow, best shared while it's still warm.",
            Art.SMOKY, Art.K_PIZZA
        ),
        HouseSpec(
            "house-bitter-sunset", "metal",
            listOf(Ingredient("postpunk", 2), Ingredient("shoegaze", 1)),
            "Bitter Sunset", "Charred Radicchio",
            "Sharp edges, soft finish.", "Sharp edges, soft center.",
            Art.BITTER, Art.K_RADICCHIO
        ),
        HouseSpec(
            "house-velvet-tiramisu", "soul",
            listOf(Ingredient("rnb", 2), Ingredient("bossanova", 1)),
            "Velvet Tiramisu", "Velvet Tiramisu",
            "Rich, layered, and unhurried.", "Rich, layered, and unhurried.",
            Art.TIRAMISU, Art.K_TIRAMISU
        )
    )

    private val classicSpecs = listOf(
        HouseSpec(
            "house-midnight-espresso", "hiphop",
            listOf(Ingredient("lofi", 2), Ingredient("triphop", 2)),
            "Midnight Espresso", "Midnight Affogato",
            "For the hour when the room finally quiets down.", "For the morning the house is still asleep.",
            Art.ESPRESSO, Art.K_AFFOGATO
        ),
        HouseSpec(
            "house-gin-tonicic", "house",
            listOf(Ingredient("disco", 2), Ingredient("funk", 1)),
            "Gin & Tonicic", "Citrus Ceviche",
            "Cold, bright, and impossible to sit still through.", "Cold, bright, and impossible to sit still through.",
            Art.MARTINI, Art.K_CEVICHE
        ),
        HouseSpec(
            "house-caramel-apple", "country",
            listOf(Ingredient("indiefolk", 2), Ingredient("ambient", 1)),
            "Caramel Apple", "Caramel Apple Tart",
            "Woody and warm, like a porch in October.", "Woody and warm, like a porch in October.",
            Art.CARAMEL, Art.K_TART
        ),
        HouseSpec(
            "house-peach-static", "funk",
            listOf(Ingredient("dnb", 1), Ingredient("garage", 2)),
            "Peach Static", "Peach Galette",
            "Sweet up front, electric underneath.", "Sweet up front, electric underneath.",
            Art.PEACH, Art.K_GALETTE
        )
    )

    private val allSpecs: List<HouseSpec> = tonightSpecs + classicSpecs

    /** Tonight's menu — the bar's own recipes, always available. */
    fun tonight(concept: Concept): List<Blend> = tonightSpecs.map { buildHouse(it, concept) }

    /** House classics — the shorter list further down the menu. */
    fun classics(concept: Concept): List<Blend> = classicSpecs.map { buildHouse(it, concept) }

    fun menu(concept: Concept): List<Blend> = tonight(concept) + classics(concept)

    fun findHouseBlend(id: String, concept: Concept): Blend? =
        allSpecs.firstOrNull { it.id == id }?.let { buildHouse(it, concept) }

    /**
     * Re-serves a stored blend in the current concept: house recipes swap their
     * name, tagline, and photograph; a user's own mix keeps the identity it was
     * mixed under.
     */
    fun adapt(blend: Blend, concept: Concept): Blend {
        val spec = allSpecs.firstOrNull { it.id == blend.id } ?: return blend
        return buildHouse(spec, concept)
    }
}
