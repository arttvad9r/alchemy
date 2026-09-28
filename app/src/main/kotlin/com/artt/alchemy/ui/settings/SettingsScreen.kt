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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor

@Composable
fun SettingsScreen(
    state: AlchemyUiState,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.nav_settings), contentDescription = null, modifier = Modifier.size(40.dp))
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, color = Gold)
        }
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
        }
        SettingsPanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.ic_help), contentDescription = null, modifier = Modifier.size(SETTING_ICON_SIZE))
                Text(stringResource(R.string.help), style = MaterialTheme.typography.titleLarge)
            }
            Text(stringResource(R.string.help_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onRequestReset,
            colors = ButtonDefaults.buttonColors(containerColor = RESET_COLOR, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("settings_reset")
        ) {
            Text(stringResource(R.string.reset_progress))
        }
    }
    if (state.isResetConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDismissReset,
            title = { Text(stringResource(R.string.reset_confirmation_title)) },
            text = { Text(stringResource(R.string.reset_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmReset) { Text(stringResource(R.string.reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissReset) { Text(stringResource(R.string.cancel)) }
            }
        )
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
        Switch(
            checked = enabled,
            onCheckedChange = onChanged,
            modifier = Modifier
                .testTag(tag)
                .semantics { contentDescription = label }
        )
    }
}

private val SETTING_ICON_SIZE = 32.dp
private val RESET_COLOR = Color(0xFFB3263A)
