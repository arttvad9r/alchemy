package com.artt.alchemy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.ui.AlchemyUiState

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
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        SettingToggle(
            label = stringResource(R.string.sound),
            enabled = state.progress.soundEnabled,
            tag = "settings_sound",
            onChanged = onSoundChanged
        )
        SettingToggle(
            label = stringResource(R.string.vibration),
            enabled = state.progress.vibrationEnabled,
            tag = "settings_vibration",
            onChanged = onVibrationChanged
        )
        Text(stringResource(R.string.help), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.help_text))
        Button(
            onClick = onRequestReset,
            modifier = Modifier.testTag("settings_reset")
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
private fun SettingToggle(label: String, enabled: Boolean, tag: String, onChanged: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = enabled,
            onCheckedChange = onChanged,
            modifier = Modifier
                .testTag(tag)
                .semantics { contentDescription = label }
        )
    }
}
