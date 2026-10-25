package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_points",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["run_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["run_id", "recorded_at_epoch_ms"])]
)
/** Linha Room que guarda uma posição GPS e seu trecho de rota. */
data class RunPointEntity(
    /** Chave local da coordenada GPS. */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Corrida à qual esta posição pertence. */
    @ColumnInfo(name = "run_id")
    val runId: Long,
    /** Latitude em graus decimais. */
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    /** Longitude em graus decimais. */
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    /** Instante de leitura em milissegundos desde a época Unix. */
    @ColumnInfo(name = "recorded_at_epoch_ms")
    val recordedAtEpochMs: Long,
    /** Precisão informada pelo GPS, em metros. */
    @ColumnInfo(name = "accuracy_meters")
    val accuracyMeters: Float,
    /** Índice do trecho, usado para não ligar pontos separados por pausa. */
    @ColumnInfo(name = "segment_index")
    val segmentIndex: Int
)
