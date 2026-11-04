package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.RunWorkoutTrigger

/** Etapa e marcos de execução de um treino ligado a uma única corrida. */
@Entity(
    tableName = "run_workout_steps",
    primaryKeys = ["run_id", "step_index"],
    foreignKeys = [ForeignKey(entity = RunEntity::class, parentColumns = ["id"], childColumns = ["run_id"], onDelete = ForeignKey.CASCADE)]
)
data class RunWorkoutStepEntity(
    @ColumnInfo(name = "run_id") val runId: Long,
    @ColumnInfo(name = "step_index") val stepIndex: Int,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "trigger_type") val triggerType: String,
    @ColumnInfo(name = "duration_target_ms") val durationTargetMs: Long?,
    @ColumnInfo(name = "distance_target_meters") val distanceTargetMeters: Double?,
    @ColumnInfo(name = "elevation_change_target_meters") val elevationChangeTargetMeters: Double?,
    @ColumnInfo(name = "is_rest_interval") val isRestInterval: Boolean,
    @ColumnInfo(name = "repetition") val repetition: Int,
    @ColumnInfo(name = "repetition_count") val repetitionCount: Int,
    @ColumnInfo(name = "pace_target_faster_seconds_per_km") val paceTargetFasterSecondsPerKm: Int?,
    @ColumnInfo(name = "pace_target_slower_seconds_per_km") val paceTargetSlowerSecondsPerKm: Int?,
    @ColumnInfo(name = "started_at_duration_ms") val startedAtDurationMs: Long?,
    @ColumnInfo(name = "started_at_distance_meters") val startedAtDistanceMeters: Double?,
    @ColumnInfo(name = "completed_at_duration_ms") val completedAtDurationMs: Long?,
    @ColumnInfo(name = "completed_at_distance_meters") val completedAtDistanceMeters: Double?,
    @ColumnInfo(name = "minimum_elevation_meters") val minimumElevationMeters: Double?,
    @ColumnInfo(name = "minimum_elevation_at_duration_ms") val minimumElevationAtDurationMs: Long?,
    @ColumnInfo(name = "maximum_elevation_meters") val maximumElevationMeters: Double?,
    @ColumnInfo(name = "maximum_elevation_at_duration_ms") val maximumElevationAtDurationMs: Long?
)

/** Converte uma linha antiga ou desconhecida em um gatilho manual seguro. */
fun RunWorkoutStepEntity.toModel() = RunWorkoutStep(
    stepIndex = stepIndex,
    description = description,
    trigger = RunWorkoutTrigger.entries.firstOrNull { it.name == triggerType } ?: RunWorkoutTrigger.MANUAL,
    durationTargetMs = durationTargetMs,
    distanceTargetMeters = distanceTargetMeters,
    elevationChangeTargetMeters = elevationChangeTargetMeters,
    isRestInterval = isRestInterval,
    repetition = repetition,
    repetitionCount = repetitionCount,
    paceTarget = if (paceTargetFasterSecondsPerKm != null && paceTargetSlowerSecondsPerKm != null &&
        RunPaceTarget.isValidRange(paceTargetFasterSecondsPerKm, paceTargetSlowerSecondsPerKm)
    ) RunPaceTarget(paceTargetFasterSecondsPerKm, paceTargetSlowerSecondsPerKm) else null,
    startedAtDurationMs = startedAtDurationMs,
    startedAtDistanceMeters = startedAtDistanceMeters,
    completedAtDurationMs = completedAtDurationMs,
    completedAtDistanceMeters = completedAtDistanceMeters,
    minimumElevationMeters = minimumElevationMeters,
    minimumElevationAtDurationMs = minimumElevationAtDurationMs,
    maximumElevationMeters = maximumElevationMeters,
    maximumElevationAtDurationMs = maximumElevationAtDurationMs
)

/** Cria o snapshot da etapa sem atribuir um ID de corrida ainda não gerado. */
fun RunWorkoutStep.toEntity(runId: Long) = RunWorkoutStepEntity(
    runId = runId,
    stepIndex = stepIndex,
    description = description,
    triggerType = trigger.name,
    durationTargetMs = durationTargetMs,
    distanceTargetMeters = distanceTargetMeters,
    elevationChangeTargetMeters = elevationChangeTargetMeters,
    isRestInterval = isRestInterval,
    repetition = repetition,
    repetitionCount = repetitionCount,
    paceTargetFasterSecondsPerKm = paceTarget?.fasterSecondsPerKm,
    paceTargetSlowerSecondsPerKm = paceTarget?.slowerSecondsPerKm,
    startedAtDurationMs = startedAtDurationMs,
    startedAtDistanceMeters = startedAtDistanceMeters,
    completedAtDurationMs = completedAtDurationMs,
    completedAtDistanceMeters = completedAtDistanceMeters,
    minimumElevationMeters = minimumElevationMeters,
    minimumElevationAtDurationMs = minimumElevationAtDurationMs,
    maximumElevationMeters = maximumElevationMeters,
    maximumElevationAtDurationMs = maximumElevationAtDurationMs
)
