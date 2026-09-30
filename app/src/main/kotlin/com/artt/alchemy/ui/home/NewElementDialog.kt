package com.artt.alchemy.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementLinks
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.FactText
import com.artt.alchemy.ui.components.FinalBadge
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.components.LocalReducedMotion
import com.artt.alchemy.ui.components.RarityBadge
import com.artt.alchemy.ui.components.elementFact
import com.artt.alchemy.ui.components.elementName
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.components.rarity
import com.artt.alchemy.ui.theme.Gold
import kotlinx.coroutines.delay

private const val ICON_DELAY_MILLIS = 80L
private const val BADGE_DELAY_MILLIS = 320L
private const val BADGE_FADE_MILLIS = 250
private const val ICON_START_SCALE = 0.3f
private const val GLOW_SIZE = 220
private const val GLOW_ALPHA = 0.85f

/** The card for an element opened for the first time: the icon springs in over a glow, the rarity badge follows. */
@Composable
fun NewElementDialog(element: ElementDefinition, onDismiss: () -> Unit, onClick: () -> Unit) {
    val iconIn = remember { Animatable(0f) }
    val badgeIn = remember { Animatable(0f) }
    val reducedMotion = LocalReducedMotion.current
    val iconSpec = motion(spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    val badgeSpec = motion(tween<Float>(BADGE_FADE_MILLIS))
    LaunchedEffect(Unit) {
        if (!reducedMotion) delay(ICON_DELAY_MILLIS)
        iconIn.animateTo(1f, iconSpec)
    }
    LaunchedEffect(Unit) {
        if (!reducedMotion) delay(BADGE_DELAY_MILLIS)
        badgeIn.animateTo(1f, badgeSpec)
    }
    AlchemyDialog(onDismissRequest = onDismiss, panelRes = R.drawable.dialog_gold) {
        // Scrolls on small screens with large text; the button stays below it, always in reach.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.new_element_title), style = MaterialTheme.typography.headlineSmall, color = Gold)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                Image(
                    painter = painterResource(if (element.rarity == ElementRarity.EPIC) R.drawable.fx_glow_purple_orb else R.drawable.fx_glow_gold_orb),
                    contentDescription = null,
                    modifier = Modifier
                        .size(GLOW_SIZE.dp)
                        .graphicsLayer { alpha = iconIn.value * GLOW_ALPHA }
                )
                FramedElementIcon(
                    element,
                    Modifier
                        .width(140.dp)
                        .scale(ICON_START_SCALE + (1f - ICON_START_SCALE) * iconIn.value)
                        .graphicsLayer { alpha = iconIn.value.coerceIn(0f, 1f) }
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 8.dp).graphicsLayer { alpha = badgeIn.value }
            ) {
                RarityBadge(element.rarity)
                if (element.id in ElementLinks.finalElementIds) FinalBadge()
            }
            Text(stringResource(R.string.new_element_message, elementName(element.id)), textAlign = TextAlign.Center)
            FactText(elementFact(element.id), Modifier.padding(top = 8.dp, bottom = 16.dp))
        }
        AlchemyButton(stringResource(R.string.ok), ButtonStyle.GOLD, onClick = {
            onClick()
            onDismiss()
        })
    }
}
