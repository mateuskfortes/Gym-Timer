package com.example.gymtimer2.domain.model

import kotlin.math.roundToInt

internal const val RUN_STEP_RATE_SAMPLE_INTERVAL_MS = 2_000L
private const val MAX_RUN_STEP_RATE_AGE_MS = 5_000L
private const val MIN_RUN_CADENCE_STEPS_PER_MINUTE = 30
private const val MAX_RUN_CADENCE_STEPS_PER_MINUTE = 300

internal fun isValidRunCadence(stepsPerMinute: Int): Boolean =
    stepsPerMinute in MIN_RUN_CADENCE_STEPS_PER_MINUTE..MAX_RUN_CADENCE_STEPS_PER_MINUTE

/** Retorna verdadeiro quando a preferência, o sensor e a permissão permitem coletar cadência. */
internal fun shouldCollectRunStepRate(
    preferenceEnabled: Boolean,
    sensorAvailable: Boolean,
    permissionGranted: Boolean
): Boolean = preferenceEnabled && sensorAvailable && permissionGranted

/** Amostra de cadência agregada, com contagem e duração desde a amostra anterior. */
data class RunStepRateSample(
    val stepsPerMinute: Int,
    val stepCount: Int,
    val timestampEpochMs: Long,
    val intervalDurationMs: Long = 0
)

/** Leitura temporária da cadência recebida durante uma sessão. */
data class RunStepRateReading(
    /** Sessão ativa à qual pertencem os passos. */
    val runId: Long,
    /** Instante monotônico do último passo detectado, em milissegundos. */
    val measuredAtElapsedMs: Long,
    /** Passos por minuto calculados na janela recente. */
    val stepsPerMinute: Int
)

/** Converte o contador cumulativo do sensor em amostras de cadência. */
class RunStepRateAccumulator {
    private var previousSensorElapsedMs: Long? = null
    private var previousTotalSteps: Int? = null
    private var stepsPerMinute = 0
    private var lastRateUpdateElapsedMs: Long? = null
    private var accumulatedStepCount = 0
    private var previousSampleElapsedMs: Long? = null

    /** Atualiza a taxa quando o contador avança ao menos cinco passos. */
    fun recordCounterSample(timestampElapsedMs: Long, totalSteps: Int) {
        val previousTimestamp = previousSensorElapsedMs
        val previousSteps = previousTotalSteps
        if (previousTimestamp == null || previousSteps == null) {
            previousSensorElapsedMs = timestampElapsedMs
            previousTotalSteps = totalSteps
            return
        }

        if (totalSteps < previousSteps) {
            previousSensorElapsedMs = timestampElapsedMs
            previousTotalSteps = totalSteps
            stepsPerMinute = 0
            lastRateUpdateElapsedMs = null
            accumulatedStepCount = 0
            return
        }

        val stepDelta = totalSteps - previousSteps
        val elapsedMs = timestampElapsedMs - previousTimestamp
        if (stepDelta < MIN_BATCH_STEP_COUNT || elapsedMs <= 0L) return

        accumulatedStepCount += stepDelta
        stepsPerMinute = (stepDelta * MILLIS_PER_MINUTE / elapsedMs).roundToInt()
        lastRateUpdateElapsedMs = timestampElapsedMs
        previousSensorElapsedMs = timestampElapsedMs
        previousTotalSteps = totalSteps
    }

    /** Emite uma amostra para o período e zera sua contagem acumulada. */
    fun sample(nowElapsedMs: Long, timestampEpochMs: Long): RunStepRateSample {
        val rateAgeMs = lastRateUpdateElapsedMs?.let { nowElapsedMs - it }
        val isRateFresh = rateAgeMs?.let { it in 0L until MAX_RUN_STEP_RATE_AGE_MS } == true
        val currentRate = if (!isRateFresh || stepsPerMinute < MIN_REPORTED_STEPS_PER_MINUTE) {
            0
        } else {
            stepsPerMinute
        }
        val intervalDurationMs = previousSampleElapsedMs
            ?.let { nowElapsedMs - it }
            ?.takeIf { it > 0L }
            ?: RUN_STEP_RATE_SAMPLE_INTERVAL_MS
        previousSampleElapsedMs = nowElapsedMs
        return RunStepRateSample(currentRate, accumulatedStepCount, timestampEpochMs, intervalDurationMs).also {
            accumulatedStepCount = 0
        }
    }

