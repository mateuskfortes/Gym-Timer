package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort

@Entity(
    tableName = "run_local_segment_efforts",
    primaryKeys = ["run_id", "route_id"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["run_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlannedRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["route_id"])]
)
data class RunLocalSegmentEffortEntity(
    @ColumnInfo(name = "run_id")
    val runId: Long,
    @ColumnInfo(name = "route_id")
    val routeId: Long,
    @ColumnInfo(name = "progress_meters", defaultValue = "0.0")
    val progressMeters: Double = 0.0,
    @ColumnInfo(name = "started_at_duration_ms")
    val startedAtDurationMs: Long? = null,
    @ColumnInfo(name = "completed_at_duration_ms")
    val completedAtDurationMs: Long? = null
)

fun RunLocalSegmentEffortEntity.toLocalSegmentEffort() = RunLocalSegmentEffort(
    routeId = routeId,
    progressMeters = progressMeters,
    startedAtDurationMs = startedAtDurationMs,
    completedAtDurationMs = completedAtDurationMs
)

fun RunLocalSegmentEffort.toLocalSegmentEffortEntity(runId: Long) = RunLocalSegmentEffortEntity(
    runId = runId,
    routeId = routeId,
    progressMeters = progressMeters,
    startedAtDurationMs = startedAtDurationMs,
    completedAtDurationMs = completedAtDurationMs
)
