package com.artt.alchemy.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.game.Combination
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RecipesPaletteTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var store: ProgressStore
    private lateinit var original: PlayerProgress
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun rememberProgress() {
        store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        original = store.load()
        store.save(initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam")).copy(onboardingSeen = true))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForScene()
    }

    @After
    fun restoreProgress() {
        scenario?.close()
        store.save(original)
    }

    @Test
    fun tappingARecipePutsBothIngredientsOnTheWorkspaceAndOpensHome() {
        composeRule.onNodeWithTag("nav_recipes").performClick()

        composeRule.onNodeWithTag("recipe_fire|water").performClick()

        composeRule.onNodeWithTag("screen_recipes").assertDoesNotExist()
        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription("Огонь")).assertIsDisplayed()
        composeRule.onNode(hasTestTag("workspace_item") and hasContentDescription("Вода")).assertIsDisplayed()
    }

    @Test
    fun recipeGroupFilterKeepsOnlyRecipesOfThatGroup() {
        composeRule.onNodeWithTag("nav_recipes").performClick()
        composeRule.onNodeWithTag("recipe_fire|water").assertIsDisplayed()

        // The tab row scrolls sideways and composes only what is on screen, so bring the later tabs in first.
        composeRule.onNodeWithTag("recipes_group_nature").performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("recipes_group_life").performClick()
        composeRule.onNodeWithTag("recipe_fire|water").assertDoesNotExist()

        composeRule.onNodeWithTag("recipes_group_nature").performClick()
        composeRule.onNodeWithTag("recipe_fire|water").assertIsDisplayed()

        composeRule.onNodeWithTag("recipes_group_all").performClick()
        composeRule.onNodeWithTag("recipe_fire|water").assertIsDisplayed()
    }

    @Test
    fun paletteSortChangesTheOrderOfElementsAndIsRemembered() {
        assertTrue("newest first", left("palette_steam") < left("palette_fire"))

        chooseSort("alphabet")
        assertTrue("Вода before Воздух", left("palette_water") < left("palette_air"))
        assertTrue("Земля before Огонь", left("palette_earth") < left("palette_fire"))

        chooseSort("group")
        assertTrue("catalog order kept inside a group", left("palette_fire") < left("palette_water"))

        scenario?.recreate()
        composeRule.waitForIdle()
        assertTrue("the choice survives a restart", left("palette_fire") < left("palette_water"))

        chooseSort("recent")
        assertTrue("newest first again", left("palette_steam") < left("palette_fire"))
    }

    private fun chooseSort(option: String) {
        composeRule.onNodeWithTag("palette_sort").performClick()
        composeRule.onNodeWithTag("palette_sort_$option").performClick()
        composeRule.waitForIdle()
    }

    private fun left(tag: String): Float = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.left
}
