package com.artt.alchemy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artt.alchemy.R
import com.artt.alchemy.ui.achievements.AchievementToast
import com.artt.alchemy.ui.achievements.AchievementsScreen
import com.artt.alchemy.ui.achievements.achievementsById
import com.artt.alchemy.ui.components.LocalReducedMotion
import com.artt.alchemy.ui.components.LocalScreenOpening
import com.artt.alchemy.ui.components.ScreenPadding
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.components.rememberScreenOpening
import com.artt.alchemy.ui.components.systemAnimationsOff
import com.artt.alchemy.ui.elements.ElementsScreen
import com.artt.alchemy.ui.home.CompletionDialog
import com.artt.alchemy.ui.home.HomeScreen
import com.artt.alchemy.ui.recipes.RecipesScreen
import com.artt.alchemy.ui.settings.SettingsScreen
import com.artt.alchemy.ui.theme.AlchemyTheme
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.HOME_DIM
import com.artt.alchemy.ui.theme.panel

// Each room keeps enough contrast for its screen while still remaining visible behind translucent UI.
private const val ELEMENTS_DIM = 0.50f
private const val RECIPES_DIM = 0.50f
private const val ACHIEVEMENTS_DIM = 0.50f
private const val SETTINGS_DIM = 0.52f
private const val TOP_SCRIM_HEIGHT = 0.22f
private const val TOP_SCRIM_ALPHA = 0.75f
private const val UNSELECTED_ICON_ALPHA = 0.55f
private const val TAB_FADE_MILLIS = 180

// Screens drift a little towards the tab chosen. The new one starts fading in at once and the old one lingers at
// first, so their fades overlap and some screen is always in view; the drift only hints at the direction.
private const val TAB_ENTER_MILLIS = 240
private const val TAB_EXIT_MILLIS = 200
private const val TAB_SLIDE_SHARE = 32

// The selected tab's pill grows out from the icon and the icon hops as it is chosen.
private const val INDICATOR_START_WIDTH = 0.45f
private const val ICON_HOP_SCALE = 0.78f
private val NAV_BAR_HEIGHT = 58.dp
private val NAV_ICON_SIZE = 26.dp
private val NAV_TOP_LINE = 1.dp
private val NavTopLine = Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = 0.4f), Color.Transparent))

/** The whole game; [sceneReady] turns true once the launch splash has gone, and the music waits for it. */
@Composable
fun AlchemyApp(viewModel: AlchemyViewModel = viewModel(), sceneReady: Boolean = true) {
    val state = viewModel.state
    // Music plays only while the app is on screen, and never over the splash.
    LifecycleResumeEffect(state.progress.musicEnabled, sceneReady) {
        if (sceneReady) viewModel.resumeMusic()
        onPauseOrDispose { viewModel.pauseMusic() }
    }
    val reducedMotion = systemAnimationsOff(LocalContext.current)
    AlchemyTheme {
        CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // Every tab owns a room background. The five resources intentionally start from the
                // same artwork; they can now be replaced independently without touching screen code.
                Crossfade(
                    targetState = state.selectedTab,
                    animationSpec = motion(tween(TAB_FADE_MILLIS)),
                    label = "roomBackground"
                ) { tab ->
                    Image(
                        painter = painterResource(tab.backgroundRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                val dim by animateFloatAsState(
                    targetValue = state.selectedTab.backgroundDim,
                    animationSpec = motion(tween(TAB_FADE_MILLIS)),
                    label = "backgroundDim"
                )
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = dim)))
                // The status bar and the gold headings sit on the top of the picture, which can be bright.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(TOP_SCRIM_HEIGHT)
                        .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = TOP_SCRIM_ALPHA), Color.Transparent)))
                )
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = { AlchemyNavigationBar(selectedTab = state.selectedTab, onSelect = viewModel::selectTab) }
                ) { padding ->
                    // Each screen keeps its scroll position, search and filters while another tab is open.
                    val tabStates = rememberSaveableStateHolder()
                    AnimatedContent(targetState = state.selectedTab, transitionSpec = tabTransition(reducedMotion), label = "tab") { tab ->
                        tabStates.SaveableStateProvider(tab.name) {
                            // Lists on the screen that has just appeared arrive in a cascade.
                            CompositionLocalProvider(LocalScreenOpening provides rememberScreenOpening()) {
                                TabScreen(tab, state, viewModel, Modifier.padding(padding))
                            }
                        }
                    }
                }
                // Reveals take the stage one at a time, in the order the queue gives them.
                if (state.reveal == Reveal.Completion && state.combinationEffect == null) {
                    CompletionDialog(state.progress, onDismiss = viewModel::dismissCompletion, onClick = viewModel::onButtonClick)
                }
                AchievementBanner(state, viewModel)
            }
        }
    }
}

