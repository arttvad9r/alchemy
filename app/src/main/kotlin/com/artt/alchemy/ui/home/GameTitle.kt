package com.artt.alchemy.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.ui.components.WholeWordsAutoSize
import com.artt.alchemy.ui.components.shimmer

private val TitleGradient = Brush.verticalGradient(listOf(Color(0xFFFFEFB8), Color(0xFFF3C35B), Color(0xFFD08A26)))
private val TitleShadowOffset = Offset(0f, 3f)
private const val TITLE_SHADOW_BLUR = 10f
private val LOGO_SIZE = 64.dp
private val TITLE_SIZE = 38.sp
private val TAGLINE_SIZE = 13.sp
private val TITLE_MIN_SIZE = 10.sp
private val TAGLINE_MIN_SIZE = 9.sp

/**
 * The game's name as on the concept: a golden serif wordmark beside the spell book, with the motto under it. [actions]
 * sit on the name's line, so the motto runs under them across the whole width and fits in one line.
 * Like the book, the wordmark and motto ignore the system font scale, and the name may shrink far: with a large font the
 * actions grow and leave the name little room, yet "Алхимия" must stay whole. The actions keep the system scale.
 */
@Composable
fun GameTitle(modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val density = LocalDensity.current
    val unscaled = Density(density.density, fontScale = 1f)
    // A shadow in the theme's darkest colour keeps the gold readable on any background.
    val titleShadow = Shadow(color = MaterialTheme.colorScheme.background, offset = TitleShadowOffset, blurRadius = TITLE_SHADOW_BLUR)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(LOGO_SIZE))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalDensity provides unscaled) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            brush = TitleGradient,
                            shadow = titleShadow,
                            fontSize = TITLE_SIZE,
                            lineHeight = TITLE_SIZE
                        ),
                        maxLines = 1,
                        autoSize = WholeWordsAutoSize(min = TITLE_MIN_SIZE, max = TITLE_SIZE),
                        // Now and then a gleam runs across the gold, as over polished metal.
                        modifier = Modifier.weight(1f).shimmer()
                    )
                }
                actions()
            }
            CompositionLocalProvider(LocalDensity provides unscaled) {
                // The motto is shown whole or not at all: cut short, it reads as a mistake.
                var taglineFits by remember { mutableStateOf(true) }
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = TAGLINE_SIZE, fontWeight = FontWeight.Medium, shadow = titleShadow),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    autoSize = WholeWordsAutoSize(min = TAGLINE_MIN_SIZE, max = TAGLINE_SIZE),
                    onTextLayout = { taglineFits = !it.hasVisualOverflow },
                    modifier = Modifier.alpha(if (taglineFits) 1f else 0f).testTag("game_tagline")
                )
            }
        }
    }
}
