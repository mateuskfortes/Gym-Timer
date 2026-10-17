package com.example.gymtimer2.timer_overlay_service

import android.os.Build
import android.view.WindowManager

fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
} else {
    @Suppress("DEPRECATION")
    WindowManager.LayoutParams.TYPE_PHONE
}
