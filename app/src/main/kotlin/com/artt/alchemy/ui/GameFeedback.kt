package com.artt.alchemy.ui

import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceResult
import com.artt.alchemy.game.WorkspaceState

/** What a workspace event meant to the player, answered with a sound and a vibration. */
enum class GameFeedback {
    PLACE,
    COMBINE,
    DISCOVER,
    DISCOVER_GRAND,
    NO_MATCH,
    REMOVE,
    CLEAR
}

/**
 * The single feedback for [event], the most telling one when an event did several things:
 * an element dropped onto another is a mix, not a placement. Plain moves give none.
 * [discovered] is the rarity of an element opened for the first time by this event, if any.
 */
fun workspaceFeedback(event: WorkspaceEvent, before: WorkspaceState, result: WorkspaceResult, discovered: ElementRarity?): GameFeedback? {
    val beforeIds = before.items.mapTo(HashSet()) { it.instanceId }
    val afterIds = result.workspace.items.mapTo(HashSet()) { it.instanceId }
    return when {
        result.combination != null -> discoveryFeedback(discovered)
        result.attemptedMix -> GameFeedback.NO_MATCH
        event is WorkspaceEvent.Clear -> GameFeedback.CLEAR.takeIf { before.items.isNotEmpty() }
        !afterIds.containsAll(beforeIds) -> GameFeedback.REMOVE
        !beforeIds.containsAll(afterIds) -> GameFeedback.PLACE
        else -> null
    }
}

private fun discoveryFeedback(discovered: ElementRarity?): GameFeedback = when (discovered) {
    null -> GameFeedback.COMBINE
    ElementRarity.EPIC, ElementRarity.LEGENDARY -> GameFeedback.DISCOVER_GRAND
    else -> GameFeedback.DISCOVER
}
