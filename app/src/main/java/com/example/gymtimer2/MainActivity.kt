package com.example.gymtimer2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.gymtimer2.timer_overlay_service.canDrawOverlays
import com.example.gymtimer2.timer_overlay_service.requestOverlayPermission
import com.example.gymtimer2.timer_overlay_service.startOverlay
import com.example.gymtimer2.ui.theme.GymTimerTheme

class MainActivity : ComponentActivity() {
    private var pendingExerciseId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingExerciseId = savedInstanceState?.getInt("pending_exercise_id")?.takeIf { it > 0 }

        enableEdgeToEdge()

        setContent {
            GymTimerTheme {
                MainScreen(
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
