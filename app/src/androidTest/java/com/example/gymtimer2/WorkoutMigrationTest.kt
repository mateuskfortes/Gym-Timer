package com.example.gymtimer2

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutMigrationTest {
    @Test
    fun existingExercisesMoveToFirstDayInIdOrder() {
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
            .addMigrations(WorkoutDatabase.MIGRATION_7_8)
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
        } finally {
            roomDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }
}
