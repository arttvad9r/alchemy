package com.artt.alchemy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.game.WorkspaceState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressStoreTest {
    private val store = ProgressStore(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    @After
    fun clearWorkspace() {
        store.saveWorkspace(WorkspaceState())
        store.saveTipStep(0)
    }

    @Test
    fun theWorkspaceComesBackAsItWasLeft() {
        store.saveWorkspace(WorkspaceState(listOf(WorkspaceItem(7, "fire", 0.25f, 0.5f), WorkspaceItem(9, "water", 0.75f, 0.1f)), nextInstanceId = 10))

        val loaded = store.loadWorkspace(AlchemyCatalog.baseElementIds)

        assertEquals(listOf("fire" to (0.25f to 0.5f), "water" to (0.75f to 0.1f)), loaded.items.map { it.elementId to (it.xFraction to it.yFraction) })
        assertEquals(loaded.items.size + 1L, loaded.nextInstanceId)
        assertEquals(loaded.items.size, loaded.items.map { it.instanceId }.toSet().size)
    }

    @Test
    fun elementsThatAreNotOpenAreLeftOff() {
        store.saveWorkspace(WorkspaceState(listOf(WorkspaceItem(1, "fire", 0.5f, 0.5f), WorkspaceItem(2, "steam", 0.5f, 0.5f)), nextInstanceId = 3))

        assertEquals(listOf("fire"), store.loadWorkspace(AlchemyCatalog.baseElementIds).items.map { it.elementId })
    }

    @Test
    fun theTipOnScreenIsKept() {
        store.saveTipStep(2)

        assertEquals(2, store.loadTipStep())
    }
}
