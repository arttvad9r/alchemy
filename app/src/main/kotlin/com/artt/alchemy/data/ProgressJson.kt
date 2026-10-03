package com.artt.alchemy.data

import com.artt.alchemy.game.ElementSort
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val PROGRESS_VERSION = 1

fun PlayerProgress.toJson(): String = JSONObject().apply {
    put("version", PROGRESS_VERSION)
    put("unlockedIds", JSONArray(unlockedIds.toList()))
    put("discoveryOrder", JSONArray(discoveryOrder))
    put("knownRecipeKeys", JSONArray(knownRecipeKeys.toList()))
    put("successfulMixCount", successfulMixCount)
    put("mixAttemptCount", mixAttemptCount)
    put("soundEnabled", soundEnabled)
    put("vibrationEnabled", vibrationEnabled)
    put("musicEnabled", musicEnabled)
    put("musicVolume", musicVolume.toDouble())
    put("effectsVolume", effectsVolume.toDouble())
    put("onboardingSeen", onboardingSeen)
    put("paletteSort", paletteSort.name)
    activeHint?.let { put("activeHint", JSONObject().put("recipeKey", it.recipeKey).put("step", it.step)) }
}.toString()

/** Reads progress written by [toJson]; returns null when the text is not a supported save. */
fun parsePlayerProgress(text: String): PlayerProgress? = try {
    val json = JSONObject(text)
    val unlocked = json.optJSONArray("unlockedIds")
    if (json.optInt("version") != PROGRESS_VERSION || unlocked == null) {
        null
    } else {
        val defaults = initialPlayerProgress()
        PlayerProgress(
            unlockedIds = unlocked.strings().toSet(),
            discoveryOrder = json.optJSONArray("discoveryOrder")?.strings().orEmpty(),
            knownRecipeKeys = json.optJSONArray("knownRecipeKeys")?.strings().orEmpty().toSet(),
            successfulMixCount = json.optInt("successfulMixCount"),
            mixAttemptCount = json.optInt("mixAttemptCount"),
            soundEnabled = json.optBoolean("soundEnabled", defaults.soundEnabled),
            vibrationEnabled = json.optBoolean("vibrationEnabled", defaults.vibrationEnabled),
            musicEnabled = json.optBoolean("musicEnabled", defaults.musicEnabled),
            musicVolume = json.optDouble("musicVolume", 1.0).toFloat(),
            effectsVolume = json.optDouble("effectsVolume", 1.0).toFloat(),
            onboardingSeen = json.optBoolean("onboardingSeen"),
            paletteSort = ElementSort.entries.firstOrNull { it.name == json.optString("paletteSort") } ?: defaults.paletteSort,
            activeHint = json.optJSONObject("activeHint")?.let {
                ActiveHint(recipeKey = it.optString("recipeKey"), step = it.optInt("step"))
            }
        ).sanitized()
    }
} catch (_: JSONException) {
    null
}

private fun JSONArray.strings(): List<String> = (0 until length()).mapNotNull { optString(it).takeIf(String::isNotEmpty) }
