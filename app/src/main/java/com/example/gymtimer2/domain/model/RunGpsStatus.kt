package com.example.gymtimer2.domain.model

/** Qualifica a disponibilidade do sinal usado para gravar a sessão. */
enum class RunGpsStatus { SEARCHING, NO_SIGNAL, WEAK, GOOD, DISABLED }

/** Leitura temporária do provedor GPS para atualizar as métricas ao vivo. */
data class RunGpsReading(
    /** Sessão à qual pertence esta leitura. */
    val runId: Long,
    /** Instante da leitura no relógio monotônico do Android. */
    val receivedAtElapsedMs: Long?,
    /** Precisão horizontal em metros. */
    val accuracyMeters: Float?,
    /** Indica se o provedor continua habilitado. */
    val providerEnabled: Boolean,
    /** Velocidade GPS recente em metros por segundo. */
    val speedMetersPerSecond: Float? = null,
    /** Instante monotônico em que a solicitação GPS começou, antes da primeira posição. */
    val requestStartedAtElapsedMs: Long? = null
)

/** Classifica o sinal GPS pela sessão, idade e precisão horizontal da leitura. */
fun runGpsStatus(reading: RunGpsReading?, runId: Long, nowElapsedMs: Long): RunGpsStatus {
    if (reading == null || reading.runId != runId) return RunGpsStatus.SEARCHING
    if (!reading.providerEnabled) return RunGpsStatus.DISABLED
    val receivedAt = reading.receivedAtElapsedMs ?: return reading.statusBeforeFirstFix(nowElapsedMs)
    return reading.statusForFix(receivedAt, nowElapsedMs)
}

private fun RunGpsReading.statusBeforeFirstFix(nowElapsedMs: Long): RunGpsStatus =
    requestStartedAtElapsedMs?.let { requestStartedAt ->
        if (nowElapsedMs - requestStartedAt >= RUN_GPS_SIGNAL_TIMEOUT_MS) {
            RunGpsStatus.NO_SIGNAL
        } else {
            RunGpsStatus.SEARCHING
        }
    } ?: RunGpsStatus.SEARCHING

private fun RunGpsReading.statusForFix(receivedAtElapsedMs: Long, nowElapsedMs: Long): RunGpsStatus {
    val age = nowElapsedMs - receivedAtElapsedMs
    if (age < 0) return RunGpsStatus.SEARCHING
    if (age > RUN_GPS_SIGNAL_TIMEOUT_MS) return RunGpsStatus.NO_SIGNAL
    val accuracy = accuracyMeters ?: return RunGpsStatus.WEAK
    return if (accuracy.isFinite() && accuracy in 0f..<RUN_GPS_ACCURATE_LIMIT_METERS) {
        RunGpsStatus.GOOD
    } else {
        RunGpsStatus.WEAK
    }
}

private const val RUN_GPS_SIGNAL_TIMEOUT_MS = 15_000L
private const val RUN_GPS_ACCURATE_LIMIT_METERS = 50f
private const val RUN_CURRENT_SPEED_MAX_AGE_MS = 3_000L

/** Converte uma leitura GPS recente em pace atual, sem estimar quando o sinal é fraco. */
fun currentPaceSecondsPerKm(reading: RunGpsReading?, runId: Long, nowElapsedMs: Long): Double? {
    val currentReading = reading?.takeIf { it.isCurrentFor(runId, nowElapsedMs) } ?: return null
    if (runGpsStatus(currentReading, runId, nowElapsedMs) != RunGpsStatus.GOOD) return null
    return currentReading.paceSecondsPerKm()
}

private fun RunGpsReading.isCurrentFor(runId: Long, nowElapsedMs: Long): Boolean {
    val ageMs = receivedAtElapsedMs?.let { nowElapsedMs - it } ?: return false
    return this.runId == runId && providerEnabled && ageMs in 0 until RUN_CURRENT_SPEED_MAX_AGE_MS
}

private fun RunGpsReading.paceSecondsPerKm(): Double? {
    val speed = speedMetersPerSecond?.takeIf {
        it.isFinite() && it > 0f
    } ?: return null
    return 1_000.0 / speed
}
