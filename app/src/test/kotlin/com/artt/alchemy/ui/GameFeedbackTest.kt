package com.artt.alchemy.ui

import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.AlchemyEngine
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.reduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameFeedbackTest {
    private val engine = AlchemyEngine(AlchemyCatalog)

    private fun feedbackFor(state: WorkspaceState, event: WorkspaceEvent, discovered: ElementRarity? = null) = workspaceFeedback(event, state, reduce(state, event, engine), discovered)

    private val fireOnly = WorkspaceState(items = listOf(WorkspaceItem(1, "fire", 0.5f, 0.5f)), nextInstanceId = 2)

    @Test
    fun spawning_on_free_space_places() {
        assertEquals(GameFeedback.PLACE, feedbackFor(WorkspaceState(), WorkspaceEvent.Spawn("fire", 0.2f, 0.3f)))
    }

    @Test
    fun spawning_onto_an_ingredient_is_a_mix_not_a_placement() {
        assertEquals(GameFeedback.COMBINE, feedbackFor(fireOnly, WorkspaceEvent.Spawn("water", 0.5f, 0.5f)))
        assertEquals(GameFeedback.DISCOVER, feedbackFor(fireOnly, WorkspaceEvent.Spawn("water", 0.5f, 0.5f), discovered = ElementRarity.COMMON))
        assertEquals(GameFeedback.DISCOVER, feedbackFor(fireOnly, WorkspaceEvent.Spawn("water", 0.5f, 0.5f), discovered = ElementRarity.RARE))
        assertEquals(GameFeedback.DISCOVER_GRAND, feedbackFor(fireOnly, WorkspaceEvent.Spawn("water", 0.5f, 0.5f), discovered = ElementRarity.EPIC))
        assertEquals(GameFeedback.DISCOVER_GRAND, feedbackFor(fireOnly, WorkspaceEvent.Spawn("water", 0.5f, 0.5f), discovered = ElementRarity.LEGENDARY))
    }

    @Test
    fun invalid_pair_is_no_match() {
        val state = WorkspaceState(
            items = listOf(WorkspaceItem(1, "fire", 0.3f, 0.3f), WorkspaceItem(2, "fire", 0.3f, 0.3f)),
            nextInstanceId = 3
        )

        assertEquals(GameFeedback.NO_MATCH, feedbackFor(state, WorkspaceEvent.ResolveOverlap(2, 0.3f, 0.3f)))
    }

    @Test
    fun dragging_out_removes() {
        assertEquals(GameFeedback.REMOVE, feedbackFor(fireOnly, WorkspaceEvent.Move(1, 1.2f, 0.5f)))
    }

    @Test
    fun clearing_gives_feedback_only_when_something_was_there() {
        assertEquals(GameFeedback.CLEAR, feedbackFor(fireOnly, WorkspaceEvent.Clear))
        assertNull(feedbackFor(WorkspaceState(), WorkspaceEvent.Clear))
    }

    @Test
    fun moving_inside_gives_none() {
        assertNull(feedbackFor(fireOnly, WorkspaceEvent.Move(1, 0.6f, 0.5f)))
    }
}
