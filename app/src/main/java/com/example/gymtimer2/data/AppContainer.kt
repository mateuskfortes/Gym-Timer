package com.example.gymtimer2.data

import android.content.Context
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.preferences.AppSettingsRepository
import com.example.gymtimer2.data.repository.SongArtworkRepository
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.data.repository.WorkoutRepository

class AppContainer(context: Context) {

    val appSettingsRepository = AppSettingsRepository(context)
    val songArtworkRepository = SongArtworkRepository(context.applicationContext)

    private val database = WorkoutDatabase.getDatabase(context)

    private val exerciseDao = database.exerciseDao()
    private val songDao = database.songDao()
    private val chorusDao = database.chorusDao()
    private val exerciseChorusDao = database.exerciseChorusDao()
    private val workoutDayDao = database.workoutDayDao()
    private val exerciseScheduleDao = database.exerciseScheduleDao()
    private val runDao = database.runDao()

    /** Repositório local usado pela aba Corridas e pelo serviço GPS. */
    val runRepository = RunRepository(runDao)

    val workoutRepository = WorkoutRepository(
        context = context.applicationContext,
        exerciseDao = exerciseDao,
        songDao = songDao,
        chorusDao = chorusDao,
        exerciseChorusDao = exerciseChorusDao,
        workoutDayDao = workoutDayDao,
        exerciseScheduleDao = exerciseScheduleDao,
        database = database
    )
}
