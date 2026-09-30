package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.Recipe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CompletionScreenTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var store: ProgressStore
    private lateinit var original: PlayerProgress
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun saveOriginal() {
        store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        original = store.load()
    }

    @After
    fun restoreProgress() {
        scenario?.close()
        store.save(original)
    }

    @Test
    fun theLastElementShowsTheFinishCardOnceAndTheAchievementsReopenIt() {
        val recipe = startOneElementShort()

        composeRule.onNodeWithTag("nav_achievements").performClick()
        composeRule.onNodeWithTag("achievement_completion").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_home").performClick()

        composeRule.onNodeWithTag("palette_${recipe.firstId}").dragIntoWorkspace()
        composeRule.onNodeWithTag("palette_${recipe.secondId}").dragIntoWorkspace()
        composeRule.waitUntilShown(hasText(OK))
        composeRule.onNodeWithTag("completion_dialog").assertDoesNotExist()

        composeRule.onNodeWithText(OK).performClick()
        composeRule.waitUntilShown(hasTestTag("completion_dialog"))
        composeRule.onNodeWithTag("completion_dialog").assertIsDisplayed()
        composeRule.onNodeWithText(OK).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodesWithTag("completion_dialog").fetchSemanticsNodes().isEmpty() }

        scenario?.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("completion_dialog").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_achievements").performClick()
        composeRule.onNodeWithTag("achievement_completion").assertIsDisplayed().performClick()
        composeRule.waitUntilShown(hasTestTag("completion_dialog"))
        composeRule.onNodeWithText(OK).performClick()
    }

    private fun startOneElementShort(): Recipe {
        val recipe = AlchemyCatalog.recipes.last()
        val all = AlchemyCatalog.elements.map { it.id }
        val ingredients = listOf(recipe.firstId, recipe.secondId)
        val others = all.filterNot { it == recipe.resultId || it in ingredients }
        store.save(
            initialPlayerProgress().copy(
                onboardingSeen = true,
                unlockedIds = (all - recipe.resultId).toSet(),
                discoveryOrder = others + ingredients.distinct()
            )
        )
        scenario = ActivityScenario.launch(MainActivity::class.java)
        return recipe
    }

    private fun ComposeTestRule.waitUntilShown(matcher: SemanticsMatcher) {
        waitUntil(TIMEOUT_MILLIS) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun SemanticsNodeInteraction.dragIntoWorkspace() {
        val sourceBounds = fetchSemanticsNode().boundsInRoot
        val workspaceBounds = composeRule.onNodeWithTag("home_workspace").fetchSemanticsNode().boundsInRoot
        val target = Offset(
            workspaceBounds.left + workspaceBounds.width * 0.5f - sourceBounds.left,
            workspaceBounds.top + workspaceBounds.height * 0.5f - sourceBounds.top
        )
        performTouchInput {
            down(Offset(width * 0.5f, height * 0.5f))
            moveTo(target)
            up()
        }
    }
}

private const val OK = "Понятно"
private const val TIMEOUT_MILLIS = 5_000L
