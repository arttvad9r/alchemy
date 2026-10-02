package com.artt.alchemy.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.AlchemyEngine
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.reduce
import com.artt.alchemy.ui.components.LocalReducedMotion
import com.artt.alchemy.ui.components.elementName
import com.artt.alchemy.ui.home.HomeScreen
import com.artt.alchemy.ui.theme.AlchemyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspaceAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val state = mutableStateOf(AlchemyUiState(initialPlayerProgress().copy(onboardingSeen = true)))
    private val engine = AlchemyEngine(AlchemyCatalog)

    @Test
    fun accessibility_click_can_mix_two_ingredients() {
        show(listOf(WorkspaceItem(1, "fire", 0.3f, 0.5f), WorkspaceItem(2, "water", 0.7f, 0.5f)))
        item("fire").performClick()
        composeRule.onNodeWithTag("workspace_partner_2").performClick()
        composeRule.runOnIdle { assertEquals(listOf("steam"), state.value.workspace.items.map { it.elementId }) }
    }

    @Test
    fun inaccessible_pair_does_not_move_or_remove_ingredients() {
        val items = listOf(WorkspaceItem(1, "steam", 0.3f, 0.5f), WorkspaceItem(2, "earth", 0.7f, 0.5f))
        show(items)
        item("steam").performClick()
        composeRule.onNodeWithTag("workspace_partner_2").performClick()
        composeRule.runOnIdle { assertEquals(items, state.value.workspace.items) }
    }

    @Test
    fun accessibility_remove_action_removes_only_selected_item() {
        show(listOf(WorkspaceItem(1, "fire", 0.3f, 0.5f), WorkspaceItem(2, "water", 0.7f, 0.5f)))
        val actions = item("fire").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle { assertTrue(actions.last().action()) }
        composeRule.runOnIdle { assertEquals(listOf("water"), state.value.workspace.items.map { it.elementId }) }
    }

    @Test
    fun touch_drag_still_reaches_canvas_and_selects_visible_top_item() {
        show(listOf(WorkspaceItem(1, "earth", 0.5f, 0.5f), WorkspaceItem(2, "steam", 0.5f, 0.5f)))
        composeRule.onNodeWithTag("workspace_canvas").performTouchInput {
            down(center)
            moveTo(center + Offset(width * 0.2f, 0f))
            up()
        }
        composeRule.runOnIdle {
            assertEquals(0.5f, state.value.workspace.items.first().xFraction)
            assertTrue(state.value.workspace.items.last().xFraction > 0.6f)
        }
        composeRule.onNodeWithTag("workspace_combine_dialog").assertDoesNotExist()
    }

    private fun item(elementId: String) = composeRule.onNode(
        hasTestTag("workspace_item") and hasContentDescription(
            InstrumentationRegistry.getInstrumentation().targetContext.resources.elementName(elementId)
        )
    )

    private fun show(items: List<WorkspaceItem>) {
        state.value = state.value.copy(workspace = WorkspaceState(items, 3))
        composeRule.setContent {
            AlchemyTheme(state.value.progress.theme) {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    HomeScreen(
                        state = state.value,
                        onEvent = { event -> state.value = state.value.copy(workspace = reduce(state.value.workspace, event, engine).workspace) },
                        onDismissNewElement = {}, onPickUp = {}, onClick = {}, onPaletteSort = {},
                        onEffectConsumed = {}, onTransitionsConsumed = {}, onSkipTips = {}, onShowTips = {}
                    )
                }
            }
        }
    }
}
