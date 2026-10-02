package com.artt.alchemy.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.data.AppTheme
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

private const val PANEL_ALPHA = 0.85f

/** Translucent panel color that lets the scene background show through. */
val ColorScheme.panel: Color get() = surface.copy(alpha = PANEL_ALPHA)
val ColorScheme.panelBorder: Color get() = outlineVariant

private val AetherColors = darkColorScheme(
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
    outlineVariant = Color(0xFF34458F),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF2D0006)
)

// Ember and Verdant are Aether moved to another hue the way tools/build_ui_assets.py tints the art:
// accents keep their brightness, and dark surfaces lose most of their colour so they read as
// warm charcoal and deep pine rather than rust and emerald.
private val EmberColors = AetherColors.copy(
    primary = Color(0xFFF0A070),
    onPrimary = Color(0xFF2A1A10),
    primaryContainer = Color(0xFF6B4029),
    onPrimaryContainer = Color(0xFFFFE8DA),
    secondaryContainer = Color(0xFF4A3428),
    onSecondaryContainer = Color(0xFFF2E8E2),
    tertiary = Color(0xFFF2B56B),
    background = Color(0xFF140F0C),
    onBackground = Color(0xFFF2E8E2),
    surface = Color(0xFF221915),
    onSurface = Color(0xFFF2E8E2),
    surfaceVariant = Color(0xFF33271F),
    onSurfaceVariant = Color(0xFFD9C5BA),
    surfaceContainerLowest = Color(0xFF140F0C),
    surfaceContainerLow = Color(0xFF221915),
    surfaceContainer = Color(0xFF2A201A),
    surfaceContainerHigh = Color(0xFF33261F),
    surfaceContainerHighest = Color(0xFF3E2E25),
    outline = Color(0xFF8F6650),
    outlineVariant = Color(0xFF6E4E3C)
)

private val VerdantColors = AetherColors.copy(
    primary = Color(0xFF45BCA6),
    onPrimary = Color(0xFF06201B),
    primaryContainer = Color(0xFF0F4F45),
    onPrimaryContainer = Color(0xFFD5ECE6),
    secondaryContainer = Color(0xFF1C3E38),
    onSecondaryContainer = Color(0xFFDFEDE9),
    tertiary = Color(0xFF7FC8D8),
    background = Color(0xFF080F0D),
    onBackground = Color(0xFFDFEDE9),
    surface = Color(0xFF0E1A17),
    onSurface = Color(0xFFDFEDE9),
    surfaceVariant = Color(0xFF172825),
    onSurfaceVariant = Color(0xFFA7C5BE),
    surfaceContainerLowest = Color(0xFF080F0D),
    surfaceContainerLow = Color(0xFF0E1A17),
    surfaceContainer = Color(0xFF13201E),
    surfaceContainerHigh = Color(0xFF172724),
    surfaceContainerHighest = Color(0xFF1A302B),
    outline = Color(0xFF3A7A6E),
    outlineVariant = Color(0xFF25584F)
)

private fun AppTheme.colors() = when (this) {
    AppTheme.AETHER -> AetherColors
    AppTheme.EMBER -> EmberColors
    AppTheme.VERDANT -> VerdantColors
}

/** The theme being drawn; art and colours that differ per theme read it. */
val LocalAppTheme = staticCompositionLocalOf { AppTheme.AETHER }

private fun themed(base: Int, ember: Int, verdant: Int) = base to mapOf(AppTheme.EMBER to ember, AppTheme.VERDANT to verdant)

