package com.rork.soundbar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rork.soundbar.R

/**
 * Playfair Display ships as a variable font; each entry pins a weight axis so the
 * high-contrast serif keeps its character instead of being synthetically bolded.
 */
@OptIn(ExperimentalTextApi::class)
val DisplaySerif: FontFamily = FontFamily(
    Font(
        R.font.playfair_display,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.playfair_display,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.playfair_display,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.playfair_display,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

val AppTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = DisplaySerif, fontWeight = FontWeight.Medium),
        displayMedium = base.displayMedium.copy(fontFamily = DisplaySerif, fontWeight = FontWeight.Medium),
        displaySmall = base.displaySmall.copy(fontFamily = DisplaySerif, fontWeight = FontWeight.Medium),
        headlineLarge = base.headlineLarge.copy(fontFamily = DisplaySerif, fontWeight = FontWeight.Medium),
        headlineMedium = base.headlineMedium.copy(fontFamily = DisplaySerif, fontWeight = FontWeight.Medium),
        headlineSmall = base.headlineSmall.copy(fontFamily = DisplaySerif, fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontFamily = DisplaySerif, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium,
        titleSmall = base.titleSmall,
        bodyLarge = base.bodyLarge,
        bodyMedium = base.bodyMedium,
        bodySmall = base.bodySmall,
        labelLarge = base.labelLarge,
        labelMedium = base.labelMedium,
        labelSmall = base.labelSmall
    )
}

/** Small gold section eyebrow used above menu sections and mixing steps. */
val EyebrowStyle: TextStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.6.sp
)
