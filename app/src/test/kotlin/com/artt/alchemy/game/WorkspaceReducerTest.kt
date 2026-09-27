package com.artt.alchemy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceReducerTest {
    private val engine = AlchemyEngine(AlchemyCatalog)

    @Test
    fun spawn_creates_a_temporary_instance_without_consuming_unlock() {
        val result = reduce(WorkspaceState(), WorkspaceEvent.Spawn("fire", 0.2f, 0.3f), engine)

        assertEquals(listOf(WorkspaceItem(1, "fire", 0.2f, 0.3f)), result.workspace.items)
        assertEquals(2, result.workspace.nextInstanceId)
        assertNull(result.combination)
    }

    @Test
    fun invalid_overlap_keeps_both_workspace_items() {
        val state = WorkspaceState(
            items = listOf(WorkspaceItem(1, "fire", 0.3f, 0.3f), WorkspaceItem(2, "fire", 0.3f, 0.3f)),
            nextInstanceId = 3
        )

        val result = reduce(state, WorkspaceEvent.ResolveOverlap(2, 0.3f, 0.3f), engine)

        assertEquals(state, result.workspace)
        assertNull(result.combination)
    }

    @Test
    fun successful_overlap_removes_exactly_two_items_and_spawns_result_at_contact() {
        val state = WorkspaceState(
            items = listOf(
                WorkspaceItem(1, "fire", 0.3f, 0.3f),
                WorkspaceItem(2, "water", 0.3f, 0.3f),
                WorkspaceItem(3, "earth", 0.8f, 0.8f)
            ),
            nextInstanceId = 4
        )

        val result = reduce(state, WorkspaceEvent.ResolveOverlap(2, 0.35f, 0.35f), engine)

        assertEquals(listOf(WorkspaceItem(3, "earth", 0.8f, 0.8f), WorkspaceItem(4, "steam", 0.35f, 0.35f)), result.workspace.items)
        assertEquals(5, result.workspace.nextInstanceId)
        assertEquals(Combination("fire", "water", "steam"), result.combination)
        assertEquals(true, result.attemptedMix)
    }

    @Test
    fun spawning_onto_an_ingredient_resolves_the_recipe_without_leaving_an_overlap() {
        val state = WorkspaceState(items = listOf(WorkspaceItem(1, "fire", 0.5f, 0.5f)), nextInstanceId = 2)

        val result = reduce(state, WorkspaceEvent.Spawn("water", 0.5f, 0.5f), engine)

        assertEquals(listOf(WorkspaceItem(3, "steam", 0.5f, 0.5f)), result.workspace.items)
        assertEquals(Combination("fire", "water", "steam"), result.combination)
        assertEquals(true, result.attemptedMix)
    }

    @Test
    fun nearby_but_non_overlapping_items_do_not_combine_or_count_as_an_attempt() {
        val state = WorkspaceState(
            items = listOf(WorkspaceItem(1, "fire", 0.3f, 0.3f), WorkspaceItem(2, "water", 0.45f, 0.3f)),
            nextInstanceId = 3
        )

        val result = reduce(state, WorkspaceEvent.ResolveOverlap(2, 0.45f, 0.3f), engine)

        assertEquals(state, result.workspace)
        assertNull(result.combination)
        assertEquals(false, result.attemptedMix)
    }

    @Test
    fun moving_outside_bounds_removes_only_that_workspace_item() {
        val state = WorkspaceState(
            items = listOf(WorkspaceItem(1, "fire", 0.2f, 0.2f), WorkspaceItem(2, "water", 0.8f, 0.8f)),
            nextInstanceId = 3
        )

        val result = reduce(state, WorkspaceEvent.Move(1, -0.01f, 0.2f), engine)

        assertEquals(listOf(WorkspaceItem(2, "water", 0.8f, 0.8f)), result.workspace.items)
    }

    @Test
    fun clear_removes_all_workspace_items() {
        val result = reduce(
            WorkspaceState(items = listOf(WorkspaceItem(1, "fire", 0.2f, 0.2f)), nextInstanceId = 2),
            WorkspaceEvent.Clear,
            engine
        )

        assertEquals(WorkspaceState(nextInstanceId = 2), result.workspace)
    }
}
