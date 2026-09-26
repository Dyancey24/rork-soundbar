package com.rork.soundbar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
 * One app, two moods: the bar pours after dark, the kitchen serves in daylight.
 * Pass the active [concept] and the whole scheme follows it.
 */
@Composable
fun AppTheme(
    concept: Concept = Concept.BAR,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (concept == Concept.KITCHEN) KitchenColors else SpeakeasyColors,
        typography = AppTypography,
        content = content
    )
}
