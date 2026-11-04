package com.example.gymtimer2

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunLapEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.entity.RunBestEffortOverrideEntity
import com.example.gymtimer2.data.entity.PlannedRouteEntity
import com.example.gymtimer2.data.entity.PlannedRoutePointEntity
import com.example.gymtimer2.data.entity.RunStepRateEventEntity
import com.example.gymtimer2.data.repository.RunPrivacyZoneRepository
import com.example.gymtimer2.data.repository.RunWorkoutTemplateRepository
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.data.entity.toModel
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunWorkoutTrigger
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutSetPlan
import com.example.gymtimer2.domain.model.RunWorkoutStepPlan
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWeather
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
/** Verifica a migração Room e a preservação dos dados de treino. */
class WorkoutMigrationTest {
    @Test
    fun versionFiftyTwoPreservesLapsAndAddsElapsedTime() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-lap-elapsed-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/52.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1000, 20000, 100.0, 'RUNNING', 0)"
            )
            db.execSQL(
                "INSERT INTO run_laps (id, run_id, duration_ms, distance_meters, recorded_at_epoch_ms) " +
                    "VALUES (7, 42, 10000, 50.0, 11000)"
            )
            db.version = 52
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            assertEquals(53, database.openHelper.writableDatabase.version)
            val oldLap = database.runDao().getLaps(42L).single()
            assertEquals(10_000L, oldLap.durationMs)
            assertEquals(50.0, oldLap.distanceMeters, 0.0)
            assertNull(oldLap.elapsedDurationMs)

            database.runDao().insertLap(
                RunLapEntity(runId = 42L, durationMs = 20_000L, elapsedDurationMs = 35_000L, distanceMeters = 100.0)
            )
            assertEquals(35_000L, database.runDao().getLaps(42L).last().elapsedDurationMs)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionFortySevenAddsWeatherColumnsWithoutLosingSavedRuns() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-weather-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/47.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1000, 5000, 25.0, 'COMPLETED', 0)"
            )
            db.version = 47
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            assertEquals(52, database.openHelper.writableDatabase.version)
            assertTrue(database.runPrivacyZoneDao().getAll().isEmpty())
            val repository = RunRepository(database.runDao())
            val originalRun = checkNotNull(repository.getRun(42L))
            assertEquals(5_000L, originalRun.durationMs)
            assertEquals(25.0, originalRun.distanceMeters, 0.0)
            assertNull(originalRun.weather)
            assertEquals(RunStartEndHideDistance.NONE, originalRun.startEndHideDistance)

            val weather = RunWeather(18.5, 17.8, 61.0, 9.2, 1_793_577_600_000L)
            repository.updateWeather(42L, weather)
            assertEquals(weather, repository.getRun(42L)?.weather)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun privacyZonesAreSavedUpdatedAndDeletedLocally() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = RunPrivacyZoneRepository(database.runPrivacyZoneDao())
            repository.save(RunPrivacyZone(name = "Home", latitude = -23.5, longitude = -46.6, radiusMeters = 400))
            val original = repository.getAll().single()
            assertTrue(original.id > 0)

            repository.save(original.copy(name = "Apartment", radiusMeters = 600))
            assertEquals("Apartment", repository.getAll().single().name)
            assertEquals(600, repository.getAll().single().radiusMeters)

            repository.delete(original.id)
            assertTrue(repository.getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun workoutTemplatesCanBeSavedUpdatedAndDeletedLocally() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = RunWorkoutTemplateRepository(database.runWorkoutTemplateDao())
            val plan = RunWorkoutPlan(
                name = "Intervalado",
                sets = listOf(
                    RunWorkoutSetPlan(
                        steps = listOf(
                            RunWorkoutStepPlan(
                                description = "Ritmo forte",
                                trigger = RunWorkoutTrigger.DISTANCE,
                                distanceTargetMeters = 400.0,
                                paceTarget = RunPaceTarget(270, 300)
                            )
                        ),
                        repetitions = 4
                    )
                )
            )

            val id = checkNotNull(repository.save(null, plan))
            assertEquals(plan, repository.templates.first().single().plan)

            val updatedPlan = plan.copy(name = "Quatro tiros")
            assertEquals(id, repository.save(id, updatedPlan))
            assertEquals(updatedPlan, repository.templates.first().single().plan)

            repository.delete(id)
            assertTrue(repository.templates.first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun versionFortyFourPreservesMediaAndAddsMediaFlags() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-media-flags-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/44.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1000, 5000, 25.0, 'COMPLETED', 0)"
            )
            db.execSQL("INSERT INTO run_photos (id, run_id, file_name) VALUES (7, 42, 'saved.jpg')")
            db.version = 44
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val media = database.runDao().getRunMedia(42L).single()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("saved.jpg", media.fileName)
            assertFalse(media.isFeatured)
            assertFalse(media.isVideo)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionFortyFivePreservesFeaturedMediaAndAddsVideoType() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-media-video-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/45.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1000, 5000, 25.0, 'COMPLETED', 0)"
            )
            db.execSQL("INSERT INTO run_photos (id, run_id, file_name, is_featured) VALUES (7, 42, 'cover.jpg', 1)")
            db.version = 45
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val media = database.runDao().getRunMedia(42L).single()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("cover.jpg", media.fileName)
            assertTrue(media.isFeatured)
            assertFalse(media.isVideo)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionFortySixPreservesRunsWithoutInventingBodyMass() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-body-mass-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/46.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1000, 5000, 25.0, 'COMPLETED', 0)"
            )
            db.version = 46
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertNull(run.bodyMassKgAtStart)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionFortyThreePreservesRouteAndAddsOptionalActiveDuration() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-point-active-duration-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/43.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1000, 5000, 25.0, 'COMPLETED', 0)"
            )
            db.execSQL(
                "INSERT INTO run_points (id, run_id, latitude, longitude, recorded_at_epoch_ms, accuracy_meters, segment_index) " +
                    "VALUES (7, 42, -23.5, -46.6, 2000, 5.0, 0)"
            )
            db.version = 43
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val point = database.runDao().getRunPoints(42L).single().toModel()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(7L, point.id)
            assertEquals(-23.5, point.latitude, 0.0)
            assertNull(point.activeDurationMs)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionFortyTwoPreservesManualLapsWithoutRecordedTimestamp() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-lap-timestamp-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/42.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1000, 5000, 25.0, 'COMPLETED', 0)"
            )
            db.execSQL(
                "INSERT INTO run_laps (id, run_id, duration_ms, distance_meters) VALUES (7, 42, 3000, 15.0)"
            )
            db.version = 42
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val lap = database.runDao().observeLaps(42L).first().single()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(7L, lap.id)
            assertEquals(3_000L, lap.durationMs)
            assertEquals(15.0, lap.distanceMeters, 0.0)
            assertNull(lap.recordedAtEpochMs)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionFortyMigratesLocalSegmentEffortsAndPreservesOtherAttempts() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-local-segment-efforts-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/40.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO planned_routes (id, name, distanceMeters, importedAtEpochMs, is_local_segment) " +
                    "VALUES (7, 'Parque', 1000.0, 1, 1)"
            )
            db.execSQL(
                "INSERT INTO planned_routes (id, name, distanceMeters, importedAtEpochMs, is_local_segment) " +
                    "VALUES (8, 'GPX comum', 1000.0, 2, 0)"
            )
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, " +
                    "planned_route_id, local_segment_route_id, local_segment_progress_meters, " +
                    "local_segment_started_at_duration_ms, local_segment_completed_at_duration_ms) " +
                    "VALUES (42, 1, 1000, 1000.0, 'COMPLETED', 0, 7, 7, 750.0, 100, 900)"
            )
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, " +
                    "planned_route_id, local_segment_route_id, local_segment_progress_meters, " +
                    "local_segment_started_at_duration_ms, local_segment_completed_at_duration_ms) " +
                    "VALUES (43, 1, 1000, 1000.0, 'COMPLETED', 0, 7, NULL, 800.0, 150, 950)"
            )
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, " +
                    "planned_route_id, local_segment_progress_meters, " +
                    "local_segment_started_at_duration_ms, local_segment_completed_at_duration_ms) " +
                    "VALUES (45, 1, 1000, 1000.0, 'COMPLETED', 0, 8, 500.0, 100, 600)"
            )
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, " +
                    "local_segment_route_id, local_segment_progress_meters, local_segment_started_at_duration_ms) " +
                    "VALUES (44, 1, 1000, 1000.0, 'COMPLETED', 0, 99, 600.0, 200)"
            )
            db.version = 40
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(
                RunLocalSegmentEffort(7L, 750.0, 100L, 900L),
                database.runDao().getForRun(42L).single().let {
                    RunLocalSegmentEffort(it.routeId, it.progressMeters, it.startedAtDurationMs, it.completedAtDurationMs)
                }
            )
            val migratedRun = checkNotNull(database.runDao().getRun(42L))
            assertNull(migratedRun.localSegmentRouteId)
            assertEquals(0.0, migratedRun.localSegmentProgressMeters, 0.0)
            assertNull(migratedRun.localSegmentStartedAtDurationMs)

            assertEquals(
                RunLocalSegmentEffort(7L, 800.0, 150L, 950L),
                database.runDao().getForRun(43L).single().let {
                    RunLocalSegmentEffort(it.routeId, it.progressMeters, it.startedAtDurationMs, it.completedAtDurationMs)
                }
            )
            val selectedLocalRouteRun = checkNotNull(database.runDao().getRun(43L))
            assertNull(selectedLocalRouteRun.localSegmentStartedAtDurationMs)

            val ordinaryRouteRun = checkNotNull(database.runDao().getRun(45L))
            assertTrue(database.runDao().getForRun(45L).isEmpty())
            assertEquals(500.0, ordinaryRouteRun.localSegmentProgressMeters, 0.0)
            assertEquals(100L, ordinaryRouteRun.localSegmentStartedAtDurationMs)

            val orphanedRun = checkNotNull(database.runDao().getRun(44L))
            assertTrue(database.runDao().getForRun(44L).isEmpty())
            assertEquals(99L, orphanedRun.localSegmentRouteId)
            assertEquals(600.0, orphanedRun.localSegmentProgressMeters, 0.0)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyNineKeepsRoutesAndAddsLocalSegmentIdentity() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "local-segment-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/39.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1, 1000, 120.0, 'COMPLETED', 0)"
            )
            db.execSQL("INSERT INTO planned_routes (id, name, distanceMeters, importedAtEpochMs) VALUES (7, 'GPX', 120.0, 1)")
            db.execSQL(
                "INSERT INTO planned_route_points (route_id, point_index, latitude, longitude, segment_index) " +
                    "VALUES (7, 0, -23.55, -46.63, 0)"
            )
            db.version = 39
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertNull(database.runDao().getRun(42L)?.localSegmentRouteId)
            assertFalse(database.plannedRouteDao().observeRoutes().first().single().isLocalSegment)
            assertTrue(database.plannedRouteDao().observeLocalSegmentStarts().first().isEmpty())

            database.plannedRouteDao().insertImportedRoute(
                PlannedRouteEntity(
                    name = "Segmento",
                    distanceMeters = 120.0,
                    importedAtEpochMs = 2L,
                    isLocalSegment = true
                ),
                listOf(PlannedRoutePointEntity(0L, 0, -23.56, -46.64, 0))
            )
            val start = database.plannedRouteDao().observeLocalSegmentStarts().first().single()
            assertEquals("Segmento", start.name)
            assertEquals(-23.56, start.latitude, 0.0)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyEightAddsRouteFavoritesWithoutChangingSavedRoutes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "planned-route-favorites-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/38.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO planned_routes (id, name, distanceMeters, importedAtEpochMs) VALUES (7, 'Parque', 120.0, 1)")
            db.execSQL("INSERT INTO planned_routes (id, name, distanceMeters, importedAtEpochMs) VALUES (8, 'Pista', 200.0, 2)")
            db.execSQL(
                "INSERT INTO planned_route_points (route_id, point_index, latitude, longitude, segment_index) " +
                    "VALUES (7, 0, -23.55, -46.63, 0)"
            )
            db.version = 38
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val route = database.plannedRouteDao().observeRoutes().first().first { it.id == 7L }
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Parque", route.name)
            assertFalse(route.isFavorite)
            database.plannedRouteDao().setFavorite(route.id, true)
            val routes = database.plannedRouteDao().observeRoutes().first()
            assertTrue(routes.first().isFavorite)
            assertEquals(listOf("Parque", "Pista"), routes.map { it.name })
            assertEquals(-23.55, database.plannedRouteDao().observePoints(route.id).first().single().latitude, 0.0)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtySevenAddsPhotoTableWithoutChangingSavedRuns() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-photos-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/37.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, title) " +
                    "VALUES (42, 1, 1000, 120.0, 'COMPLETED', 0, 'Corrida preservada')"
            )
            db.version = 37
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val savedRun = checkNotNull(database.runDao().getRun(42L))
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Corrida preservada", savedRun.title)
            assertTrue(database.runDao().getRunMedia(42L).isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtySixRoutePointsKeepCoordinatesAndStartWithoutElevation() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "planned-route-elevation-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/36.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO planned_routes (id, name, distanceMeters, importedAtEpochMs) VALUES (7, 'Parque', 120.0, 1)")
            db.execSQL("INSERT INTO planned_route_points (route_id, point_index, latitude, longitude, segment_index) VALUES (7, 0, -23.55, -46.63, 0)")
            db.version = 36
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val point = database.plannedRouteDao().observePoints(7L).first().single()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(-23.55, point.latitude, 0.0)
            assertEquals(-46.63, point.longitude, 0.0)
            assertNull(point.altitudeMeters)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyFiveGearItemsBecomeActiveByDefault() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-gear-retirement-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/35.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO run_gears (id, name, is_default) VALUES (7, 'Tênis salvo', 1)")
            db.version = 35
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val gear = database.runGearDao().observeGears().first().single()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Tênis salvo", gear.name)
            assertTrue(gear.isDefault)
            assertFalse(gear.isRetired)
            assertNull(gear.photoFileName)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyFourEquipmentNamesMoveIntoCatalogAndKeepActivitySnapshot() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-gear-catalog-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/34.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, finished_at_epoch_ms, duration_ms, distance_meters, " +
                    "status, current_segment, gear_name, private_notes) " +
                    "VALUES (42, 1, 2, 60000, 1250.0, 'COMPLETED', 0, 'Tênis antigo', 'nota')"
            )
            db.version = 34
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            val gear = database.runGearDao().observeGears().first().single()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Tênis antigo", run.gearName)
            assertEquals("nota", run.privateNotes)
            assertEquals(gear.id, run.gearId)
            assertEquals(1_250.0, gear.distanceMeters, 0.0)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionEighteenWorkoutStepsSurviveElevationTriggerMigration() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-workout-elevation-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/18.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, workout_title) VALUES (42, 1, 5000, 120.0, 'RUNNING', 0, 'Intervalado')")
            db.execSQL("INSERT INTO run_workout_steps (run_id, step_index, description, trigger_type, duration_target_ms, distance_target_meters, is_rest_interval, repetition, repetition_count, pace_target_faster_seconds_per_km, pace_target_slower_seconds_per_km, started_at_duration_ms, started_at_distance_meters, completed_at_duration_ms, completed_at_distance_meters) VALUES (42, 0, 'Trote', 'DURATION', 60000, NULL, 0, 1, 1, 300, 360, 0, 0, NULL, NULL)")
            db.version = 18
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                WorkoutDatabase.MIGRATION_18_19,
                WorkoutDatabase.MIGRATION_19_20,
                WorkoutDatabase.MIGRATION_23_24,
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            )
            .build()
        try {
            val saved = database.runDao().getWorkoutSteps(42L).single()
            val step = saved.toModel()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Trote", step.description)
            assertEquals(RunWorkoutTrigger.DURATION, step.trigger)
            assertEquals(300, step.paceTarget?.fasterSecondsPerKm)
            assertNull(step.elevationChangeTargetMeters)
            assertNull(step.minimumElevationMeters)
            assertNull(step.maximumElevationMeters)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionTwentyRoutePointsDefaultToUnfilteredAfterMigration() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-filtered-point-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/20.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) VALUES (42, 1, 5000, 8.0, 'COMPLETED', 0)")
            db.execSQL("INSERT INTO run_points (run_id, latitude, longitude, recorded_at_epoch_ms, accuracy_meters, segment_index) VALUES (42, -23.55, -46.63, 2, 4, 0)")
            db.version = 20
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                WorkoutDatabase.MIGRATION_23_24,
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            )
            .build()
        try {
            val point = database.runDao().observePoints(42L).first().single()
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(-23.55, point.latitude, 0.0)
            assertFalse(point.isFiltered)
            assertNull(point.elapsedRealtimeMs)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionTwentyOneRunsGainLocalSegmentStateWithoutDataLoss() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-local-segment-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/21.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1, 5000, 120.0, 'COMPLETED', 0)"
            )
            db.version = 21
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                WorkoutDatabase.MIGRATION_23_24,
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            )
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(5_000L, run.durationMs)
            assertEquals(120.0, run.distanceMeters, 0.0)
            assertEquals(0.0, run.localSegmentProgressMeters, 0.0)
            assertNull(run.localSegmentStartedAtDurationMs)
            assertNull(run.localSegmentCompletedAtDurationMs)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    /** A migração adiciona velocidades vazias sem remover a rota gravada na versão 12. */
    @Test
    fun versionTwelveRouteSurvivesCadenceAndPlannedRouteMigrations() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-speed-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/12.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(indexPosition).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO runs (id, started_at_epoch_ms, finished_at_epoch_ms, duration_ms, distance_meters, status, current_segment, title) VALUES (42, 1, 2, 5000, 8.0, 'COMPLETED', 0, 'Corrida antiga')")
            db.execSQL("INSERT INTO run_points (run_id, latitude, longitude, recorded_at_epoch_ms, accuracy_meters, segment_index) VALUES (42, -23.55, -46.63, 2, 4, 0)")
            db.version = 12
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                WorkoutDatabase.MIGRATION_7_8,
                WorkoutDatabase.MIGRATION_8_9,
                WorkoutDatabase.MIGRATION_9_10,
                WorkoutDatabase.MIGRATION_17_18,
                WorkoutDatabase.MIGRATION_18_19,
                WorkoutDatabase.MIGRATION_19_20,
                WorkoutDatabase.MIGRATION_23_24,
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            )
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            val point = database.runDao().observePoints(42L).first().single()
            assertEquals("Corrida antiga", run.title)
            assertEquals(8.0, run.distanceMeters, 0.0)
            assertNull(run.plannedRouteRunId)
            assertEquals(-23.55, point.latitude, 0.0)
            assertNull(point.speedMetersPerSecond)
            assertNull(point.speedAccuracyMetersPerSecond)
            assertNull(point.cadenceStepsPerMinute)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    /** A coluna nova deve preservar corridas e pontos criados pela versão 9. */
    @Test
    fun versionNineRunsAndRoutesSurviveTitleMigration() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-title-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/9.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(indexPosition).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL("INSERT INTO runs (id, started_at_epoch_ms, finished_at_epoch_ms, duration_ms, distance_meters, status, current_segment) VALUES (42, 1, 2, 5000, 8.0, 'COMPLETED', 0)")
            db.execSQL("INSERT INTO run_points (run_id, latitude, longitude, recorded_at_epoch_ms, accuracy_meters, segment_index) VALUES (42, -23.55, -46.63, 2, 4, 0)")
            db.version = 9
        }
        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                WorkoutDatabase.MIGRATION_9_10,
                WorkoutDatabase.MIGRATION_17_18,
                WorkoutDatabase.MIGRATION_18_19,
                WorkoutDatabase.MIGRATION_19_20,
                WorkoutDatabase.MIGRATION_23_24,
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            ).build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals(5_000L, run.durationMs)
            assertEquals(8.0, run.distanceMeters, 0.0)
            assertNull(run.title)
            assertEquals(1, database.runDao().observePoints(42L).first().size)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionTwentyThreeCadenceSamplesMigrateWithoutDataLoss() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-cadence-event-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/23.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1, 5000, 20.0, 'COMPLETED', 0)"
            )
            db.execSQL(
                "INSERT INTO run_points (run_id, latitude, longitude, recorded_at_epoch_ms, accuracy_meters, " +
                    "segment_index, cadence_steps_per_minute) VALUES " +
                    "(42, -23.55, -46.63, 2000, 4, 0, 168), " +
                    "(42, -23.56, -46.64, 4000, 4, 0, 180)"
            )
            db.version = 23
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                WorkoutDatabase.MIGRATION_23_24,
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            )
            .build()
        try {
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(
                listOf(168, 180),
                database.runDao().observeStepRateEvents(42L).first().map { it.stepsPerMinute }
            )
            assertEquals(
                listOf(2000L, 4000L),
                database.runDao().observeStepRateEvents(42L).first().map { it.timestampEpochMs }
            )
            assertEquals(
                listOf(0L, 0L),
                database.runDao().observeStepRateEvents(42L).first().map { it.intervalDurationMs }
            )
            assertEquals(
                listOf(168, 180),
                database.runDao().observePoints(42L).first().map { it.cadenceStepsPerMinute }
            )

            database.runDao().insertStepRateEvent(
                RunStepRateEventEntity(
                    runId = 42L,
                    stepsPerMinute = 172,
                    stepCount = 8,
                    timestampEpochMs = 6000L,
                    intervalDurationMs = 2_000L
                )
            )
            assertEquals(3, database.runDao().observeStepRateEvents(42L).first().size)
            assertEquals(
                2_000L,
                database.runDao().observeStepRateEvents(42L).first().last().intervalDurationMs
            )
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    /** Mantém corridas antigas e salva o tipo escolhido em sessões novas. */
    fun versionTwentyFourRunsDefaultToRunAndPersistTrailRun() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-activity-type-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/24.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment) " +
                    "VALUES (42, 1, 5000, 120.0, 'COMPLETED', 0)"
            )
            db.version = 24
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            )
            .build()
        try {
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("RUN", database.runDao().getRun(42L)?.activityType)
            assertEquals(120.0, database.runDao().getRun(42L)?.distanceMeters ?: 0.0, 0.0)

            val repository = RunRepository(database.runDao())
            val trailRunId = repository.startRun(2L, activityType = RunActivityType.TRAIL_RUN)
            val virtualRunId = repository.startRun(3L, activityType = RunActivityType.VIRTUAL_RUN)
            val walkId = repository.startRun(4L, activityType = RunActivityType.WALK)
            val hikeId = repository.startRun(5L, activityType = RunActivityType.HIKE)
            val wheelchairRunId = repository.startRun(6L, activityType = RunActivityType.WHEELCHAIR)

            assertEquals("TRAIL_RUN", database.runDao().getRun(trailRunId)?.activityType)
            assertEquals(RunActivityType.TRAIL_RUN, repository.getRun(trailRunId)?.activityType)
            assertEquals(RunActivityType.VIRTUAL_RUN, repository.getRun(virtualRunId)?.activityType)
            assertEquals(RunActivityType.WALK, repository.getRun(walkId)?.activityType)
            assertEquals(RunActivityType.HIKE, repository.getRun(hikeId)?.activityType)
            assertEquals(RunActivityType.WHEELCHAIR, repository.getRun(wheelchairRunId)?.activityType)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionTwentySixRunsKeepRouteAlertsEnabledByDefault() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-route-alert-mute-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/26.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, activity_type) " +
                    "VALUES (42, 1, 5000, 120.0, 'RUNNING', 0, 'RUN')"
            )
            db.version = 26
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_26_27, WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertFalse(database.runDao().getRun(42L)?.routeAlertsMuted ?: true)

            val repository = RunRepository(database.runDao())
            repository.muteRouteAlerts(42L)
            assertTrue(repository.getRun(42L)?.routeAlertsMuted ?: false)
            assertEquals(120.0, repository.getRun(42L)?.distanceMeters ?: 0.0, 0.0)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    /** Confirma que a migração mantém exercícios e aceita persistência de uma corrida. */
    fun existingWorkoutAndRunTablesSurviveMigrationToCurrentVersion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "workout-migration-test"
        context.deleteDatabase(databaseName)

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).use { db ->
            db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, weight INTEGER NOT NULL, weightUnitCode INTEGER NOT NULL, chorusDelay INTEGER NOT NULL DEFAULT 0, restPeriod INTEGER NOT NULL DEFAULT 80000)")
            db.execSQL("CREATE TABLE songs (id INTEGER NOT NULL, title TEXT NOT NULL, artist TEXT NOT NULL, uri TEXT NOT NULL, duration_ms INTEGER, album TEXT, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE choruses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, song_id INTEGER NOT NULL, name TEXT NOT NULL, start_ms INTEGER NOT NULL, volume REAL, FOREIGN KEY(song_id) REFERENCES songs(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX index_choruses_song_id ON choruses(song_id)")
            db.execSQL("CREATE TABLE exercise_choruses (exercise_id INTEGER NOT NULL, chorus_id INTEGER NOT NULL, PRIMARY KEY(exercise_id, chorus_id), FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(chorus_id) REFERENCES choruses(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX index_exercise_choruses_exercise_id ON exercise_choruses(exercise_id)")
            db.execSQL("CREATE INDEX index_exercise_choruses_chorus_id ON exercise_choruses(chorus_id)")
            db.execSQL("INSERT INTO exercises (id, name, weight, weightUnitCode, chorusDelay, restPeriod) VALUES (7, 'Second', 20, 0, 0, 60000)")
            db.execSQL("INSERT INTO exercises (id, name, weight, weightUnitCode, chorusDelay, restPeriod) VALUES (3, 'First', 10, 0, 0, 90000)")
            db.version = 7
        }

        val roomDatabase = Room.databaseBuilder(context, WorkoutDatabase::class.java, databaseName)
            .addMigrations(
                WorkoutDatabase.MIGRATION_7_8,
                WorkoutDatabase.MIGRATION_8_9,
                WorkoutDatabase.MIGRATION_9_10,
                WorkoutDatabase.MIGRATION_17_18,
                WorkoutDatabase.MIGRATION_18_19,
                WorkoutDatabase.MIGRATION_19_20,
                WorkoutDatabase.MIGRATION_23_24,
                WorkoutDatabase.MIGRATION_24_25,
                WorkoutDatabase.MIGRATION_25_26,
                WorkoutDatabase.MIGRATION_26_27,
                WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36
            )
            .build()
        try {
            val db = roomDatabase.openHelper.writableDatabase
            db.query("SELECT name FROM workout_days WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Dia 1", cursor.getString(0))
            }
            db.query("SELECT exercise_id, sort_order FROM exercise_schedule ORDER BY sort_order").use { cursor ->
                cursor.moveToFirst()
                assertEquals(3, cursor.getInt(0))
                assertEquals(0, cursor.getInt(1))
                cursor.moveToNext()
                assertEquals(7, cursor.getInt(0))
                assertEquals(1, cursor.getInt(1))
                assertEquals(2, cursor.count)
            }
            db.query("SELECT SUM(weight) FROM exercises").use { cursor ->
                cursor.moveToFirst()
                assertEquals(30, cursor.getInt(0))
            }
            assertEquals(47, db.version)

            val runId = roomDatabase.runDao().insertRun(RunEntity(startedAtEpochMs = 1_700_000_000_000L))
            roomDatabase.runDao().appendPoint(
                RunPointEntity(
                    runId = runId,
                    latitude = -23.55,
                    longitude = -46.63,
                    recordedAtEpochMs = 1_700_000_002_000L,
                    accuracyMeters = 4f,
                    segmentIndex = 0,
                    speedMetersPerSecond = 2.5f,
                    speedAccuracyMetersPerSecond = 0.5f,
                    cadenceStepsPerMinute = 176,
                    elapsedRealtimeMs = 1_234L
                ),
                distanceMeters = 8.0
            )

            val savedPoint = roomDatabase.runDao().observePoints(runId).first().single()
            assertEquals(-23.55, savedPoint.latitude, 0.0)
            assertEquals(2.5f, savedPoint.speedMetersPerSecond!!, 0f)
            assertEquals(0.5f, savedPoint.speedAccuracyMetersPerSecond!!, 0f)
            assertEquals(176, savedPoint.cadenceStepsPerMinute!!)
            assertEquals(1_234L, savedPoint.elapsedRealtimeMs)
            assertEquals(8.0, roomDatabase.runDao().getRun(runId)?.distanceMeters ?: 0.0, 0.0)
            assertNull(roomDatabase.runDao().getRun(runId)?.plannedRouteId)
            assertNull(roomDatabase.runDao().getRun(runId)?.paceTargetFasterSecondsPerKm)
            assertNull(roomDatabase.runDao().getRun(runId)?.paceTargetSlowerSecondsPerKm)
        } finally {
            roomDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun versionTwentySevenRunsKeepDataAndGetEmptySaveMetadata() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-save-metadata-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/27.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(indexPosition).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, title) " +
                    "VALUES (42, 1, 5000, 8.0, 'COMPLETED', 0, 'Corrida antiga')"
            )
            db.version = 27
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_27_28, WorkoutDatabase.MIGRATION_28_29, WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals("Corrida antiga", run.title)
            assertEquals(8.0, run.distanceMeters, 0.0)
            assertNull(run.description)
            assertFalse(run.isCommute)
            assertNull(run.perceivedExertion)
            assertEquals("", run.activityTags)
            assertEquals(48, database.openHelper.writableDatabase.version)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionTwentyNineRunsKeepDataAndGetEmptyActivityTags() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-activity-tag-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/29.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(indexPosition).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, title, description, is_commute, perceived_exertion) " +
                    "VALUES (42, 1, 5000, 8.0, 'COMPLETED', 0, 'Corrida salva', 'Nota preservada', 1, 7)"
            )
            db.version = 29
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_29_30, WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Corrida salva", run.title)
            assertEquals("Nota preservada", run.description)
            assertTrue(run.isCommute)
            assertEquals(7, run.perceivedExertion)
            assertEquals("", run.activityTags)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyRunsKeepDataAndCanStoreBestEffortOverrides() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-best-effort-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/30.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(indexPosition).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, activity_tags) " +
                    "VALUES (42, 1, 5000, 8.0, 'COMPLETED', 0, 'RACE')"
            )
            db.version = 30
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_30_31, WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            val override = RunBestEffortOverrideEntity(42L, "ONE_KILOMETER", 240_000L)
            database.runDao().saveOverride(override)

            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("RACE", run.activityTags)
            assertEquals(override, database.runDao().observeOverrides(42L).first().single())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyOneRunsKeepDataAndGetElapsedTimeColumns() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-elapsed-time-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/31.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(indexPosition).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, activity_tags) " +
                    "VALUES (42, 1000, 50000, 100.0, 'COMPLETED', 0, 'RACE')"
            )
            db.version = 31
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals(50_000L, run.durationMs)
            assertEquals(100.0, run.distanceMeters, 0.0)
            assertEquals("RACE", run.activityTags)
            assertNull(run.elapsedDurationMs)
            assertNull(run.elapsedRealtimeAnchorMs)
            assertNull(run.elapsedEpochAnchorMs)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyTwoRunsKeepDataAndGetEmptyEquipmentName() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-equipment-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/32.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, title) " +
                    "VALUES (42, 1, 5000, 8.0, 'COMPLETED', 0, 'Corrida antiga')"
            )
            db.version = 32
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Corrida antiga", run.title)
            assertEquals(8.0, run.distanceMeters, 0.0)
            assertNull(run.gearName)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionThirtyThreeRunsKeepEquipmentAndGetEmptyPrivateNotes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "run-private-notes-migration-test"
        context.deleteDatabase(name)
        val schema = instrumentation.context.assets.open(
            "com.example.gymtimer2.data.database.WorkoutDatabase/33.json"
        ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (position in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            db.execSQL(
                "INSERT INTO runs (id, started_at_epoch_ms, duration_ms, distance_meters, status, current_segment, title, gear_name) " +
                    "VALUES (42, 1, 5000, 8.0, 'COMPLETED', 0, 'Corrida antiga', 'Tênis azul')"
            )
            db.version = 33
        }

        val database = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_34_35, WorkoutDatabase.MIGRATION_35_36)
            .build()
        try {
            val run = checkNotNull(database.runDao().getRun(42L))
            assertEquals(48, database.openHelper.writableDatabase.version)
            assertEquals("Corrida antiga", run.title)
            assertEquals("Tênis azul", run.gearName)
            assertNull(run.privateNotes)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
