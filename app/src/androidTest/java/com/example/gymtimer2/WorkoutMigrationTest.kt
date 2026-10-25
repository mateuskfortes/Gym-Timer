package com.example.gymtimer2

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
/** Verifica a migração Room e a preservação dos dados de treino. */
class WorkoutMigrationTest {
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
            .addMigrations(WorkoutDatabase.MIGRATION_9_10).build()
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
    /** Confirma que a migração mantém exercícios e aceita persistência de uma corrida. */
    fun existingWorkoutAndRunTablesSurviveMigrationToVersionTen() = runBlocking {
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
            .addMigrations(WorkoutDatabase.MIGRATION_7_8, WorkoutDatabase.MIGRATION_8_9, WorkoutDatabase.MIGRATION_9_10)
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
            assertEquals(10, db.version)

            val runId = roomDatabase.runDao().insertRun(RunEntity(startedAtEpochMs = 1_700_000_000_000L))
            roomDatabase.runDao().appendPoint(
                RunPointEntity(
                    runId = runId,
                    latitude = -23.55,
                    longitude = -46.63,
                    recordedAtEpochMs = 1_700_000_002_000L,
                    accuracyMeters = 4f,
                    segmentIndex = 0
                ),
                distanceMeters = 8.0
            )

            val savedPoint = roomDatabase.runDao().observePoints(runId).first().single()
            assertEquals(-23.55, savedPoint.latitude, 0.0)
            assertEquals(8.0, roomDatabase.runDao().getRun(runId)?.distanceMeters ?: 0.0, 0.0)
        } finally {
            roomDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }
}
