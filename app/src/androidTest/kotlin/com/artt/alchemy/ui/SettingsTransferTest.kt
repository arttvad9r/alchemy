package com.artt.alchemy.ui

import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.artt.alchemy.data.parsePlayerProgress
import com.artt.alchemy.data.toJson
import com.artt.alchemy.game.WorkspaceState
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
        // A workspace left by another test would lie on this one's board.
        store.saveWorkspace(WorkspaceState())
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
        // The file brings progress; the volume stays as set on this device.
        assertEquals(1f, store.load().musicVolume, 0.001f)
        assertNull(store.load().activeHint)
    }

    private fun importFile(text: String) {
        val file = File(context.cacheDir, "import-test.json").apply { writeText(text) }
        scenario.onActivity { activity ->
            ViewModelProvider(activity)[AlchemyViewModel::class.java].readImport(Uri.fromFile(file))
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("import_dialog").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("transfer_dialog").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun anOversizedSaveIsRejectedWithoutReplacingProgress() {
        val text = initialPlayerProgress().toJson().dropLast(1) + ",\"padding\":\"" + "x".repeat(256 * 1024) + "\"}"

        importFile(text)

        composeRule.onNodeWithTag("import_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("transfer_dialog").assertIsDisplayed()
        scenario.onActivity { activity ->
            assertEquals(TransferResult.IMPORT_INVALID, ViewModelProvider(activity)[AlchemyViewModel::class.java].state.transferResult)
        }
        assertEquals(initialPlayerProgress().copy(reducedMotion = false, onboardingSeen = true), store.load())
    }

    @Test
    fun slowImportLeavesTheMainThreadFreeAndResetDiscardsItsResult() {
        val imported = initialPlayerProgress().copy(unlockedIds = initialPlayerProgress().unlockedIds + "steam")
        val uri = delayedDocument("slow-import.json", imported.toJson())
        val started = SystemClock.elapsedRealtime()
        scenario.onActivity { activity -> ViewModelProvider(activity)[AlchemyViewModel::class.java].readImport(uri) }

        assertTrue("The main thread waited for the document provider", SystemClock.elapsedRealtime() - started < 1_500)
        scenario.onActivity { activity -> ViewModelProvider(activity)[AlchemyViewModel::class.java].confirmReset() }
        // Give the provider time to finish even if cancelling its caller cannot interrupt openFile.
        SystemClock.sleep(3_500)
        scenario.onActivity { activity ->
            val state = ViewModelProvider(activity)[AlchemyViewModel::class.java].state
            assertNull(state.pendingImport)
            assertNull(state.transferResult)
            assertEquals(initialPlayerProgress().copy(reducedMotion = false), state.progress)
        }
    }

    @Test
    fun slowExportLeavesTheMainThreadFreeAndWritesTheRequestedSnapshot() {
        val uri = delayedDocument("slow-export.json")
        val started = SystemClock.elapsedRealtime()
        scenario.onActivity { activity -> ViewModelProvider(activity)[AlchemyViewModel::class.java].exportProgress(uri) }

        assertTrue("The main thread waited for the document provider", SystemClock.elapsedRealtime() - started < 1_500)
        composeRule.onNodeWithTag("settings_sound").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("transfer_dialog").fetchSemanticsNodes().isNotEmpty() }
        val immediateUri = uri.buildUpon().clearQuery().build()
        val exported = context.contentResolver.openInputStream(immediateUri)!!.use { parsePlayerProgress(it.readBytes().decodeToString())!! }
        assertTrue(exported.soundEnabled)
        assertEquals(false, store.load().soundEnabled)
        scenario.onActivity { activity ->
            assertEquals(TransferResult.EXPORTED, ViewModelProvider(activity)[AlchemyViewModel::class.java].state.transferResult)
        }
    }

    private fun delayedDocument(name: String, text: String? = null): Uri = Uri.Builder()
        .scheme("content")
        .authority("com.artt.alchemy.test.transfer")
        .appendPath(name)
        .appendQueryParameter("delayMillis", "3000")
        .apply { text?.let { appendQueryParameter("text", it) } }
        .build()

    @Test
    fun aNewImportDiscardsThePreviousPendingSaveBeforeReadingItsDocument() {
        importFile(initialPlayerProgress().copy(unlockedIds = initialPlayerProgress().unlockedIds + "steam").toJson())
        val next = initialPlayerProgress().copy(unlockedIds = initialPlayerProgress().unlockedIds + "mud")

        scenario.onActivity { activity ->
            val model = ViewModelProvider(activity)[AlchemyViewModel::class.java]
            model.readImport(delayedDocument("replacement-import.json", next.toJson()))
            assertNull(model.state.pendingImport)
            assertNull(model.state.transferResult)
            model.confirmImport()
            assertTrue("The previous file must not be imported", "steam" !in model.state.progress.unlockedIds)
        }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("import_dialog").fetchSemanticsNodes().isNotEmpty() }
        scenario.onActivity { activity ->
            val state = ViewModelProvider(activity)[AlchemyViewModel::class.java].state
            assertTrue("mud" in state.pendingImport!!.unlockedIds)
            assertNull(state.transferResult)
        }
    }

    @Test
    fun anInvalidReplacementImportLeavesOnlyTheErrorResult() {
        importFile(initialPlayerProgress().copy(unlockedIds = initialPlayerProgress().unlockedIds + "steam").toJson())

        importFile("not a save")

        scenario.onActivity { activity ->
            val state = ViewModelProvider(activity)[AlchemyViewModel::class.java].state
            assertNull(state.pendingImport)
            assertEquals(TransferResult.IMPORT_INVALID, state.transferResult)
        }
        composeRule.onNodeWithTag("import_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("transfer_dialog").assertIsDisplayed()
    }

    @Test
    fun aNewExportDiscardsThePreviousTransferDialog() {
        importFile(initialPlayerProgress().toJson())

        scenario.onActivity { activity ->
            val model = ViewModelProvider(activity)[AlchemyViewModel::class.java]
            model.exportProgress(delayedDocument("replacement-export.json"))
            assertNull(model.state.pendingImport)
            assertNull(model.state.transferResult)
        }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("transfer_dialog").fetchSemanticsNodes().isNotEmpty() }
        scenario.onActivity { activity ->
            val state = ViewModelProvider(activity)[AlchemyViewModel::class.java].state
            assertNull(state.pendingImport)
            assertEquals(TransferResult.EXPORTED, state.transferResult)
        }
        composeRule.onNodeWithTag("import_dialog").assertDoesNotExist()
    }

    @Test
    fun resettingWhileExportOpensTheDocumentStillWritesTheRequestedSnapshot() {
        val suffix = SystemClock.elapsedRealtime()
        val marker = "export-opened-$suffix"
        val uri = delayedDocument("export-before-reset-$suffix.json").buildUpon().appendQueryParameter("marker", marker).build()
        val expected = initialPlayerProgress().copy(reducedMotion = false, onboardingSeen = true)
        scenario.onActivity { activity ->
            ViewModelProvider(activity)[AlchemyViewModel::class.java].exportProgress(uri)
        }
        val markerUri = uri.buildUpon().clearQuery().path(marker).build()
        composeRule.waitUntil(5_000) {
            runCatching { context.contentResolver.openInputStream(markerUri)?.use { it.read() == 1 } }.getOrDefault(false) == true
        }
        scenario.onActivity { activity -> ViewModelProvider(activity)[AlchemyViewModel::class.java].confirmReset() }
        val immediateUri = uri.buildUpon().clearQuery().build()
        var exported: PlayerProgress? = null
        composeRule.waitUntil(5_000) {
            exported = runCatching {
                context.contentResolver.openInputStream(immediateUri)?.use { parsePlayerProgress(it.readBytes().decodeToString()) }
            }.getOrNull()
            exported != null
        }
        assertEquals(expected, exported)
        scenario.onActivity { activity ->
            val state = ViewModelProvider(activity)[AlchemyViewModel::class.java].state
            assertEquals(initialPlayerProgress().copy(reducedMotion = false), state.progress)
            assertNull(state.pendingImport)
            assertNull(state.transferResult)
        }
    }
}
