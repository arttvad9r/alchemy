package com.artt.alchemy.game

private const val OVERLAP_DISTANCE_SQUARED = 0.02f
private const val MIDDLE = 0.5f

private val automaticSpawnCandidates = listOf(
    0.5f to 0.5f,
    0.1f to 0.1f, 0.9f to 0.1f, 0.1f to 0.9f, 0.9f to 0.9f,
    0.5f to 0.1f, 0.5f to 0.9f, 0.1f to 0.5f, 0.9f to 0.5f,
    0.3f to 0.3f, 0.7f to 0.3f, 0.3f to 0.7f, 0.7f to 0.7f,
    0.3f to 0.1f, 0.7f to 0.1f, 0.3f to 0.9f, 0.7f to 0.9f,
    0.1f to 0.3f, 0.9f to 0.3f, 0.1f to 0.7f, 0.9f to 0.7f,
    0.5f to 0.3f, 0.5f to 0.7f, 0.3f to 0.5f, 0.7f to 0.5f
)

fun reduce(state: WorkspaceState, event: WorkspaceEvent, engine: AlchemyEngine): WorkspaceResult = when (event) {
    is WorkspaceEvent.Spawn -> {
        val item = WorkspaceItem(state.nextInstanceId, event.elementId, event.xFraction, event.yFraction)
        resolveOverlap(
            state.copy(items = state.items + item, nextInstanceId = state.nextInstanceId + 1),
            WorkspaceEvent.ResolveOverlap(item.instanceId, event.xFraction, event.yFraction),
            engine
        )
    }

    // A tapped element is only put down: on a crowded workspace its spot may touch a neighbour, but it never mixes there.
    is WorkspaceEvent.SpawnAutomatically -> {
        val (xFraction, yFraction) = automaticSpawnPosition(state.items, event.keepClear)
        val item = WorkspaceItem(state.nextInstanceId, event.elementId, xFraction, yFraction)
        WorkspaceResult(state.copy(items = state.items + item, nextInstanceId = state.nextInstanceId + 1))
    }

    is WorkspaceEvent.Move -> {
        val item = state.items.find { it.instanceId == event.instanceId } ?: return WorkspaceResult(state)
        val items = if (event.xFraction !in 0f..1f || event.yFraction !in 0f..1f) {
            state.items - item
        } else {
            state.items.map { current ->
                if (current.instanceId == item.instanceId) current.copy(xFraction = event.xFraction, yFraction = event.yFraction) else current
            }
        }
        WorkspaceResult(state.copy(items = items))
    }

    is WorkspaceEvent.Remove -> WorkspaceResult(state.copy(items = state.items.filterNot { it.instanceId == event.instanceId }))

    WorkspaceEvent.Clear -> WorkspaceResult(state.copy(items = emptyList()))

    is WorkspaceEvent.ResolveOverlap -> resolveOverlap(state, event, engine)
}

private fun automaticSpawnPosition(items: List<WorkspaceItem>, keepClear: BoardHalf?): Pair<Float, Float> {
    val allowed = automaticSpawnCandidates.filterNot { (_, y) -> keepClear != null && halfOf(y) == keepClear }
    return allowed.maxBy { (x, y) ->
        items.minOfOrNull { item -> squaredDistance(item.xFraction, item.yFraction, x, y) } ?: Float.MAX_VALUE
    }
}

/** The half a row of the workspace lies in; the middle row belongs to neither. */
fun halfOf(yFraction: Float): BoardHalf? = when {
    yFraction < MIDDLE -> BoardHalf.UPPER
    yFraction > MIDDLE -> BoardHalf.LOWER
    else -> null
}

private fun resolveOverlap(state: WorkspaceState, event: WorkspaceEvent.ResolveOverlap, engine: AlchemyEngine): WorkspaceResult {
    val dragged = state.items.find { it.instanceId == event.draggedInstanceId } ?: return WorkspaceResult(state)
    val target = overlapTarget(state.items, dragged.instanceId, event.xFraction, event.yFraction) ?: return WorkspaceResult(state)
    val resultId = engine.combine(dragged.elementId, target.elementId) ?: return WorkspaceResult(state, attemptedMix = true)
    val result = WorkspaceItem(state.nextInstanceId, resultId, event.xFraction, event.yFraction)

    return WorkspaceResult(
        workspace = state.copy(
            items = state.items.filterNot { it.instanceId == dragged.instanceId || it.instanceId == target.instanceId } + result,
            nextInstanceId = state.nextInstanceId + 1
        ),
        combination = Combination(target.elementId, dragged.elementId, resultId),
        attemptedMix = true
    )
}

/**
 * The item a dragged one dropped at this position would mix with, by the same rule the reducer applies: the nearest in
 * reach, and of equally near ones the last added, which is drawn on top.
 */
fun overlapTarget(items: List<WorkspaceItem>, draggedInstanceId: Long, xFraction: Float, yFraction: Float): WorkspaceItem? = items
    .asReversed()
    .filter { it.instanceId != draggedInstanceId }
    .map { it to squaredDistance(it.xFraction, it.yFraction, xFraction, yFraction) }
    .filter { (_, distance) -> distance <= OVERLAP_DISTANCE_SQUARED }
    .minByOrNull { (_, distance) -> distance }
    ?.first

private fun squaredDistance(firstX: Float, firstY: Float, secondX: Float, secondY: Float): Float = (firstX - secondX) * (firstX - secondX) + (firstY - secondY) * (firstY - secondY)
