package com.artt.alchemy.game

import org.junit.Assert.assertTrue
import org.junit.Test

class ElementTextsTest {
    private val languages = mapOf("ru" to ElementResources.russian, "en" to ElementResources.english)

    @Test
    fun every_element_has_a_name_and_a_fact_in_every_language() {
        languages.forEach { (language, texts) ->
            val missing = AlchemyCatalog.elements.map { it.id }.filter { texts.name(it).isNullOrBlank() || texts.fact(it).isNullOrBlank() }
            assertTrue("Missing in $language: $missing", missing.isEmpty())
        }
    }

    @Test
    fun facts_are_short_enough_to_read_at_a_glance() {
        languages.forEach { (language, texts) ->
            val tooLong = AlchemyCatalog.elements.map { it.id }.filter { texts.fact(it).orEmpty().length > MAX_FACT_LENGTH }
            assertTrue("Too long in $language: $tooLong", tooLong.isEmpty())
        }
    }

    @Test
    fun english_texts_are_translated_rather_than_copied() {
        val cyrillic = Regex("[А-Яа-яЁё]")
        val copied = AlchemyCatalog.elements.map { it.id }.filter {
            cyrillic.containsMatchIn(ElementResources.english.name(it).orEmpty()) || cyrillic.containsMatchIn(ElementResources.english.fact(it).orEmpty())
        }
        assertTrue("Cyrillic in English texts: $copied", copied.isEmpty())
    }

    private companion object {
        const val MAX_FACT_LENGTH = 130
    }
}
