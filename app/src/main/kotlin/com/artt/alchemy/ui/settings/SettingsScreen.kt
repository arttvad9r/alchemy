package com.artt.alchemy.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.AlchemyToggle
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor

@Composable
fun SettingsScreen(
    state: AlchemyUiState,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onMusicChanged: (Boolean) -> Unit,
    onRequestReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onDismissReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings")
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenBanner(stringResource(R.string.settings))
        SettingsPanel {
            SettingToggle(
                label = stringResource(R.string.sound),
                iconRes = R.drawable.ic_audio,
                enabled = state.progress.soundEnabled,
                tag = "settings_sound",
                onChanged = onSoundChanged
            )
            SettingToggle(
                label = stringResource(R.string.vibration),
                iconRes = R.drawable.ic_haptics,
                enabled = state.progress.vibrationEnabled,
                tag = "settings_vibration",
                onChanged = onVibrationChanged
            )
            SettingToggle(
                label = stringResource(R.string.music),
                iconRes = R.drawable.ic_music,
                enabled = state.progress.musicEnabled,
                tag = "settings_music",
                onChanged = onMusicChanged
            )
        }
        SettingsPanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.ic_help), contentDescription = null, modifier = Modifier.size(SETTING_ICON_SIZE))
                Text(stringResource(R.string.help), style = MaterialTheme.typography.titleLarge)
            }
            Text(stringResource(R.string.help_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AlchemyButton(
            text = stringResource(R.string.reset_progress),
            style = ButtonStyle.RED,
            onClick = onRequestReset,
            modifier = Modifier.fillMaxWidth().testTag("settings_reset")
        )
    }
    if (state.isResetConfirmationVisible) {
        AlchemyDialog(onDismissRequest = onDismissReset, panelRes = R.drawable.dialog_blue) {
            Text(stringResource(R.string.reset_confirmation_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.reset_confirmation_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AlchemyButton(stringResource(R.string.cancel), ButtonStyle.DARK, onDismissReset)
                AlchemyButton(stringResource(R.string.reset_confirm), ButtonStyle.RED, onConfirmReset)
            }
        }
    }
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelColor, RoundedCornerShape(16.dp))
            .border(1.dp, PanelBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SettingToggle(label: String, iconRes: Int, enabled: Boolean, tag: String, onChanged: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(SETTING_ICON_SIZE))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        AlchemyToggle(
            checked = enabled,
            onCheckedChange = onChanged,
            modifier = Modifier
                .testTag(tag)
                .semantics { contentDescription = label }
        )
    }
}

private val SETTING_ICON_SIZE = 32.dp
