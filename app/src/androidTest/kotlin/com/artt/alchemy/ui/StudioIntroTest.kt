package com.artt.alchemy.ui

import android.os.SystemClock
import android.widget.VideoView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.allViews
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
        // Natural completion must beat the four-second decoder watchdog.
        assertTrue(SystemClock.elapsedRealtime() - startedAt < 4500)
    }

    @Test
    fun completedIntroDoesNotReplayOnRecreation() {
        waitForGame()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_intro").assertDoesNotExist()
    }

    @Test
    fun backgroundPausesIntroInsteadOfSkippingIt() {
        composeRule.onNodeWithTag("studio_intro").assertIsDisplayed()
        val video = composeRule.runOnUiThread {
            composeRule.activity.window.decorView.allViews.filterIsInstance<VideoView>().first()
        }
        composeRule.waitUntil(timeoutMillis = 7000) { composeRule.runOnUiThread { video.currentPosition >= 1000 } }
        val pausedAt = composeRule.runOnUiThread { video.currentPosition }
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        SystemClock.sleep(3000)
        val resumedAt = SystemClock.elapsedRealtime()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.onNodeWithTag("studio_intro").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 3000) { composeRule.runOnUiThread { video.currentPosition >= pausedAt } }
        // Restoring the frame is faster than playing the first second all over again.
        assertTrue(SystemClock.elapsedRealtime() - resumedAt < pausedAt)
        waitForGame()
    }

    @Test
    fun tapsAndBackCannotSkipIntro() {
        composeRule.onNodeWithTag("studio_intro_skip").assertDoesNotExist()
        composeRule.onNodeWithTag("studio_intro").performTouchInput { click(center) }
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("studio_intro").assertIsDisplayed()
        waitForGame()
    }

    @Test
    fun recreationDuringIntroDoesNotSkipIt() {
        composeRule.onNodeWithTag("studio_intro").assertIsDisplayed()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("studio_intro").assertIsDisplayed()
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
