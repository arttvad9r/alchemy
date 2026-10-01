package com.artt.alchemy.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.data.ProgressStore
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Marks the first-run tips as seen before the activity starts, so they never cover the plate in tests about something else. */
class SkipOnboardingRule : TestWatcher() {
    override fun starting(description: Description) {
        val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.save(store.load().copy(onboardingSeen = true))
    }
}

/** Waits out the studio intro: until the game's navigation bar is on screen, taps have nothing to land on. */
fun ComposeTestRule.waitForScene() {
    waitUntil(timeoutMillis = 10000) { onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
}

fun skippingOnboarding(rule: AndroidComposeTestRule<*, *>): TestRule = RuleChain.outerRule(SkipOnboardingRule()).around(rule).around(
    object : TestWatcher() {
        override fun starting(description: Description) = rule.waitForScene()
    }
)
