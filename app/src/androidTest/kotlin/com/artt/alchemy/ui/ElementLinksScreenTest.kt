package com.artt.alchemy.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.Combination
import com.artt.alchemy.game.ElementLinks
import com.artt.alchemy.game.elementFacts
import com.artt.alchemy.ui.components.withTypographicBinding
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ElementLinksScreenTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var store: ProgressStore
    private lateinit var original: PlayerProgress
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun rememberProgress() {
        store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        original = store.load()
    }

    @After
    fun restoreProgress() {
        scenario?.close()
        store.save(original)
    }

    @Test
    fun linksOpenTheLinkedElementAndShowWhereItCameFrom() {
        launchWith(initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam")))
        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithTag("element_fire").performClick()

        composeRule.onNodeWithTag("link_steam").performScrollTo().performClick()

        composeRule.onNodeWithText(withTypographicBinding(elementFacts.getValue("steam"))).assertIsDisplayed()
        composeRule.onNodeWithTag("link_water").assertExists()
        composeRule.onNodeWithTag("element_links_note").assertExists()

        composeRule.onNodeWithTag("link_fire").performScrollTo().performClick()
        composeRule.onNodeWithText(withTypographicBinding(elementFacts.getValue("fire"))).assertIsDisplayed()
    }

    @Test
    fun aFinalElementIsMarkedAndHasNothingToCombineWith() {
        val finalId = ElementLinks.finalElementIds.first()
        launchWith(everythingOpen())
        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithTag("screen_elements").performScrollToNode(hasTestTag("element_$finalId"))
        composeRule.onNodeWithTag("element_$finalId").performClick()

        composeRule.onNodeWithTag("element_details").assertIsDisplayed()
        composeRule.onNode(hasTestTag("element_links_note") and hasText("Больше ни с чем не сочетается")).assertExists()
    }

    private fun launchWith(progress: PlayerProgress) {
        store.save(progress.copy(onboardingSeen = true))
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun everythingOpen(): PlayerProgress = AlchemyCatalog.recipes.fold(initialPlayerProgress()) { progress, recipe ->
        progress.recordAttempt(Combination(recipe.firstId, recipe.secondId, recipe.resultId))
    }
}
