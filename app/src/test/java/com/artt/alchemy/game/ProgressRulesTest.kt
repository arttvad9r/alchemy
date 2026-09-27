package com.artt.alchemy.game

import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.data.reset
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRulesTest {
    @Test
    fun initial_progress_contains_only_the_four_base_elements() {
        assertEquals(AlchemyCatalog.baseElementIds, initialPlayerProgress().unlockedIds)
    }

    @Test
    fun first_successful_recipe_unlocks_result_and_marks_recipe_known() {
        val progress = initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam"))

        assertEquals(AlchemyCatalog.baseElementIds + "steam", progress.unlockedIds)
        assertEquals(setOf(recipeKey("fire", "water")), progress.knownRecipeKeys)
        assertEquals(1, progress.successfulMixCount)
        assertEquals(1, progress.mixAttemptCount)
    }

    @Test
    fun repeated_successful_recipe_does_not_add_a_second_unique_unlock() {
        val first = initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam"))
        val repeated = first.recordAttempt(Combination("water", "fire", "steam"))

        assertEquals(AlchemyCatalog.baseElementIds + "steam", repeated.unlockedIds)
        assertEquals(1, repeated.knownRecipeKeys.size)
        assertEquals(2, repeated.successfulMixCount)
    }

    @Test
    fun reset_restores_initial_progress() {
        val changed = initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam"))

        assertEquals(initialPlayerProgress(), changed.reset())
    }
}
