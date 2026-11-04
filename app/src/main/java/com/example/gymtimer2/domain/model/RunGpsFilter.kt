package com.example.gymtimer2.domain.model

import kotlin.math.abs

internal data class RunGpsFilterResult(
    val distanceMeters: Double?,
    val resetsAutoResumeWindow: Boolean = false
)

/** Aplica os filtros de precisão, aceleração e distância usados na gravação ao vivo. */
internal class RunGpsFilter {
    private var lastAcceptedPoint: GpsPoint? = null
    private var lastCandidateElapsedMs: Long? = null
    private var previousAccelerationPoint: GpsPoint? = null
    private var lastAccelerationPoint: GpsPoint? = null

    /** Filtra uma leitura e informa se ela invalida a janela de retomada automática. */
    fun filter(point: GpsPoint): RunGpsFilterResult {
        if (!point.hasValidAccuracy()) return RunGpsFilterResult(null, resetsAutoResumeWindow = true)
        val currentElapsedMs = point.elapsedRealtimeMs ?: return RunGpsFilterResult(null)
        if (!isValidCandidate(point, currentElapsedMs)) return RunGpsFilterResult(null)
        lastCandidateElapsedMs = currentElapsedMs
        if (hasExcessiveAcceleration(point, currentElapsedMs)) {
            return RunGpsFilterResult(null, resetsAutoResumeWindow = true)
        }

        rememberAccelerationPoint(point)
        return RunGpsFilterResult(distanceFromLastAccepted(point))
    }

    /** Reinicia o histórico, opcionalmente usando o último ponto aceito como âncora. */
    fun reset(anchor: GpsPoint? = null) {
        lastAcceptedPoint = anchor
        lastCandidateElapsedMs = anchor?.elapsedRealtimeMs
        previousAccelerationPoint = null
        lastAccelerationPoint = null
    }

    private fun hasExcessiveAcceleration(point: GpsPoint, currentElapsedMs: Long): Boolean {
        val previous = previousAccelerationPoint ?: return false
        val last = lastAccelerationPoint ?: return false
        val previousElapsedMs = previous.elapsedRealtimeMs ?: return false
        val lastElapsedMs = last.elapsedRealtimeMs ?: return false
        val previousIntervalSeconds = abs(lastElapsedMs - previousElapsedMs) / MILLIS_PER_SECOND
        val currentIntervalSeconds = abs(currentElapsedMs - lastElapsedMs) / MILLIS_PER_SECOND
        if (previousIntervalSeconds == 0.0 || currentIntervalSeconds == 0.0) return false

        val previousSpeed = RunMetrics.distanceMeters(previous, last) / previousIntervalSeconds
        val currentSpeed = RunMetrics.distanceMeters(last, point) / currentIntervalSeconds
        val acceleration = (currentSpeed - previousSpeed) / currentIntervalSeconds
        return acceleration >= MAX_ACCELERATION_METERS_PER_SECOND_SQUARED
    }

    private fun rememberAccelerationPoint(point: GpsPoint) {
        previousAccelerationPoint = lastAccelerationPoint
        lastAccelerationPoint = point
    }

    private fun isValidCandidate(point: GpsPoint, elapsedRealtimeMs: Long): Boolean {
        if (point.isFiltered || elapsedRealtimeMs <= 0L) return false
        val previousElapsedMs = lastCandidateElapsedMs ?: return true
        return elapsedRealtimeMs > previousElapsedMs
    }

    private fun distanceFromLastAccepted(point: GpsPoint): Double? {
        val previous = lastAcceptedPoint
        if (previous == null) {
            lastAcceptedPoint = point
            return 0.0
        }
        val distanceMeters = RunMetrics.distanceMeters(previous, point)
        if (distanceMeters < MIN_DISTANCE_METERS) return null
        lastAcceptedPoint = point
        return distanceMeters
    }

    private companion object {
        const val MIN_DISTANCE_METERS = 3.0
        const val MAX_ACCELERATION_METERS_PER_SECOND_SQUARED = 11.0
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
