package com.artt.alchemy.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.game.ElementRarity

val Gold = Color(0xFFF3C35B)

private const val REGULAR_WEIGHT = 400
private const val MEDIUM_WEIGHT = 500
private const val BOLD_WEIGHT = 700
private const val TITLE_LINE_GAP = 6
private const val LINING_NUMERALS = "lnum"
private const val BODY_LINE_GAP = 8
private const val DISPLAY_LARGE_SP = 57
private const val DISPLAY_MEDIUM_SP = 45
private const val DISPLAY_SMALL_SP = 36
private const val HEADLINE_LARGE_SP = 36
private const val HEADLINE_MEDIUM_SP = 32
private const val HEADLINE_SMALL_SP = 28
private const val TITLE_LARGE_SP = 26
private const val TITLE_MEDIUM_SP = 20
private const val TITLE_SMALL_SP = 17
private const val BODY_LARGE_SP = 17
private const val BODY_MEDIUM_SP = 15
private const val BODY_SMALL_SP = 13
private const val LABEL_LARGE_SP = 17
private const val LABEL_MEDIUM_SP = 14
private const val LABEL_SMALL_SP = 12

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

@OptIn(ExperimentalTextApi::class)
private fun alegreya(weight: Int) = Font(R.font.alegreya, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Single-weight display face shared by every gold heading, so none of them is synthesized bold. */
val TitleFontFamily = FontFamily(Font(R.font.underdog))

/** Readable serif for labels, buttons and running text; Cormorant's Cyrillic is too fanciful at small sizes. */
val BodyFontFamily = FontFamily(alegreya(REGULAR_WEIGHT), alegreya(MEDIUM_WEIGHT), alegreya(BOLD_WEIGHT))

private fun TextStyle.display(size: Int) = copy(
    fontFamily = TitleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = (size + TITLE_LINE_GAP).sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = LINING_NUMERALS
)

private fun TextStyle.text(size: Int, weight: FontWeight, lineGap: Int = BODY_LINE_GAP) = copy(
    fontFamily = BodyFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size + lineGap).sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = LINING_NUMERALS
)

private val AlchemyTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.display(DISPLAY_LARGE_SP),
        displayMedium = base.displayMedium.display(DISPLAY_MEDIUM_SP),
        displaySmall = base.displaySmall.display(DISPLAY_SMALL_SP),
        headlineLarge = base.headlineLarge.display(HEADLINE_LARGE_SP),
        headlineMedium = base.headlineMedium.display(HEADLINE_MEDIUM_SP),
        headlineSmall = base.headlineSmall.display(HEADLINE_SMALL_SP),
        titleLarge = base.titleLarge.display(TITLE_LARGE_SP),
        titleMedium = base.titleMedium.text(TITLE_MEDIUM_SP, FontWeight.Bold),
        titleSmall = base.titleSmall.text(TITLE_SMALL_SP, FontWeight.Bold),
        bodyLarge = base.bodyLarge.text(BODY_LARGE_SP, FontWeight.Normal),
        bodyMedium = base.bodyMedium.text(BODY_MEDIUM_SP, FontWeight.Normal),
        bodySmall = base.bodySmall.text(BODY_SMALL_SP, FontWeight.Normal),
        labelLarge = base.labelLarge.text(LABEL_LARGE_SP, FontWeight.Bold),
        labelMedium = base.labelMedium.text(LABEL_MEDIUM_SP, FontWeight.Medium),
        labelSmall = base.labelSmall.text(LABEL_SMALL_SP, FontWeight.Medium)
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