@Composable
private fun TabScreen(tab: AppTab, state: AlchemyUiState, viewModel: AlchemyViewModel, modifier: Modifier) {
    when (tab) {
        AppTab.HOME -> HomeScreen(
            state = state,
            onEvent = viewModel::onWorkspaceEvent,
            onDismissNewElement = viewModel::dismissNewElement,
            onPickUp = viewModel::onPickUp,
            onClick = viewModel::onButtonClick,
            onPaletteSort = viewModel::setPaletteSort,
            onEffectConsumed = viewModel::consumeCombinationEffect,
            onTransitionsConsumed = viewModel::consumeItemTransitions,
            onSkipTips = viewModel::skipTips,
            modifier = modifier
        )

        AppTab.ELEMENTS -> ElementsScreen(
            progress = state.progress,
            freshIds = state.freshElementIds,
            onSeen = viewModel::markElementsSeen,
            onClick = viewModel::onButtonClick,
            modifier = modifier
        )

        AppTab.RECIPES -> RecipesScreen(
            progress = state.progress,
            onPlaceRecipe = viewModel::placeRecipe,
            onRequestHint = viewModel::requestHint,
            onPlaceHint = viewModel::placeHint,
            modifier = modifier
        )

        AppTab.ACHIEVEMENTS -> AchievementsScreen(state.progress, onOpenCompletion = viewModel::showCompletion, modifier = modifier)

        AppTab.SETTINGS -> SettingsScreen(
            state = state,
            onSoundChanged = viewModel::setSoundEnabled,
            onVibrationChanged = viewModel::setVibrationEnabled,
            onMusicChanged = viewModel::setMusicEnabled,
            onRequestReset = viewModel::requestReset,
            onConfirmReset = viewModel::confirmReset,
            onDismissReset = viewModel::dismissReset,
            onMusicVolumeChanged = viewModel::setMusicVolume,
            onMusicVolumeFinished = viewModel::saveMusicVolume,
            onEffectsVolumeChanged = viewModel::setEffectsVolume,
            onEffectsVolumeFinished = viewModel::previewEffectsVolume,
            onExport = viewModel::exportProgress,
            onImportPicked = viewModel::readImport,
            onConfirmImport = viewModel::confirmImport,
            onDismissImport = viewModel::dismissImport,
            onDismissTransferResult = viewModel::dismissTransferResult,
            modifier = modifier
        )
    }
}

/** Screens slide towards the tab chosen, so the bar reads as a row of places side by side. */
private fun tabTransition(reducedMotion: Boolean): AnimatedContentTransitionScope<AppTab>.() -> ContentTransform = {
    if (reducedMotion) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
        val enter = slideInHorizontally(tween(TAB_ENTER_MILLIS, easing = FastOutSlowInEasing)) { width -> direction * width / TAB_SLIDE_SHARE } +
            fadeIn(tween(TAB_ENTER_MILLIS, easing = LinearOutSlowInEasing))
        val exit = slideOutHorizontally(tween(TAB_EXIT_MILLIS, easing = FastOutSlowInEasing)) { width -> -direction * width / TAB_SLIDE_SHARE } +
            fadeOut(tween(TAB_EXIT_MILLIS, easing = FastOutLinearInEasing))
        enter togetherWith exit
    }
}

// On stage once the discovery cards and the finale ahead of it are done, and never over a mix's effect.
@Composable
private fun BoxScope.AchievementBanner(state: AlchemyUiState, viewModel: AlchemyViewModel) {
    val achievement = (state.reveal as? Reveal.Achievement)?.id?.let(achievementsById::getValue)
    if (achievement != null && state.combinationEffect == null) {
        key(achievement.id) {
            AchievementToast(
                achievement = achievement,
                onShown = viewModel::onAchievementShown,
                onDismiss = { viewModel.dismissAchievement(achievement.id) },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(ScreenPadding)
            )
        }
    }
}

@Composable
private fun AlchemyNavigationBar(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    val labelStyle = navigationLabelStyle(AppTab.entries.map { stringResource(it.labelRes) })
    // Lower than the Material bar: the icon, its selection pill and the label sit close together.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.panel)
            // A thin gilded edge where the bar meets the scene.
            .drawBehind { drawRect(NavTopLine, size = Size(size.width, NAV_TOP_LINE.toPx())) }
            .navigationBarsPadding()
            .height(NAV_BAR_HEIGHT)
            .selectableGroup()
    ) {
        AppTab.entries.forEach { tab ->
            NavigationItem(tab, selected = selectedTab == tab, labelStyle = labelStyle, onClick = { onSelect(tab) })
        }
    }
}

