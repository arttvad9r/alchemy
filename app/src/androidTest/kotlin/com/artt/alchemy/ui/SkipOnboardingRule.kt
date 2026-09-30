package com.artt.alchemy.ui

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

fun skippingOnboarding(rule: TestRule): TestRule = RuleChain.outerRule(SkipOnboardingRule()).around(rule)
