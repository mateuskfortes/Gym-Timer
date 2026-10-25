package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "runs", indices = [Index(value = ["status", "started_at_epoch_ms"])])
/** Linha Room que guarda métricas e estado de uma corrida. */
data class RunEntity(
    /** Chave local da corrida. */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Data de início, em milissegundos desde a época Unix. */
    @ColumnInfo(name = "started_at_epoch_ms")
    val startedAtEpochMs: Long,
    /** Data de finalização; fica nula enquanto a corrida estiver aberta. */
    @ColumnInfo(name = "finished_at_epoch_ms")
    val finishedAtEpochMs: Long? = null,
    /** Tempo ativo acumulado, sem incluir pausas. */
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0,
    /** Distância total aceita pelo filtro GPS, em metros. */
    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double = 0.0,
    /** Estado serializado definido em RunStatus. */
    @ColumnInfo(name = "status")
    val status: String = "STARTING",
    /** Índice usado para separar trechos entre pausas. */
    @ColumnInfo(name = "current_segment")
    val currentSegment: Int = 0,
    /** Título opcional; corridas anteriores à versão 10 permanecem sem título. */
    @ColumnInfo(name = "title")
    val title: String? = null
)
