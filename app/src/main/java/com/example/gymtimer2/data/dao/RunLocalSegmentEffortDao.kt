package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.gymtimer2.data.entity.RunLocalSegmentEffortEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunLocalSegmentEffortDao {
    @Query("SELECT * FROM run_local_segment_efforts WHERE run_id = :runId ORDER BY route_id")
    fun observeForRun(runId: Long): Flow<List<RunLocalSegmentEffortEntity>>

    @Query("SELECT * FROM run_local_segment_efforts WHERE run_id = :runId ORDER BY route_id")
    suspend fun getForRun(runId: Long): List<RunLocalSegmentEffortEntity>

    @Query(
        "SELECT run_local_segment_efforts.* FROM run_local_segment_efforts " +
            "INNER JOIN runs ON runs.id = run_local_segment_efforts.run_id " +
            "WHERE runs.status = 'COMPLETED'"
    )
    fun observeCompleted(): Flow<List<RunLocalSegmentEffortEntity>>

    @Query("SELECT status FROM runs WHERE id = :runId")
    suspend fun getRunStatus(runId: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(effort: RunLocalSegmentEffortEntity)

    @Query("DELETE FROM run_local_segment_efforts WHERE run_id = :runId")
    suspend fun deleteForRun(runId: Long)

    @Transaction
    suspend fun saveWhileRunning(effort: RunLocalSegmentEffortEntity) {
        if (getRunStatus(effort.runId) == "RUNNING") upsert(effort)
    }
}
