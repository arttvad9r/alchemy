package com.artt.alchemy.ui

import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.AlchemyEngine
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.reduce
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceTransitionsTest {
    private val engine = AlchemyEngine(AlchemyCatalog)

    private fun transitionsFor(state: WorkspaceState, event: WorkspaceEvent) = itemTransitions(state, reduce(state, event, engine))

    @Test
    fun spawned_item_appears_at_its_position() {
        val transitions = transitionsFor(WorkspaceState(), WorkspaceEvent.Spawn("fire", 0.2f, 0.3f))

        assertEquals(listOf(ItemTransition(1, TransitionKind.APPEAR, 0.2f, 0.3f)), transitions)
    }

    @Test
    fun item_dragged_out_vanishes_at_its_last_position_inside() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "fire", 0.95f, 0.5f)), nextInstanceId = 2)

        val transitions = transitionsFor(state, WorkspaceEvent.Move(1, 1.2f, 0.5f))

        assertEquals(listOf(ItemTransition(1, TransitionKind.VANISH, 0.95f, 0.5f)), transitions)
    }

    @Test
    fun moving_inside_the_workspace_has_no_transition() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "fire", 0.5f, 0.5f)), nextInstanceId = 2)

        assertEquals(emptyList<ItemTransition>(), transitionsFor(state, WorkspaceEvent.Move(1, 0.6f, 0.5f)))
    }

    @Test
    fun clearing_vanishes_every_item() {
        val state = WorkspaceState(
            items = listOf(WorkspaceItem(1, "fire", 0.2f, 0.2f), WorkspaceItem(2, "water", 0.8f, 0.8f)),
            nextInstanceId = 3
        )

        assertEquals(
            listOf(
                ItemTransition(1, TransitionKind.VANISH, 0.2f, 0.2f),
                ItemTransition(2, TransitionKind.VANISH, 0.8f, 0.8f)
            ),
            transitionsFor(state, WorkspaceEvent.Clear)
        )
    }

    @Test
    fun combination_leaves_the_effect_to_the_combination() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "water", 0.5f, 0.5f)), nextInstanceId = 2)

        assertEquals(emptyList<ItemTransition>(), transitionsFor(state, WorkspaceEvent.Spawn("fire", 0.5f, 0.5f)))
    }
}
