package com.artt.alchemy.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Shrinks text from [max] towards [min] until it fits its lines and no line break falls inside a word,
 * so a narrow screen or large font never shows "Энерг-ия". At [min] the text is shown as it lays out.
 */
data class WholeWordsAutoSize(val min: TextUnit, val max: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var size = max.value
        while (size > min.value) {
            val layout = performLayout(constraints, text, size.sp)
            if (!layout.hasVisualOverflow && layout.breaksOnlyBetweenWords(text)) return size.sp
            size -= STEP
        }
        return min
    }
}

private fun TextLayoutResult.breaksOnlyBetweenWords(text: AnnotatedString): Boolean = (0 until lineCount - 1).all { line ->
    val end = getLineEnd(line)
    end == 0 || end >= text.length || text[end - 1].isWhitespace() || text[end].isWhitespace()
}

private const val STEP = 0.5f
