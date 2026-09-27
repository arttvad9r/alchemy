package com.artt.alchemy.game

private const val OVERLAP_DISTANCE_SQUARED = 0.02f

fun reduce(state: WorkspaceState, event: WorkspaceEvent, engine: AlchemyEngine): WorkspaceResult = when (event) {
    is WorkspaceEvent.Spawn -> {
        val item = WorkspaceItem(state.nextInstanceId, event.elementId, event.xFraction, event.yFraction)
        resolveOverlap(
            state.copy(items = state.items + item, nextInstanceId = state.nextInstanceId + 1),
            WorkspaceEvent.ResolveOverlap(item.instanceId, event.xFraction, event.yFraction),
            engine
        )
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

private fun resolveOverlap(state: WorkspaceState, event: WorkspaceEvent.ResolveOverlap, engine: AlchemyEngine): WorkspaceResult {
    val dragged = state.items.find { it.instanceId == event.draggedInstanceId } ?: return WorkspaceResult(state)
    val target = state.items.firstOrNull { item ->
        item.instanceId != dragged.instanceId &&
            squaredDistance(item.xFraction, item.yFraction, event.xFraction, event.yFraction) <= OVERLAP_DISTANCE_SQUARED
    } ?: return WorkspaceResult(state)
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

private fun squaredDistance(firstX: Float, firstY: Float, secondX: Float, secondY: Float): Float = (firstX - secondX) * (firstX - secondX) + (firstY - secondY) * (firstY - secondY)
