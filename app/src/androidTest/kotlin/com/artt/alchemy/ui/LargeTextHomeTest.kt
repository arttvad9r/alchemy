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
import com.artt.alchemy.data.AppTheme
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.ui.home.HomeScreen
import com.artt.alchemy.ui.theme.AlchemyTheme
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
                AlchemyTheme(AppTheme.AETHER) {
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
                            onSkipTips = {},
                            onShowTips = {}
                        )
                    }
                }
            }
        }
        listOf("Открытые элементы", "Воздух", "Земля", "Огонь", "Вода").forEach { text ->
            if (text != "Открытые элементы") {
                composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
            }
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("$text must fit at twice the system text size", layouts.single().hasVisualOverflow)
        }
    }
}
