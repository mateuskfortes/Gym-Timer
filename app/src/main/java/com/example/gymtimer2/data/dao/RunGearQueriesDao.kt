package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.gymtimer2.data.entity.RunGearSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface RunGearQueriesDao {
    @Query(
        "SELECT run_gears.id, run_gears.name, " +
            "COALESCE(SUM(runs.distance_meters), 0) AS distanceMeters, run_gears.is_default AS isDefault, " +
                "run_gears.is_retired AS isRetired, run_gears.photo_file_name AS photoFileName " +
            "FROM run_gears LEFT JOIN runs ON runs.gear_id = run_gears.id AND runs.status = 'COMPLETED' " +
            "GROUP BY run_gears.id ORDER BY run_gears.is_default DESC, run_gears.name COLLATE NOCASE"
    )
    fun observeGears(): Flow<List<RunGearSummaryRow>>

    @Query("SELECT id FROM run_gears WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findIdByName(name: String): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM run_gears WHERE is_default = 1)")
    suspend fun hasDefault(): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM run_gears WHERE id = :gearId)")
    suspend fun exists(gearId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM run_gears WHERE id = :gearId AND is_retired = 0)")
    suspend fun existsActive(gearId: Long): Boolean

    @Query("SELECT is_retired FROM run_gears WHERE id = :gearId")
    suspend fun isRetired(gearId: Long): Boolean?

    @Query("SELECT photo_file_name FROM run_gears WHERE id = :gearId")
    suspend fun getPhotoFileName(gearId: Long): String?
}
