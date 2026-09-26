package com.rork.soundbar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rork.soundbar.data.Blend
import com.rork.soundbar.data.GenreCatalog

/** House photography with the theme scrim, used everywhere artwork appears. */
@Composable
fun BlendArtwork(
    imageUrl: String,
    modifier: Modifier = Modifier,
    scrim: Boolean = true
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (scrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(photoScrim(MaterialTheme.colorScheme.background))
            )
        }
    }
}

/** The hero pour on tonight's menu. */
@Composable
fun FeaturedBlendCard(
    blend: Blend,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            BlendArtwork(
                imageUrl = blend.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
            )
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = blend.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IngredientTag("${GenreCatalog.name(blend.baseGenreId)} base", emphasized = true)
                        blend.ingredients.take(2).forEach { ingredient ->
                            IngredientTag(GenreCatalog.name(ingredient.genreId))
                        }
                    }
                    Text(
                        text = blend.servingLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PourButton(
                        onClick = onPlay,
                        filled = true,
                        isPlaying = isPlaying,
                        size = 52,
                        contentDescription = if (isPlaying) "Pause ${blend.name}" else "Play ${blend.name}"
                    )
                    Text(
                        text = if (isPlaying) "Playing" else "Play",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/** Half-width menu card for the second row of tonight's pours. */
@Composable
fun CompactBlendCard(
    blend: Blend,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpen,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            BlendArtwork(
                imageUrl = blend.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
            )
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = blend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MaterialTheme.typography.headlineSmall.fontFamily,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = blend.ingredients.joinToString(" · ") { GenreCatalog.name(it.genreId) },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                PourButton(
                    onClick = onPlay,
                    isPlaying = isPlaying,
                    size = 36,
                    contentDescription = if (isPlaying) "Pause ${blend.name}" else "Play ${blend.name}"
                )
            }
        }
    }
}

/** Menu list row for the house classics — glyph, name, genre line, pour button. */
@Composable
fun ClassicBlendRow(
    blend: Blend,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GenreCatalog.icon(blend.ingredients.firstOrNull()?.genreId ?: blend.baseGenreId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blend.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = MaterialTheme.typography.headlineSmall.fontFamily,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = blend.ingredients.joinToString(" · ") { GenreCatalog.name(it.genreId) },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PourButton(
                onClick = onPlay,
                filled = true,
                isPlaying = isPlaying,
                size = 40,
                contentDescription = if (isPlaying) "Pause ${blend.name}" else "Play ${blend.name}"
            )
        }
    }
}
