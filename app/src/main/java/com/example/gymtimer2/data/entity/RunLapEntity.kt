package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_laps",
    foreignKeys = [ForeignKey(entity = RunEntity::class, parentColumns = ["id"], childColumns = ["run_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["run_id", "id"])]
)
/** Marco de tempo e distância acumulados para uma volta manual. */
data class RunLapEntity(
    /** Identificador local do marco. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Corrida proprietária da volta. */
    @ColumnInfo(name = "run_id") val runId: Long,
    /** Tempo ativo acumulado no instante da marcação. */
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    /** Tempo decorrido acumulado, incluindo pausas; nulo nas voltas antigas. */
    @ColumnInfo(name = "elapsed_duration_ms") val elapsedDurationMs: Long? = null,
    /** Distância acumulada no instante da marcação, em metros. */
    @ColumnInfo(name = "distance_meters") val distanceMeters: Double,
    /** Horário civil da marcação, ausente nas corridas anteriores à versão 43. */
    @ColumnInfo(name = "recorded_at_epoch_ms") val recordedAtEpochMs: Long? = null
)
