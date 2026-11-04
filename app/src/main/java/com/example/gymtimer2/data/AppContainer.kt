package com.example.gymtimer2.data

import android.content.Context
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.R
import com.example.gymtimer2.data.preferences.AppSettingsRepository
import com.example.gymtimer2.data.repository.SongArtworkRepository
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.data.repository.PlannedRouteRepository
import com.example.gymtimer2.data.repository.ValhallaRouteRepository
import com.example.gymtimer2.data.repository.OpenStreetMapPoiRepository
import com.example.gymtimer2.data.repository.RunGearRepository
import com.example.gymtimer2.data.repository.RunMediaStorage
import com.example.gymtimer2.data.repository.RunExportRepository
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.data.repository.RunWeatherRepository
import com.example.gymtimer2.data.repository.RunPrivacyZoneRepository
import com.example.gymtimer2.data.repository.RunWorkoutTemplateRepository

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
    private val plannedRouteDao = database.plannedRouteDao()
    private val runGearDao = database.runGearDao()
    private val runPrivacyZoneDao = database.runPrivacyZoneDao()
    private val runWorkoutTemplateDao = database.runWorkoutTemplateDao()
    private val runMediaStorage = RunMediaStorage(context.applicationContext)

    /** Repositório local usado pela aba Corridas e pelo serviço GPS. */
    val runRepository = RunRepository(runDao, runMediaStorage)
    val runExportRepository = RunExportRepository(
        context.applicationContext.contentResolver,
        context.cacheDir
    )
    val runGearRepository = RunGearRepository(runGearDao, runMediaStorage)
    val plannedRouteRepository = PlannedRouteRepository(
        plannedRouteDao = plannedRouteDao,
        contentResolver = context.applicationContext.contentResolver,
        defaultRouteName = context.getString(R.string.run_imported_route_name)
    )
    val valhallaRouteRepository = ValhallaRouteRepository()
    val openStreetMapPoiRepository = OpenStreetMapPoiRepository()
    val runWeatherRepository = RunWeatherRepository()
    val runPrivacyZoneRepository = RunPrivacyZoneRepository(runPrivacyZoneDao)
    val runWorkoutTemplateRepository = RunWorkoutTemplateRepository(runWorkoutTemplateDao)

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
