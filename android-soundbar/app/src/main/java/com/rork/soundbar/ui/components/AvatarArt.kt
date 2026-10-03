package com.rork.soundbar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.Avatar

/**
 * The house's avatar catalogue: every mark a reader can wear, drawn from the
 * icon set, and the fixed colour pairs it can be inked in. The catalogue is
 * identical on every device, so a glyph id plus a palette index is all that
 * needs to travel the pass or the cloud — no images, ever.
 */
val AvatarGlyphs: List<Triple<String, ImageVector, String>> = listOf(
    Triple("vinyl", Icons.Filled.Album, "Vinyl"),
    Triple("note", Icons.Filled.MusicNote, "Note"),
    Triple("headphones", Icons.Filled.Headphones, "Headphones"),
    Triple("cocktail", Icons.Filled.LocalBar, "Cocktail"),
    Triple("chef", Icons.Filled.Restaurant, "Chef"),
    Triple("radio", Icons.Filled.Radio, "Radio"),
    Triple("mic", Icons.Filled.Mic, "Microphone"),
    Triple("star", Icons.Filled.Star, "Star")
)

/** Background to ink pairs — fixed so both themes and every peer render alike. */
private val AvatarPalettes: List<Pair<Color, Color>> = listOf(
    Color(0xFF2E2013) to Color(0xFFF3E9DC), // espresso / cream
    Color(0xFFC9A227) to Color(0xFF2E2013), // gold / espresso
    Color(0xFFB4552D) to Color(0xFFFFF3E4), // terracotta / cream
    Color(0xFF3E5C48) to Color(0xFFF1EAD8), // bottle green / cream
    Color(0xFF31465F) to Color(0xFFE8EEF5), // midnight blue / ice
    Color(0xFF7A2E3F) to Color(0xFFF7E6D9)  // burgundy / blush
)

fun avatarGlyphIcon(glyph: String): ImageVector? =
    AvatarGlyphs.firstOrNull { it.first == glyph }?.second

fun avatarPalette(index: Int): Pair<Color, Color> =
    AvatarPalettes[index.coerceIn(0, AvatarPalettes.lastIndex)]

/**
 * Draws a reader's avatar: the mark inked on its colour disc, centred in a
 * circle. Without an avatar it falls back to the house's neutral initial or
 * person silhouette, so every rank, card, and plaque has a face.
 */
@Composable
fun AvatarArt(
    avatar: Avatar?,
    fallbackText: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    if (avatar == null) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val initial = fallbackText?.trim()?.take(1)?.uppercase()
                if (initial.isNullOrEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.55f)
                    )
                } else {
                    Text(
                        text = initial,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        val (background, ink) = avatarPalette(avatar.palette)
        Surface(
            shape = CircleShape,
            color = background,
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = avatarGlyphIcon(avatar.glyph) ?: Icons.Filled.Star,
                    contentDescription = contentDescription,
                    tint = ink,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
    }
}
