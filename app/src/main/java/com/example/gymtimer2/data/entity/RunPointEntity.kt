package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtimer2.domain.model.RunPointModel

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
    val segmentIndex: Int,
    @ColumnInfo(name = "altitude_meters") val altitudeMeters: Double? = null,
    @ColumnInfo(name = "vertical_accuracy_meters") val verticalAccuracyMeters: Float? = null,
    /** Velocidade reportada pelo GPS em metros por segundo. */
    @ColumnInfo(name = "speed_meters_per_second") val speedMetersPerSecond: Float? = null,
    /** Margem de erro da velocidade, quando o Android a informa. */
    @ColumnInfo(name = "speed_accuracy_meters_per_second") val speedAccuracyMetersPerSecond: Float? = null,
    /** Cadência recebida do sensor de passos, em passos por minuto. */
    @ColumnInfo(name = "cadence_steps_per_minute") val cadenceStepsPerMinute: Int? = null,
    @ColumnInfo(name = "is_filtered", defaultValue = "0") val isFiltered: Boolean = false,
    /** Tempo monotônico do GPS, nulo em pontos preservados de versões anteriores. */
    @ColumnInfo(name = "elapsed_realtime_ms") val elapsedRealtimeMs: Long? = null,
    /** Tempo ativo acumulado quando a posição foi capturada, nulo em registros antigos. */
    @ColumnInfo(name = "active_duration_ms") val activeDurationMs: Long? = null
)

/** Converte uma leitura GPS persistida para o modelo do domínio. */
fun RunPointEntity.toModel() = RunPointModel(
    id = id,
    runId = runId,
    latitude = latitude,
    longitude = longitude,
    recordedAtEpochMs = recordedAtEpochMs,
    accuracyMeters = accuracyMeters,
    segmentIndex = segmentIndex,
    altitudeMeters = altitudeMeters,
    verticalAccuracyMeters = verticalAccuracyMeters,
    speedMetersPerSecond = speedMetersPerSecond,
    speedAccuracyMetersPerSecond = speedAccuracyMetersPerSecond,
    cadenceStepsPerMinute = cadenceStepsPerMinute,
    isFiltered = isFiltered,
    elapsedRealtimeMs = elapsedRealtimeMs,
    activeDurationMs = activeDurationMs
)
