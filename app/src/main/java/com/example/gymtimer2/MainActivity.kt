package com.example.gymtimer2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
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
                    RunTrackingServiceController(this)
                )
            )
            val runState by runViewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = settings.themeMode == ThemeMode.DARK
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            GymTimerTheme(darkTheme = darkTheme, accentColor = settings.accentColor) {
                MainScreen(
                    settings = settings,
                    onThemeSelected = settingsViewModel::selectTheme,
                    onAccentSelected = settingsViewModel::selectAccent,
                    runState = runState,
                    onStartRun = runViewModel::startRun,
                    onPauseRun = runViewModel::pauseRun,
                    onResumeRun = runViewModel::resumeRun,
                    onFinishRun = runViewModel::finishRun,
                    onDiscardRun = runViewModel::discardRun,
                    onSelectRun = runViewModel::selectRun,
                    onDeleteRun = runViewModel::deleteCompletedRun,
                    onBackFromRunDetails = runViewModel::clearSelectedRun,
                    onOpenActiveRun = runViewModel::openActiveRun,
                    onCloseActiveRun = runViewModel::closeActiveRun,
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

    override fun onSaveInstanceState(outState: Bundle) {
        pendingExerciseId?.let { outState.putInt("pending_exercise_id", it) }
        super.onSaveInstanceState(outState)
    }
}
