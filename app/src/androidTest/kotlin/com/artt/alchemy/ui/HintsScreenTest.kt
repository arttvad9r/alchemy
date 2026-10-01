package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.game.Recipe
import com.artt.alchemy.game.nextHintRecipe
import com.artt.alchemy.ui.components.elementName
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HintsScreenTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var store: ProgressStore
    private lateinit var original: PlayerProgress
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun startFresh() {
        store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        original = store.load()
        store.save(initialPlayerProgress().copy(onboardingSeen = true))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForScene()
    }

    @After
    fun restoreProgress() {
        scenario?.close()
        store.save(original)
    }

    @Test
    fun aHintRevealsOneIngredientThenTheSecondAndStaysAcrossARestart() {
        openRecipes()
        composeRule.onNodeWithTag("hint_card").assertDoesNotExist()

        composeRule.onNodeWithTag("hint_request").performClick()
        composeRule.onNodeWithTag("hint_card").assertIsDisplayed()
        composeRule.onNodeWithTag("hint_more").assertIsDisplayed()

        scenario?.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_recipes").performClick()
        composeRule.onNodeWithTag("hint_more").assertIsDisplayed()

        composeRule.onNodeWithTag("hint_more").performClick()
        composeRule.onNodeWithTag("hint_card").assertIsDisplayed()
        composeRule.onNodeWithTag("hint_more").assertDoesNotExist()
        composeRule.onNodeWithTag("hint_request").assertDoesNotExist()
    }

    @Test
    fun tappingTheHintPutsItsIngredientsOnTheWorkspace() {
        val recipe = firstHint()
        openRecipes()
        composeRule.onNodeWithTag("hint_request").performClick()
        composeRule.onNodeWithTag("hint_more").performClick()

        composeRule.onNodeWithTag("hint_place").performClick()

        composeRule.onNodeWithTag("screen_recipes").assertDoesNotExist()
        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription(name(recipe.firstId))).assertIsDisplayed()
    }

    @Test
    fun mixingTheHintedElementRetiresTheHintAndTheNextOneIsDifferent() {
        val recipe = firstHint()
        openRecipes()
        composeRule.onNodeWithTag("hint_request").performClick()
        val before = store.load().activeHint

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.onNodeWithTag("palette_${recipe.firstId}").dragIntoWorkspace()
        composeRule.onNodeWithTag("palette_${recipe.secondId}").dragIntoWorkspace()
        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription(name(recipe.resultId))).assertIsDisplayed()

        openRecipes()
        composeRule.onNodeWithTag("hint_card").assertDoesNotExist()
        composeRule.onNodeWithTag("hint_request").performClick()
        composeRule.waitForIdle()
        assertNotEquals(before, store.load().activeHint)
    }

    private fun firstHint(): Recipe = nextHintRecipe(initialPlayerProgress().unlockedIds)!!

    private fun name(elementId: String): String = InstrumentationRegistry.getInstrumentation().targetContext.resources.elementName(elementId)

    private fun openRecipes() {
        composeRule.onNodeWithTag("nav_recipes").performClick()
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
