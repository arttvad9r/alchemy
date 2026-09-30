package com.artt.alchemy.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.TransferResult
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.AlchemyIconButton
import com.artt.alchemy.ui.components.AlchemySlider
import com.artt.alchemy.ui.components.AlchemyToggle
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.components.ScreenPadding
import com.artt.alchemy.ui.components.systemAnimationsOff
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: AlchemyUiState,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onMusicChanged: (Boolean) -> Unit,
    onRequestReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onDismissReset: () -> Unit,
    onMusicVolumeChanged: (Float) -> Unit,
    onEffectsVolumeChanged: (Float) -> Unit,
    onEffectsVolumeFinished: () -> Unit,
    onReducedMotionChanged: (Boolean) -> Unit,
    onExport: (Uri) -> Unit,
    onImportPicked: (Uri) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImport: () -> Unit,
    onDismissTransferResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportPicked)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings")
            .padding(ScreenPadding)
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
            VolumeSlider(
                label = stringResource(R.string.effects_volume),
                iconRes = R.drawable.ic_audio,
                value = state.progress.effectsVolume,
                tag = "settings_effects_volume",
                onChange = onEffectsVolumeChanged,
                onFinished = onEffectsVolumeFinished
            )
            VolumeSlider(
                label = stringResource(R.string.music_volume),
                iconRes = R.drawable.ic_music,
                value = state.progress.musicVolume,
                tag = "settings_music_volume",
                onChange = onMusicVolumeChanged
            )
        }
        SettingsPanel {
            val reduced = state.progress.reducedMotion ?: systemAnimationsOff(LocalContext.current)
            SettingToggle(
                label = stringResource(R.string.reduced_motion),
                iconRes = null,
                enabled = reduced,
                tag = "settings_reduced_motion",
                onChanged = onReducedMotionChanged
            )
            Text(stringResource(R.string.reduced_motion_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsPanel {
            Text(stringResource(R.string.progress_transfer), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.progress_transfer_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AlchemyButton(
                    text = stringResource(R.string.export_progress),
                    style = ButtonStyle.BLUE,
                    onClick = { exportLauncher.launch(EXPORT_FILE_NAME) },
                    modifier = Modifier.weight(1f).testTag("settings_export")
                )
                AlchemyButton(
                    text = stringResource(R.string.import_progress),
                    style = ButtonStyle.BLUE,
                    onClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
                    modifier = Modifier.weight(1f).testTag("settings_import")
                )
            }
        }
        SettingsPanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.ic_help), contentDescription = null, modifier = Modifier.size(SETTING_ICON_SIZE))
                Text(stringResource(R.string.help), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.help_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(stringResource(R.string.reset_confirmation_title), style = MaterialTheme.typography.titleLarge, color = Gold, textAlign = TextAlign.Center)
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
    state.pendingImport?.let { pending ->
        AlchemyDialog(onDismissRequest = onDismissImport, panelRes = R.drawable.dialog_blue) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.testTag("import_dialog")) {
                Text(stringResource(R.string.import_confirmation_title), style = MaterialTheme.typography.titleLarge, color = Gold, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.import_confirmation_message, pending.unlockedIds.size, AlchemyCatalog.elements.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AlchemyButton(stringResource(R.string.cancel), ButtonStyle.DARK, onDismissImport, Modifier.testTag("import_cancel"))
                    AlchemyButton(stringResource(R.string.import_confirm), ButtonStyle.RED, onConfirmImport, Modifier.testTag("import_confirm"))
                }
            }
        }
    }
    state.transferResult?.let { result ->
        AlchemyDialog(onDismissRequest = onDismissTransferResult, panelRes = R.drawable.dialog_blue) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.testTag("transfer_dialog")) {
                Text(
                    stringResource(result.messageRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AlchemyIconButton(R.drawable.ic_check, stringResource(R.string.ok), onDismissTransferResult, Modifier.testTag("transfer_ok"))
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
            .background(PanelColor, RoundedCornerShape(20.dp))
            .border(1.dp, PanelBorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SettingToggle(label: String, iconRes: Int?, enabled: Boolean, tag: String, onChanged: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (iconRes != null) Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(SETTING_ICON_SIZE))
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

@Composable
private fun VolumeSlider(
    label: String,
    iconRes: Int,
    value: Float,
    tag: String,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(SETTING_ICON_SIZE))
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AlchemySlider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onFinished,
            modifier = Modifier.fillMaxWidth().testTag(tag).semantics { contentDescription = label }
        )
    }
}

private val TransferResult.messageRes: Int
    get() = when (this) {
        TransferResult.EXPORTED -> R.string.transfer_exported
        TransferResult.EXPORT_FAILED -> R.string.transfer_export_failed
        TransferResult.IMPORTED -> R.string.transfer_imported
        TransferResult.IMPORT_INVALID -> R.string.transfer_import_invalid
    }

private const val EXPORT_FILE_NAME = "alchemy-progress.json"
private const val EXPORT_MIME_TYPE = "application/json"
private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/*", "*/*")
private val SETTING_ICON_SIZE = 32.dp
