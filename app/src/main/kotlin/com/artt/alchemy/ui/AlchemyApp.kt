package com.artt.alchemy.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artt.alchemy.R
import com.artt.alchemy.ui.achievements.AchievementsScreen
import com.artt.alchemy.ui.elements.ElementsScreen
import com.artt.alchemy.ui.home.HomeScreen
import com.artt.alchemy.ui.recipes.RecipesScreen
import com.artt.alchemy.ui.settings.SettingsScreen
import com.artt.alchemy.ui.theme.AlchemyTheme
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelColor

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
            // The home scene stays bright; list screens dim it so text keeps its contrast.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = if (state.selectedTab == AppTab.HOME) 0.2f else 0.7f))
            )
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = { AlchemyNavigationBar(selectedTab = state.selectedTab, onSelect = viewModel::selectTab) }
            ) { padding ->
                when (state.selectedTab) {
                    AppTab.HOME -> HomeScreen(
                        state = state,
                        onEvent = viewModel::onWorkspaceEvent,
                        onDismissNewElement = viewModel::dismissNewElement,
                        onPickUp = viewModel::onPickUp,
                        onClick = viewModel::onButtonClick,
                        onEffectConsumed = viewModel::consumeCombinationEffect,
                        onTransitionsConsumed = viewModel::consumeItemTransitions,
                        modifier = Modifier.padding(padding)
                    )
                    AppTab.ELEMENTS -> ElementsScreen(state.progress, Modifier.padding(padding))
                    AppTab.RECIPES -> RecipesScreen(state.progress, Modifier.padding(padding))
                    AppTab.ACHIEVEMENTS -> AchievementsScreen(state.progress, Modifier.padding(padding))
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
    }
}

@Composable
private fun AlchemyNavigationBar(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    val labelStyle = navigationLabelStyle(AppTab.entries.map { stringResource(it.labelRes) })
    NavigationBar(containerColor = PanelColor, tonalElevation = 0.dp) {
        AppTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    Image(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).alpha(if (selected) 1f else 0.6f)
                    )
                },
                label = { Text(stringResource(tab.labelRes), style = labelStyle, maxLines = 1, softWrap = false) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = Gold,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
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
