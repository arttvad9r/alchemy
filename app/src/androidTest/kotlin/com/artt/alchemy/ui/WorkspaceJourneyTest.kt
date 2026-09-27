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
        composeRule.onNodeWithTag("palette_fire").dragIntoWorkspace(0.3f)
        composeRule
            .onNode(hasTestTag("workspace_canvas") and hasContentDescription("Огонь"))
            .assertIsDisplayed()

        composeRule.onNodeWithTag("palette_water").dragIntoWorkspace(0.7f)

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

    @Test
    fun longPressShowsElementPreviewUnderTheFinger() {
        composeRule.onNodeWithTag("palette_fire").performTouchInput {
            down(Offset(width * 0.5f, height * 0.5f))
            advanceEventTime(600)
            moveTo(Offset(width * 0.5f, -300f))
        }

        composeRule.onNodeWithTag("drag_preview").assertIsDisplayed()
    }

    @Test
    fun swipingPaletteDoesNotSpawnAnElement() {
        composeRule.onNodeWithTag("palette_fire").performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.5f), Offset(width * 0.5f, -600f), 300)
        }

        composeRule.onNodeWithTag("workspace_canvas").assertContentDescriptionEquals("")
    }

    private fun SemanticsNodeInteraction.dragIntoWorkspace(targetXFraction: Float) {
        val sourceBounds = fetchSemanticsNode().boundsInRoot
        val workspaceBounds = composeRule.onNodeWithTag("home_workspace").fetchSemanticsNode().boundsInRoot
        val target = Offset(
            workspaceBounds.left + workspaceBounds.width * targetXFraction - sourceBounds.left,
            workspaceBounds.top + workspaceBounds.height * 0.5f - sourceBounds.top
        )

        performTouchInput {
            down(Offset(width * 0.5f, height * 0.5f))
            advanceEventTime(600)
            moveTo(target)
            up()
        }
    }
}
