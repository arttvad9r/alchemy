package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceResult
import com.artt.alchemy.game.WorkspaceState

enum class TransitionKind {
    APPEAR,
    VANISH,

    // Swept away with the rest by clearing the workspace: housekeeping, so quieter than a removal.
    SWEEP,
    SHAKE
}

/** An element that came onto, left, or was refused on the workspace, marked with a short effect at its position. */
data class ItemTransition(
    val instanceId: Long,
    val kind: TransitionKind,
    val xFraction: Float,
    val yFraction: Float
)

/** A transition at one moment of its playback; [origin] is where an appearing item flies in from, in workspace fractions. */
data class TransitionFrame(val transition: ItemTransition, val progress: Float, val origin: Offset? = null)

/** An element that merges into a combination's result. */
data class EffectSource(val elementId: String, val xFraction: Float, val yFraction: Float)

/**
 * Items an event added to or removed from the workspace, and the one refused when a mix found no recipe.
 * A combination has its own effect, so it adds none.
 * A removed item keeps its last position inside the workspace, where it was before being dragged out.
 */
fun itemTransitions(before: WorkspaceState, result: WorkspaceResult, event: WorkspaceEvent): List<ItemTransition> {
    if (result.combination != null) return emptyList()
    val beforeIds = before.items.mapTo(HashSet()) { it.instanceId }
    val afterIds = result.workspace.items.mapTo(HashSet()) { it.instanceId }
    val appeared = result.workspace.items
        .filterNot { it.instanceId in beforeIds }
        .map { ItemTransition(it.instanceId, TransitionKind.APPEAR, it.xFraction, it.yFraction) }
    val vanished = before.items
        .filterNot { it.instanceId in afterIds }
        .map { ItemTransition(it.instanceId, if (event is WorkspaceEvent.Clear) TransitionKind.SWEEP else TransitionKind.VANISH, it.xFraction, it.yFraction) }
    val refusedId = when {
        !result.attemptedMix -> null
        event is WorkspaceEvent.ResolveOverlap -> event.draggedInstanceId
        else -> appeared.singleOrNull()?.instanceId
    }
    val refused = result.workspace.items
        .filter { it.instanceId == refusedId }
        .map { ItemTransition(it.instanceId, TransitionKind.SHAKE, it.xFraction, it.yFraction) }
    return appeared + vanished + refused
}

/**
 * The two elements a combination merges at their actual source positions. A palette drop has no
 * source in the previous workspace, so its frame starts at the result's position.
 */
fun effectSources(before: WorkspaceState, result: WorkspaceResult): List<EffectSource> {
    val combination = result.combination ?: return emptyList()
    val merged = result.workspace.items.last()
    val afterIds = result.workspace.items.mapTo(HashSet()) { it.instanceId }
    val dropped = EffectSource(combination.secondId, merged.xFraction, merged.yFraction)
    val removed = before.items.filterNot { it.instanceId in afterIds }
    val target = removed.firstOrNull { it.elementId == combination.firstId } ?: return listOf(dropped)
    val source = removed.firstOrNull { it.instanceId != target.instanceId && it.elementId == combination.secondId }
    val sourceFrame = source?.let { EffectSource(it.elementId, it.xFraction, it.yFraction) } ?: dropped
    return listOf(EffectSource(target.elementId, target.xFraction, target.yFraction), sourceFrame)
}
