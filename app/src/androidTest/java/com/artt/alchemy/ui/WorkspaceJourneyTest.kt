package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.artt.alchemy.MainActivity
import org.junit.Rule
import org.junit.Test

class WorkspaceJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun draggingKnownIngredientsTogetherShowsResultOnWorkspace() {
        composeRule.onNodeWithTag("palette_fire").performClick()
        composeRule.onNodeWithTag("palette_water").performClick()
        composeRule.onNodeWithTag("workspace_canvas").performTouchInput {
            swipe(Offset(width * 0.68f, height * 0.5f), Offset(width * 0.32f, height * 0.5f), 300)
        }
        composeRule.onNode(hasTestTag("workspace_canvas") and hasContentDescription("Пар")).assertIsDisplayed()
    }
}
