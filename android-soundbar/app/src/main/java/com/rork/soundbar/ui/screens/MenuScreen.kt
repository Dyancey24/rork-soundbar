package com.rork.soundbar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.Blend
import com.rork.soundbar.data.BlendBar
import com.rork.soundbar.data.StreamingPlatform
import com.rork.soundbar.ui.components.ClassicBlendRow
import com.rork.soundbar.ui.components.CompactBlendCard
import com.rork.soundbar.ui.components.ConceptToggle
import com.rork.soundbar.ui.components.Eyebrow
import com.rork.soundbar.ui.components.FeaturedBlendCard
import com.rork.soundbar.ui.components.HairlineDivider
import com.rork.soundbar.ui.components.SectionTitle
import com.rork.soundbar.ui.theme.LocalConcept
import java.util.Calendar

/** The house menu — the bar's or kitchen's own recipes, laid out like a menu card. */
@Composable
fun MenuScreen(
    playingBlendId: String?,
    isPlaying: Boolean,
    onOpenBlend: (String) -> Unit,
    onPlayBlend: (Blend) -> Unit,
    onTogglePlay: () -> Unit,
    selectedPlatform: StreamingPlatform,
    onSelectPlatform: (StreamingPlatform) -> Unit,
    onToggleConcept: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val featured = BlendBar.tonight(concept).first()
    val alsoTonight = BlendBar.tonight(concept).drop(1)

    fun poursNow(blend: Blend): Boolean = playingBlendId == blend.id && isPlaying

    fun onPourPressed(blend: Blend) {
        if (playingBlendId == blend.id) onTogglePlay() else onPlayBlend(blend)
    }

    Box(modifier = modifier.fillMaxSize()) {
        BarGlow()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item("header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Soundbar",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${greeting()} · ${concept.openLine}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    ConceptToggle(concept = concept, onToggle = onToggleConcept)
                }
            }

            item("platform") {
                PlatformRow(
                    label = concept.streamingLabel,
                    selected = selectedPlatform,
                    onSelect = onSelectPlatform,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            item("tonight-label") {
                Eyebrow(concept.tonightLabel, modifier = Modifier.padding(top = 8.dp))
            }

            item("featured") {
                FeaturedBlendCard(
                    blend = featured,
                    isPlaying = poursNow(featured),
                    onOpen = { onOpenBlend(featured.id) },
                    onPlay = { onPourPressed(featured) }
                )
            }

            item("also-tonight") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    alsoTonight.forEach { blend ->
                        CompactBlendCard(
                            blend = blend,
                            isPlaying = poursNow(blend),
                            onOpen = { onOpenBlend(blend.id) },
                            onPlay = { onPourPressed(blend) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item("classics-label") {
                SectionTitle(concept.classicsTitle, modifier = Modifier.padding(top = 10.dp))
            }

            item("classics") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        BlendBar.classics(concept).forEachIndexed { index, blend ->
                            if (index > 0) {
                                HairlineDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp))
                            }
                            ClassicBlendRow(
                                blend = blend,
                                isPlaying = poursNow(blend),
                                onOpen = { onOpenBlend(blend.id) },
                                onPlay = { onPourPressed(blend) }
                            )
                        }
                    }
                }
            }

            item("footer") {
                Text(
                    text = concept.menuFooter,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )
            }
        }
    }
}

/** Warm light bloom behind the top of every tab, for depth. */
@Composable
fun BarGlow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                Brush.radialGradient(
                    0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    1f to MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )
            )
    )
}

/** Chooses which streaming house play presses open. */
@Composable
private fun PlatformRow(
    label: String,
    selected: StreamingPlatform,
    onSelect: (StreamingPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        StreamingPlatform.entries.forEach { platform ->
            PlatformPill(
                platform = platform,
                isSelected = platform == selected,
                onClick = { onSelect(platform) }
            )
        }
    }
}

@Composable
private fun PlatformPill(
    platform: StreamingPlatform,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = platform.displayName,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Late one tonight"
}
