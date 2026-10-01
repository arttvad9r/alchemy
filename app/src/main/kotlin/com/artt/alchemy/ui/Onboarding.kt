package com.artt.alchemy.ui

import com.artt.alchemy.game.BoardHalf
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.game.WorkspaceResult
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.halfOf

const val TIP_COUNT = 3

/**
 * The first-run tip to show after [event], when tip [step] was on screen. Each tip gives way once the player has done
 * what it asks: put an element on the workspace, drop one onto another, drag one off the edge. One event can complete
 * several tips in a row. [TIP_COUNT] means the tips are done.
 */
fun tipAfter(step: Int, event: WorkspaceEvent, before: WorkspaceState, result: WorkspaceResult): Int {
    val beforeIds = before.items.mapTo(HashSet()) { it.instanceId }
    val afterIds = result.workspace.items.mapTo(HashSet()) { it.instanceId }
    val placed = event is WorkspaceEvent.Spawn || event is WorkspaceEvent.SpawnAutomatically
    val removed = event is WorkspaceEvent.Move && !afterIds.containsAll(beforeIds)
    var next = step
    while (next < TIP_COUNT && tipDone(next, placed, result.attemptedMix, removed)) next++
    return next
}

/**
 * The half of the workspace where a tip after the first sits: the one with fewer elements in it. Elements placed
 * automatically keep out of it, so the tip never hides what the player is about to handle.
 */
fun tipHalf(items: List<WorkspaceItem>): BoardHalf {
    val upper = items.count { halfOf(it.yFraction) == BoardHalf.UPPER }
    val lower = items.count { halfOf(it.yFraction) == BoardHalf.LOWER }
    return if (upper <= lower) BoardHalf.UPPER else BoardHalf.LOWER
}

private fun tipDone(step: Int, placed: Boolean, mixed: Boolean, removed: Boolean): Boolean = when (step) {
    0 -> placed
    1 -> mixed
    else -> removed
}
