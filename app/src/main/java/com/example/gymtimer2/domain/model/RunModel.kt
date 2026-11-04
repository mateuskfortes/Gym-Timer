@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.domain.model

/** Estados que a sessão pode ter durante e depois da gravação. */
enum class RunStatus {
    STARTING,
    RUNNING,
    PAUSED,
    AUTO_PAUSED,
    INTERRUPTED,
    COMPLETED;

    /** Uma falha do processo pode retomar qualquer sessão que não foi pausada manualmente. */
    val canRecoverAfterProcessDeath: Boolean
        get() = this == STARTING || this == RUNNING || this == AUTO_PAUSED || this == INTERRUPTED
}

/** Tipos de atividade a pé que podem ser selecionados antes de iniciar a gravação. */
enum class RunActivityType {
    /** Corrida comum. */
    RUN,
    /** Corrida em trilha. */
    TRAIL_RUN,
    /** Corrida virtual. */
    VIRTUAL_RUN,
    /** Caminhada. */
    WALK,
    /** Trilha a pé. */
    HIKE,
    /** Atividade registrada com cadeira de rodas. */
    WHEELCHAIR;

    /** O Strava usa o acelerômetro para pausa automática em corrida e corrida em trilha. */
    val usesAccelerometerAutoPause: Boolean
        get() = this == RUN || this == TRAIL_RUN
}

/** Marcadores opcionais para classificar uma corrida salva. */
enum class RunActivityTag {
    /** Prova cronometrada. */
    RACE,
    /** Corrida de longa distância. */
    LONG_RUN,
    /** Treino intervalado ou em pista. */
    WORKOUT,
    /** Competição sem foco principal em tempo. */
    COMPETITION,
    /** Corrida de recuperação. */
    RECOVERY,
    /** Corrida acompanhada por um animal. */
    WITH_PET,
    /** Corrida acompanhada por uma criança. */
    WITH_KID,
    /** Corrida associada a uma causa. */
    FOR_A_CAUSE;

    companion object {
        /** Mantém uma ordem estável ao gravar os marcadores em uma única coluna. */
        fun serialize(tags: Set<RunActivityTag>): String =
            entries.filter { it in tags }.joinToString(",") { it.name }

        /** Ignora valores desconhecidos para continuar lendo registros futuros. */
        fun parse(value: String?): Set<RunActivityTag> = value.orEmpty()
            .split(',')
            .mapNotNull { name -> entries.firstOrNull { it.name == name.trim() } }
            .toSet()
    }
}

