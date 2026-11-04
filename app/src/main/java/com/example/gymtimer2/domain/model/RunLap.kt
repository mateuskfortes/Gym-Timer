package com.example.gymtimer2.domain.model

/** Marcador manual com as métricas acumuladas no instante da volta. */
data class RunLap(
    val id: Long,
    val durationMs: Long,
    val distanceMeters: Double,
    val recordedAtEpochMs: Long? = null,
    val elapsedDurationMs: Long? = null
)

/** Métricas desde a última volta manual concluída. */
data class RunLapMetrics(
    val durationMs: Long,
    val distanceMeters: Double,
    val elapsedDurationMs: Long? = null
) {
    /** Pace da volta atual em segundos por quilômetro, ou nulo sem distância. */
    val paceSecondsPerKm: Double?
        get() = RunMetrics.averagePaceSecondsPerKm(durationMs, distanceMeters)
}

/** Calcula as métricas acumuladas desde a volta anterior. */
fun RunLap.metricsSince(previousLap: RunLap?): RunLapMetrics = RunLapMetrics(
    durationMs = (durationMs - (previousLap?.durationMs ?: 0L)).coerceAtLeast(0L),
    distanceMeters = (distanceMeters - (previousLap?.distanceMeters ?: 0.0)).coerceAtLeast(0.0),
    elapsedDurationMs = elapsedDurationSince(previousLap)
)

private fun RunLap.elapsedDurationSince(previousLap: RunLap?): Long? = elapsedDurationMs?.let { current ->
    val previous = if (previousLap == null) 0L else previousLap.elapsedDurationMs ?: return null
    (current - previous).coerceAtLeast(0L)
}
