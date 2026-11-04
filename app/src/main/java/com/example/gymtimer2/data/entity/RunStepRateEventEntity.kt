package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Amostra de cadência e passos acumulados em um intervalo curto da corrida. */
@Entity(
    tableName = "run_step_rate_events",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["run_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["run_id", "timestamp_epoch_ms"])]
)
data class RunStepRateEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "run_id")
    val runId: Long,
    @ColumnInfo(name = "step_rate")
    val stepsPerMinute: Int,
    @ColumnInfo(name = "step_count", defaultValue = "0")
    val stepCount: Int = 0,
    @ColumnInfo(name = "timestamp_epoch_ms")
    val timestampEpochMs: Long,
    @ColumnInfo(name = "interval_duration_ms", defaultValue = "0")
    val intervalDurationMs: Long = 0
)
