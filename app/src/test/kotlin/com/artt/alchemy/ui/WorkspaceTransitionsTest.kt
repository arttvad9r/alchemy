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

    private fun transitionsFor(state: WorkspaceState, event: WorkspaceEvent) = itemTransitions(state, reduce(state, event, engine), event)

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
                ItemTransition(1, TransitionKind.SWEEP, 0.2f, 0.2f),
                ItemTransition(2, TransitionKind.SWEEP, 0.8f, 0.8f)
            ),
            transitionsFor(state, WorkspaceEvent.Clear)
        )
    }

    @Test
    fun combination_leaves_the_effect_to_the_combination() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "water", 0.5f, 0.5f)), nextInstanceId = 2)

        assertEquals(emptyList<ItemTransition>(), transitionsFor(state, WorkspaceEvent.Spawn("fire", 0.5f, 0.5f)))
    }

    @Test
    fun refused_mix_shakes_the_dragged_item() {
        val state = WorkspaceState(
            items = listOf(WorkspaceItem(1, "fire", 0.5f, 0.5f), WorkspaceItem(2, "fire", 0.9f, 0.9f)),
            nextInstanceId = 3
        )

        val transitions = transitionsFor(state, WorkspaceEvent.ResolveOverlap(2, 0.5f, 0.5f))

        assertEquals(listOf(ItemTransition(2, TransitionKind.SHAKE, 0.9f, 0.9f)), transitions)
    }

    @Test
    fun spawned_item_that_finds_no_recipe_appears_and_shakes() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "fire", 0.5f, 0.5f)), nextInstanceId = 2)

        val transitions = transitionsFor(state, WorkspaceEvent.Spawn("fire", 0.5f, 0.5f))

        assertEquals(
            listOf(
                ItemTransition(2, TransitionKind.APPEAR, 0.5f, 0.5f),
                ItemTransition(2, TransitionKind.SHAKE, 0.5f, 0.5f)
            ),
            transitions
        )
    }

    @Test
    fun dragged_element_merges_where_it_was_dropped_and_the_other_slides_in() {
        val state = WorkspaceState(
            items = listOf(WorkspaceItem(1, "water", 0.4f, 0.4f), WorkspaceItem(2, "fire", 0.45f, 0.45f)),
            nextInstanceId = 3
        )

        val sources = effectSources(state, reduce(state, WorkspaceEvent.ResolveOverlap(2, 0.45f, 0.45f), engine))

        assertEquals(listOf(EffectSource("water", 0.4f, 0.4f), EffectSource("fire", 0.45f, 0.45f)), sources)
    }

    @Test
    fun spawned_element_merges_with_the_one_it_landed_on() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "water", 0.5f, 0.5f)), nextInstanceId = 2)

        val sources = effectSources(state, reduce(state, WorkspaceEvent.Spawn("fire", 0.5f, 0.5f), engine))

        assertEquals(listOf(EffectSource("water", 0.5f, 0.5f), EffectSource("fire", 0.5f, 0.5f)), sources)
    }

    @Test
    fun no_combination_has_no_sources() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "fire", 0.1f, 0.1f)), nextInstanceId = 2)

        assertEquals(emptyList<EffectSource>(), effectSources(state, reduce(state, WorkspaceEvent.Spawn("water", 0.9f, 0.9f), engine)))
    }
}
