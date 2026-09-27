package com.artt.alchemy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artt.alchemy.R
import com.artt.alchemy.ui.home.HomeScreen

@Composable
fun AlchemyApp(viewModel: AlchemyViewModel = viewModel()) {
    val state = viewModel.state
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = state.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Text(stringResource(tab.shortLabelRes)) },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { padding ->
                when (state.selectedTab) {
                    AppTab.HOME -> HomeScreen(state, viewModel::onWorkspaceEvent, Modifier.padding(padding))
                    else -> PlaceholderScreen(state.selectedTab, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(tab: AppTab, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().testTag("screen_${tab.name.lowercase()}")) {
        Text(text = stringResource(tab.labelRes), style = MaterialTheme.typography.headlineMedium)
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

private val AppTab.shortLabelRes: Int
    get() = when (this) {
        AppTab.HOME -> R.string.tab_home_short
        AppTab.ELEMENTS -> R.string.tab_elements_short
        AppTab.RECIPES -> R.string.tab_recipes_short
        AppTab.ACHIEVEMENTS -> R.string.tab_achievements_short
        AppTab.SETTINGS -> R.string.tab_settings_short
    }

private val AppTab.testTag: String
    get() = "nav_${name.lowercase()}"
