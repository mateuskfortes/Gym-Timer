package com.example.gymtimer2

import android.app.Application
import com.example.gymtimer2.data.AppContainer
import com.example.gymtimer2.player.MusicPlayerManager

class GymApplication : Application() {

    lateinit var container: AppContainer
    lateinit var musicPlayerManager: MusicPlayerManager

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        musicPlayerManager = MusicPlayerManager(this)
    }
}