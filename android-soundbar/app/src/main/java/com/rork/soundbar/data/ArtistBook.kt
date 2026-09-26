package com.rork.soundbar.data

import kotlin.math.absoluteValue

/** One name on a recipe card, tagged by how widely known the act is. */
data class RecipeArtist(val name: String, val isPopular: Boolean)

/**
 * The artist roster behind every recipe card: each genre keeps a small pool of
 * household names and a pool of underground acts. A blend's lineup is drawn
 * deterministically from its id, so the same recipe always lists the same
 * two popular artists and three deep cuts.
 */
object ArtistBook {

    private const val POPULAR_PICKS = 2
    private const val UNDERGROUND_PICKS = 3

    private val popular: Map<String, List<String>> = mapOf(
        "jazz" to listOf("Miles Davis", "John Coltrane", "Ella Fitzgerald"),
        "house" to listOf("Daft Punk", "Calvin Harris", "Fred again.."),
        "hiphop" to listOf("Kendrick Lamar", "Drake", "Travis Scott"),
        "country" to listOf("Johnny Cash", "Dolly Parton", "Chris Stapleton"),
        "metal" to listOf("Metallica", "Slipknot", "Iron Maiden"),
        "soul" to listOf("Aretha Franklin", "Marvin Gaye", "Otis Redding"),
        "funk" to listOf("James Brown", "George Clinton", "Earth, Wind & Fire"),
        "classical" to listOf("Ludwig van Beethoven", "Wolfgang Amadeus Mozart", "Johann Sebastian Bach"),
        "dreampop" to listOf("Beach House", "Cocteau Twins", "Mazzy Star"),
        "bossanova" to listOf("Tom Jobim", "Astrud Gilberto", "Stan Getz"),
        "triphop" to listOf("Massive Attack", "Portishead", "Tricky"),
        "gospel" to listOf("Kirk Franklin", "Mahalia Jackson", "Yolanda Adams"),
        "ambient" to listOf("Brian Eno", "Tycho", "Ólafur Arnalds"),
        "shoegaze" to listOf("My Bloody Valentine", "Slowdive", "Ride"),
        "postpunk" to listOf("Joy Division", "The Cure", "Siouxsie and the Banshees"),
        "disco" to listOf("Donna Summer", "Chic", "Bee Gees"),
        "lofi" to listOf("J Dilla", "Nujabes", "Clams Casino"),
        "indiefolk" to listOf("Bon Iver", "Fleet Foxes", "Sufjan Stevens"),
        "rnb" to listOf("The Weeknd", "SZA", "Frank Ocean"),
        "afrobeat" to listOf("Burna Boy", "Wizkid", "Fela Kuti"),
        "synthwave" to listOf("Kavinsky", "The Midnight", "Gunship"),
        "reggae" to listOf("Bob Marley", "Peter Tosh", "Toots and the Maytals"),
        "dnb" to listOf("Pendulum", "Chase & Status", "Sub Focus"),
        "psychrock" to listOf("Tame Impala", "Pink Floyd", "MGMT"),
        "neosoul" to listOf("Erykah Badu", "D'Angelo", "Anderson .Paak"),
        "garage" to listOf("The White Stripes", "Arctic Monkeys", "The Hives"),
        "bluegrass" to listOf("Alison Krauss", "Billy Strings", "Blue Highway"),
        "coldwave" to listOf("Molchat Doma", "Lebanon Hanover", "Boy Harsher")
    )

