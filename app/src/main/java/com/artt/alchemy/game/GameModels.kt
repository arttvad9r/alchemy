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
    val name: String,
    val group: ElementGroup,
    val color: Long
)

data class Recipe(
    val firstId: String,
    val secondId: String,
    val resultId: String
)

fun recipeKey(firstId: String, secondId: String): String = listOf(firstId, secondId).sorted().joinToString(separator = "|")
