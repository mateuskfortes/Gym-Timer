package com.example.gymtimer2

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.preferences.AppSettingsRepository
import com.example.gymtimer2.data.entity.RunMediaEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.entity.RunLapEntity
import com.example.gymtimer2.data.entity.RunStepRateEventEntity
import com.example.gymtimer2.data.entity.PlannedRouteEntity
import com.example.gymtimer2.data.entity.PlannedRoutePointEntity
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.data.repository.RunMediaStorage
import com.example.gymtimer2.data.repository.RunGearRepository
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutSetPlan
import com.example.gymtimer2.domain.model.RunWorkoutStepPlan
import com.example.gymtimer2.domain.model.RunWorkoutTrigger
import com.example.gymtimer2.domain.model.RunHeatmapCell
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunWeather
import com.example.gymtimer2.domain.model.RunTemperatureUnit
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.runElevationReadingOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica salvamento com título e descarte sem afetar corridas concluídas. */
class RunPersistenceTest {
    @Test
    fun distanceUnitPreferenceReloads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousUnit = repository.settings.value.runDistanceUnit
        try {
            repository.setRunDistanceUnit(RunDistanceUnit.IMPERIAL)

            assertEquals(RunDistanceUnit.IMPERIAL, AppSettingsRepository(context).settings.value.runDistanceUnit)
        } finally {
            repository.setRunDistanceUnit(previousUnit)
        }
    }

    @Test
    fun bodyMassPreferenceReloadsAndCanBeCleared() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousBodyMassKg = repository.settings.value.runBodyMassKg
        try {
            repository.setRunBodyMassKg(72.5)

            assertEquals(72.5, AppSettingsRepository(context).settings.value.runBodyMassKg!!, 0.0)
            repository.setRunBodyMassKg(null)
            assertNull(AppSettingsRepository(context).settings.value.runBodyMassKg)
        } finally {
            repository.setRunBodyMassKg(previousBodyMassKg)
        }
    }

    @Test
    fun weatherPreferenceReloadsAndCanBeDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousValue = repository.settings.value.runWeatherEnabled
        try {
            repository.setRunWeatherEnabled(true)
            assertTrue(AppSettingsRepository(context).settings.value.runWeatherEnabled)
            repository.setRunWeatherEnabled(false)
            assertFalse(AppSettingsRepository(context).settings.value.runWeatherEnabled)
        } finally {
            repository.setRunWeatherEnabled(previousValue)
        }
    }

    @Test
    fun liveSegmentsPreferenceReloadsAndCanBeDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousValue = repository.settings.value.runLiveSegmentsEnabled
        try {
            repository.setRunLiveSegmentsEnabled(false)

            assertFalse(AppSettingsRepository(context).settings.value.runLiveSegmentsEnabled)
            repository.setRunLiveSegmentsEnabled(true)
            assertTrue(AppSettingsRepository(context).settings.value.runLiveSegmentsEnabled)
        } finally {
            repository.setRunLiveSegmentsEnabled(previousValue)
        }
    }

    @Test
    fun routeCuePreferencesReload() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousShowAll = repository.settings.value.runDisplayAllTurnCues
        val previousCount = repository.settings.value.runUpcomingTurnCueCount
        try {
            repository.setRunDisplayAllTurnCues(true)
            repository.setRunUpcomingTurnCueCount(4)

            val reloaded = AppSettingsRepository(context).settings.value
            assertTrue(reloaded.runDisplayAllTurnCues)
            assertEquals(4, reloaded.runUpcomingTurnCueCount)
        } finally {
            repository.setRunDisplayAllTurnCues(previousShowAll)
            repository.setRunUpcomingTurnCueCount(previousCount)
        }
    }

    @Test
    fun temperatureUnitPreferenceReloads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousValue = repository.settings.value.runTemperatureUnit
        try {
            repository.setRunTemperatureUnit(RunTemperatureUnit.FAHRENHEIT)
            assertEquals(
                RunTemperatureUnit.FAHRENHEIT,
                AppSettingsRepository(context).settings.value.runTemperatureUnit
            )
        } finally {
            repository.setRunTemperatureUnit(previousValue)
        }
    }

    @Test
    fun startEndHideDistancePreferenceReloads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousValue = repository.settings.value.runStartEndHideDistance
        try {
            repository.setRunStartEndHideDistance(RunStartEndHideDistance.METERS_800)

            assertEquals(
                RunStartEndHideDistance.METERS_800,
                AppSettingsRepository(context).settings.value.runStartEndHideDistance
            )
        } finally {
            repository.setRunStartEndHideDistance(previousValue)
        }
    }

    @Test
    fun activeRunMapCameraSnapshotReloadsAndClears() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppSettingsRepository(context)
        val previousSnapshot = repository.runMapCameraSnapshot.value
        val snapshot = RunMapCameraSnapshot(
            runId = System.currentTimeMillis(),
            latitude = -23.55,
            longitude = -46.63,
            zoom = 14.5,
            tilt = 20.0,
            bearing = 90.0,
            followsUser = false
        )
        try {
            repository.setRunMapCameraSnapshot(snapshot)

            assertEquals(snapshot, AppSettingsRepository(context).runMapCameraSnapshot.value)
            repository.clearRunMapCameraSnapshot()
            assertNull(AppSettingsRepository(context).runMapCameraSnapshot.value)
        } finally {
            if (previousSnapshot == null) repository.clearRunMapCameraSnapshot()
            else repository.setRunMapCameraSnapshot(previousSnapshot)
        }
    }

    @Test
    fun weatherSnapshotPersistsWithTheRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val runId = repository.startRun(1L)
            val weather = RunWeather(18.5, 17.8, 61.0, 9.2, 1_793_577_600_000L)

            repository.updateWeather(runId, weather)

            assertEquals(weather, repository.getRun(runId)?.weather)
        } finally {
            database.close()
        }
    }

    @Test
    fun startEndHideDistanceIsCapturedWithTheRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val runId = repository.startRun(
                startedAtEpochMs = 1L,
                startEndHideDistance = RunStartEndHideDistance.METERS_800
            )

            assertEquals(
                RunStartEndHideDistance.METERS_800,
                repository.getRun(runId)?.startEndHideDistance
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun bodyMassIsCapturedAtRunStartForTheSavedEstimate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val runId = repository.startRun(1L, bodyMassKgAtStart = 72.5)

            assertEquals(72.5, repository.getRun(runId)?.bodyMassKgAtStart ?: 0.0, 0.0)
        } finally {
            database.close()
        }
    }

    @Test
    fun activityPhotosPersistAndTheirFilesAreRemovedWithThePhotoOrRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        val photoDirectory = File(context.filesDir, "run_photos").apply { mkdirs() }
        val firstMedia = File(photoDirectory, "${UUID.randomUUID()}.image").apply { writeBytes(byteArrayOf(1)) }
        val secondMedia = File(photoDirectory, "${UUID.randomUUID()}.video").apply { writeBytes(byteArrayOf(2)) }
        try {
            val repository = RunRepository(database.runDao(), RunMediaStorage(context))
            val runId = repository.startRun(1L)
            repository.finishRun(runId, 1_000L, 2_000L)
            database.runDao().insertRunMedia(
                listOf(
                    RunMediaEntity(runId = runId, fileName = firstMedia.name),
                    RunMediaEntity(runId = runId, fileName = secondMedia.name, isVideo = true)
                )
            )

            val media = repository.observeRunMedia(runId).first()
            assertEquals(listOf(firstMedia.name, secondMedia.name), media.map { it.fileName })
            assertEquals(listOf(false, true), media.map { it.isVideo })
            repository.setFeaturedRunMedia(runId, media.last().id)
            assertEquals(listOf(false, true), repository.observeRunMedia(runId).first().map { it.isFeatured })
            repository.setFeaturedRunMedia(runId, Long.MAX_VALUE)
            assertEquals(listOf(false, true), repository.observeRunMedia(runId).first().map { it.isFeatured })

            repository.deleteRunMedia(media.first().id)
            assertFalse(firstMedia.exists())
            assertTrue(secondMedia.exists())

            assertTrue(repository.deleteCompletedRun(runId))
            assertFalse(secondMedia.exists())
            assertTrue(repository.observeRunMedia(runId).first().isEmpty())
        } finally {
            firstMedia.delete()
            secondMedia.delete()
            database.close()
        }
    }

    @Test
    fun selectedGearAndCompletedDistanceAreSavedLocally() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        val gearPhoto = File(context.filesDir, "run_photos/${UUID.randomUUID()}.image").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
        }
        try {
            val repository = RunRepository(database.runDao())
            val gearRepository = RunGearRepository(database.runGearDao(), RunMediaStorage(context))
            val gear = gearRepository.getOrCreate("Tênis de treino")
            assertEquals(1, database.runGearDao().updatePhotoFileName(gear.id, gearPhoto.name))
            assertEquals(gearPhoto.name, gearRepository.gears.first().single().photoFileName)
            gearRepository.setPhoto(gear.id, null)
            assertFalse(gearPhoto.exists())
            assertNull(gearRepository.gears.first().single().photoFileName)

            val runId = repository.startRun(1L)
            repository.appendLocation(runId, GpsPoint(-23.55, -46.63, 2L, 5f), 1_250.0)
            repository.finishRun(
                runId,
                60_000L,
                61_000L,
                RunSaveDetails(gearName = gear.name, gearId = gear.id)
            )

            val savedRun = checkNotNull(repository.getRun(runId))
            assertEquals(gear.id, savedRun.gearId)
            assertEquals("Tênis de treino", savedRun.gearName)

            val raceGear = gearRepository.getOrCreate("Tênis de prova")
            assertTrue(repository.updateSaveDetails(runId, RunSaveDetails(gearName = raceGear.name, gearId = raceGear.id)))
            val updatedRun = checkNotNull(repository.getRun(runId))
            val gears = gearRepository.gears.first()
            val trainingGear = gears.first { it.id == gear.id }
            val updatedGear = gears.first { it.id == raceGear.id }

            assertEquals(raceGear.id, updatedRun.gearId)
            assertEquals("Tênis de prova", updatedRun.gearName)
            assertEquals(0.0, trainingGear.distanceMeters, 0.0)
            assertTrue(trainingGear.isDefault)
            assertEquals(1_250.0, updatedGear.distanceMeters, 0.0)

            assertFalse(gearRepository.rename(raceGear.id, gear.name))
            assertTrue(gearRepository.rename(raceGear.id, "Tênis de competição"))
            gearRepository.setRetired(gear.id, true)
            val retiredTrainingGear = gearRepository.gears.first().first { it.id == gear.id }
            assertTrue(retiredTrainingGear.isRetired)
            assertFalse(retiredTrainingGear.isDefault)
            assertTrue(gearRepository.rename(raceGear.id, raceGear.name))
            gearRepository.setRetired(gear.id, false)
            val reactivatedTrainingGear = gearRepository.gears.first().first { it.id == gear.id }
            assertFalse(reactivatedTrainingGear.isRetired)
        } finally {
            gearPhoto.delete()
            database.close()
        }
    }

    @Test
    fun filteredGpsPointIsSavedWithoutChangingRunDistance() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.appendLocation(
                id,
                GpsPoint(-23.55, -46.63, 2L, 151f, isFiltered = true),
                distanceMeters = 0.0
            )

            val point = repository.observePoints(id).first().single()
            assertTrue(point.isFiltered)
            assertEquals(0.0, repository.getRun(id)?.distanceMeters ?: -1.0, 0.0)
        } finally {
            database.close()
        }
    }

    @Test
    fun weeklyHeatmapIncludesOnlyRecentCompletedAndUnfilteredCells() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val oldRunId = repository.startRun(99L)
            repository.appendLocation(oldRunId, GpsPoint(-23.55, -46.63, 100L, 5f), 0.0)
            repository.finishRun(oldRunId, 1_000L, 150L)

            val recentRunId = repository.startRun(120L)
            repository.appendLocation(recentRunId, GpsPoint(-23.56, -46.64, 130L, 5f), 0.0)
            repository.appendLocation(recentRunId, GpsPoint(-23.56001, -46.64001, 135L, 5f), 0.0)
            repository.appendLocation(
                recentRunId,
                GpsPoint(-23.57, -46.65, 140L, 5f, isFiltered = true),
                0.0
            )
            repository.finishRun(recentRunId, 1_000L, 150L)

            val secondRecentRunId = repository.startRun(160L)
            repository.appendLocation(secondRecentRunId, GpsPoint(-23.56, -46.64, 165L, 5f), 0.0)
            repository.finishRun(secondRecentRunId, 1_000L, 170L)

            val unfinishedRunId = repository.startRun(130L)
            repository.appendLocation(unfinishedRunId, GpsPoint(-23.58, -46.66, 140L, 5f), 0.0)

            val cells = repository.observeWeeklyHeatmapCells(100L, 200L).first()
            assertEquals(listOf(RunHeatmapCell(-23.56, -46.64, 2L)), cells)
        } finally {
            database.close()
        }
    }

    @Test
    fun historyHeatmapGroupsAcceptedPointsFromCompletedRuns() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val completedRunId = repository.startRun(1L)
            repository.appendLocation(completedRunId, GpsPoint(-23.55011, -46.63011, 2L, 5f), 0.0)
            repository.appendLocation(completedRunId, GpsPoint(-23.55014, -46.63014, 3L, 5f), 0.0)
            repository.appendLocation(
                completedRunId,
                GpsPoint(-23.55013, -46.63013, 4L, 5f, isFiltered = true),
                0.0
            )
            repository.finishRun(completedRunId, 1_000L, 10L)

            val secondCompletedRunId = repository.startRun(5L)
            repository.appendLocation(secondCompletedRunId, GpsPoint(-23.55012, -46.63012, 6L, 5f), 0.0)
            repository.finishRun(secondCompletedRunId, 1_000L, 10L)

            val unfinishedRunId = repository.startRun(11L)
            repository.appendLocation(unfinishedRunId, GpsPoint(-23.55012, -46.63012, 12L, 5f), 0.0)

            assertEquals(
                listOf(RunHeatmapCell(-23.5501, -46.6301, 2L)),
                repository.getCompletedRunHeatmapCells()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun structuredWorkoutAndProgressPersistWithTheRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val plan = RunWorkoutPlan(
                name = "Intervalado",
                sets = listOf(
                    RunWorkoutSetPlan(
                        steps = listOf(
                            RunWorkoutStepPlan(
                                "Aquecimento", RunWorkoutTrigger.DURATION,
                                durationTargetMs = 10_000
                            ),
                            RunWorkoutStepPlan("Recuperação", RunWorkoutTrigger.MANUAL, isRestInterval = true)
                        ),
                        repetitions = 2,
                        omitLastRecoveryStep = true
                    )
                )
            )
            val runId = repository.startRun(1L, workoutPlan = plan)

            assertEquals("Intervalado", repository.getRun(runId)?.workoutTitle)
            assertEquals(3, repository.observeWorkoutSteps(runId).first().size)
            assertEquals(0, repository.advanceWorkoutProgress(runId, 10_000, 100.0).single().stepIndex)
            assertEquals(1, repository.advanceWorkoutProgress(runId, 15_000, 150.0, manualAdvance = true).single().stepIndex)
            assertEquals(2, repository.advanceWorkoutProgress(runId, 25_000, 250.0).single().stepIndex)

            val savedSteps = repository.observeWorkoutSteps(runId).first()
            assertEquals(listOf(10_000L, 15_000L, 25_000L), savedSteps.map { it.completedAtDurationMs })
            assertEquals(250.0, savedSteps.last().completedAtDistanceMeters!!, 0.0)
        } finally {
            database.close()
        }
    }

    @Test
    fun elevationWorkoutProgressPersistsWithTheRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val plan = RunWorkoutPlan(
                "Subidas",
                listOf(RunWorkoutSetPlan(listOf(
                    RunWorkoutStepPlan("Subida", RunWorkoutTrigger.ELEVATION, elevationChangeTargetMeters = 10.0)
                )))
            )
            val runId = repository.startRun(1L, workoutPlan = plan)
            val reading = { altitude: Double -> checkNotNull(runElevationReadingOrNull(altitude, 1f)) }

            repository.advanceWorkoutProgress(runId, 0L, 0.0, elevationReading = reading(100.0))
            repository.advanceWorkoutProgress(runId, 1_000L, 10.0, elevationReading = reading(120.0))
            val completed = repository.advanceWorkoutProgress(runId, 2_000L, 20.0, elevationReading = reading(110.0))
            val saved = repository.observeWorkoutSteps(runId).first().single()

            assertEquals(0, completed.single().stepIndex)
            assertEquals(10.0, saved.elevationChangeTargetMeters!!, 0.0)
            assertEquals(100.0, saved.minimumElevationMeters!!, 0.0)
            assertEquals(120.0, saved.maximumElevationMeters!!, 0.0)
            assertEquals(2_000L, saved.completedAtDurationMs)
        } finally {
            database.close()
        }
    }

    @Test
    fun importedRoutePointsAndRunSelectionAreStoredLocally() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val routeId = database.plannedRouteDao().insertImportedRoute(
                PlannedRouteEntity(name = "Parque", distanceMeters = 120.0, importedAtEpochMs = 1L),
                listOf(
                    PlannedRoutePointEntity(0L, 0, -23.55, -46.63, 0, 760.5),
                    PlannedRoutePointEntity(0L, 1, -23.551, -46.63, 0, 762.0)
                )
            )
            val runId = RunRepository(database.runDao()).startRun(2L, plannedRouteId = routeId)
            val routePoints = database.plannedRouteDao().observePoints(routeId).first()

            assertEquals(routeId, database.runDao().getRun(runId)?.plannedRouteId)
            assertEquals(listOf(0, 1), routePoints.map { it.pointIndex })
            assertEquals(-23.551, routePoints.last().latitude, 0.0)
            assertEquals(762.0, routePoints.last().altitudeMeters!!, 0.0)
        } finally {
            database.close()
        }
    }

    @Test
    fun localSegmentProgressAndAttemptTimesPersistWithTheRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val runId = repository.startRun(1L, plannedRouteId = 7L)
            repository.beginRun(runId)
            repository.updateLocalSegmentProgress(runId, 120.0, 5_000L, null, 7L)
            repository.updateLocalSegmentProgress(runId, 500.0, 5_000L, 25_000L, 8L)

            val run = checkNotNull(repository.getRun(runId))
            assertEquals(500.0, run.localSegmentProgressMeters, 0.0)
            assertEquals(5_000L, run.localSegmentStartedAtDurationMs)
            assertEquals(25_000L, run.localSegmentCompletedAtDurationMs)
            assertEquals(7L, run.localSegmentRouteId)
        } finally {
            database.close()
        }
    }

    @Test
    fun selectedRouteIsStoredOnTheNewRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val sourceRunId = repository.startRun(1L)
            val newRunId = repository.startRun(2L, sourceRunId)

            assertEquals(sourceRunId, repository.getRun(newRunId)?.plannedRouteRunId)
        } finally {
            database.close()
        }
    }

    @Test
    fun paceTargetIsSavedAndRestoredWithRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val expectedTarget = RunPaceTarget(330, 360)
            val runId = repository.startRun(1L, paceTarget = expectedTarget)

            assertEquals(expectedTarget, repository.getRun(runId)?.paceTarget)
        } finally {
            database.close()
        }
    }

    @Test
    fun selectedRouteCannotBeDeletedUntilItsRunEnds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val routeId = repository.startRun(1L)
            repository.finishRun(routeId, 60_000L, 2L)
            val activeRunId = repository.startRun(3L, routeId)

            assertFalse(repository.deleteCompletedRun(routeId))
            assertNotNull(repository.getRun(routeId))

            repository.discardUnfinishedRun(activeRunId)
            assertTrue(repository.deleteCompletedRun(routeId))
            assertNull(repository.getRun(routeId))
        } finally {
            database.close()
        }
    }

    @Test
    fun automaticPauseAndResumePreserveTimeDistanceAndSegment() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.beginRun(id)
            repository.appendLocation(id, GpsPoint(0.0, 0.0, 1L, 5f), 250.0)
            repository.autoPauseRun(id, 60_000)
            assertEquals(RunStatus.AUTO_PAUSED, repository.getRun(id)?.status)
            repository.resumeRun(id)
            val run = checkNotNull(repository.getRun(id))
            assertEquals(RunStatus.RUNNING, run.status)
            assertEquals(60_000L, run.durationMs)
            assertEquals(250.0, run.distanceMeters, 0.0)
            assertEquals(1, run.currentSegment)
        } finally {
            database.close()
        }
    }
    @Test
    fun lapsPersistWithRunAndAreDeletedWithIt() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.beginRun(id)
            repository.appendLocation(
                id,
                GpsPoint(0.0, 0.0, 1L, 5f, 723.5, 3f, 2.5f, 0.5f, cadenceStepsPerMinute = 176),
                250.0
            )
            repository.recordLap(id, 60_000L, recordedAtEpochMs = 2L, elapsedDurationMs = 75_000L)
            repository.pauseRun(id, 60_000L)
            repository.recordLap(id, 70_000L)
            repository.finishRun(id, 60_000L, 2L)
            val lap = repository.observeLaps(id).first().single()
            val point = repository.observePoints(id).first().single()
            assertEquals(723.5, point.altitudeMeters!!, 0.0)
            assertEquals(3f, point.verticalAccuracyMeters!!, 0f)
            assertEquals(2.5f, point.speedMetersPerSecond!!, 0f)
            assertEquals(0.5f, point.speedAccuracyMetersPerSecond!!, 0f)
            assertEquals(176, point.cadenceStepsPerMinute!!)
            assertEquals(60_000L, lap.durationMs)
            assertEquals(75_000L, lap.elapsedDurationMs)
            assertEquals(250.0, lap.distanceMeters, 0.0)
            assertEquals(2L, lap.recordedAtEpochMs)
            repository.deleteCompletedRun(id)
            assertEquals(0, repository.observeLaps(id).first().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun cropPersistsSelectedPointsAndTrimsCadenceInOneTransaction() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val startedAt = 1_000L
            val runId = repository.startRun(startedAt)
            repository.beginRun(runId)
            repeat(4) { index ->
                repository.appendLocation(
                    runId,
                    GpsPoint(
                        latitude = 0.0,
                        longitude = index * 0.001,
                        recordedAtEpochMs = startedAt + index * 10_000L,
                        accuracyMeters = 5f,
                        elapsedRealtimeMs = 1_000L + index * 10_000L
                    ),
                    distanceMeters = if (index == 0) 0.0 else 100.0,
                    activeDurationMs = index * 10_000L
                )
            }
            repository.finishRun(runId, 30_000L, 31_000L, elapsedDurationMs = 30_000L)
            database.runDao().insertStepRateEvent(
                RunStepRateEventEntity(runId = runId, stepsPerMinute = 170, stepCount = 17, timestampEpochMs = 5_000L, intervalDurationMs = 4_000L)
            )
            database.runDao().insertStepRateEvent(
                RunStepRateEventEntity(runId = runId, stepsPerMinute = 172, stepCount = 28, timestampEpochMs = 15_000L, intervalDurationMs = 10_000L)
            )
            database.runDao().insertStepRateEvent(
                RunStepRateEventEntity(runId = runId, stepsPerMinute = 175, stepCount = 17, timestampEpochMs = 25_000L, intervalDurationMs = 10_000L)
            )
            val pointIds = repository.observePoints(runId).first().map { it.id }

            val activeRouteId = repository.startRun(32_000L, plannedRouteRunId = runId)
            assertEquals(
                RunCropOutcome.ACTIVE_ROUTE_IN_USE,
                repository.cropCompletedRun(runId, pointIds[1], pointIds[2])
            )
            repository.discardUnfinishedRun(activeRouteId)

            assertEquals(
                RunCropOutcome.SAVED,
                repository.cropCompletedRun(runId, pointIds[1], pointIds[2])
            )
            val savedRun = checkNotNull(repository.getRun(runId))
            assertEquals(10_000L, savedRun.durationMs)
            assertTrue(savedRun.distanceMeters in 100.0..120.0)
            assertEquals(listOf(pointIds[1], pointIds[2]), repository.observePoints(runId).first().map { it.id })
            val cadence = repository.observeStepRateSamples(runId).first()
            assertEquals(1, cadence.size)
            assertEquals(4_000L, cadence.single().intervalDurationMs)
        } finally {
            database.close()
        }
    }

    @Test
    fun concurrentTimeAndLocationsPreserveAllMetricsAndCompletedState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.beginRun(id)
            listOf(
                async(Dispatchers.IO) { repeat(100) { repository.updateDuration(id, (it + 1) * 1_000L) } },
                async(Dispatchers.IO) { repeat(100) { index ->
                    repository.appendLocation(id, GpsPoint(0.0, index * 0.0001, index.toLong(), 5f), 10.0)
                } }
            ).awaitAll()
            repository.finishRun(id, 100_000, 2L, RunSaveDetails(title = "Corrida"))
            repository.updateDuration(id, 200_000)
            repository.markInterrupted(id)
            repository.resumeRun(id)
            val run = checkNotNull(repository.getRun(id))
            assertEquals(100_000L, run.durationMs)
            assertEquals(1_000.0, run.distanceMeters, 0.0)
            assertEquals(RunStatus.COMPLETED, run.status)
            assertEquals(100, repository.observePoints(id).first().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun elapsedTimeCheckpointSurvivesPauseAndStaleUpdate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.beginRun(id, 0L, 100L, 1_000L)
            repository.updateDuration(id, 10_000L, 20_000L, 20_000L, 11_000L)
            repository.pauseRun(id, 12_000L, 22_000L, 22_000L, 13_000L)

            repository.updateDuration(id, 11_000L, 21_000L, 21_000L, 12_000L)
            val paused = checkNotNull(repository.getRun(id))
            assertEquals(12_000L, paused.durationMs)
            assertEquals(22_000L, paused.elapsedDurationMs)
            assertEquals(22_000L, paused.elapsedRealtimeAnchorMs)
            assertEquals(13_000L, paused.elapsedEpochAnchorMs)

            repository.resumeRun(id, 30_000L, 30_000L, 31_000L)
            assertEquals(30_000L, repository.getRun(id)?.elapsedDurationMs)
        } finally {
            database.close()
        }
    }

    /** A finalização conserva métricas e rota e impede que comandos repetidos substituam o título. */
    @Test
    fun finishingPersistsTitleAndRetainsRoute() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.updateDuration(id, 8_000L)
            database.runDao().insertPoint(RunPointEntity(runId = id, latitude = -23.55, longitude = -46.63,
                recordedAtEpochMs = 2L, accuracyMeters = 4f, segmentIndex = 0))
            repository.finishRun(
                id,
                5_000L,
                3L,
                RunSaveDetails(
                    "  Corrida no parque  ",
                    "  Treino depois do trabalho  ",
                    true,
                    7,
                    gearName = "  Tênis de trilha  ",
                    privateNotes = "  Volta em recuperação  "
                )
            )
            repository.finishRun(id, 4_000L, 4L, RunSaveDetails("Outro título", "Outra descrição", false))
            val saved = checkNotNull(repository.getRun(id))
            assertEquals("Corrida no parque", saved.title)
            assertEquals("Treino depois do trabalho", saved.description)
            assertEquals("Tênis de trilha", saved.gearName)
            assertEquals("Volta em recuperação", saved.privateNotes)
            assertTrue(saved.isCommute)
            assertEquals(7, saved.perceivedExertion)
            assertEquals(RunStatus.COMPLETED, saved.status)
            assertEquals(8_000L, saved.durationMs)
            assertEquals(1, repository.observePoints(id).first().size)
            repository.discardUnfinishedRun(id)
            assertNotNull(repository.getRun(id))
        } finally {
            database.close()
        }
    }

    /** Valores fora da escala não atravessam a fronteira do repositório. */
    @Test
    fun invalidPerceivedExertionIsNotPersisted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.finishRun(id, 1_000L, 2L, RunSaveDetails(perceivedExertion = 11))
            assertNull(repository.getRun(id)?.perceivedExertion)
        } finally {
            database.close()
        }
    }

    /** Editar metadados mantém as métricas e não altera sessões ainda abertas. */
    @Test
    fun updatesOnlyCompletedRunSaveDetails() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val completedId = repository.startRun(1L)
            repository.finishRun(completedId, 5_000L, 2L, RunSaveDetails("Corrida", "Descrição", true, 4))
            val activeId = repository.startRun(3L)

            assertTrue(
                repository.updateSaveDetails(
                    completedId,
                    RunSaveDetails(
                        "  Corrida alterada  ",
                        "   ",
                        false,
                        9,
                        RunActivityType.TRAIL_RUN,
                        setOf(RunActivityTag.RACE, RunActivityTag.WITH_PET),
                        gearName = "Tênis de asfalto",
                        privateNotes = "  Evitar esse ritmo na próxima  "
                    )
                )
            )
            assertFalse(repository.updateSaveDetails(activeId, RunSaveDetails(title = "Não salvar")))
            val updated = checkNotNull(repository.getRun(completedId))
            assertEquals("Corrida alterada", updated.title)
            assertNull(updated.description)
            assertFalse(updated.isCommute)
            assertEquals(9, updated.perceivedExertion)
            assertEquals(RunActivityType.TRAIL_RUN, updated.activityType)
            assertEquals(setOf(RunActivityTag.RACE, RunActivityTag.WITH_PET), updated.activityTags)
            assertEquals("Tênis de asfalto", updated.gearName)
            assertEquals("Evitar esse ritmo na próxima", updated.privateNotes)
            assertEquals(5_000L, updated.durationMs)
        } finally {
            database.close()
        }
    }

    /** Descartar cada estado aberto remove os pontos e não cria uma entrada no histórico. */
    @Test
    fun discardingUnfinishedRunsRemovesTheirRoutes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = database.runDao()
            val repository = RunRepository(dao)
            for (status in RunStatus.entries.filter { it != RunStatus.COMPLETED }) {
                val id = repository.startRun(1L)
                dao.updateState(id, status.name, null)
                dao.insertPoint(RunPointEntity(runId = id, latitude = -23.55, longitude = -46.63,
                    recordedAtEpochMs = 2L, accuracyMeters = 4f, segmentIndex = 0))
                repository.discardUnfinishedRun(id)
                assertNull(repository.getRun(id))
                assertEquals(0, repository.observePoints(id).first().size)
            }
            assertEquals(0, repository.completedRuns.first().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun localSegmentEffortsPersistIndependentlyForTheSameRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val firstRouteId = database.plannedRouteDao().insertRoute(
                PlannedRouteEntity(name = "Parque", distanceMeters = 1_000.0, importedAtEpochMs = 1L, isLocalSegment = true)
            )
            val secondRouteId = database.plannedRouteDao().insertRoute(
                PlannedRouteEntity(name = "Pista", distanceMeters = 500.0, importedAtEpochMs = 2L, isLocalSegment = true)
            )
            val runId = repository.startRun(1L)
            repository.beginRun(runId)

            repository.saveLocalSegmentEffort(runId, RunLocalSegmentEffort(firstRouteId, 100.0, 1_000L, null))
            repository.saveLocalSegmentEffort(runId, RunLocalSegmentEffort(secondRouteId, 200.0, 2_000L, null))
            repository.saveLocalSegmentEffort(runId, RunLocalSegmentEffort(firstRouteId, 450.0, 1_000L, 5_000L))

            val efforts = repository.getLocalSegmentEfforts(runId).associateBy(RunLocalSegmentEffort::routeId)
            assertEquals(2, efforts.size)
            assertEquals(450.0, efforts.getValue(firstRouteId).progressMeters, 0.0)
            assertEquals(4_000L, efforts.getValue(firstRouteId).durationMs)
            assertEquals(200.0, efforts.getValue(secondRouteId).progressMeters, 0.0)

            repository.finishRun(runId, 10_000L, 11_000L)
            repository.saveLocalSegmentEffort(runId, RunLocalSegmentEffort(secondRouteId, 500.0, 2_000L, 9_000L))
            assertEquals(2, repository.completedLocalSegmentEfforts.first().size)
            assertEquals(
                200.0,
                repository.getLocalSegmentEfforts(runId).single { it.routeId == secondRouteId }.progressMeters,
                0.0
            )
        } finally {
            database.close()
        }
    }
}
