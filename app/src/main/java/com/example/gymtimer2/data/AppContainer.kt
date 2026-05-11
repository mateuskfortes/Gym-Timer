package com.example.gymtimer2.data

import android.content.Context
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.repository.WorkoutRepository

class AppContainer(context: Context) {

    private val database = WorkoutDatabase.getDatabase(context)

    private val exerciseDao = database.exerciseDao()

    val workoutRepository = WorkoutRepository(exerciseDao)
}