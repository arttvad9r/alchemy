package com.artt.alchemy.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.game.Combination
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FirstRunTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var store: ProgressStore
    private lateinit var original: PlayerProgress
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun rememberProgress() {
        store = ProgressStore(context)
        original = store.load()
    }

    @After
    fun restoreProgress() {
        scenario?.close()
        store.save(original)
    }

    @Test
    fun tipsWalkThroughThreeStepsAndAreDoneAfterTheLast() {
        launchWith(initialPlayerProgress())
        composeRule.onNodeWithTag("first_run_tip").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.tip_tap)).assertIsDisplayed()

        composeRule.onNodeWithTag("first_run_tip").performClick()
        composeRule.onNodeWithText(context.getString(R.string.tip_combine)).assertIsDisplayed()

        composeRule.onNodeWithTag("tip_next").performClick()
        composeRule.onNodeWithText(context.getString(R.string.tip_remove)).assertIsDisplayed()

        composeRule.onNodeWithTag("tip_next").performClick()
        composeRule.onNodeWithTag("first_run_tip").assertDoesNotExist()
        assertTrue(store.load().onboardingSeen)
    }

    @Test
    fun aTapOnTheCrossSkipsTheTipsForGoodAndTheInfoButtonBringsThemBack() {
        launchWith(initialPlayerProgress())
        composeRule.onNodeWithTag("tip_skip").performClick()
        composeRule.onNodeWithTag("first_run_tip").assertDoesNotExist()

        scenario?.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("first_run_tip").assertDoesNotExist()

        composeRule.onNodeWithTag("show_tips").performClick()
        composeRule.onNodeWithText(context.getString(R.string.tip_tap)).assertIsDisplayed()
        assertFalse(store.load().onboardingSeen)
    }

    @Test
    fun resettingProgressShowsTheTipsAgain() {
        launchWith(initialPlayerProgress().copy(onboardingSeen = true))
        composeRule.onNodeWithTag("first_run_tip").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_reset").performScrollTo().performClick()
        composeRule.onNodeWithText("Сбросить").performClick()
        composeRule.onNodeWithTag("nav_home").performClick()

        composeRule.onNodeWithTag("first_run_tip").assertIsDisplayed()
    }

    @Test
    fun theBackIconInACardReturnsToTheCardItWasOpenedFrom() {
        launchWith(initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam")).copy(onboardingSeen = true))
        composeRule.onNodeWithTag("nav_elements").performClick()
        composeRule.onNodeWithTag("element_fire").performClick()
        composeRule.onNodeWithTag("dialog_back").assertDoesNotExist()

        composeRule.onNodeWithTag("link_steam").performScrollTo().performClick()
        composeRule.onNodeWithTag("dialog_back").assertIsDisplayed()
        composeRule.onNodeWithTag("link_steam").assertDoesNotExist()

        composeRule.onNodeWithTag("dialog_back").performClick()
        composeRule.onNodeWithTag("dialog_back").assertDoesNotExist()
        composeRule.onNodeWithTag("link_steam").assertExists()
    }

    private fun launchWith(progress: PlayerProgress) {
        store.save(progress)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
}
