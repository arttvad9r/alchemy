package com.artt.alchemy.ui

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.data.ProgressStore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class StudioIntroTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(object : TestWatcher() {
        override fun starting(description: Description) {
            val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
            store.save(store.load().copy(onboardingSeen = true, reducedMotion = description.methodName == "reducedMotionShowsStillThenEntersGame"))
        }
    }).around(composeRule)

    @Test
    fun videoCompletesAndEntersGame() {
        val startedAt = SystemClock.elapsedRealtime()
        composeRule.onNodeWithTag("studio_intro_video").assertIsDisplayed()
        waitForGame()
        // Natural completion must beat the five-second decoder watchdog.
        assertTrue(SystemClock.elapsedRealtime() - startedAt < 4500)
    }

    @Test
    fun skipEntersGameAndRecreationDoesNotReplayIntro() {
        composeRule.onNodeWithTag("studio_intro_skip").performClick()
        waitForGame()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_intro").assertDoesNotExist()
    }

    @Test
    fun leavingDuringIntroDoesNotReplayItOnReturn() {
        composeRule.onNodeWithTag("studio_intro").assertIsDisplayed()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitForGame()
    }

    @Test
    fun reducedMotionShowsStillThenEntersGame() {
        composeRule.onNodeWithTag("studio_intro_poster").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_intro_video").assertDoesNotExist()
        waitForGame()
    }

    private fun waitForGame() {
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_intro").assertDoesNotExist()
    }
}
