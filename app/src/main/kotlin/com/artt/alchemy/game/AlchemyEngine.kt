package com.artt.alchemy.game

class AlchemyEngine(private val catalog: AlchemyCatalog) {
    fun combine(firstId: String, secondId: String): String? = catalog.recipeResultsByKey[recipeKey(firstId, secondId)]
}
