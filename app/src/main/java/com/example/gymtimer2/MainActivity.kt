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
    private var overlayPermissionRequested = false
    private var pendingOpenOverlay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GymTimerTheme {
                MainScreen(
                    onOpenOverlayClick = {exerciseId: Int ->
                        if (canDrawOverlays(this)) {
                            startOverlay(this, exerciseId)
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
            startOverlay(this, 1
            )
            overlayPermissionRequested = false
            pendingOpenOverlay = false
        }
    }
}