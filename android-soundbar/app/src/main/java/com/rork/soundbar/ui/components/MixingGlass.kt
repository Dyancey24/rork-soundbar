package com.rork.soundbar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.rork.soundbar.ui.theme.Gold
import com.rork.soundbar.ui.theme.GoldSoft
import kotlin.math.sin

/** One poured band of liquid inside the vessel. */
data class GlassLayer(val color: Color, val parts: Int)

/**
 * The live preview vessel, drawn from scratch. A cut-crystal tumbler at the
 * bar, a wide mixing bowl in the kitchen. Layers stack top to bottom in recipe
 * order and the liquid column animates as doses change, so it reads as a live
 * preview of the mix rather than a static illustration.
 */
@Composable
fun MixingGlass(
    layers: List<GlassLayer>,
    modifier: Modifier = Modifier,
    bowl: Boolean = false,
    accent: Color = Gold,
    accentSoft: Color = GoldSoft
) {
    val totalParts = layers.sumOf { it.parts }.coerceAtLeast(1)
    val targetFill = (0.44f + 0.052f * totalParts).coerceIn(0.44f, 0.94f)
    val fill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 190f),
        label = "glassFill"
    )
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val phase by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawVessel(layers = layers, fill = fill, phase = phase, bowl = bowl, accent = accent, accentSoft = accentSoft)
    }
}

private fun DrawScope.drawVessel(
    layers: List<GlassLayer>,
    fill: Float,
    phase: Float,
    bowl: Boolean,
    accent: Color,
    accentSoft: Color
) {
    val w = size.width
    val h = size.height

    // A tumbler is tall and narrow with a rounded heel; a bowl is wide and squat.
    val rimY = if (bowl) h * 0.08f else h * 0.06f
    val baseY = if (bowl) h * 0.95f else h * 0.97f
    val topInset = if (bowl) w * 0.02f else w * 0.06f
    val bottomInset = if (bowl) w * 0.30f else w * 0.14f
    val rimRy = if (bowl) w * 0.09f else w * 0.075f
    val upperCurve = if (bowl) 0.30f else 0.16f
    val lowerCurve = if (bowl) 0.10f else 0.05f

    val interior = Path().apply {
        moveTo(topInset, rimY)
        lineTo(w - topInset, rimY)
        cubicTo(
            w - topInset, baseY - h * upperCurve,
            w - bottomInset, baseY - h * lowerCurve,
            w - bottomInset, baseY
        )
        lineTo(bottomInset, baseY)
        cubicTo(
            bottomInset, baseY - h * lowerCurve,
            topInset, baseY - h * upperCurve,
            topInset, rimY
        )
        close()
    }

    // Vessel body tint before anything is poured.
    drawPath(
        path = interior,
        brush = Brush.verticalGradient(
            0f to Color(0x14FFFFFF),
            1f to Color(0x0AFFFFFF)
        )
    )

    val liquidTop = rimY + (baseY - rimY) * (1f - fill)
    val liquidHeight = baseY - liquidTop
    val totalParts = layers.sumOf { it.parts }.coerceAtLeast(1)

    clipPath(interior) {
        var cursor = liquidTop
        layers.forEachIndexed { index, layer ->
            val bandHeight = liquidHeight * layer.parts / totalParts
            drawRect(
                brush = Brush.verticalGradient(
                    0f to layer.color.copy(alpha = 0.98f),
                    0.5f to lighten(layer.color, 0.10f),
                    1f to layer.color.copy(alpha = 0.98f),
                    startY = cursor,
                    endY = cursor + bandHeight
                ),
                topLeft = Offset(0f, cursor),
                size = Size(w, bandHeight)
            )
            // Soft feather where two liquids meet.
            if (index > 0) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color(0x33000000),
                        1f to Color.Transparent
                    ),
                    topLeft = Offset(0f, cursor - h * 0.012f),
                    size = Size(w, h * 0.028f)
                )
            }
            cursor += bandHeight
        }

        if (bowl) {
            // Glossy glaze catching the daylight across the top of the mix.
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0x66FFFFFF),
                    1f to Color.Transparent
                ),
                topLeft = Offset(0f, liquidTop),
                size = Size(w, h * 0.045f)
            )
        } else {
            // Crema head sitting on the top pour.
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFFE8D5B5).copy(alpha = 0.85f),
                    1f to Color(0xFFCBAE85).copy(alpha = 0.15f)
                ),
                topLeft = Offset(0f, liquidTop),
                size = Size(w, h * 0.028f)
            )
        }

        // Slow travelling gleam across the liquid column.
        val gleamX = w * (0.5f + 0.42f * sin(phase))
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to Color(0x1FFFFFFF),
                1f to Color.Transparent,
                startX = gleamX - w * 0.22f,
                endX = gleamX + w * 0.22f
            ),
            topLeft = Offset(0f, liquidTop),
            size = Size(w, baseY - liquidTop)
        )

        // Cut-crystal facets on the lower half of the tumbler.
        if (!bowl) {
            val facetTop = baseY - h * 0.30f
            for (i in 0..5) {
                val x = w * (0.10f + i * 0.16f)
                drawLine(
                    color = Color(0x1AFFFFFF),
                    start = Offset(x, facetTop),
                    end = Offset(x - w * 0.02f, baseY),
                    strokeWidth = w * 0.012f
                )
            }
        }
    }

    // Rim ellipse and inner lip.
    drawOval(
        color = Color(0x33FFFFFF),
        topLeft = Offset(topInset, rimY - rimRy),
        size = Size(w - topInset * 2, rimRy * 2),
        style = Stroke(width = w * 0.012f)
    )
    drawArc(
        color = accentSoft.copy(alpha = 0.55f),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(topInset, rimY - rimRy),
        size = Size(w - topInset * 2, rimRy * 2),
        style = Stroke(width = w * 0.01f)
    )

    // Vessel outline plus a bright edge catching the room light.
    drawPath(
        path = interior,
        color = Color(0x40FFFFFF),
        style = Stroke(width = w * 0.014f)
    )
    drawLine(
        brush = Brush.verticalGradient(
            0f to accent.copy(alpha = 0.35f),
            1f to Color.Transparent
        ),
        start = Offset(topInset + w * 0.035f, rimY + h * 0.04f),
        end = Offset(bottomInset + w * 0.045f, baseY - h * 0.08f),
        strokeWidth = w * 0.02f
    )

    // Pool of warm light under the vessel.
    drawOval(
        brush = Brush.radialGradient(
            0f to accent.copy(alpha = 0.18f),
            1f to Color.Transparent,
            center = Offset(w / 2f, baseY + h * 0.015f),
            radius = w * 0.55f
        ),
        topLeft = Offset(w * 0.05f, baseY - h * 0.02f),
        size = Size(w * 0.9f, h * 0.07f)
    )
}

private fun lighten(color: Color, amount: Float): Color = Color(
    red = (color.red + amount).coerceAtMost(1f),
    green = (color.green + amount).coerceAtMost(1f),
    blue = (color.blue + amount).coerceAtMost(1f),
    alpha = color.alpha
)
