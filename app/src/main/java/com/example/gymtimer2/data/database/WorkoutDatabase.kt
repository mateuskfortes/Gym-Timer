package com.example.gymtimer2.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gymtimer2.BuildConfig
import com.example.gymtimer2.data.dao.ChorusDao
import com.example.gymtimer2.data.dao.ExerciseDao
import com.example.gymtimer2.data.dao.ExerciseChorusDao
import com.example.gymtimer2.data.dao.ExerciseScheduleDao
import com.example.gymtimer2.data.dao.SongDao
import com.example.gymtimer2.data.dao.RunDao
import com.example.gymtimer2.data.dao.WorkoutDayDao
import com.example.gymtimer2.data.entity.ChorusEntity
import com.example.gymtimer2.data.entity.ExerciseChorusEntity
import com.example.gymtimer2.data.entity.ExerciseEntity
import com.example.gymtimer2.data.entity.ExerciseScheduleEntity
import com.example.gymtimer2.data.entity.SongEntity
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.entity.WorkoutDayEntity

@Database(
    entities = [
        ExerciseEntity::class,
        SongEntity::class,
        ChorusEntity::class,
        ExerciseChorusEntity::class,
        WorkoutDayEntity::class,
        ExerciseScheduleEntity::class,
        RunEntity::class,
        RunPointEntity::class
    ],
    version = 10,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
        AutoMigration (from = 2, to = 3),
        AutoMigration (from = 3, to = 4),
        AutoMigration (from = 4, to = 5, spec = WorkoutDatabase.Migration4To5::class),
        AutoMigration (from = 5, to = 6),
        AutoMigration (from = 6, to = 7)
    ],
    exportSchema = true)
abstract class WorkoutDatabase : RoomDatabase() {

    @DeleteColumn.Entries(
        DeleteColumn(
            tableName = "songs",
            columnName = "start_at_ms"
        )
    )
    class Migration4To5 : AutoMigrationSpec

    abstract fun exerciseDao(): ExerciseDao

    abstract fun songDao(): SongDao

    abstract fun chorusDao(): ChorusDao

    abstract fun exerciseChorusDao(): ExerciseChorusDao

    abstract fun workoutDayDao(): WorkoutDayDao

    abstract fun exerciseScheduleDao(): ExerciseScheduleDao

    abstract fun runDao(): RunDao

    companion object {
        /** Acrescenta o título opcional sem alterar corridas ou percursos existentes. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN title TEXT")
            }
        }

        /** Cria as tabelas locais de corridas e pontos GPS sem remover dados existentes. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `runs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `started_at_epoch_ms` INTEGER NOT NULL,
                            `finished_at_epoch_ms` INTEGER,
                            `duration_ms` INTEGER NOT NULL,
                            `distance_meters` REAL NOT NULL,
                            `status` TEXT NOT NULL,
                            `current_segment` INTEGER NOT NULL
                        )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_runs_status_started_at_epoch_ms` " +
                        "ON `runs` (`status`, `started_at_epoch_ms`)"
                )
                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `run_points` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `run_id` INTEGER NOT NULL,
                            `latitude` REAL NOT NULL,
                            `longitude` REAL NOT NULL,
                            `recorded_at_epoch_ms` INTEGER NOT NULL,
                            `accuracy_meters` REAL NOT NULL,
                            `segment_index` INTEGER NOT NULL,
                            FOREIGN KEY(`run_id`) REFERENCES `runs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_run_points_run_id_recorded_at_epoch_ms` " +
                        "ON `run_points` (`run_id`, `recorded_at_epoch_ms`)"
                )
            }
        }

        /** Cria os dias de treino e associa os exercícios antigos ao Dia 1. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                db.execSQL("INSERT INTO `workout_days` (`id`, `name`) VALUES (1, 'Dia 1')")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercise_schedule` (
                        `exercise_id` INTEGER NOT NULL,
                        `day_id` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        PRIMARY KEY(`exercise_id`),
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`day_id`) REFERENCES `workout_days`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_schedule_day_id_sort_order` ON `exercise_schedule` (`day_id`, `sort_order`)")
                db.execSQL("""
                    INSERT INTO `exercise_schedule` (`exercise_id`, `day_id`, `sort_order`)
                    SELECT e.id, 1, (SELECT COUNT(*) - 1 FROM exercises earlier WHERE earlier.id <= e.id)
                    FROM exercises e
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val databaseName = if (BuildConfig.DEBUG) {
                    "gym_timer_database_dev"
                } else {
                    "gym_timer_database"
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    databaseName
                )
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("INSERT INTO `workout_days` (`id`, `name`) VALUES (1, 'Dia 1')")
                        }
                    })
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
