package com.artt.alchemy.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.artt.alchemy.MainActivity
import com.artt.alchemy.game.elementFacts
import org.junit.Rule
import org.junit.Test

class CollectionScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun elementsGridStartsWithFirstCardOnLeft() {
        composeRule.onNodeWithTag("nav_elements").performClick()

        composeRule.onNodeWithTag("element_fire").assertLeftPositionInRootIsEqualTo(4.dp)
    }

    @Test
    fun collectionTabsShowRealCollectionContent() {
        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithText("Огонь").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_recipes").performClick()
        composeRule.onNodeWithTag("screen_recipes").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_achievements").performClick()
        composeRule.onNodeWithTag("screen_achievements").assertIsDisplayed()
    }

    @Test
    fun collectionScreensShowTheirTitleBanners() {
        mapOf("elements" to "Элементы", "recipes" to "Рецепты", "achievements" to "Достижения").forEach { (tab, title) ->
            composeRule.onNodeWithTag("nav_$tab").performClick()
            composeRule.onNode(isHeading() and hasText(title)).assertIsDisplayed()
        }
    }

    @Test
    fun openElementShowsItsFactAndCloses() {
        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithTag("element_fire").performClick()

        composeRule.onNodeWithText(elementFacts.getValue("fire")).assertIsDisplayed()

        composeRule.onNodeWithTag("dialog_close").performClick()
        composeRule.onNodeWithTag("element_details").assertDoesNotExist()
    }
}
