package com.example.gymtimer2

import android.app.Application
import com.example.gymtimer2.data.AppContainer

class GymApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}