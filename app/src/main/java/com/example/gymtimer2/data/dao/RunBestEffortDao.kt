@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymtimer2.data.entity.RunBestEffortOverrideEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunBestEffortDao {
    @Query(
        "SELECT run_points.* FROM run_points INNER JOIN runs ON runs.id = run_points.run_id " +
            "WHERE runs.status = 'COMPLETED' AND run_points.run_id = :runId " +
            "AND run_points.is_filtered = 0 ORDER BY run_points.id"
    )
    suspend fun getCompletedPoints(runId: Long): List<RunPointEntity>

    @Query("SELECT * FROM run_best_effort_overrides WHERE run_id = :runId ORDER BY distance")
    fun observeOverrides(runId: Long): Flow<List<RunBestEffortOverrideEntity>>

    @Query("DELETE FROM run_best_effort_overrides WHERE run_id = :runId")
    suspend fun deleteOverrides(runId: Long)

    @Query("SELECT * FROM run_best_effort_overrides ORDER BY run_id, distance")
    fun observeAllOverrides(): Flow<List<RunBestEffortOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverride(override: RunBestEffortOverrideEntity)

    @Query("DELETE FROM run_best_effort_overrides WHERE run_id = :runId AND distance = :distance")
    suspend fun deleteOverride(runId: Long, distance: String)
}
