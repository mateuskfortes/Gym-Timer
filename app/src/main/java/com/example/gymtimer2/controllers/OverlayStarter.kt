package com.example.gymtimer2.controllers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.gymtimer2.domain.model.ChorusWithSongModel
import com.example.gymtimer2.domain.model.ExerciseModel

const val EXTRA_EXERCISE_ID = "EXERCISE_ID"
fun startOverlay(context: Context, exerciseId: Int) {
    if (OverlayService.isRunning) return

    if (Settings.canDrawOverlays(context)) {
        val intent = Intent(context, OverlayService::class.java).apply {
            putExtra(EXTRA_EXERCISE_ID, exerciseId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}