package com.artt.alchemy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.ui.home.HomeScreen
import com.artt.alchemy.ui.theme.AlchemyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class LargeTextHomeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun largeSystemTextKeepsThePaletteHeadingAndBasicNamesReadable() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 2f)) {
                AlchemyTheme {
                    Box(Modifier.size(360.dp, 600.dp)) {
                        HomeScreen(
                            state = AlchemyUiState(progress = initialPlayerProgress().copy(onboardingSeen = true)),
                            onEvent = {},
                            onDismissNewElement = {},
                            onPickUp = {},
                            onClick = {},
                            onPaletteSort = {},
                            onEffectConsumed = {},
                            onTransitionsConsumed = {},
                            onSkipTips = {}

                        )
                    }
                }
            }
        }
        // The heading and the motto under the game's name sit above the palette; the names are in it.
        val headings = setOf("Открыто", "Соединяй · Открывай · Создавай")
        (headings + listOf("Воздух", "Земля", "Огонь", "Вода")).forEach { text ->
            if (text !in headings) {
                composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
            }
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("$text must fit at twice the system text size", layouts.single().hasVisualOverflow)
            if (text !in headings) assertEquals("A basic name must not break inside a word", 1, layouts.single().lineCount)
        }
    }
}
