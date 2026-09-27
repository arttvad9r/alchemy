package com.artt.alchemy.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.artt.alchemy.MainActivity
import org.junit.Rule
import org.junit.Test

class CollectionScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun collectionTabsShowRealCollectionContent() {
        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithText("Огонь").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_recipes").performClick()
        composeRule.onNodeWithTag("screen_recipes").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_achievements").performClick()
        composeRule.onNodeWithTag("screen_achievements").assertIsDisplayed()
    }
}
