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

class MainActivity : ComponentActivity() {
    private var pendingExerciseId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingExerciseId = savedInstanceState?.getInt("pending_exercise_id")?.takeIf { it > 0 }

        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory((application as GymApplication).container.appSettingsRepository)
            )
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
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
