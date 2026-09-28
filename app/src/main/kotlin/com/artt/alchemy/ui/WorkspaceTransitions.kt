package com.artt.alchemy.ui

import com.artt.alchemy.game.WorkspaceResult
import com.artt.alchemy.game.WorkspaceState

enum class TransitionKind {
    APPEAR,
    VANISH
}

/** An element that came onto or left the workspace, marked with a short effect at its position. */
data class ItemTransition(
    val instanceId: Long,
    val kind: TransitionKind,
    val xFraction: Float,
    val yFraction: Float
)

/**
 * Items an event added to or removed from the workspace. A combination has its own effect, so it adds none.
 * A removed item keeps its last position inside the workspace, where it was before being dragged out.
 */
fun itemTransitions(before: WorkspaceState, result: WorkspaceResult): List<ItemTransition> {
    if (result.combination != null) return emptyList()
    val beforeIds = before.items.mapTo(HashSet()) { it.instanceId }
    val afterIds = result.workspace.items.mapTo(HashSet()) { it.instanceId }
    val appeared = result.workspace.items
        .filterNot { it.instanceId in beforeIds }
        .map { ItemTransition(it.instanceId, TransitionKind.APPEAR, it.xFraction, it.yFraction) }
    val vanished = before.items
        .filterNot { it.instanceId in afterIds }
        .map { ItemTransition(it.instanceId, TransitionKind.VANISH, it.xFraction, it.yFraction) }
    return appeared + vanished
}
