package com.rork.soundbar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpeakeasyColors = darkColorScheme(
    primary = Gold,
    onPrimary = EspressoDeep,
    primaryContainer = Leather,
    onPrimaryContainer = Ivory,
    secondary = Ember,
    onSecondary = Ivory,
    secondaryContainer = BarHigh,
    onSecondaryContainer = GoldSoft,
    tertiary = GoldSoft,
    onTertiary = EspressoDeep,
    background = Espresso,
    onBackground = Ivory,
    surface = Espresso,
    onSurface = Ivory,
    surfaceVariant = Bar,
    onSurfaceVariant = Ash,
    surfaceContainer = Bar,
    surfaceContainerHigh = BarHigh,
    surfaceContainerHighest = BarHigh,
    surfaceContainerLow = Espresso,
    surfaceContainerLowest = EspressoDeep,
    outline = Leather,
    outlineVariant = Hairline,
    error = Ember,
    onError = Ivory
)

private val KitchenColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Cream,
    primaryContainer = Honey,
    onPrimaryContainer = Cocoa,
    secondary = Olive,
    onSecondary = Cream,
    secondaryContainer = CreamHigh,
    onSecondaryContainer = OliveDeep,
    tertiary = Honey,
    onTertiary = Cocoa,
    background = Linen,
    onBackground = Cocoa,
    surface = Linen,
    onSurface = Cocoa,
    surfaceVariant = CreamHigh,
    onSurfaceVariant = Clay,
    surfaceContainer = Cream,
    surfaceContainerHigh = CreamHigh,
    surfaceContainerHighest = CreamHigh,
    surfaceContainerLow = Linen,
    surfaceContainerLowest = Cream,
    outline = KitchenOutline,
    outlineVariant = KitchenHairline,
    error = Ember,
    onError = Cream
)

/**
 * Soft kitchen palette: the same brasserie late in the evening — dim, warm,
 * and quiet. The terracotta and olive stay, but everything rests a register
 * lower than the bright morning linen.
 */
private val SoftKitchenColors = darkColorScheme(
    primary = Terracotta,
    onPrimary = Linen,
    primaryContainer = Color(0xFF4A2A1C),
    onPrimaryContainer = Color(0xFFF2C7A8),
    secondary = Color(0xFF8A9660),
    onSecondary = Linen,
    secondaryContainer = Color(0xFF33301F),
    onSecondaryContainer = Color(0xFFD5D8B4),
    tertiary = Color(0xFFC9A05A),
    onTertiary = Color(0xFF241C14),
    background = Color(0xFF241C14),
    onBackground = Color(0xFFF0E4D4),
    surface = Color(0xFF241C14),
    onSurface = Color(0xFFF0E4D4),
    surfaceVariant = Color(0xFF2E241A),
    onSurfaceVariant = Color(0xFFA9947E),
    surfaceContainer = Color(0xFF2A2118),
    surfaceContainerHigh = Color(0xFF332A1F),
    surfaceContainerHighest = Color(0xFF332A1F),
    surfaceContainerLow = Color(0xFF241C14),
    surfaceContainerLowest = Color(0xFF1C150E),
    outline = Color(0xFF5C4A38),
    outlineVariant = Color(0x33C9A05A),
    error = Ember,
    onError = Linen
)

/**
 * One app, two moods — and the kitchen serves both shifts: the bright daylight
 * brasserie or its softer evening light. Pass the active [concept] (and, for
 * the kitchen, [isKitchenDark]) and the whole scheme follows.
 */
@Composable
fun AppTheme(
    concept: Concept = Concept.BAR,
    isKitchenDark: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = when {
            concept == Concept.KITCHEN && isKitchenDark -> SoftKitchenColors
            concept == Concept.KITCHEN -> KitchenColors
            else -> SpeakeasyColors
        },
        typography = AppTypography,
        content = content
    )
}
