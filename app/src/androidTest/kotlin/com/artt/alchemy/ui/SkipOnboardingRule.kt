package com.artt.alchemy.ui

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.game.WorkspaceState
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Starts each test from fresh progress with an empty workspace and the first-run tips seen, so tips never cover the plate
 * in tests about something else and nothing a previous run left on the device changes what is on screen. The progress
 * found on the device is put back afterwards.
 */
class SkipOnboardingRule : TestWatcher() {
    private val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private var original: PlayerProgress? = null

    override fun starting(description: Description) {
        original = store.load()
        store.saveWorkspace(WorkspaceState())
        store.saveTipStep(0)
        store.save(initialPlayerProgress().copy(onboardingSeen = true))
    }

    override fun finished(description: Description) {
        original?.let(store::save)
        store.saveWorkspace(WorkspaceState())
        store.saveTipStep(0)
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

/** Switches the system animations off for the tests [applies] to, as the player can in the system settings, and back afterwards. */
class SystemAnimationsOffRule(private val applies: (Description) -> Boolean = { true }) : TestWatcher() {
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private var original: String? = null

    override fun starting(description: Description) {
        if (!applies(description)) return
        original = shell("settings get global animator_duration_scale").trim()
        shell("settings put global animator_duration_scale 0")
    }

    override fun finished(description: Description) {
        if (!applies(description)) return
        shell(original?.takeIf { it != "null" }?.let { "settings put global animator_duration_scale $it" } ?: "settings delete global animator_duration_scale")
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
}
