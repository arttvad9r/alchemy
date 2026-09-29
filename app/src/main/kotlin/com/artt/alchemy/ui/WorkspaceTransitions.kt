package com.artt.alchemy.ui

import androidx.compose.ui.geometry.Offset
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceResult
import com.artt.alchemy.game.WorkspaceState

enum class TransitionKind {
    APPEAR,
    VANISH,
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
        .map { ItemTransition(it.instanceId, TransitionKind.VANISH, it.xFraction, it.yFraction) }
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
 * The two elements a combination merges: the one dropped stays at the result's position, the other
 * slides in from where it lay.
 */
fun effectSources(before: WorkspaceState, result: WorkspaceResult): List<EffectSource> {
    val combination = result.combination ?: return emptyList()
    val merged = result.workspace.items.last()
    val afterIds = result.workspace.items.mapTo(HashSet()) { it.instanceId }
    val dropped = EffectSource(combination.secondId, merged.xFraction, merged.yFraction)
    val target = before.items
        .filterNot { it.instanceId in afterIds }
        .maxByOrNull { (it.xFraction - merged.xFraction).let { dx -> dx * dx } + (it.yFraction - merged.yFraction).let { dy -> dy * dy } }
        ?: return listOf(dropped)
    return listOf(EffectSource(combination.firstId, target.xFraction, target.yFraction), dropped)
}