/** Dados salvos de uma corrida e as métricas calculadas a partir do tempo escolhido para a atividade. */
data class RunModel(
    /** Identificador local da sessão. */
    val id: Long,
    /** Instante em que a pessoa iniciou a corrida. */
    val startedAtEpochMs: Long,
    /** Instante de finalização, ou nulo enquanto a sessão estiver aberta. */
    val finishedAtEpochMs: Long?,
    /** Tempo ativo acumulado. Pausas não entram neste valor. */
    val durationMs: Long,
    /** Distância aceita pelo filtro GPS, em metros. */
    val distanceMeters: Double,
    /** Estado atual da sessão. */
    val status: RunStatus,
    /** Número do trecho atual. Um novo trecho começa após cada retomada. */
    val currentSegment: Int,
    /** Título opcional definido ao salvar a corrida. */
    val title: String? = null,
    /** ID local da corrida cujo traçado serve como percurso-alvo. */
    val plannedRouteRunId: Long? = null,
    /** ID local do percurso GPX selecionado, se não houver corrida-alvo. */
    val plannedRouteId: Long? = null,
    /** ID do segmento local associado automaticamente à tentativa desta corrida. */
    val localSegmentRouteId: Long? = null,
    /** Ritmo mais rápido da meta, em segundos por quilômetro. */
    val paceTargetFasterSecondsPerKm: Int? = null,
    /** Ritmo mais lento da meta, em segundos por quilômetro. */
    val paceTargetSlowerSecondsPerKm: Int? = null,
    /** Nome do treino estruturado copiado para esta sessão. */
    val workoutTitle: String? = null,
    /** Distância avançada no percurso-alvo local, em metros. */
    val localSegmentProgressMeters: Double = 0.0,
    /** Tempo ativo em que a tentativa no percurso começou. */
    val localSegmentStartedAtDurationMs: Long? = null,
    /** Tempo ativo em que a tentativa no percurso terminou. */
    val localSegmentCompletedAtDurationMs: Long? = null,
    /** Tipo de atividade selecionado antes da gravação. */
    val activityType: RunActivityType = RunActivityType.RUN,
    /** Indica que os avisos de afastamento foram silenciados até o fim desta corrida. */
    val routeAlertsMuted: Boolean = false,
    /** Descrição opcional salva com a atividade. */
    val description: String? = null,
    /** Indica que a atividade foi usada como deslocamento. */
    val isCommute: Boolean = false,
    /** Esforço percebido informado ao salvar a atividade, de 1 a 10. */
    val perceivedExertion: Int? = null,
    /** Marcadores adicionais salvos com a atividade. */
    val activityTags: Set<RunActivityTag> = emptySet(),
    /** Tempo decorrido confirmado pelo relógio monotônico, incluindo pausas. */
    val elapsedDurationMs: Long? = null,
    /** Instante monotônico do último checkpoint do tempo decorrido. */
    val elapsedRealtimeAnchorMs: Long? = null,
    /** Instante civil do último checkpoint, usado após reinício do aparelho. */
    val elapsedEpochAnchorMs: Long? = null,
    /** Nome local do equipamento associado à atividade. */
    val gearName: String? = null,
    /** Nota pessoal armazenada somente neste dispositivo. */
    val privateNotes: String? = null,
    /** ID do equipamento local associado à atividade. */
    val gearId: Long? = null,
    /** Massa corporal capturada no início para estimar calorias sem alterar o histórico depois. */
    val bodyMassKgAtStart: Double? = null,
    /** Snapshot meteorológico opcional mantido neste dispositivo. */
    val weather: RunWeather? = null,
    /** Distância das extremidades removida dos arquivos exportados. */
    val startEndHideDistance: RunStartEndHideDistance = RunStartEndHideDistance.NONE
) {
    /** Corridas marcadas como prova usam o tempo decorrido nas métricas do resumo. */
    val usesElapsedTimeForMetrics: Boolean
        get() = RunActivityTag.RACE in activityTags && finishedAtEpochMs != null

    /** Duração usada para pace e velocidade, incluindo pausas em provas. */
    val performanceDurationMs: Long
        get() = if (usesElapsedTimeForMetrics) {
            elapsedDurationMs ?: maxOf(durationMs, (finishedAtEpochMs!! - startedAtEpochMs).coerceAtLeast(0L))
        } else durationMs

    /** Faixa de pace válida, ou nula para corridas sem meta. */
    val paceTarget: RunPaceTarget?
        get() = if (paceTargetFasterSecondsPerKm != null && paceTargetSlowerSecondsPerKm != null &&
            RunPaceTarget.isValidRange(paceTargetFasterSecondsPerKm, paceTargetSlowerSecondsPerKm)) {
            RunPaceTarget(paceTargetFasterSecondsPerKm, paceTargetSlowerSecondsPerKm)
        } else null

    /** Pace médio em segundos por quilômetro, ou nulo sem distância. */
    val averagePaceSecondsPerKm: Double?
        get() = RunMetrics.averagePaceSecondsPerKm(performanceDurationMs, distanceMeters)

    /** Velocidade média em quilômetros por hora. */
    val averageSpeedKmPerHour: Double?
        get() = if (performanceDurationMs > 0 && distanceMeters >= 0 && distanceMeters.isFinite()) {
            distanceMeters * 3_600.0 / performanceDurationMs
        } else null

    /** Estimativa local; nula para atividades antigas sem massa configurada. */
    val estimatedCaloriesKcal: Int?
        get() = if (activityType in ESTIMATED_CALORIE_RUN_TYPES) {
            estimateRunCaloriesKcal(bodyMassKgAtStart, distanceMeters)
        } else null
}

private val ESTIMATED_CALORIE_RUN_TYPES = setOf(
    RunActivityType.RUN,
    RunActivityType.TRAIL_RUN,
    RunActivityType.VIRTUAL_RUN
)

