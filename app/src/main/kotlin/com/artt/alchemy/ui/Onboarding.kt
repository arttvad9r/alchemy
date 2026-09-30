package com.artt.alchemy.ui

import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceResult
import com.artt.alchemy.game.WorkspaceState

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

private fun tipDone(step: Int, placed: Boolean, mixed: Boolean, removed: Boolean): Boolean = when (step) {
    0 -> placed
    1 -> mixed
    else -> removed
}
