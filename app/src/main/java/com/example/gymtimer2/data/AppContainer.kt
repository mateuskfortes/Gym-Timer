package com.example.gymtimer2.data

import android.content.Context
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.repository.WorkoutRepository

class AppContainer(context: Context) {

    private val database = WorkoutDatabase.getDatabase(context)

    private val exerciseDao = database.exerciseDao()
    private val songDao = database.songDao()
    private val chorusDao = database.chorusDao()
    private val exerciseChorusDao = database.exerciseChorusDao()

    val workoutRepository = WorkoutRepository(
        context = context.applicationContext,
        exerciseDao = exerciseDao,
        songDao = songDao,
        chorusDao = chorusDao,
        exerciseChorusDao = exerciseChorusDao
    )
}