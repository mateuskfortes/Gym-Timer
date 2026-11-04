@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gymtimer2.data.entity.RunWorkoutStepEntity
import com.example.gymtimer2.data.entity.toEntity
import com.example.gymtimer2.data.entity.toModel
import com.example.gymtimer2.domain.model.RunElevationReading
import com.example.gymtimer2.domain.model.advanceRunWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface RunWorkoutDao {
    @Insert
    suspend fun insertWorkoutSteps(steps: List<RunWorkoutStepEntity>)

    @Query("DELETE FROM run_workout_steps WHERE run_id = :runId")
    suspend fun deleteWorkoutSteps(runId: Long)

    @Update
    suspend fun updateWorkoutSteps(steps: List<RunWorkoutStepEntity>)

    @Query("SELECT * FROM run_workout_steps WHERE run_id = :runId ORDER BY step_index")
    fun observeWorkoutSteps(runId: Long): Flow<List<RunWorkoutStepEntity>>

    @Query("SELECT * FROM run_workout_steps WHERE run_id = :runId ORDER BY step_index")
    suspend fun getWorkoutSteps(runId: Long): List<RunWorkoutStepEntity>

    @Query("SELECT * FROM run_workout_steps WHERE run_id = :runId AND started_at_duration_ms IS NOT NULL AND completed_at_duration_ms IS NULL ORDER BY step_index LIMIT 1")
    suspend fun getActiveWorkoutStep(runId: Long): RunWorkoutStepEntity?

    @Transaction
    suspend fun advanceWorkoutProgress(
        runId: Long,
        durationMs: Long,
        distanceMeters: Double,
        manualAdvance: Boolean = false,
        elevationReading: RunElevationReading? = null
    ): List<RunWorkoutStepEntity> {
        val steps = getWorkoutSteps(runId)
        val transition = advanceRunWorkout(
            steps = steps.map { it.toModel() },
            durationMs = durationMs,
            distanceMeters = distanceMeters,
            manualAdvance = manualAdvance,
            elevationReading = elevationReading
        )
        val previousSteps = steps.associate { it.stepIndex to it.toModel() }
        val changedSteps = transition.steps.filter { it != previousSteps[it.stepIndex] }.map { it.toEntity(runId) }
        if (changedSteps.isNotEmpty()) updateWorkoutSteps(changedSteps)
        return transition.completedSteps.map { it.toEntity(runId) }
    }
}
