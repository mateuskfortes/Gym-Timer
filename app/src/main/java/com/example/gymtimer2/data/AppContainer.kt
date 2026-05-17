package com.example.gymtimer2.data

import android.content.Context
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.repository.ChorusRepository
import com.example.gymtimer2.data.repository.SongRepository
import com.example.gymtimer2.data.repository.SavedSongRepository
import com.example.gymtimer2.data.repository.WorkoutRepository

class AppContainer(context: Context) {

    private val database = WorkoutDatabase.getDatabase(context)

    private val exerciseDao = database.exerciseDao()
    private val songDao = database.songDao()
    private val chorusDao = database.chorusDao()

    val songRepository = SongRepository(context.applicationContext)
    val workoutRepository = WorkoutRepository(exerciseDao)
    val savedSongRepository = SavedSongRepository(songDao)
    @Suppress("unused")
    val chorusRepository = ChorusRepository(chorusDao)
}