/** Calcula o tempo decorrido desde o último checkpoint sem usar o relógio civil no mesmo boot. */
fun RunModel.elapsedDurationAt(nowElapsedRealtimeMs: Long, nowEpochMs: Long): Long {
    val savedDurationMs = elapsedDurationMs ?: return maxOf(
        durationMs,
        (nowEpochMs - startedAtEpochMs).coerceAtLeast(0L)
    )
    val monotonicDeltaMs = elapsedRealtimeAnchorMs
        ?.let { nowElapsedRealtimeMs - it }
        ?.takeIf { it >= 0L }
    val fallbackDeltaMs = elapsedEpochAnchorMs
        ?.let { (nowEpochMs - it).coerceAtLeast(0L) }
        ?: 0L
    return maxOf(durationMs, savedDurationMs + (monotonicDeltaMs ?: fallbackDeltaMs))
}

/** Uma posição GPS salva para desenhar a rota. */
data class RunPointModel(
    /** Chave da coordenada salva. */
    val id: Long,
    /** Corrida proprietária deste ponto. */
    val runId: Long,
    /** Latitude em graus decimais. */
    override val latitude: Double,
    /** Longitude em graus decimais. */
    override val longitude: Double,
    /** Instante da leitura em milissegundos desde a época Unix. */
    val recordedAtEpochMs: Long,
    /** Precisão reportada pelo GPS em metros. */
    val accuracyMeters: Float,
    /** Trecho de rota associado a esta posição. */
    override val segmentIndex: Int,
    override val altitudeMeters: Double? = null,
    override val verticalAccuracyMeters: Float? = null,
    /** Velocidade reportada pelo GPS em metros por segundo. */
    val speedMetersPerSecond: Float? = null,
    /** Margem de erro da velocidade, quando o Android a informa. */
    val speedAccuracyMetersPerSecond: Float? = null,
    /** Cadência do sensor de passos em passos por minuto. */
    val cadenceStepsPerMinute: Int? = null,
    /** Indica leitura GPS preservada, mas ignorada nas métricas e na rota visível. */
    override val isFiltered: Boolean = false,
    /** Tempo monotônico do aparelho, ausente em pontos salvos antes da versão 23. */
    val elapsedRealtimeMs: Long? = null,
    /** Tempo ativo acumulado no instante desta posição; ausente em rotas antigas. */
    val activeDurationMs: Long? = null
) : RoutePoint

/** Uma corrida e os pontos usados para desenhar sua rota. */
data class RunWithRoute(
    /** Métricas e estado da sessão. */
    val run: RunModel,
    /** Coordenadas ordenadas que formam a rota. */
    val points: List<RunPointModel>
)

/** Posição GPS em unidades do sistema Android. */
data class GpsPoint(
    /** Latitude em graus decimais. */
    val latitude: Double,
    /** Longitude em graus decimais. */
    val longitude: Double,
    /** Instante da leitura em milissegundos desde a época Unix. */
    val recordedAtEpochMs: Long,
    /** Precisão reportada pelo GPS em metros. */
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val verticalAccuracyMeters: Float? = null,
    /** Velocidade informada pelo GPS em metros por segundo. */
    val speedMetersPerSecond: Float? = null,
    /** Margem de erro informada pelo provedor de localização. */
    val speedAccuracyMetersPerSecond: Float? = null,
    /** Cadência observada na posição GPS, em passos por minuto. */
    val cadenceStepsPerMinute: Int? = null,
    /** Leitura preservada no histórico que não deve alterar métricas ou geometria. */
    val isFiltered: Boolean = false,
    /** Tempo monotônico usado para comparar leituras sem depender do relógio civil. */
    val elapsedRealtimeMs: Long? = null
)

private const val MAX_GPS_ACCURACY_METERS = 150.0

/** Cálculos puros usados pelo gravador e pela interface. */
object RunMetrics {
    private const val MIN_DISTANCE_METERS = 2.0
    private const val LIVE_MIN_DISTANCE_METERS = 3.0
    /** Faixa de velocidade aceita para métricas de corrida, em metros por segundo. */
    const val MIN_SPEED_METERS_PER_SECOND = 0.5f
    const val MAX_SPEED_METERS_PER_SECOND = 15f

