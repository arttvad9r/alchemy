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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
                        onFeedbackHandled = viewModel::consumeCombinationFeedback,
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
                label = { Text(stringResource(tab.labelRes), maxLines = 1) },
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
