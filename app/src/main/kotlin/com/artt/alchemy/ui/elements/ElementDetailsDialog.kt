package com.artt.alchemy.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.elementFacts
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.components.RarityBadge
import com.artt.alchemy.ui.components.rarity
import com.artt.alchemy.ui.theme.Gold

/** An open element up close, with its interesting fact. */
@Composable
fun ElementDetailsDialog(element: ElementDefinition, onDismiss: () -> Unit) {
    AlchemyDialog(onDismissRequest = onDismiss, panelRes = R.drawable.dialog_blue) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.verticalScroll(rememberScrollState()).testTag("element_details")
        ) {
            FramedElementIcon(element, Modifier.width(120.dp))
            Text(element.name, style = MaterialTheme.typography.headlineSmall, color = Gold, modifier = Modifier.padding(top = 8.dp))
            RarityBadge(element.rarity, Modifier.padding(top = 4.dp))
            Text(
                elementFacts.getValue(element.id),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            AlchemyButton(stringResource(R.string.close), ButtonStyle.BLUE, onDismiss, Modifier.padding(top = 16.dp).testTag("element_details_close"))
        }
    }
}