    /** Retorna a distância se o ponto tiver precisão e velocidade válidas. */
    fun acceptedDistanceMeters(previous: GpsPoint?, current: GpsPoint): Double? {
        if (current.isFiltered) return null
        if (!current.hasValidAccuracy()) return null
        if (previous == null) return 0.0

        val hasMonotonicTime = previous.elapsedRealtimeMs != null && current.elapsedRealtimeMs != null
        val elapsedMs = elapsedIntervalMs(previous, current)
        if (elapsedMs <= 0L) return null

        val distanceMeters = distanceMeters(previous, current)
        val minimumDistanceMeters = if (hasMonotonicTime) LIVE_MIN_DISTANCE_METERS else MIN_DISTANCE_METERS
        if (distanceMeters < minimumDistanceMeters) return null

        val speedMetersPerSecond = distanceMeters / (elapsedMs / 1_000.0)
        if (!hasMonotonicTime && speedMetersPerSecond > MAX_SPEED_METERS_PER_SECOND.toDouble()) return null

        return distanceMeters
    }

    /** Usa tempo monotônico quando ambos os pontos o possuem e tempo civil nos dados antigos. */
    internal fun elapsedIntervalMs(previous: GpsPoint, current: GpsPoint): Long {
        val previousElapsedMs = previous.elapsedRealtimeMs
        val currentElapsedMs = current.elapsedRealtimeMs
        return if (previousElapsedMs != null && currentElapsedMs != null) {
            currentElapsedMs - previousElapsedMs
        } else {
            current.recordedAtEpochMs - previous.recordedAtEpochMs
        }
    }

    /** Calcula o pace médio. Milissegundos por metro equivalem a segundos por quilômetro. */
    fun averagePaceSecondsPerKm(durationMs: Long, distanceMeters: Double): Double? {
        if (durationMs <= 0L || distanceMeters <= 0.0) return null
        return durationMs / distanceMeters
    }

    /** Usa o pace da parcial atual sem treino; pausas e treinos usam o pace médio. */
    fun compactPaceSecondsPerKm(run: RunModel, currentSplitPaceSecondsPerKm: Double?): Double? =
        if (run.status == RunStatus.RUNNING && run.workoutTitle == null) {
            currentSplitPaceSecondsPerKm
        } else {
            run.averagePaceSecondsPerKm
        }

    /** Treinos estruturados usam métricas por etapa em vez das parciais automáticas. */
    fun shouldShowAutomaticSplits(run: RunModel): Boolean = run.workoutTitle == null

    /** Calcula as métricas desde a última marca manual concluída. */
    fun currentLapMetrics(
        activeDurationMs: Long,
        totalDistanceMeters: Double,
        completedLaps: List<RunLap>
    ): RunLapMetrics? {
        val lastLap = completedLaps.lastOrNull()
        if (!totalDistanceMeters.isFinite() || lastLap?.distanceMeters?.isFinite() == false) return null
        return RunLapMetrics(
            durationMs = (activeDurationMs - (lastLap?.durationMs ?: 0L)).coerceAtLeast(0L),
            distanceMeters = (totalDistanceMeters - (lastLap?.distanceMeters ?: 0.0)).coerceAtLeast(0.0)
        )
    }

    /** Formata o tempo como mm:ss ou hh:mm:ss. */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /** Formata o pace e mostra um marcador quando ainda não há distância. */
    fun formatPace(
        secondsPerKm: Double?,
        distanceUnit: RunDistanceUnit = RunDistanceUnit.METRIC
    ): String {
        if (secondsPerKm == null || !secondsPerKm.isFinite()) return "--:--/${distanceUnit.paceSuffix}"
        val totalSeconds = distanceUnit.paceSecondsPerUnit(secondsPerKm).toLong().coerceAtLeast(0L)
        return "%d:%02d/${distanceUnit.paceSuffix}".format(totalSeconds / 60L, totalSeconds % 60L)
    }

    /** Calcula a distância geográfica entre duas coordenadas com Haversine. */
    internal fun distanceMeters(first: GpsPoint, second: GpsPoint): Double {
        return distanceMetersBetweenCoordinates(
            first.latitude,
            first.longitude,
            second.latitude,
            second.longitude
        )
    }
}

/** Valida coordenadas e precisão antes de usar uma leitura GPS nas métricas. */
internal fun GpsPoint.hasValidAccuracy(): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0 &&
        accuracyMeters.isFinite() && accuracyMeters > 0f && accuracyMeters <= MAX_GPS_ACCURACY_METERS
