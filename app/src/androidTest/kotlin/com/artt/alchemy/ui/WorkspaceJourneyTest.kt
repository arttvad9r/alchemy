package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.artt.alchemy.MainActivity
import org.junit.Rule
import org.junit.Test

class WorkspaceJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun draggingPaletteItemsIntoWorkspacePlacesThemAtTheirDropPositions() {
        composeRule.onNodeWithTag("palette_fire").dragIntoWorkspace(2f)
        composeRule.onNodeWithTag("palette_water").dragIntoWorkspace(3.2f)

        composeRule
            .onNode(hasTestTag("workspace_canvas") and hasContentDescription("Огонь, Вода"))
            .assertIsDisplayed()
    }

    @Test
    fun tappingPaletteItemDoesNotAddItToWorkspace() {
        composeRule.onNodeWithTag("palette_fire").performTouchInput {
            val center = Offset(width / 2f, height / 2f)
            down(center)
            up()
        }

        composeRule.onNodeWithTag("workspace_canvas").assertContentDescriptionEquals("")
    }

    private fun SemanticsNodeInteraction.dragIntoWorkspace(targetXMultiplier: Float) {
        performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.5f), Offset(width * targetXMultiplier, -600f), 300)
        }
    }
}
