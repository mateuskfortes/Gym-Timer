package com.example.gymtimer2

import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import com.example.gymtimer2.domain.model.ThemeMode
import com.example.gymtimer2.timer_overlay_service.canDrawOverlays
import com.example.gymtimer2.timer_overlay_service.requestOverlayPermission
import com.example.gymtimer2.timer_overlay_service.startOverlay
import com.example.gymtimer2.ui.theme.GymTimerTheme
import com.example.gymtimer2.ui.screens.settings.SettingsViewModel
import com.example.gymtimer2.ui.screens.runs.RunViewModel
import com.example.gymtimer2.ui.screens.runs.screenActions
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingServiceController

/** Monta a interface principal e compartilha as dependências da gravação GPS. */
class MainActivity : ComponentActivity() {
    private var pendingExerciseId: Int? = null

    /** Cria ViewModels com os repositórios do container e observa seu estado. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingExerciseId = savedInstanceState?.getInt("pending_exercise_id")?.takeIf { it > 0 }

        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory((application as GymApplication).container.appSettingsRepository)
            )
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val container = (application as GymApplication).container
            val runViewModel: RunViewModel = viewModel(
                factory = RunViewModel.factory(
                    container.runRepository,
                    container.plannedRouteRepository,
                    container.valhallaRouteRepository,
                    container.openStreetMapPoiRepository,
                    RunTrackingServiceController(this),
                    container.runGearRepository,
                    container.runExportRepository,
                    container.appSettingsRepository,
                    container.runPrivacyZoneRepository,
                    container.runWorkoutTemplateRepository
                )
            )
            val runState by runViewModel.uiState.collectAsStateWithLifecycle()
            val runPrivacyZones by runViewModel.privacyZones.collectAsStateWithLifecycle()
            val runActions = runViewModel.screenActions(runState.runGears)
            val darkTheme = settings.themeMode == ThemeMode.DARK
            val showRunOnLockScreen = shouldShowRunOnLockScreen(
                preferenceEnabled = settings.runShowControlsOnLockScreen,
                isActiveRunOpen = runState.isActiveRunOpen,
                hasActiveRun = runState.activeRun != null
            )
            ApplyRunWindowAppearance(darkTheme, showRunOnLockScreen)
            GymTimerTheme(darkTheme = darkTheme, accentColor = settings.accentColor) {
                MainScreen(
                    settings = settings,
                    onThemeSelected = settingsViewModel::selectTheme,
                    onAccentSelected = settingsViewModel::selectAccent,
                    onRunDistanceUnitChanged = settingsViewModel::setRunDistanceUnit,
                    onRunTemperatureUnitChanged = settingsViewModel::setRunTemperatureUnit,
                    onRunStartEndHideDistanceChanged = settingsViewModel::setRunStartEndHideDistance,
                    onRunOffRouteAlertsChanged = settingsViewModel::setRunOffRouteAlertsEnabled,
                    onRunOffRouteNotificationsChanged = settingsViewModel::setRunOffRouteNotificationsEnabled,
                    onRunAutoPauseChanged = settingsViewModel::setRunAutoPause,
                    onRunKeepScreenAwakeChanged = settingsViewModel::setRunKeepScreenAwake,
                    onRunScreenDimTimeoutChanged = settingsViewModel::setRunScreenDimTimeout,
                    onRunLockScreenControlsChanged = settingsViewModel::setRunShowControlsOnLockScreen,
                    onRunManualLapsChanged = settingsViewModel::setRunManualLapsEnabled,
                    onRunStepRateSensorChanged = settingsViewModel::setRunStepRateSensorEnabled,
                    onRunLiveSegmentsChanged = settingsViewModel::setRunLiveSegmentsEnabled,
                    onRunDisplayAllTurnCuesChanged = settingsViewModel::setRunDisplayAllTurnCues,
                    onRunUpcomingTurnCueCountChanged = settingsViewModel::setRunUpcomingTurnCueCount,
                    onRunBodyMassKgChanged = settingsViewModel::setRunBodyMassKg,
                    onRunWeatherChanged = settingsViewModel::setRunWeatherEnabled,
                    runState = runState,
                    runActions = runActions,
                    runPrivacyZones = runPrivacyZones,
                    onSaveRunPrivacyZone = runViewModel::savePrivacyZone,
                    onDeleteRunPrivacyZone = runViewModel::deletePrivacyZone,
                    onOpenOverlayClick = {exerciseId: Int ->
                        if (canDrawOverlays(this)) {
                            startOverlay(this, exerciseId)
                        } else {
                            pendingExerciseId = exerciseId
                            requestOverlayPermission(this)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val exerciseId = pendingExerciseId
        if (exerciseId != null && canDrawOverlays(this)) {
            pendingExerciseId = null
            startOverlay(this, exerciseId)
        }
    }

    @Composable
    private fun ApplyRunWindowAppearance(darkTheme: Boolean, showRunOnLockScreen: Boolean) {
        SideEffect {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            applyRunLockScreenVisibility(showRunOnLockScreen)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingExerciseId?.let { outState.putInt("pending_exercise_id", it) }
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    private fun applyRunLockScreenVisibility(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(show)
        } else if (show) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }
}

internal fun shouldShowRunOnLockScreen(
    preferenceEnabled: Boolean,
    isActiveRunOpen: Boolean,
    hasActiveRun: Boolean
): Boolean = preferenceEnabled && isActiveRunOpen && hasActiveRun
