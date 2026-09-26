package com.rork.soundbar.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rork.soundbar.ui.theme.Concept
import com.rork.soundbar.ui.theme.EyebrowStyle
import com.rork.soundbar.ui.theme.Ivory
import com.rork.soundbar.ui.theme.Gold

/** Small accent, letter-spaced label used above every menu section. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = EyebrowStyle,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/** Serif section title for the softer, non-eyebrow headings. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/** The circular accent play affordance that repeats across menu rows and cards. */
@Composable
fun PourButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    isPlaying: Boolean = false,
    size: Int = 44,
    contentDescription: String = "Play"
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = if (filled) accent else Color.Transparent,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else accent,
        border = if (filled) null else androidx.compose.foundation.BorderStroke(1.dp, accent)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = contentDescription,
                modifier = Modifier.size((size * 0.5).dp)
            )
        }
    }
}

/** Three accent bars bouncing in place — the "this one is playing right now" marker. */
@Composable
fun NowPouringBars(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition(label = "bars")
    val heights = listOf(0.45f, 1f, 0.7f).mapIndexed { index, peak ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = peak,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 480 + index * 170),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }
    Row(
        modifier = modifier
            .size(width = 18.dp, height = 16.dp)
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        heights.forEach { animated ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((16 * animated.value).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/** Bottom-up scrim so display type stays readable over photography. */
fun photoScrim(bottom: Color): Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.45f to bottom.copy(alpha = 0.35f),
    1f to bottom.copy(alpha = 0.95f)
)

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 0.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * The bar-or-kitchen switch, seated in the top-right of every tab header. The
 * icon previews where the toggle leads: dining out of the bar, drinking in of
 * the kitchen.
 */
@Composable
fun ConceptToggle(concept: Concept, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (concept == Concept.BAR) Icons.Outlined.Restaurant else Icons.Outlined.LocalBar,
            contentDescription = concept.switchLabel,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
