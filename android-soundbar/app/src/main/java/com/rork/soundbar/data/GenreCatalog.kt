package com.rork.soundbar.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Nightlife
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The house's full stock of genres. Base spirits anchor a blend; the rest are
 * poured in as modifiers. Liquid colours are what the mixing glass renders, and
 * energy (1 hushed to 5 electric) powers the tray's intensity sort.
 */
object GenreCatalog {

    private val icons: Map<String, ImageVector> = mapOf(
        "jazz" to Icons.Outlined.Piano,
        "house" to Icons.Outlined.Public,
        "hiphop" to Icons.Outlined.Speaker,
        "country" to Icons.Outlined.Landscape,
        "metal" to Icons.Outlined.Bolt,
        "soul" to Icons.Outlined.Flare,
        "funk" to Icons.Outlined.Album,
        "classical" to Icons.Outlined.MusicNote,
        "dreampop" to Icons.Outlined.FilterDrama,
        "bossanova" to Icons.Outlined.BeachAccess,
        "triphop" to Icons.Outlined.Headphones,
        "gospel" to Icons.Outlined.Church,
        "ambient" to Icons.Outlined.NightsStay,
        "shoegaze" to Icons.Outlined.BlurOn,
        "postpunk" to Icons.Outlined.GraphicEq,
        "disco" to Icons.Outlined.Diamond,
        "lofi" to Icons.Outlined.LocalCafe,
        "indiefolk" to Icons.Outlined.Forest,
        "rnb" to Icons.Outlined.Spa,
        "afrobeat" to Icons.Outlined.WbSunny,
        "synthwave" to Icons.Outlined.Nightlife,
        "reggae" to Icons.Outlined.Waves,
        "dnb" to Icons.Outlined.LocalFireDepartment,
        "psychrock" to Icons.Outlined.WbTwilight,
        "neosoul" to Icons.Outlined.Radio,
        "garage" to Icons.Outlined.Terrain,
        "bluegrass" to Icons.Outlined.Grass,
        "coldwave" to Icons.Outlined.AcUnit
    )

    val all: List<Genre> = listOf(
        Genre("jazz", "Jazz", "Brassy · Loose · Late", Color(0xFF3A1F14), true, listOf("bossanova", "triphop", "gospel", "ambient", "neosoul", "lofi"), 2),
        Genre("house", "House", "Pulsing · Bright · Endless", Color(0xFF1F3A44), true, listOf("disco", "afrobeat", "synthwave", "dnb", "garage", "ambient"), 4),
        Genre("hiphop", "Hip-Hop", "Heavy · Sharp · Grounded", Color(0xFF2E2417), true, listOf("triphop", "neosoul", "lofi", "funk", "rnb", "dnb"), 4),
        Genre("country", "Country", "Dusty · Honest · Wide", Color(0xFF4A2E15), true, listOf("indiefolk", "bluegrass", "psychrock", "gospel", "ambient"), 3),
        Genre("metal", "Metal", "Hot · Dense · Relentless", Color(0xFF2B1414), true, listOf("postpunk", "shoegaze", "coldwave", "psychrock", "dnb"), 5),
        Genre("soul", "Soul", "Warm · Rich · Aching", Color(0xFF5A2A16), true, listOf("gospel", "rnb", "funk", "neosoul", "bossanova"), 3),
        Genre("funk", "Funk", "Greasy · Springy · Loud", Color(0xFF6B3B12), true, listOf("disco", "afrobeat", "soul", "rnb", "garage"), 4),
        Genre("classical", "Classical", "Clear · Vast · Formal", Color(0xFF23303A), true, listOf("ambient", "psychrock", "shoegaze", "coldwave"), 1),

        Genre("dreampop", "Dream pop", "Hazy · Soft · Floating", Color(0xFF6E3A57), false, emptyList(), 1),
        Genre("bossanova", "Bossa nova", "Smooth · Warm · Laid-back", Color(0xFF7A5A1E), false, emptyList(), 2),
        Genre("triphop", "Trip hop", "Downtempo · Moody · Groovy", Color(0xFF3B2E4A), false, emptyList(), 1),
        Genre("gospel", "Gospel", "Uplifting · Soulful · Powerful", Color(0xFFB07A22), false, emptyList(), 3),
        Genre("ambient", "Ambient", "Atmospheric · Calm · Open", Color(0xFF2C4A4A), false, emptyList(), 1),
        Genre("shoegaze", "Shoegaze", "Blurred · Loud · Sweet", Color(0xFF4A3560), false, emptyList(), 3),
        Genre("postpunk", "Post-punk", "Angular · Cold · Tense", Color(0xFF33343F), false, emptyList(), 4),
        Genre("disco", "Disco", "Glittering · Fast · Joyful", Color(0xFFC28A24), false, emptyList(), 4),
        Genre("lofi", "Lo-fi", "Dusty · Sleepy · Close", Color(0xFF54402B), false, emptyList(), 1),
        Genre("indiefolk", "Indie folk", "Woody · Tender · Plain", Color(0xFF4C5A33), false, emptyList(), 2),
        Genre("rnb", "R&B", "Silky · Slow · Physical", Color(0xFF7B2F3F), false, emptyList(), 2),
        Genre("afrobeat", "Afrobeat", "Rolling · Sunlit · Communal", Color(0xFFB05B1E), false, emptyList(), 4),
        Genre("synthwave", "Synthwave", "Neon · Chrome · Nocturnal", Color(0xFF7A2A6A), false, emptyList(), 3),
        Genre("reggae", "Reggae", "Tidal · Easy · Deep", Color(0xFF2F5A2C), false, emptyList(), 2),
        Genre("dnb", "Drum & bass", "Fast · Rolling · Electric", Color(0xFF1F4A5A), false, emptyList(), 5),
        Genre("psychrock", "Psych rock", "Swirling · Fuzzy · Strange", Color(0xFF8A4A1E), false, emptyList(), 4),
        Genre("neosoul", "Neo-soul", "Plush · Jazzy · Modern", Color(0xFF6A3A2A), false, emptyList(), 2),
        Genre("garage", "Garage", "Rusty · Skittering · Raw", Color(0xFF4A4030), false, emptyList(), 4),
        Genre("bluegrass", "Bluegrass", "Bright · Nimble · Rural", Color(0xFF6A6428), false, emptyList(), 3),
        Genre("coldwave", "Coldwave", "Icy · Distant · Stark", Color(0xFF2A3A50), false, emptyList(), 3)
    )

    private val byId: Map<String, Genre> = all.associateBy { it.id }

    val bases: List<Genre> = all.filter { it.isBase }
    val mixers: List<Genre> = all.filter { !it.isBase }

    fun find(id: String): Genre = byId[id] ?: all.first()

    fun name(id: String): String = byId[id]?.name ?: id

    fun icon(id: String): ImageVector = icons[id] ?: Icons.Outlined.MusicNote

    /** Suggested modifiers for a base, padded out so the pairings list never looks thin. */
    fun pairingsFor(baseId: String): List<Genre> {
        val base = find(baseId)
        val suggested = base.pairings.mapNotNull { byId[it] }.filter { !it.isBase }
        val filler = mixers.filter { it !in suggested }
        return (suggested + filler).take(8)
    }
}
