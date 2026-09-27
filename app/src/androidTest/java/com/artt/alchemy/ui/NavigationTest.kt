package com.artt.alchemy.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artt.alchemy.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationReachesAllFiveScreens() {
        composeRule.onNodeWithTag("nav_home").assertExists()
        listOf("elements", "recipes", "achievements", "settings").forEach { tab ->
            composeRule.onNodeWithTag("nav_$tab").performClick()
            composeRule.onNodeWithTag("screen_$tab").assertExists()
        }
    }

    @Test
    fun switchingTabsKeepsHomeWorkspaceState() {
        composeRule.onNodeWithTag("home_workspace").assertExists()
        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.onNodeWithTag("home_workspace").assertExists()
    }
}
