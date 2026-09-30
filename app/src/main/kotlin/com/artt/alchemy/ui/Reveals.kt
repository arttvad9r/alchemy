package com.artt.alchemy.ui

/** A moment the game stages for the player, one at a time: a new element's card, the finished collection, an achievement banner. */
sealed interface Reveal {
    data class Discovery(val elementId: String) : Reveal

    data object Completion : Reveal

    data class Achievement(val id: String) : Reveal
}

// A discovery follows its mix at once; the finished collection comes before the banners it earned, which can wait.
private val Reveal.stage: Int
    get() = when (this) {
        is Reveal.Discovery -> 0
        Reveal.Completion -> 1
        is Reveal.Achievement -> 2
    }

/**
 * The queue with [new] reveals added in stage order: discoveries first, then the finished collection, then achievement
 * banners, each kind in the order it happened. A reveal already waiting is not added twice.
 */
fun List<Reveal>.enqueue(new: List<Reveal>): List<Reveal> = (this + new).distinct().sortedBy { it.stage }