// Blue art with a recoloured copy per theme (see THEMED_ART and convert_card_rarities in tools/build_ui_assets.py).
private val ThemedArt = mapOf(
    themed(R.drawable.btn_blue, R.drawable.btn_blue_ember, R.drawable.btn_blue_verdant),
    themed(R.drawable.dialog_blue, R.drawable.dialog_blue_ember, R.drawable.dialog_blue_verdant),
    themed(R.drawable.tab_active, R.drawable.tab_active_ember, R.drawable.tab_active_verdant),
    themed(R.drawable.tab_inactive, R.drawable.tab_inactive_ember, R.drawable.tab_inactive_verdant),
    themed(R.drawable.toggle_on, R.drawable.toggle_on_ember, R.drawable.toggle_on_verdant),
    themed(R.drawable.field_search, R.drawable.field_search_ember, R.drawable.field_search_verdant),
    themed(R.drawable.field_dropdown, R.drawable.field_dropdown_ember, R.drawable.field_dropdown_verdant),
    themed(R.drawable.field_row, R.drawable.field_row_ember, R.drawable.field_row_verdant),
    themed(R.drawable.progress_track, R.drawable.progress_track_ember, R.drawable.progress_track_verdant),
    themed(R.drawable.progress_fill, R.drawable.progress_fill_ember, R.drawable.progress_fill_verdant),
    themed(R.drawable.slider_knob, R.drawable.slider_knob_ember, R.drawable.slider_knob_verdant),
    themed(R.drawable.banner_wide, R.drawable.banner_wide_ember, R.drawable.banner_wide_verdant),
    themed(R.drawable.scene_magic_circle, R.drawable.scene_magic_circle_ember, R.drawable.scene_magic_circle_verdant),
    themed(R.drawable.radio_on, R.drawable.radio_on_ember, R.drawable.radio_on_verdant),
    themed(R.drawable.card_base, R.drawable.card_base_ember, R.drawable.card_base_verdant),
    themed(R.drawable.ic_plus, R.drawable.ic_plus_ember, R.drawable.ic_plus_verdant),
    themed(R.drawable.ic_forward, R.drawable.ic_forward_ember, R.drawable.ic_forward_verdant),
    themed(R.drawable.ic_close, R.drawable.ic_close_ember, R.drawable.ic_close_verdant),
    themed(R.drawable.radio_off, R.drawable.radio_off_ember, R.drawable.radio_off_verdant),
    themed(R.drawable.toggle_off, R.drawable.toggle_off_ember, R.drawable.toggle_off_verdant),
    themed(R.drawable.btn_dark, R.drawable.btn_dark_ember, R.drawable.btn_dark_verdant),
    themed(R.drawable.dialog_gold, R.drawable.dialog_gold_ember, R.drawable.dialog_gold_verdant),
    themed(R.drawable.tooltip_bubble, R.drawable.tooltip_bubble_ember, R.drawable.tooltip_bubble_verdant),
    themed(R.drawable.achievement_locked, R.drawable.achievement_locked_ember, R.drawable.achievement_locked_verdant),
    themed(R.drawable.frame_base, R.drawable.frame_base_ember, R.drawable.frame_base_verdant),
    themed(R.drawable.frame_common, R.drawable.frame_common_ember, R.drawable.frame_common_verdant),
    themed(R.drawable.frame_rare, R.drawable.frame_rare_ember, R.drawable.frame_rare_verdant),
    themed(R.drawable.frame_epic, R.drawable.frame_epic_ember, R.drawable.frame_epic_verdant),
    themed(R.drawable.frame_legendary, R.drawable.frame_legendary_ember, R.drawable.frame_legendary_verdant),
    themed(R.drawable.ic_info, R.drawable.ic_info_ember, R.drawable.ic_info_verdant),
    themed(R.drawable.ic_help, R.drawable.ic_help_ember, R.drawable.ic_help_verdant),
    themed(R.drawable.ic_back, R.drawable.ic_back_ember, R.drawable.ic_back_verdant),
    themed(R.drawable.ic_recent, R.drawable.ic_recent_ember, R.drawable.ic_recent_verdant),
    themed(R.drawable.ic_audio, R.drawable.ic_audio_ember, R.drawable.ic_audio_verdant),
    themed(R.drawable.ic_haptics, R.drawable.ic_haptics_ember, R.drawable.ic_haptics_verdant),
    themed(R.drawable.nav_recipes, R.drawable.nav_recipes_ember, R.drawable.nav_recipes_verdant),
    themed(R.drawable.nav_settings, R.drawable.nav_settings_ember, R.drawable.nav_settings_verdant),
    themed(R.drawable.fx_selected_ring, R.drawable.fx_selected_ring_ember, R.drawable.fx_selected_ring_verdant),
    themed(R.drawable.fx_sparkles_blue, R.drawable.fx_sparkles_blue_ember, R.drawable.fx_sparkles_blue_verdant),
    themed(R.drawable.fx_energy_ring, R.drawable.fx_energy_ring_ember, R.drawable.fx_energy_ring_verdant),
    themed(R.drawable.fx_shockwave_ring, R.drawable.fx_shockwave_ring_ember, R.drawable.fx_shockwave_ring_verdant),
    themed(R.drawable.fx_combine_flash, R.drawable.fx_combine_flash_ember, R.drawable.fx_combine_flash_verdant),
    themed(R.drawable.card_common, R.drawable.card_common_ember, R.drawable.card_common_verdant),
    themed(R.drawable.card_rare, R.drawable.card_rare_ember, R.drawable.card_rare_verdant),
    themed(R.drawable.card_epic, R.drawable.card_epic_ember, R.drawable.card_epic_verdant),
    themed(R.drawable.card_legendary, R.drawable.card_legendary_ember, R.drawable.card_legendary_verdant)
)

/** The current theme's copy of [res], or [res] itself when it is the same in every theme. */
@Composable
@DrawableRes
fun themedArt(@DrawableRes res: Int): Int = ThemedArt[res]?.get(LocalAppTheme.current) ?: res

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
fun AlchemyTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = theme.colors()
    MaterialTheme(colorScheme = colors, typography = AlchemyTypography) {
        // Screens draw over the scene image rather than a Surface, so light text must be the default.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, LocalAppTheme provides theme, content = content)
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

val AppTheme.backgroundRes: Int
    get() = when (this) {
        AppTheme.AETHER -> R.drawable.bg_aether
        AppTheme.EMBER -> R.drawable.bg_ember
        AppTheme.VERDANT -> R.drawable.bg_verdant
    }

/** The light specks drifting over the workspace alongside the gold ones. */
val AppTheme.moteTint: Color
    get() = when (this) {
        AppTheme.AETHER -> Color(0xFF9CC3FF)
        AppTheme.EMBER -> Color(0xFFFFB98A)
        AppTheme.VERDANT -> Color(0xFF9CE3D2)
    }

/** How much the home screen darkens the picture: the brighter the background, the more. */
val AppTheme.homeDim: Float
    get() = when (this) {
        AppTheme.AETHER -> 0.2f
        AppTheme.EMBER -> 0.2f
        AppTheme.VERDANT -> 0.25f
    }