@Composable
private fun RowScope.NavigationItem(tab: AppTab, selected: Boolean, labelStyle: TextStyle, onClick: () -> Unit) {
    val iconAlpha by animateFloatAsState(if (selected) 1f else UNSELECTED_ICON_ALPHA, motion(tween(TAB_FADE_MILLIS)), label = "navIcon")
    // The pill grows out from the icon with a little overshoot, and shrinks back into it when another tab is chosen.
    val indicator by animateFloatAsState(
        if (selected) 1f else 0f,
        motion(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)),
        label = "navIndicator"
    )
    val labelColor by animateColorAsState(if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant, motion(tween(TAB_FADE_MILLIS)), label = "navLabel")
    val pillColor = MaterialTheme.colorScheme.secondaryContainer
    val hop = remember { Animatable(1f) }
    val hopSpec = motion(spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    LaunchedEffect(selected) {
        if (selected) {
            hop.snapTo(ICON_HOP_SCALE)
            hop.animateTo(1f, hopSpec)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(tab.testTag)
    ) {
        Image(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            modifier = Modifier
                .drawBehind {
                    val shown = indicator.coerceAtLeast(0f)
                    if (shown > 0f) {
                        val width = size.width * (INDICATOR_START_WIDTH + (1f - INDICATOR_START_WIDTH) * shown)
                        drawRoundRect(
                            color = pillColor.copy(alpha = 0.78f * shown.coerceAtMost(1f)),
                            topLeft = Offset((size.width - width) / 2, 0f),
                            size = Size(width, size.height),
                            cornerRadius = CornerRadius(size.height / 2)
                        )
                    }
                }
                .padding(horizontal = 14.dp, vertical = 2.dp)
                .size(NAV_ICON_SIZE)
                .graphicsLayer {
                    scaleX = hop.value
                    scaleY = hop.value
                    // The icon hops up as it lands.
                    translationY = (hop.value - 1f) * size.height
                }
                .alpha(iconAlpha)
        )
        Text(
            stringResource(tab.labelRes),
            style = labelStyle,
            color = labelColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// Before the system text scale, so very large text still gets a readable floor of its own.
private const val NAV_LABEL_MIN_SIZE_SP = 6f
private const val NAV_LABEL_SIZE_STEP_SP = 0.5f

// Room a navigation item keeps around its label.
private val NAV_LABEL_PADDING = 16.dp

/**
 * The label style at the largest size where every label fits its item on one line, shared by all
 * labels so large system text shrinks them together instead of cutting some off.
 */
@Composable
private fun navigationLabelStyle(labels: List<String>): TextStyle {
    val base = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer()
    val itemWidth = LocalWindowInfo.current.containerSize.width / labels.size -
        with(LocalDensity.current) { NAV_LABEL_PADDING.roundToPx() }
    return remember(labels, base, itemWidth) {
        val fitting = generateSequence(base.fontSize.value) { it - NAV_LABEL_SIZE_STEP_SP }
            .takeWhile { it >= NAV_LABEL_MIN_SIZE_SP }
            .firstOrNull { size -> labels.all { measurer.measure(it, base.copy(fontSize = size.sp), maxLines = 1).size.width <= itemWidth } }
        base.copy(fontSize = (fitting ?: NAV_LABEL_MIN_SIZE_SP).sp)
    }
}

private val AppTab.labelRes: Int
    get() = when (this) {
        AppTab.HOME -> R.string.tab_home
        AppTab.ELEMENTS -> R.string.tab_elements
        AppTab.RECIPES -> R.string.tab_recipes
        AppTab.ACHIEVEMENTS -> R.string.tab_achievements
        AppTab.SETTINGS -> R.string.tab_settings
    }

private val AppTab.iconRes: Int
    get() = when (this) {
        AppTab.HOME -> R.drawable.nav_home
        AppTab.ELEMENTS -> R.drawable.nav_elements
        AppTab.RECIPES -> R.drawable.nav_recipes
        AppTab.ACHIEVEMENTS -> R.drawable.nav_achievements
        AppTab.SETTINGS -> R.drawable.nav_settings
    }

private val AppTab.backgroundRes: Int
    get() = when (this) {
        AppTab.HOME -> R.drawable.bg_home
        AppTab.ELEMENTS -> R.drawable.bg_elements
        AppTab.RECIPES -> R.drawable.bg_recipes
        AppTab.ACHIEVEMENTS -> R.drawable.bg_achievements
        AppTab.SETTINGS -> R.drawable.bg_settings
    }

private val AppTab.backgroundDim: Float
    get() = when (this) {
        AppTab.HOME -> HOME_DIM
        AppTab.ELEMENTS -> ELEMENTS_DIM
        AppTab.RECIPES -> RECIPES_DIM
        AppTab.ACHIEVEMENTS -> ACHIEVEMENTS_DIM
        AppTab.SETTINGS -> SETTINGS_DIM
    }

private val AppTab.testTag: String
    get() = "nav_${name.lowercase()}"
