package com.artt.alchemy.game

import com.artt.alchemy.data.ActiveHint
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.parsePlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.data.toJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressJsonTest {
    private val played: PlayerProgress = initialPlayerProgress()
        .recordAttempt(Combination("fire", "water", "steam"))
        .recordAttempt(null)
        .copy(
            soundEnabled = false,
            musicVolume = 0.25f,
            effectsVolume = 0.5f,
            reducedMotion = true,
            onboardingSeen = true,
            paletteSort = ElementSort.GROUP,
            activeHint = ActiveHint(recipeKey("earth", "water"), step = 2)
        )

    @Test
    fun progress_survives_a_round_trip() {
        assertEquals(played, parsePlayerProgress(played.toJson()))
    }

    @Test
    fun following_the_system_motion_setting_survives_a_round_trip() {
        val system = played.copy(reducedMotion = null, activeHint = null)

        assertEquals(system, parsePlayerProgress(system.toJson()))
    }

    @Test
    fun unknown_elements_recipes_and_hints_are_dropped() {
        val text = """
            {"version":1,"unlockedIds":["fire","no_such_element","steam"],"discoveryOrder":["steam","no_such_element"],
            "knownRecipeKeys":["fire|water","a|b"],"activeHint":{"recipeKey":"a|b","step":1}}
        """.trimIndent()

        val progress = parsePlayerProgress(text)!!

        assertEquals(setOf("fire", "water", "earth", "air", "steam"), progress.unlockedIds)
        assertEquals(listOf("steam", "fire", "water", "earth", "air"), progress.discoveryOrder)
        assertEquals(setOf(recipeKey("fire", "water")), progress.knownRecipeKeys)
        assertNull(progress.activeHint)
    }

    @Test
    fun text_that_is_not_a_supported_save_is_rejected() {
        listOf("", "not json", "[]", "{}", """{"version":2,"unlockedIds":[]}""", """{"version":1}""").forEach { text ->
            assertNull(text, parsePlayerProgress(text))
        }
    }

    @Test
    fun a_save_without_settings_gets_defaults() {
        val progress = parsePlayerProgress("""{"version":1,"unlockedIds":["fire"]}""")!!

        assertEquals(initialPlayerProgress(), progress)
    }
}
