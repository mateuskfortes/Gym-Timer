package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/** Exceção local ao tempo calculado para uma distância de uma corrida. */
@Entity(
    tableName = "run_best_effort_overrides",
    primaryKeys = ["run_id", "distance"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["run_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RunBestEffortOverrideEntity(
    @ColumnInfo(name = "run_id") val runId: Long,
    @ColumnInfo(name = "distance") val distance: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    @ColumnInfo(name = "is_removed", defaultValue = "0") val isRemoved: Boolean = false
)
