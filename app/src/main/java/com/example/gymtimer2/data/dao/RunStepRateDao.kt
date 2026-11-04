@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymtimer2.data.entity.RunStepRateEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunStepRateDao {
    @Insert
    suspend fun insertStepRateEvent(event: RunStepRateEventEntity)

    @Query("SELECT * FROM run_step_rate_events WHERE run_id = :runId ORDER BY timestamp_epoch_ms, id")
    fun observeStepRateEvents(runId: Long): Flow<List<RunStepRateEventEntity>>

    @Query(
        "DELETE FROM run_step_rate_events WHERE run_id = :runId " +
            "AND (timestamp_epoch_ms < :startEpochMs OR timestamp_epoch_ms > :endEpochMs)"
    )
    suspend fun deleteStepRateEventsOutsideRange(runId: Long, startEpochMs: Long, endEpochMs: Long)

    @Query(
        "UPDATE run_step_rate_events SET interval_duration_ms = " +
            "MIN(interval_duration_ms, timestamp_epoch_ms - :startEpochMs) " +
            "WHERE run_id = :runId AND timestamp_epoch_ms BETWEEN :startEpochMs AND :endEpochMs " +
            "AND interval_duration_ms > timestamp_epoch_ms - :startEpochMs"
    )
    suspend fun trimStepRateIntervalsAtRangeStart(runId: Long, startEpochMs: Long, endEpochMs: Long)
}