    /** Descarta a linha de base anterior quando a corrida pausa ou termina. */
    fun reset() {
        previousSensorElapsedMs = null
        previousTotalSteps = null
        stepsPerMinute = 0
        lastRateUpdateElapsedMs = null
        accumulatedStepCount = 0
        previousSampleElapsedMs = null
    }

    private companion object {
        const val MIN_BATCH_STEP_COUNT = 5
        const val MIN_REPORTED_STEPS_PER_MINUTE = 60
        const val MILLIS_PER_MINUTE = 60_000.0
    }
}

/** Oculta a cadência de outra sessão ou sem passos recentes.
 * @param reading Última amostra disponibilizada pelo serviço.
 * @param runId Identificador da sessão que a tela está apresentando.
 * @param nowElapsedMs Horário monotônico atual em milissegundos.
 * @return Passos por minuto somente se a leitura ainda for válida.
 */
fun currentRunStepRate(reading: RunStepRateReading?, runId: Long, nowElapsedMs: Long): Int? {
    if (reading == null || reading.runId != runId) return null
    val ageMs = nowElapsedMs - reading.measuredAtElapsedMs
    if (ageMs !in 0L..MAX_RUN_STEP_RATE_AGE_MS) return null
    return reading.stepsPerMinute.takeIf(::isValidRunCadence)
}

/** Anexa ao ponto GPS a última cadência recente da mesma corrida. */
fun GpsPoint.withCurrentRunCadence(reading: RunStepRateReading?, runId: Long, nowElapsedMs: Long): GpsPoint =
    copy(cadenceStepsPerMinute = currentRunStepRate(reading, runId, nowElapsedMs))

/** Calcula média por tempo entre pontos GPS, com fallback para corridas sem intervalos válidos. */
fun averageRunCadence(points: List<RunPointModel>): Int? {
    val samples = points.filter { it.cadenceStepsPerMinute?.let(::isValidRunCadence) == true }
    if (samples.isEmpty()) return null

    val intervals = samples.zipWithNext().mapNotNull { (previous, current) ->
        if (previous.runId != current.runId || previous.segmentIndex != current.segmentIndex) return@mapNotNull null
        val previousElapsedMs = previous.elapsedRealtimeMs
        val currentElapsedMs = current.elapsedRealtimeMs
        val durationMs = if (previousElapsedMs != null && currentElapsedMs != null) {
            currentElapsedMs - previousElapsedMs
        } else {
            current.recordedAtEpochMs - previous.recordedAtEpochMs
        }
        if (durationMs !in 1L..MAX_RUN_STEP_RATE_AGE_MS) return@mapNotNull null
        val cadence = current.cadenceStepsPerMinute ?: return@mapNotNull null
        cadence to durationMs
    }
    if (intervals.isNotEmpty()) {
        val totalDurationMs = intervals.sumOf { it.second }
        return (intervals.sumOf { it.first.toLong() * it.second }.toDouble() / totalDurationMs).roundToInt()
    }

    return samples.mapNotNull { it.cadenceStepsPerMinute }.average().roundToInt()
}

/** Calcula a média ponderada pelo tempo das amostras independentes de cadência. */
fun averageRunCadenceFromEvents(samples: List<RunStepRateSample>): Int? {
    val validSamples = samples.filter { isValidRunCadence(it.stepsPerMinute) && it.stepCount > 0 }
    if (validSamples.isEmpty()) return null

    // shortcut: eventos antigos não registram duração, use contagem até que a corrida só tenha amostras novas.
    if (validSamples.all { it.intervalDurationMs > 0L }) {
        val totalDurationMs = validSamples.sumOf { it.intervalDurationMs }
        return (validSamples.sumOf { it.stepsPerMinute.toLong() * it.intervalDurationMs }.toDouble() /
            totalDurationMs).roundToInt()
    }

    val totalSteps = validSamples.sumOf { it.stepCount }
    return (validSamples.sumOf { it.stepsPerMinute.toLong() * it.stepCount }.toDouble() / totalSteps).roundToInt()
}
