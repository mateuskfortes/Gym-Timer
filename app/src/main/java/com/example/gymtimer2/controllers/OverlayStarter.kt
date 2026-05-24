package com.example.gymtimer2.controllers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.gymtimer2.domain.model.ChorusWithSongModel
import com.example.gymtimer2.domain.model.ExerciseModel

const val EXTRA_EXERCISE = "EXTRA_EXERCISE"
const val EXTRA_CHORUS = "EXTRA_CHORUS"
fun startOverlay(context: Context, exercise: ExerciseModel, choruses: List<ChorusWithSongModel>) {
    if (OverlayService.isRunning) return

    if (Settings.canDrawOverlays(context)) {
        val intent = Intent(context, OverlayService::class.java).apply {
            putExtra(EXTRA_EXERCISE, exercise)
            putExtra(EXTRA_CHORUS, ArrayList(choruses))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}