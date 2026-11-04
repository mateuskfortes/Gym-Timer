package com.example.gymtimer2.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunTemperatureUnit
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.ThemeMode
import com.example.gymtimer2.ui.screens.runs.components.RunManualLapsSetting
import com.example.gymtimer2.ui.theme.tone

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (AccentColor) -> Unit,
    onRunDistanceUnitChanged: (RunDistanceUnit) -> Unit,
    onRunTemperatureUnitChanged: (RunTemperatureUnit) -> Unit,
    onRunStartEndHideDistanceChanged: (RunStartEndHideDistance) -> Unit,
    runPrivacyZones: List<RunPrivacyZone>,
    onSaveRunPrivacyZone: (RunPrivacyZone) -> Unit,
    onDeleteRunPrivacyZone: (Long) -> Unit,
    onRunOffRouteAlertsChanged: (Boolean) -> Unit,
    onRunOffRouteNotificationsChanged: (Boolean) -> Unit,
    onRunAutoPauseChanged: (Boolean) -> Unit,
    onRunKeepScreenAwakeChanged: (Boolean) -> Unit,
    onRunScreenDimTimeoutChanged: (Long) -> Unit,
    onRunLockScreenControlsChanged: (Boolean) -> Unit,
    onRunManualLapsChanged: (Boolean) -> Unit,
    onRunStepRateSensorChanged: (Boolean) -> Unit,
    onRunLiveSegmentsChanged: (Boolean) -> Unit,
    isRunLiveSegmentsEditable: Boolean,
    onRunDisplayAllTurnCuesChanged: (Boolean) -> Unit,
    onRunUpcomingTurnCueCountChanged: (Int) -> Unit,
    onRunBodyMassKgChanged: (Double?) -> Unit,
    onRunWeatherChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp)
    ) {
        SettingsAppearance(settings, onThemeSelected, onAccentSelected)
        Spacer(Modifier.height(20.dp))
        RunDistanceUnitSetting(settings.runDistanceUnit, onRunDistanceUnitChanged)
        RunTemperatureUnitSetting(settings.runTemperatureUnit, onRunTemperatureUnitChanged)
        RunStartEndHideDistanceSetting(
            selectedDistance = settings.runStartEndHideDistance,
            distanceUnit = settings.runDistanceUnit,
            onDistanceSelected = onRunStartEndHideDistanceChanged
        )
        RunPrivacyZonesSetting(
            zones = runPrivacyZones,
            distanceUnit = settings.runDistanceUnit,
            onSave = onSaveRunPrivacyZone,
            onDelete = onDeleteRunPrivacyZone
        )
        RunManualLapsSetting(settings.runManualLapsEnabled, onRunManualLapsChanged,
            description = stringResource(R.string.run_manual_laps_description))
        RunSwitchSetting(
            title = stringResource(R.string.run_step_rate_sensor_setting),
            description = stringResource(R.string.run_step_rate_sensor_description),
            checked = settings.runStepRateSensorEnabled,
            onCheckedChange = onRunStepRateSensorChanged
        )
        RunSwitchSetting(
            title = stringResource(R.string.run_live_segments_setting),
            description = stringResource(R.string.run_live_segments_description),
            checked = settings.runLiveSegmentsEnabled,
            onCheckedChange = onRunLiveSegmentsChanged,
            enabled = isRunLiveSegmentsEditable
        )
        RunBodyMassSetting(settings.runBodyMassKg, onRunBodyMassKgChanged)
        RunSwitchSetting(
            title = stringResource(R.string.run_weather_setting),
            description = stringResource(R.string.run_weather_setting_description),
            checked = settings.runWeatherEnabled,
            onCheckedChange = onRunWeatherChanged
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val alertLabel = stringResource(R.string.run_off_route_alerts_setting)
            Text(alertLabel, modifier = Modifier.weight(1f))
            Switch(
                checked = settings.runOffRouteAlertsEnabled,
                onCheckedChange = onRunOffRouteAlertsChanged,
                modifier = Modifier.semantics { contentDescription = alertLabel }
            )
        }
        Text(
            stringResource(R.string.run_off_route_alerts_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RunSwitchSetting(
            title = stringResource(R.string.run_off_route_notifications_setting),
            description = stringResource(R.string.run_off_route_notifications_description),
            checked = settings.runOffRouteNotificationsEnabled,
            onCheckedChange = onRunOffRouteNotificationsChanged,
            enabled = settings.runOffRouteAlertsEnabled
        )
        RunSwitchSetting(
            title = stringResource(R.string.run_turn_display_all_cues_setting),
            description = stringResource(R.string.run_turn_display_all_cues_description),
            checked = settings.runDisplayAllTurnCues,
            onCheckedChange = onRunDisplayAllTurnCuesChanged
        )
        RunTurnCueCountSetting(
            count = settings.runUpcomingTurnCueCount,
            enabled = !settings.runDisplayAllTurnCues,
            onCountChanged = onRunUpcomingTurnCueCountChanged
        )
        RunDeviceSettings(
            autoPauseEnabled = settings.runAutoPause,
            onAutoPauseChanged = onRunAutoPauseChanged,
            keepScreenAwake = settings.runKeepScreenAwake,
            onKeepScreenAwakeChanged = onRunKeepScreenAwakeChanged,
            screenDimTimeoutMs = settings.runScreenDimTimeoutMs,
            onScreenDimTimeoutChanged = onRunScreenDimTimeoutChanged,
            lockScreenControls = settings.runShowControlsOnLockScreen,
            onLockScreenControlsChanged = onRunLockScreenControlsChanged
        )
    }
}

@Composable
private fun SettingsAppearance(
    settings: AppSettings,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (AccentColor) -> Unit
) {
    Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeOption(
            title = stringResource(R.string.settings_light_theme),
            icon = Icons.Default.LightMode,
            selected = settings.themeMode == ThemeMode.LIGHT,
            onClick = { onThemeSelected(ThemeMode.LIGHT) },
            modifier = Modifier.weight(1f)
        )
        ThemeOption(
            title = stringResource(R.string.settings_dark_theme),
            icon = Icons.Default.DarkMode,
            selected = settings.themeMode == ThemeMode.DARK,
            onClick = { onThemeSelected(ThemeMode.DARK) },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(28.dp))
    Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.settings_accent_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AccentColor.entries.chunked(4).forEach { colors ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { color ->
                    Box(Modifier.weight(1f)) {
                        AccentOption(
                            color = color,
                            darkTheme = settings.themeMode == ThemeMode.DARK,
                            selected = settings.accentColor == color,
                            onClick = { onAccentSelected(color) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunDeviceSettings(
    autoPauseEnabled: Boolean,
    onAutoPauseChanged: (Boolean) -> Unit,
    keepScreenAwake: Boolean,
    onKeepScreenAwakeChanged: (Boolean) -> Unit,
    screenDimTimeoutMs: Long,
    onScreenDimTimeoutChanged: (Long) -> Unit,
    lockScreenControls: Boolean,
    onLockScreenControlsChanged: (Boolean) -> Unit
) {
    RunSwitchSetting(
        title = stringResource(R.string.run_auto_pause_setting),
        description = stringResource(R.string.run_auto_pause_description),
        checked = autoPauseEnabled,
        onCheckedChange = onAutoPauseChanged
    )
    RunSwitchSetting(
        title = stringResource(R.string.run_keep_screen_awake_setting),
        description = stringResource(R.string.run_keep_screen_awake_description),
        checked = keepScreenAwake,
        onCheckedChange = onKeepScreenAwakeChanged
    )
    RunScreenDimTimeoutSetting(
        timeoutMs = screenDimTimeoutMs,
        enabled = keepScreenAwake,
        onTimeoutSelected = onScreenDimTimeoutChanged
    )
        RunLockScreenControlsSetting(lockScreenControls, onLockScreenControlsChanged)
        RunCrashRecoverySetting()
}

@Composable
private fun RunScreenDimTimeoutSetting(
    timeoutMs: Long,
    enabled: Boolean,
    onTimeoutSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        15_000L to stringResource(R.string.run_screen_dim_15_seconds),
        30_000L to stringResource(R.string.run_screen_dim_30_seconds),
        60_000L to stringResource(R.string.run_screen_dim_1_minute),
        180_000L to stringResource(R.string.run_screen_dim_3_minutes),
        300_000L to stringResource(R.string.run_screen_dim_5_minutes),
        0L to stringResource(R.string.run_screen_dim_never)
    )
    val title = stringResource(R.string.run_screen_dim_timeout_setting)
    val selectedLabel = options.firstOrNull { it.first == timeoutMs }?.second ?: options[1].second

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f))
            Box {
                TextButton(
                    enabled = enabled,
                    onClick = { expanded = true },
                    modifier = Modifier.semantics { contentDescription = title }
                ) {
                    Text(selectedLabel)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded && enabled,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (optionTimeoutMs, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onTimeoutSelected(optionTimeoutMs)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        Text(
            stringResource(R.string.run_screen_dim_timeout_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RunLockScreenControlsSetting(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    RunSwitchSetting(
        title = stringResource(R.string.run_lock_screen_controls_setting),
        description = stringResource(R.string.run_lock_screen_controls_description),
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
private fun RunSwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
    Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, if (selected) primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AccentOption(
    color: AccentColor,
    darkTheme: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = stringResource(color.labelResource())
    val swatch = if (darkTheme) color.tone().dark else color.tone().light
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .size(48.dp)
                .background(swatch, CircleShape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (darkTheme) Color(0xFF1C1C1E) else Color.White
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

private fun AccentColor.labelResource(): Int = when (this) {
    AccentColor.PURPLE -> R.string.accent_purple
    AccentColor.INDIGO -> R.string.accent_indigo
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.CYAN -> R.string.accent_cyan
    AccentColor.TEAL -> R.string.accent_teal
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.LIME -> R.string.accent_lime
    AccentColor.YELLOW -> R.string.accent_yellow
    AccentColor.ORANGE -> R.string.accent_orange
    AccentColor.RED -> R.string.accent_red
    AccentColor.PINK -> R.string.accent_pink
    AccentColor.BROWN -> R.string.accent_brown
}
