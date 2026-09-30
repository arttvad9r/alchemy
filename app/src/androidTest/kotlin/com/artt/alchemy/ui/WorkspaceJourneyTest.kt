package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.artt.alchemy.MainActivity
import org.junit.Rule
import org.junit.Test

class WorkspaceJourneyTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain = skippingOnboarding(composeRule)

    @Test
    fun draggingPaletteItemsIntoWorkspacePlacesThemAtTheirDropPositions() {
        composeRule.onNodeWithTag("palette_fire").dragIntoWorkspace(0.3f)
        composeRule
            .onNode(hasTestTag("workspace_item") and hasContentDescription("Огонь"))
            .assertIsDisplayed()

        composeRule.onNodeWithTag("palette_water").dragIntoWorkspace(0.7f)

        composeRule
            .onNode(hasTestTag("workspace_item") and hasContentDescription("Огонь"))
            .assertIsDisplayed()
        composeRule
            .onNode(hasTestTag("workspace_item") and hasContentDescription("Вода"))
            .assertIsDisplayed()
    }

    @Test
    fun dropping_a_palette_element_on_an_ingredient_combines_it_immediately() {
        composeRule.onNodeWithTag("palette_fire").dragIntoWorkspace(0.5f)
        composeRule.onNodeWithTag("palette_water").dragIntoWorkspace(0.5f)

        composeRule
            .onNode(hasTestTag("workspace_item") and hasContentDescription("Пар"))
            .assertIsDisplayed()
        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription("Огонь")).assertDoesNotExist()
        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription("Вода")).assertDoesNotExist()
    }

    @Test
    fun tappingPaletteItemAddsItToWorkspace() {
        composeRule.onNodeWithTag("palette_fire").performTouchInput {
            val center = Offset(width / 2f, height / 2f)
            down(center)
            up()
        }

        composeRule
            .onNode(hasTestTag("workspace_item") and hasContentDescription("Огонь"))
            .assertIsDisplayed()
    }

    @Test
    fun paletteHasADraggableScrollbar() {
        composeRule.onNodeWithTag("palette_scrollbar").assertIsDisplayed()
        composeRule.onNodeWithTag("palette_scroll_thumb").assertIsDisplayed()
    }

    @Test
    fun draggingPaletteItemShowsPreviewWithoutLongPress() {
        composeRule.onNodeWithTag("palette_fire").performTouchInput {
            down(Offset(width * 0.5f, height * 0.5f))
            moveTo(Offset(width * 0.5f, -300f))
        }

        composeRule.onNodeWithTag("drag_preview").assertIsDisplayed()
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
            moveTo(target)
            up()
        }
    }
}
