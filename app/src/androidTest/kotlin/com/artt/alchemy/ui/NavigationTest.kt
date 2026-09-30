package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.artt.alchemy.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain = skippingOnboarding(composeRule)

    @Test
    fun bottomNavigationReachesAllFiveScreens() {
        composeRule.onNodeWithTag("nav_home").assertExists()
        listOf("elements", "recipes", "achievements", "settings").forEach { tab ->
            composeRule.onNodeWithTag("nav_$tab").performClick()
            composeRule.onNodeWithTag("screen_$tab").assertExists()
        }
    }

    @Test
    fun all_base_elements_are_visible_in_palette() {
        listOf("fire", "water", "earth", "air").forEach { id ->
            composeRule.onNodeWithTag("palette_$id").assertIsDisplayed()
        }
    }

    @Test
    fun basePaletteUsesOneCompactRow() {
        val fireBounds = composeRule.onNodeWithTag("palette_fire").fetchSemanticsNode().boundsInRoot
        val earthBounds = composeRule.onNodeWithTag("palette_earth").fetchSemanticsNode().boundsInRoot

        assertTrue(earthBounds.top == fireBounds.top)
    }

    @Test
    fun switchingTabsKeepsHomeWorkspaceState() {
        composeRule.onNodeWithTag("palette_fire").performTouchInput {
            down(Offset(width * 0.5f, height * 0.5f))
            advanceEventTime(600)
            moveTo(Offset(width * 2f, -600f))
            up()
        }
        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription("Огонь")).assertIsDisplayed()

        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithTag("nav_home").performClick()

        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription("Огонь")).assertIsDisplayed()
    }
}