    private val underground: Map<String, List<String>> = mapOf(
        "jazz" to listOf("Marion Brown", "Joe Harriott", "Sam Rivers", "Gunter Hampel"),
        "house" to listOf("Omar-S", "Fred P", "Smallpeople", "Terre Thaemlitz"),
        "hiphop" to listOf("billy woods", "Roc Marciano", "Ka", "Prefuse 73"),
        "country" to listOf("Karen Dalton", "Michael Hurley", "Willis Alan Ramsey", "Jim Ford"),
        "metal" to listOf("Deathspell Omega", "Ulcerate", "Bölzer", "Mutilation Rites"),
        "soul" to listOf("Bettye Swann", "Arthur Alexander", "Lee Moses", "Syl Johnson"),
        "funk" to listOf("Betty Davis", "Mandrill", "Brass Construction", "Jimmy Castor Bunch"),
        "classical" to listOf("Nikolai Medtner", "George Enescu", "Dora Pejačević", "Florent Schmitt"),
        "dreampop" to listOf("Drop Nineteens", "His Name Is Alive", "Tamaryn", "Alcian Blue"),
        "bossanova" to listOf("Joyce Moreno", "Johnny Alf", "Pery Ribeiro", "Don Salvador"),
        "triphop" to listOf("Alpha", "Bows", "Smith & Mighty", "Monk & Canatella"),
        "gospel" to listOf("Sister Gertrude Morgan", "Marion Williams", "Bessie Jones", "Rev. Utah Smith"),
        "ambient" to listOf("Hiroshi Yoshimura", "Pauline Anna Strom", "Michiru Aoyama", "Jonny Nash"),
        "shoegaze" to listOf("Blind Mr. Jones", "Cranes", "Fleeting Joys", "Half String"),
        "postpunk" to listOf("The Sound", "The Comsat Angels", "Josef K", "Essential Logic"),
        "disco" to listOf("Patrick Cowley", "Inner Life", "Universal Robot Band", "John Davis"),
        "lofi" to listOf("FloFilz", "Swarvy", "Keifer", "Handbook"),
        "indiefolk" to listOf("Diane Cluck", "Jessica Pratt", "Hello Shark", "Jake Xerxes Fussell"),
        "rnb" to listOf("Dawn Richard", "ABRA", "KeiyaA", "Xavier Omär"),
        "afrobeat" to listOf("Ebo Taylor", "Pat Thomas", "Guy One", "Ata Kak"),
        "synthwave" to listOf("Daniel Deluxe", "Makeup and Vanity Set", "Mega Drive", "Dance With the Dead"),
        "reggae" to listOf("The Congos", "Yabby You", "Prince Far I", "I-Roy"),
        "dnb" to listOf("Source Direct", "Seba", "Paradox", "Omni Trio"),
        "psychrock" to listOf("Kikagaku Moyo", "Holy Wave", "Slift", "Moon Duo"),
        "neosoul" to listOf("Mereba", "Lady Wray", "Georgia Anne Muldrow", "Jimetta Rose"),
        "garage" to listOf("The Oblivians", "The Gories", "Billy Childish", "Demon's Claws"),
        "bluegrass" to listOf("Ola Belle Reed", "Roscoe Holcomb", "Hazel Dickens", "Fred Cockerham"),
        "coldwave" to listOf("Siekiera", "Kas Product", "Trisomie 21", "Norma Loy")
    )

    /** The five names printed on a recipe card: two popular, three underground. */
    fun recipeArtists(blend: Blend): List<RecipeArtist> {
        val seed = blend.id.hashCode().absoluteValue
        val genres = blend.genreIds.ifEmpty { listOf(blend.baseGenreId) }
        val popularNames = pick(seed, genres, POPULAR_PICKS) { popular[it].orEmpty() }
        val undergroundNames = pick(seed + 7, genres, UNDERGROUND_PICKS) { underground[it].orEmpty() }
        return popularNames.map { RecipeArtist(it, true) } +
            undergroundNames.map { RecipeArtist(it, false) }
    }

    /**
     * Walks the blend's genres round-robin, drawing the pool index from the seed
     * so each genre contributes and the lineup stays stable per blend.
     */
    private fun pick(
        seed: Int,
        genres: List<String>,
        count: Int,
        poolOf: (String) -> List<String>
    ): List<String> {
        val chosen = mutableListOf<String>()
        var attempts = 0
        while (chosen.size < count && attempts < count * 8) {
            val genre = genres[attempts % genres.size]
            val pool = poolOf(genre)
            if (pool.isNotEmpty()) {
                val name = pool[(seed + attempts) % pool.size]
                if (chosen.none { it.equals(name, ignoreCase = true) }) chosen += name
            }
            attempts++
        }
        return chosen
    }
}
