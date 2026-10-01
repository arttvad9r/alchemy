package com.artt.alchemy.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundMixTest {
    @Test
    fun centreIsFullVolumeOnBothSides() {
        assertEquals(1f to 1f, stereoGains(0f))
    }

    @Test
    fun edgesLeanWithoutSilencingTheOtherSide() {
        val (left, right) = stereoGains(-1f)
        assertEquals(1f, left)
        assertTrue(right in 0.3f..0.9f)
        assertEquals(stereoGains(-1f), stereoGains(1f).let { it.second to it.first })
    }

    @Test
    fun panFollowsTheWorkspaceWidth() {
        assertEquals(-1f, panAt(0f))
        assertEquals(0f, panAt(0.5f))
        assertEquals(1f, panAt(1f))
        assertEquals(1f, panAt(1.4f))
    }

    @Test
    fun twelveSemitonesDoubleTheRate() {
        assertEquals(1f, semitoneRate(0f), 1e-6f)
        assertEquals(2f, semitoneRate(12f), 1e-5f)
    }

    @Test
    fun quickMixesClimbToACeiling() {
        val streak = ComboStreak(windowMillis = 1000, maxSteps = 2)
        assertEquals(listOf(0, 1, 2, 2), listOf(0L, 500L, 900L, 1500L).map(streak::hit))
    }

    @Test
    fun aPauseOrAMissStartsOver() {
        val streak = ComboStreak(windowMillis = 1000)
        streak.hit(0)
        assertEquals(1, streak.hit(800))
        assertEquals(0, streak.hit(2000))
        streak.hit(2100)
        streak.reset()
        assertEquals(0, streak.hit(2200))
    }
}
