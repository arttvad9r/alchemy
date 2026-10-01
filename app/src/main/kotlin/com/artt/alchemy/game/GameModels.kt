package com.artt.alchemy.game

enum class ElementGroup(val color: Long) {
    NATURE(0xFFF05B47L),
    MATERIAL(0xFFC78B42L),
    LIFE(0xFF5A9A5AL),
    CIVILIZATION(0xFF5D86C9L),
    COSMOS(0xFF835AC7L)
}

data class ElementDefinition(
    val id: String,
    val group: ElementGroup,
    val color: Long
)

data class Recipe(
    val firstId: String,
    val secondId: String,
    val resultId: String
)

fun recipeKey(firstId: String, secondId: String): String = listOf(firstId, secondId).sorted().joinToString(separator = "|")

data class WorkspaceItem(
    val instanceId: Long,
    val elementId: String,
    val xFraction: Float,
    val yFraction: Float
)

data class WorkspaceState(
    val items: List<WorkspaceItem> = emptyList(),
    val nextInstanceId: Long = 1
)

data class Combination(
    val firstId: String,
    val secondId: String,
    val resultId: String
)

data class WorkspaceResult(
    val workspace: WorkspaceState,
    val combination: Combination? = null,
    val attemptedMix: Boolean = false
)

/** The upper or lower half of the workspace. */
enum class BoardHalf {
    UPPER,
    LOWER
}

sealed interface WorkspaceEvent {
    data class Spawn(val elementId: String, val xFraction: Float, val yFraction: Float) : WorkspaceEvent

    /** Puts the element in the freest spot, outside [keepClear] when that half holds something the player must see. */
    data class SpawnAutomatically(val elementId: String, val keepClear: BoardHalf? = null) : WorkspaceEvent

    data class Move(val instanceId: Long, val xFraction: Float, val yFraction: Float) : WorkspaceEvent

    data class Remove(val instanceId: Long) : WorkspaceEvent

    data object Clear : WorkspaceEvent

    data class ResolveOverlap(val draggedInstanceId: Long, val xFraction: Float, val yFraction: Float) : WorkspaceEvent
}
