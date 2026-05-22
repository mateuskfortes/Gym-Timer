package com.example.gymtimer2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.gymtimer2.controllers.canDrawOverlays
import com.example.gymtimer2.controllers.requestOverlayPermission
import com.example.gymtimer2.controllers.startOverlay
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import com.example.gymtimer2.ui.theme.GymTimerTheme

class MainActivity : ComponentActivity() {
    private var overlayPermissionRequested = false
    private var pendingOpenOverlay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GymTimerTheme {
                MainScreen(
                    onOpenOverlayClick = {exercise: ExerciseModel ->
                        if (canDrawOverlays(this)) {
                            startOverlay(this, exercise)
                        } else {
                            pendingOpenOverlay = true
                            overlayPermissionRequested = true
                            requestOverlayPermission(this)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (overlayPermissionRequested && pendingOpenOverlay && canDrawOverlays(this)) {
            startOverlay(this, ExerciseModel(1,"Sample Exercise", WeightModel(4, WeightUnit.TOTAL_KG), 3))
            overlayPermissionRequested = false
            pendingOpenOverlay = false
        }
    }
}