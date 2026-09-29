package com.artt.alchemy.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.artt.alchemy.ui.components.ScreenPadding
import com.artt.alchemy.ui.elements.ElementsScreen
import com.artt.alchemy.ui.home.CompletionDialog
import com.artt.alchemy.ui.home.HomeScreen
import com.artt.alchemy.ui.recipes.RecipesScreen
import com.artt.alchemy.ui.settings.SettingsScreen
import com.artt.alchemy.ui.theme.AlchemyTheme
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelColor

// The home scene stays bright; list screens dim it so text keeps its contrast.
private const val HOME_DIM = 0.2f
private const val LIST_DIM = 0.7f
private const val UNSELECTED_ICON_ALPHA = 0.6f
private const val TAB_FADE_MILLIS = 180
private val NAV_BAR_HEIGHT = 58.dp
private val NAV_ICON_SIZE = 26.dp

@Composable
fun AlchemyApp(viewModel: AlchemyViewModel = viewModel()) {
    val state = viewModel.state
    // Music plays only while the app is on screen.
    LifecycleResumeEffect(state.progress.musicEnabled) {
        viewModel.resumeMusic()
        onPauseOrDispose { viewModel.pauseMusic() }
    }
    AlchemyTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Image(
                painter = painterResource(R.drawable.bg_aether),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val dim by animateFloatAsState(
                targetValue = if (state.selectedTab == AppTab.HOME) HOME_DIM else LIST_DIM,
                animationSpec = tween(TAB_FADE_MILLIS),
                label = "backgroundDim"
            )
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = dim)))
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = { AlchemyNavigationBar(selectedTab = state.selectedTab, onSelect = viewModel::selectTab) }
            ) { padding ->
                Crossfade(targetState = state.selectedTab, animationSpec = tween(TAB_FADE_MILLIS), label = "tab") { tab ->
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
                            modifier = Modifier.padding(padding)
                        )
                        AppTab.ELEMENTS -> ElementsScreen(
                            progress = state.progress,
                            freshIds = state.freshElementIds,
                            onSeen = viewModel::markElementsSeen,
                            onClick = viewModel::onButtonClick,
                            modifier = Modifier.padding(padding)
                        )
                        AppTab.RECIPES -> RecipesScreen(
                            progress = state.progress,
                            onPlaceRecipe = viewModel::placeRecipe,
                            onRequestHint = viewModel::requestHint,
                            onPlaceHint = viewModel::placeHint,
                            modifier = Modifier.padding(padding)
                        )
                        AppTab.ACHIEVEMENTS -> AchievementsScreen(state.progress, onOpenCompletion = viewModel::showCompletion, modifier = Modifier.padding(padding))
                        AppTab.SETTINGS -> SettingsScreen(
                            state = state,
                            onSoundChanged = viewModel::setSoundEnabled,
                            onVibrationChanged = viewModel::setVibrationEnabled,
                            onMusicChanged = viewModel::setMusicEnabled,
                            onRequestReset = viewModel::requestReset,
                            onConfirmReset = viewModel::confirmReset,
                            onDismissReset = viewModel::dismissReset,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
            if (state.isCompletionVisible && state.newlyUnlockedId == null && state.combinationEffect == null) {
                CompletionDialog(state.progress, onDismiss = viewModel::dismissCompletion, onClick = viewModel::onButtonClick)
            }
            AchievementBanner(state, viewModel)
        }
    }
}

// Waits until a discovery card or its effect is done, so the banner never covers the reveal.
@Composable
private fun BoxScope.AchievementBanner(state: AlchemyUiState, viewModel: AlchemyViewModel) {
    val achievement = state.achievementQueue.firstOrNull()?.let(achievementsById::getValue)
    if (achievement != null && state.newlyUnlockedId == null && state.combinationEffect == null && !state.isCompletionVisible) {
        key(achievement.id) {
            AchievementToast(
                achievement = achievement,
                onShown = viewModel::onAchievementShown,
                onDismiss = viewModel::dismissAchievement,
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
            .background(PanelColor)
            .navigationBarsPadding()
            .height(NAV_BAR_HEIGHT)
            .selectableGroup()
    ) {
        AppTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            val iconAlpha by animateFloatAsState(if (selected) 1f else UNSELECTED_ICON_ALPHA, tween(TAB_FADE_MILLIS), label = "navIcon")
            val indicator by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                tween(TAB_FADE_MILLIS),
                label = "navIndicator"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
                    .testTag(tab.testTag)
            ) {
                Image(
                    painter = painterResource(tab.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .background(indicator, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                        .size(NAV_ICON_SIZE)
                        .alpha(iconAlpha)
                )
                Text(
                    stringResource(tab.labelRes),
                    style = labelStyle,
                    color = if (selected) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
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

private val AppTab.testTag: String
    get() = "nav_${name.lowercase()}"
