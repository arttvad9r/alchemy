package com.artt.alchemy.ui.achievements

import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {
    private val start = initialPlayerProgress()

    @Test
    fun crossing_a_target_completes_the_achievement_once() {
        val before = start.copy(mixAttemptCount = 49)
        val after = before.copy(mixAttemptCount = 50)
        assertEquals(listOf("experiments"), newlyCompletedAchievements(before, after))
        assertTrue(newlyCompletedAchievements(after, after.copy(mixAttemptCount = 51)).isEmpty())
    }

    @Test
    fun progress_that_completes_nothing_gives_no_achievements() {
        assertTrue(newlyCompletedAchievements(start, start.copy(mixAttemptCount = 3)).isEmpty())
    }

    @Test
    fun several_achievements_completed_together_come_in_list_order() {
        val found = AlchemyCatalog.elements.map { it.id }.filterNot { it in AlchemyCatalog.baseElementIds }.take(10)
        val before = start.copy(successfulMixCount = 24)
        val after = before.copy(successfulMixCount = 25, unlockedIds = start.unlockedIds + found)
        assertEquals(listOf("first_unlocks", "mixes"), newlyCompletedAchievements(before, after))
    }

    @Test
    fun a_reset_completes_nothing() {
        val done = start.copy(mixAttemptCount = 80)
        assertTrue(newlyCompletedAchievements(done, start).isEmpty())
    }
}
