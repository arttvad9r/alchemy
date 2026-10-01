package com.artt.alchemy.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RevealsTest {
    @Test
    fun aDiscoveryGoesAheadOfBannersAlreadyWaiting() {
        val waiting = listOf(Reveal.Achievement("first_mix"))
        assertEquals(
            listOf(Reveal.Discovery("steam"), Reveal.Achievement("first_mix"), Reveal.Achievement("ten")),
            waiting.enqueue(listOf(Reveal.Discovery("steam"), Reveal.Achievement("ten")))
        )
    }

    @Test
    fun theLastMixStagesTheCardThenTheFinaleThenTheBanners() {
        val last = listOf(Reveal.Achievement("all_elements"), Reveal.Completion, Reveal.Discovery("stone"))
        assertEquals(
            listOf(Reveal.Discovery("stone"), Reveal.Completion, Reveal.Achievement("all_elements")),
            emptyList<Reveal>().enqueue(last)
        )
    }

    @Test
    fun aRevealAlreadyWaitingIsNotQueuedTwice() {
        assertEquals(listOf(Reveal.Completion), listOf(Reveal.Completion).enqueue(listOf(Reveal.Completion)))
    }
}
