package com.example.gymtimer2.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gymtimer2.data.entity.ExerciseEntity
import com.example.gymtimer2.data.dao.ExerciseDao
import com.example.gymtimer2.data.dao.SongDao
import com.example.gymtimer2.data.entity.SongEntity

@Database(
    entities = [ExerciseEntity::class, SongEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ],
    exportSchema = true)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "gym_timer_database"
                )
                    // If a required migration (including downgrade) is missing,
                    // fall back to destructive migration to avoid runtime crashes.
                    // This will clear existing DB data. Provide explicit Migration
                    // objects if you need to preserve data across schema changes.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}