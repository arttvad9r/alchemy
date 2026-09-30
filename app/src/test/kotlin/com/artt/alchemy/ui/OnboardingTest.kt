package com.artt.alchemy.ui

import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.AlchemyEngine
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.reduce
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingTest {
    private val engine = AlchemyEngine(AlchemyCatalog)

    private fun after(step: Int, state: WorkspaceState, event: WorkspaceEvent): Pair<Int, WorkspaceState> {
        val result = reduce(state, event, engine)
        return tipAfter(step, event, state, result) to result.workspace
    }

    @Test
    fun eachTipWaitsForItsAction() {
        val (afterTap, one) = after(0, WorkspaceState(), WorkspaceEvent.SpawnAutomatically("fire"))
        assertEquals(1, afterTap)

        val (afterMove, moved) = after(1, one, WorkspaceEvent.Move(one.items.single().instanceId, 0.3f, 0.3f))
        assertEquals(1, afterMove)

        val (afterMix, mixed) = after(1, moved, WorkspaceEvent.Spawn("water", 0.3f, 0.3f))
        assertEquals(2, afterMix)

        val (afterClear, _) = after(2, mixed, WorkspaceEvent.Clear)
        assertEquals(2, afterClear)

        val (afterRemoval, _) = after(2, mixed, WorkspaceEvent.Move(mixed.items.single().instanceId, 1.4f, 0.5f))
        assertEquals(TIP_COUNT, afterRemoval)
    }

    @Test
    fun droppingStraightOntoAnotherElementCompletesTwoTipsAtOnce() {
        val (_, one) = after(0, WorkspaceState(), WorkspaceEvent.Spawn("fire", 0.5f, 0.5f))
        val (step, _) = after(0, one, WorkspaceEvent.Spawn("water", 0.5f, 0.5f))
        assertEquals(2, step)
    }

    @Test
    fun movingWithoutMixingDoesNotAdvanceTheFirstTip() {
        val (_, one) = after(1, WorkspaceState(), WorkspaceEvent.SpawnAutomatically("fire"))
        val (step, _) = after(0, one, WorkspaceEvent.Move(one.items.single().instanceId, 0.2f, 0.2f))
        assertEquals(0, step)
    }
}
