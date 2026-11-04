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
import com.example.gymtimer2.data.dao.PlannedRouteDao
import com.example.gymtimer2.data.dao.RunGearDao
import com.example.gymtimer2.data.dao.RunPrivacyZoneDao
import com.example.gymtimer2.data.dao.RunWorkoutTemplateDao
import com.example.gymtimer2.data.dao.WorkoutDayDao
import com.example.gymtimer2.data.entity.ChorusEntity
import com.example.gymtimer2.data.entity.ExerciseChorusEntity
import com.example.gymtimer2.data.entity.ExerciseEntity
import com.example.gymtimer2.data.entity.ExerciseScheduleEntity
import com.example.gymtimer2.data.entity.SongEntity
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunBestEffortOverrideEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.entity.RunStepRateEventEntity
import com.example.gymtimer2.data.entity.RunLapEntity
import com.example.gymtimer2.data.entity.RunWorkoutStepEntity
import com.example.gymtimer2.data.entity.PlannedRouteEntity
import com.example.gymtimer2.data.entity.PlannedRoutePointEntity
import com.example.gymtimer2.data.entity.RunGearEntity
import com.example.gymtimer2.data.entity.RunMediaEntity
import com.example.gymtimer2.data.entity.RunLocalSegmentEffortEntity
import com.example.gymtimer2.data.entity.RunPrivacyZoneEntity
import com.example.gymtimer2.data.entity.RunWorkoutTemplateEntity
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
        RunPointEntity::class,
        RunStepRateEventEntity::class,
        RunLapEntity::class,
        RunWorkoutStepEntity::class,
        PlannedRouteEntity::class,
        PlannedRoutePointEntity::class,
        RunBestEffortOverrideEntity::class,
        RunGearEntity::class,
        RunMediaEntity::class,
        RunLocalSegmentEffortEntity::class,
        RunPrivacyZoneEntity::class,
        RunWorkoutTemplateEntity::class
    ],
    version = 53,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
        AutoMigration (from = 2, to = 3),
        AutoMigration (from = 3, to = 4),
        AutoMigration (from = 4, to = 5, spec = WorkoutDatabase.Migration4To5::class),
        AutoMigration (from = 5, to = 6),
        AutoMigration (from = 6, to = 7),
        AutoMigration (from = 10, to = 11),
        AutoMigration (from = 11, to = 12),
        AutoMigration (from = 12, to = 13),
        AutoMigration (from = 13, to = 14),
        AutoMigration (from = 14, to = 15),
        AutoMigration (from = 15, to = 16),
        AutoMigration (from = 16, to = 17),
        AutoMigration (from = 20, to = 21),
        AutoMigration (from = 21, to = 22),
        AutoMigration (from = 22, to = 23),
        AutoMigration (from = 31, to = 32),
        AutoMigration (from = 32, to = 33),
        AutoMigration (from = 33, to = 34),
        AutoMigration (from = 36, to = 37),
        AutoMigration (from = 37, to = 38),
        AutoMigration (from = 38, to = 39),
        AutoMigration (from = 39, to = 40),
        AutoMigration (from = 40, to = 41, spec = WorkoutDatabase.Migration40To41::class),
        AutoMigration (from = 41, to = 42),
        AutoMigration (from = 42, to = 43),
        AutoMigration (from = 43, to = 44),
        AutoMigration (from = 44, to = 45),
        AutoMigration (from = 45, to = 46),
        AutoMigration (from = 46, to = 47),
        AutoMigration (from = 47, to = 48),
        AutoMigration (from = 48, to = 49),
        AutoMigration (from = 49, to = 50),
        AutoMigration (from = 50, to = 51),
        AutoMigration (from = 51, to = 52, spec = WorkoutDatabase.Migration51To52::class),
        AutoMigration (from = 52, to = 53)
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

    class Migration40To41 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO run_local_segment_efforts " +
                    "(run_id, route_id, progress_meters, started_at_duration_ms, completed_at_duration_ms) " +
                    "SELECT id, CASE WHEN EXISTS (SELECT 1 FROM planned_routes " +
                    "WHERE id = runs.local_segment_route_id) THEN local_segment_route_id ELSE planned_route_id END, " +
                    "local_segment_progress_meters, local_segment_started_at_duration_ms, " +
                    "local_segment_completed_at_duration_ms " +
                    "FROM runs WHERE EXISTS (SELECT 1 FROM planned_routes " +
                    "WHERE id = runs.local_segment_route_id) OR planned_route_id IN " +
                    "(SELECT id FROM planned_routes WHERE is_local_segment = 1)"
            )
            db.execSQL(
                "UPDATE runs SET local_segment_route_id = NULL, local_segment_progress_meters = 0.0, " +
                    "local_segment_started_at_duration_ms = NULL, " +
                    "local_segment_completed_at_duration_ms = NULL " +
                    "WHERE EXISTS (SELECT 1 FROM planned_routes " +
                    "WHERE id = runs.local_segment_route_id) OR planned_route_id IN " +
                    "(SELECT id FROM planned_routes WHERE is_local_segment = 1)"
            )
        }
    }

    @DeleteColumn(tableName = "run_workout_steps", columnName = "audio_cue_description")
    class Migration51To52 : AutoMigrationSpec

    abstract fun exerciseDao(): ExerciseDao

    abstract fun songDao(): SongDao

    abstract fun chorusDao(): ChorusDao

    abstract fun exerciseChorusDao(): ExerciseChorusDao

    abstract fun workoutDayDao(): WorkoutDayDao

    abstract fun exerciseScheduleDao(): ExerciseScheduleDao

    abstract fun runDao(): RunDao

    abstract fun plannedRouteDao(): PlannedRouteDao

    abstract fun runGearDao(): RunGearDao

    abstract fun runPrivacyZoneDao(): RunPrivacyZoneDao

    abstract fun runWorkoutTemplateDao(): RunWorkoutTemplateDao

    companion object {
        /** Marca equipamentos retirados sem apagar as atividades associadas. */
        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `run_gears` ADD COLUMN `is_retired` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Cria o catálogo local e associa os nomes de equipamento já salvos. */
        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `run_gears` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL COLLATE NOCASE, `is_default` INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_run_gears_name` ON `run_gears` (`name`)")
                db.execSQL(
                    "ALTER TABLE `runs` ADD COLUMN `gear_id` INTEGER " +
                        "REFERENCES `run_gears`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_runs_gear_id` ON `runs` (`gear_id`)")
                db.execSQL(
                    "INSERT OR IGNORE INTO `run_gears` (`name`) " +
                        "SELECT DISTINCT TRIM(`gear_name`) FROM `runs` " +
                        "WHERE `gear_name` IS NOT NULL AND TRIM(`gear_name`) != ''"
                )
                db.execSQL(
                    "UPDATE `runs` SET `gear_id` = (SELECT `id` FROM `run_gears` " +
                        "WHERE `run_gears`.`name` = TRIM(`runs`.`gear_name`) COLLATE NOCASE) " +
                        "WHERE `gear_name` IS NOT NULL AND TRIM(`gear_name`) != ''"
                )
            }
        }

        /** Acrescenta o título opcional sem alterar corridas ou percursos existentes. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN title TEXT")
            }
        }

        /** Guarda o título e as etapas do treino sem alterar corridas anteriores. */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN workout_title TEXT")
                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `run_workout_steps` (
                            `run_id` INTEGER NOT NULL,
                            `step_index` INTEGER NOT NULL,
                            `description` TEXT NOT NULL,
                            `trigger_type` TEXT NOT NULL,
                            `duration_target_ms` INTEGER,
                            `distance_target_meters` REAL,
                            `is_rest_interval` INTEGER NOT NULL,
                            `repetition` INTEGER NOT NULL,
                            `repetition_count` INTEGER NOT NULL,
                            `pace_target_faster_seconds_per_km` INTEGER,
                            `pace_target_slower_seconds_per_km` INTEGER,
                            `started_at_duration_ms` INTEGER,
                            `started_at_distance_meters` REAL,
                            `completed_at_duration_ms` INTEGER,
                            `completed_at_distance_meters` REAL,
                            PRIMARY KEY(`run_id`, `step_index`),
                            FOREIGN KEY(`run_id`) REFERENCES `runs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent()
                )
            }
        }

        /** Persiste o alvo e os extremos de elevação sem alterar o progresso anterior. */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE run_workout_steps ADD COLUMN elevation_change_target_meters REAL")
                db.execSQL("ALTER TABLE run_workout_steps ADD COLUMN minimum_elevation_meters REAL")
                db.execSQL("ALTER TABLE run_workout_steps ADD COLUMN minimum_elevation_at_duration_ms INTEGER")
                db.execSQL("ALTER TABLE run_workout_steps ADD COLUMN maximum_elevation_meters REAL")
                db.execSQL("ALTER TABLE run_workout_steps ADD COLUMN maximum_elevation_at_duration_ms INTEGER")
            }
        }

        /** Mantém a migração antiga compatível antes de remover o campo na versão 52. */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE run_workout_steps ADD COLUMN audio_cue_description TEXT")
            }
        }

        /** Separa as amostras de cadência sem perder leituras já salvas junto ao GPS. */
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `run_step_rate_events` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`run_id` INTEGER NOT NULL, " +
                        "`step_rate` INTEGER NOT NULL, " +
                        "`step_count` INTEGER NOT NULL DEFAULT 0, " +
                        "`timestamp_epoch_ms` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`run_id`) REFERENCES `runs`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_run_step_rate_events_run_id_timestamp_epoch_ms` " +
                        "ON `run_step_rate_events` (`run_id`, `timestamp_epoch_ms`)"
                )
                db.execSQL(
                    "INSERT INTO `run_step_rate_events` " +
                        "(`run_id`, `step_rate`, `step_count`, `timestamp_epoch_ms`) " +
                        "SELECT `run_id`, `cadence_steps_per_minute`, 1, `recorded_at_epoch_ms` " +
                        "FROM `run_points` WHERE `cadence_steps_per_minute` IS NOT NULL"
                )
            }
        }

        /** Acrescenta o tipo da corrida e mantém as sessões antigas como corridas comuns. */
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN activity_type TEXT NOT NULL DEFAULT 'RUN'")
            }
        }

        /** Registra o intervalo das amostras sem alterar eventos de cadência existentes. */
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE run_step_rate_events ADD COLUMN interval_duration_ms INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Permite silenciar os avisos de rota sem alterar sessões já existentes. */
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN route_alerts_muted INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds local activity metadata without changing completed runs. */
        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE runs ADD COLUMN is_commute INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds optional perceived exertion without changing existing activities. */
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN perceived_exertion INTEGER")
            }
        }

        /** Adds optional activity tags without changing existing activities. */
        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN activity_tags TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Adds local best-effort corrections without changing saved runs. */
        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `run_best_effort_overrides` (" +
                        "`run_id` INTEGER NOT NULL, `distance` TEXT NOT NULL, `duration_ms` INTEGER, " +
                        "`is_removed` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`run_id`, `distance`), " +
                        "FOREIGN KEY(`run_id`) REFERENCES `runs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
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
                    .addMigrations(
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_23_24,
                        MIGRATION_24_25,
                        MIGRATION_25_26,
                        MIGRATION_26_27,
                        MIGRATION_27_28,
                        MIGRATION_28_29,
                        MIGRATION_29_30,
                        MIGRATION_30_31,
                        MIGRATION_34_35,
                        MIGRATION_35_36
                    )
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
