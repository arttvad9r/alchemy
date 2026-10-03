package com.artt.alchemy.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.panel
import com.artt.alchemy.ui.theme.panelBorder
import kotlin.math.roundToInt

/** The parts of the settings that open on a page of their own, so the main page holds only what is changed often. */
private enum class SettingsPage { MAIN, LANGUAGE, TRANSFER, HELP }

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
    onMusicVolumeFinished: () -> Unit,
    onEffectsVolumeChanged: (Float) -> Unit,
    onEffectsVolumeFinished: () -> Unit,
    onExport: (Uri) -> Unit,
    onImportPicked: (Uri) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImport: () -> Unit,
    onDismissTransferResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    BackHandler(enabled = page != SettingsPage.MAIN) { page = SettingsPage.MAIN }
    Crossfade(targetState = page, animationSpec = motion(tween(PAGE_FADE_MILLIS)), label = "settingsPage", modifier = modifier) { shown ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(if (shown == SettingsPage.MAIN) "screen_settings" else "settings_page_${shown.name.lowercase()}")
                .padding(ScreenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (shown) {
                SettingsPage.MAIN -> MainPage(
                    state = state,
                    onSoundChanged = onSoundChanged,
                    onVibrationChanged = onVibrationChanged,
                    onMusicChanged = onMusicChanged,
                    onMusicVolumeChanged = onMusicVolumeChanged,
                    onMusicVolumeFinished = onMusicVolumeFinished,
                    onEffectsVolumeChanged = onEffectsVolumeChanged,
                    onEffectsVolumeFinished = onEffectsVolumeFinished,
                    onOpen = { page = it },
                    onRequestReset = onRequestReset
                )

                SettingsPage.LANGUAGE -> {
                    PageHeader(stringResource(R.string.language), onBack = { page = SettingsPage.MAIN })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) LanguagePage()
                }

                SettingsPage.TRANSFER -> {
                    PageHeader(stringResource(R.string.progress_transfer_title), onBack = { page = SettingsPage.MAIN })
                    TransferPage(onExport, onImportPicked)
                }

                SettingsPage.HELP -> {
                    PageHeader(stringResource(R.string.help), onBack = { page = SettingsPage.MAIN })
                    SettingsPanel {
                        Text(stringResource(R.string.help_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
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
private fun MainPage(
    state: AlchemyUiState,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onMusicChanged: (Boolean) -> Unit,
    onMusicVolumeChanged: (Float) -> Unit,
    onMusicVolumeFinished: () -> Unit,
    onEffectsVolumeChanged: (Float) -> Unit,
    onEffectsVolumeFinished: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
    onRequestReset: () -> Unit
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
            muted = !state.progress.soundEnabled,
            tag = "settings_effects_volume",
            onChange = onEffectsVolumeChanged,
            onFinished = onEffectsVolumeFinished
        )
        VolumeSlider(
            label = stringResource(R.string.music_volume),
            iconRes = R.drawable.ic_music,
            value = state.progress.musicVolume,
            muted = !state.progress.musicEnabled,
            tag = "settings_music_volume",
            onChange = onMusicVolumeChanged,
            onFinished = onMusicVolumeFinished
        )
    }
    // Each line is a full touch target, so the lines need no gaps between them.
    SettingsPanel(spacing = 0.dp) {
        // Per-app language exists from Android 13; older systems follow the system language.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val context = LocalContext.current
            PageRow(stringResource(R.string.language), languageName(appLanguage(context)), "settings_language") { onOpen(SettingsPage.LANGUAGE) }
        }
        PageRow(stringResource(R.string.progress_transfer), null, "settings_transfer") { onOpen(SettingsPage.TRANSFER) }
        PageRow(stringResource(R.string.help), null, "settings_help") { onOpen(SettingsPage.HELP) }
    }
    AlchemyButton(
        text = stringResource(R.string.reset_progress),
        style = ButtonStyle.RED,
        onClick = onRequestReset,
        modifier = Modifier.fillMaxWidth().testTag("settings_reset")
    )
}

/** A line of the main page that opens one of the other pages; [value] shows what is chosen there. */
@Composable
private fun PageRow(label: String, value: String?, tag: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NAV_ROW_MIN_HEIGHT)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Image(painterResource(R.drawable.ic_forward), contentDescription = null, modifier = Modifier.size(FORWARD_ICON_SIZE))
    }
}

/** The page's title on the ribbon, with a way back to the main page. */
@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) {
        // The ribbon keeps clear of the back button on both sides, so it stays centred.
        ScreenBanner(title, Modifier.padding(horizontal = BACK_BUTTON_CLEARANCE))
        AlchemyIconButton(R.drawable.ic_back, stringResource(R.string.back), onBack, Modifier.testTag("settings_back"))
    }
}

/** The game's own language choice, kept by the system as the app's language (Android 13+). */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LanguagePage() {
    val context = LocalContext.current
    // Changing it recreates the activity, so the choice shown is read afresh each time.
    val current = appLanguage(context)
    SettingsPanel {
        Column(modifier = Modifier.selectableGroup()) {
            AppLanguages.forEach { tag ->
                val selected = tag == current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = NAV_ROW_MIN_HEIGHT)
                        .selectable(selected = selected, role = Role.RadioButton) { if (!selected) setAppLanguage(context, tag) }
                        .testTag("language_${tag ?: "system"}")
                ) {
                    Image(painterResource(if (selected) R.drawable.radio_on else R.drawable.radio_off), contentDescription = null, modifier = Modifier.size(RADIO_SIZE))
                    Text(
                        languageName(tag),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) Gold else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferPage(onExport: (Uri) -> Unit, onImportPicked: (Uri) -> Unit) {
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportPicked)
    }
    SettingsPanel {
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
}

// Null follows the system language; the others are in res/xml/locales_config.xml.
private val AppLanguages = listOf(null, "ru", "en")

/** A language by its own name, the way people look for it; null is the system's. */
@Composable
private fun languageName(tag: String?): String = when (tag) {
    null -> stringResource(R.string.language_system)
    "ru" -> "Русский"
    else -> "English"
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun appLanguage(context: Context): String? = context.getSystemService(LocaleManager::class.java).applicationLocales.takeUnless { it.isEmpty }?.get(0)?.language

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setAppLanguage(context: Context, tag: String?) {
    context.getSystemService(LocaleManager::class.java).applicationLocales = tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
}

@Composable
private fun SettingsPanel(spacing: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.panel, shape)
            .border(1.dp, MaterialTheme.colorScheme.panelBorder, shape)
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

@Composable
private fun VolumeSlider(
    label: String,
    iconRes: Int,
    value: Float,
    muted: Boolean,
    tag: String,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit = {}
) {
    // With its sound switched off the slider dims but still moves, so the volume can be set ahead of turning it on.
    val alpha by animateFloatAsState(if (muted) MUTED_ALPHA else 1f, motion(tween(MUTE_FADE_MILLIS)), label = "muted")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.alpha(alpha)) {
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

private val RADIO_SIZE = 20.dp
private val FORWARD_ICON_SIZE = 20.dp
private val BACK_BUTTON_CLEARANCE = 48.dp
private val NAV_ROW_MIN_HEIGHT = 48.dp
private const val PAGE_FADE_MILLIS = 180

private const val EXPORT_FILE_NAME = "alchemy-progress.json"
private const val EXPORT_MIME_TYPE = "application/json"
private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/*", "*/*")
private val SETTING_ICON_SIZE = 32.dp
private const val MUTED_ALPHA = 0.45f
private const val MUTE_FADE_MILLIS = 200
