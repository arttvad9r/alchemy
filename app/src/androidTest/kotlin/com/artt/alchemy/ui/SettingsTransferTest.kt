package com.artt.alchemy.ui

import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.MainActivity
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.toJson
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsTransferTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var store: ProgressStore
    private lateinit var original: PlayerProgress
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun startWithFreshProgress() {
        store = ProgressStore(context)
        original = store.load()
        store.save(initialPlayerProgress().copy(reducedMotion = false, onboardingSeen = true))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForScene()
        composeRule.onNodeWithTag("nav_settings").performClick()
    }

    @After
    fun restoreProgress() {
        scenario.close()
        store.save(original)
    }

    @Test
    fun volumeSlidersChangeAndKeepTheVolumes() {
        composeRule.onNodeWithTag("settings_music_volume").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.4f) }
        composeRule.onNodeWithTag("settings_effects_volume").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.2f) }
        composeRule.waitForIdle()

        val range = composeRule.onNodeWithTag("settings_music_volume").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(0.4f, range.current, 0.001f)
        assertEquals(0.4f, store.load().musicVolume, 0.001f)
        assertEquals(0.2f, store.load().effectsVolume, 0.001f)

        scenario.recreate()
        composeRule.onNodeWithTag("nav_settings").performClick()
        val restored = composeRule.onNodeWithTag("settings_effects_volume").performScrollTo().fetchSemanticsNode()
        assertEquals(0.2f, restored.config[SemanticsProperties.ProgressBarRangeInfo].current, 0.001f)
    }

    @Test
    fun reducedMotionToggleIsRememberedAcrossRecreation() {
        composeRule.onNodeWithTag("settings_reduced_motion").performScrollTo().assertIsOff().performClick()
        composeRule.onNodeWithTag("settings_reduced_motion").assertIsOn()
        assertEquals(true, store.load().reducedMotion)

        scenario.recreate()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_reduced_motion").performScrollTo().assertIsOn()
    }

    @Test
    fun aFileThatIsNotASaveChangesNothing() {
        store.save(store.load().copy(soundEnabled = false))
        scenario.recreate()
        composeRule.onNodeWithTag("nav_settings").performClick()

        importFile("this is not a save")

        composeRule.onNodeWithTag("transfer_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("import_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("transfer_ok").performClick()
        assertEquals(initialPlayerProgress().copy(soundEnabled = false, reducedMotion = false, onboardingSeen = true), store.load())
    }

    @Test
    fun aSaveReplacesProgressOnlyAfterConfirmation() {
        val saved = initialPlayerProgress().copy(
            unlockedIds = initialPlayerProgress().unlockedIds + "steam",
            discoveryOrder = initialPlayerProgress().discoveryOrder + "steam",
            musicVolume = 0.3f
        )

        importFile(saved.toJson())
        composeRule.onNodeWithTag("import_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("import_cancel").performClick()
        assertTrue("steam" !in store.load().unlockedIds)

        importFile(saved.toJson())
        composeRule.onNodeWithTag("import_confirm").performClick()
        composeRule.onNodeWithTag("transfer_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("transfer_ok").performClick()

        assertTrue("steam" in store.load().unlockedIds)
        assertEquals(0.3f, store.load().musicVolume, 0.001f)
        assertNull(store.load().activeHint)
    }

    private fun importFile(text: String) {
        val file = File(context.cacheDir, "import-test.json").apply { writeText(text) }
        scenario.onActivity { activity ->
            ViewModelProvider(activity)[AlchemyViewModel::class.java].readImport(Uri.fromFile(file))
        }
        composeRule.waitForIdle()
    }
}
