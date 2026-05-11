package com.example.gymtimer2.controllers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

const val EXTRA_REST_SECONDS = "EXTRA_REST_SECONDS"
fun startOverlay(context: Context, restTime: Long) {
    if (OverlayService.isRunning) return

    if (Settings.canDrawOverlays(context)) {
        val intent = Intent(context, OverlayService::class.java).apply {
            putExtra(EXTRA_REST_SECONDS, restTime)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}