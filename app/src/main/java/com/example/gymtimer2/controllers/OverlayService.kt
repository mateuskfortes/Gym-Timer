package com.example.gymtimer2.controllers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.gymtimer2.MainActivity
import com.example.gymtimer2.R
import com.example.gymtimer2.controllers.components.FloatTimerController
import com.example.gymtimer2.domain.model.ChorusWithSongModel
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.util.extensions.serializable

class OverlayService : Service() {
    companion object {
        @Volatile
        var isRunning = false
        @Volatile
        var isClosing = false
    }
    private lateinit var floatTimerController: FloatTimerController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        isRunning = true
        isClosing = false

        startAsForeground()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val exercise = intent.serializable<ExerciseModel>(EXTRA_EXERCISE)!!
        val choruses = intent.serializable<ArrayList<ChorusWithSongModel>>(EXTRA_CHORUS) ?: arrayListOf()

        floatTimerController = FloatTimerController(
            applicationContext,
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater,
            getWindowManager(),
            exercise,
            choruses,
            onClose = { stopService() }
        )

        floatTimerController.show()
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        floatTimerController.hide()
        isRunning = false
        isClosing = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun stopService() {
        if (isClosing) return

        isClosing = true

        val back = Intent(this@OverlayService, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(back)
        stopSelf()
    }
    private fun startAsForeground() {
        val channelId = "overlay_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("GymTimer")
            .setContentText("Overlay ativa")
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }
    private fun getWindowManager(): WindowManager {
        return getSystemService(WINDOW_SERVICE) as WindowManager
    }
}

