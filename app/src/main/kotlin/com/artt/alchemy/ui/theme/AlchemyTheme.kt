package com.artt.alchemy.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.artt.alchemy.game.ElementRarity

val Gold = Color(0xFFF3C35B)

/** Translucent panel color that lets the scene background show through. */
val PanelColor = Color(0xD90D1433)
val PanelBorderColor = Color(0xFF34458F)

private val AlchemyColors = darkColorScheme(
    primary = Color(0xFF6B97FF),
    onPrimary = Color(0xFF06102E),
    primaryContainer = Color(0xFF1D3A8C),
    onPrimaryContainer = Color(0xFFDDE5FF),
    secondary = Gold,
    onSecondary = Color(0xFF2B1B00),
    secondaryContainer = Color(0xFF2A2F6B),
    onSecondaryContainer = Color(0xFFE9E7F7),
    tertiary = Color(0xFFA77BFF),
    background = Color(0xFF070B1F),
    onBackground = Color(0xFFE9E7F7),
    surface = Color(0xFF0D1433),
    onSurface = Color(0xFFE9E7F7),
    surfaceVariant = Color(0xFF16204A),
    onSurfaceVariant = Color(0xFFB7BBDD),
    surfaceContainerLowest = Color(0xFF070B1F),
    surfaceContainerLow = Color(0xFF0D1433),
    surfaceContainer = Color(0xFF111A3D),
    surfaceContainerHigh = Color(0xFF151F48),
    surfaceContainerHighest = Color(0xFF1B2655),
    outline = Color(0xFF4A5AA0),
    outlineVariant = PanelBorderColor,
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF2D0006)
)

private val AlchemyTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun AlchemyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AlchemyColors, typography = AlchemyTypography) {
        // Screens draw over the scene image rather than a Surface, so light text must be the default.
        CompositionLocalProvider(LocalContentColor provides AlchemyColors.onBackground, content = content)
    }
}

val ElementRarity.color: Color
    get() = when (this) {
        ElementRarity.BASE -> Color(0xFF8E97B3)
        ElementRarity.COMMON -> Color(0xFF3F74E0)
        ElementRarity.RARE -> Color(0xFF3FA35A)
        ElementRarity.EPIC -> Color(0xFF8B55E0)
        ElementRarity.LEGENDARY -> Color(0xFFD9A53A)
    }
