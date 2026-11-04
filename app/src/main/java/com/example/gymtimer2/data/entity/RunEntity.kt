package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "runs",
    foreignKeys = [
        ForeignKey(
            entity = RunGearEntity::class,
            parentColumns = ["id"],
            childColumns = ["gear_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["status", "started_at_epoch_ms"]), Index(value = ["gear_id"])]
)
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
    val title: String? = null,
    /** Corrida salva usada como percurso-alvo; nulo para gravações sem rota. */
    @ColumnInfo(name = "planned_route_run_id")
    val plannedRouteRunId: Long? = null,
    /** Percurso GPX importado usado como alvo; nulo para rotas gravadas antigas. */
    @ColumnInfo(name = "planned_route_id")
    val plannedRouteId: Long? = null,
    @ColumnInfo(name = "local_segment_route_id")
    val localSegmentRouteId: Long? = null,
    @ColumnInfo(name = "pace_target_faster_seconds_per_km")
    val paceTargetFasterSecondsPerKm: Int? = null,
    @ColumnInfo(name = "pace_target_slower_seconds_per_km")
    val paceTargetSlowerSecondsPerKm: Int? = null,
    /** Nome do treino intervalado copiado para esta sessão, se houver. */
    @ColumnInfo(name = "workout_title")
    val workoutTitle: String? = null,
    /** Progresso da tentativa local no percurso-alvo, em metros. */
    @ColumnInfo(name = "local_segment_progress_meters", defaultValue = "0.0")
    val localSegmentProgressMeters: Double = 0.0,
    /** Tempo ativo da corrida em que a pessoa iniciou o percurso-alvo. */
    @ColumnInfo(name = "local_segment_started_at_duration_ms")
    val localSegmentStartedAtDurationMs: Long? = null,
    /** Tempo ativo da corrida em que a pessoa concluiu o percurso-alvo. */
    @ColumnInfo(name = "local_segment_completed_at_duration_ms")
    val localSegmentCompletedAtDurationMs: Long? = null,
    /** Tipo de atividade usado no histórico, compatível com os tipos RUN e TRAIL_RUN. */
    @ColumnInfo(name = "activity_type", defaultValue = "'RUN'")
    val activityType: String = "RUN",
    /** Indica que os avisos de afastamento foram silenciados até o fim desta corrida. */
    @ColumnInfo(name = "route_alerts_muted", defaultValue = "0")
    val routeAlertsMuted: Boolean = false,
    /** Descrição opcional definida ao salvar a corrida. */
    @ColumnInfo(name = "description")
    val description: String? = null,
    /** Indica uma corrida usada como deslocamento. */
    @ColumnInfo(name = "is_commute", defaultValue = "0")
    val isCommute: Boolean = false,
    /** Esforço percebido informado na escala de 1 a 10. */
    @ColumnInfo(name = "perceived_exertion")
    val perceivedExertion: Int? = null,
    /** Marcadores serializados pelos nomes estáveis de RunActivityTag. */
    @ColumnInfo(name = "activity_tags", defaultValue = "''")
    val activityTags: String = "",
    /** Tempo decorrido desde o início, incluindo pausas, atualizado por checkpoints. */
    @ColumnInfo(name = "elapsed_duration_ms")
    val elapsedDurationMs: Long? = null,
    /** Instante monotônico do último checkpoint do tempo decorrido. */
    @ColumnInfo(name = "elapsed_realtime_anchor_ms")
    val elapsedRealtimeAnchorMs: Long? = null,
    /** Instante civil do checkpoint para recuperar o tempo se o aparelho reiniciar. */
    @ColumnInfo(name = "elapsed_epoch_anchor_ms")
    val elapsedEpochAnchorMs: Long? = null,
    /** Nome do equipamento informado ao salvar a corrida. */
    @ColumnInfo(name = "gear_name")
    val gearName: String? = null,
    /** Nota pessoal armazenada somente neste dispositivo. */
    @ColumnInfo(name = "private_notes")
    val privateNotes: String? = null,
    /** ID local do equipamento; o nome continua como snapshot histórico. */
    @ColumnInfo(name = "gear_id")
    val gearId: Long? = null,
    /** Massa corporal capturada no início para estimar calorias nesta atividade. */
    @ColumnInfo(name = "body_mass_kg_at_start")
    val bodyMassKgAtStart: Double? = null,
    /** Clima opcional da atividade; campos antigos e falhas de rede ficam nulos. */
    @Embedded(prefix = "weather_")
    val weather: RunWeatherEntity? = null,
    /** Distância das extremidades removida dos arquivos exportados. */
    @ColumnInfo(name = "start_end_hide_distance_meters", defaultValue = "0")
    val startEndHideDistanceMeters: Int = 0
)
