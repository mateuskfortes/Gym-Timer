package com.example.gymtimer2.domain.model

import kotlin.math.pow
import kotlin.math.sqrt

/** Mudança de gravação sugerida pelo detector de movimento. */
enum class RunMotionTransition { PAUSE, RESUME }

/** Detecta paradas e retomadas pela mudança suavizada da aceleração. */
class RunMotionDetector {
    private var previousSampleMs: Long? = null
    private var previousX = 0.0
    private var previousY = 0.0
    private var previousZ = 0.0
    private var fastSignal = INITIAL_FAST_SIGNAL
    private var slowSignal = MIN_SLOW_SIGNAL
    private var lastRunningSignal = INITIAL_LAST_RUNNING_SIGNAL
    private var isMoving = false

    /** Reinicia o sensor, podendo preservar que uma retomada automática já confirmou movimento. */
    fun reset(initiallyMoving: Boolean = false) {
        previousSampleMs = null
        fastSignal = if (initiallyMoving) INITIAL_LAST_RUNNING_SIGNAL else INITIAL_FAST_SIGNAL
        slowSignal = MIN_SLOW_SIGNAL
        lastRunningSignal = INITIAL_LAST_RUNNING_SIGNAL
        isMoving = initiallyMoving
    }

    /** Avalia uma amostra do acelerômetro; os eixos são medidos em m/s². */
    fun sample(timeMs: Long, x: Float, y: Float, z: Float): RunMotionTransition? {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        val previousTimeMs = previousSampleMs
        if (previousTimeMs == null) {
            saveSample(timeMs, x, y, z)
            return null
        }
        if (timeMs <= previousTimeMs) return null

        val elapsedMs = timeMs - previousTimeMs
        if (elapsedMs < MIN_SAMPLE_INTERVAL_MS) return null
        if (elapsedMs > MAX_SAMPLE_GAP_MS) {
            saveSample(timeMs, x, y, z)
            return updateMovementState()
        }

        updateSignals(elapsedMs / MILLIS_PER_SECOND, x, y, z)
        saveSample(timeMs, x, y, z)
        return updateMovementState()
    }

    private fun saveSample(timeMs: Long, x: Float, y: Float, z: Float) {
        previousSampleMs = timeMs
        previousX = x.toDouble()
        previousY = y.toDouble()
        previousZ = z.toDouble()
    }

    private fun updateSignals(elapsedSeconds: Double, x: Float, y: Float, z: Float) {
        val sampleSeconds = maxOf(elapsedSeconds, MIN_SAMPLE_INTERVAL_SECONDS)
        val jerk = jerkMagnitude(sampleSeconds, x, y, z)
        fastSignal = exponentialAverage(fastSignal, jerk, sampleSeconds, FAST_SIGNAL_HALF_LIFE_SECONDS)
        slowSignal = maxOf(
            MIN_SLOW_SIGNAL,
            exponentialAverage(slowSignal, jerk, sampleSeconds, SLOW_SIGNAL_HALF_LIFE_SECONDS)
        )
    }

    private fun jerkMagnitude(elapsedSeconds: Double, x: Float, y: Float, z: Float): Double {
        val jerkX = (x - previousX) / elapsedSeconds
        val jerkY = (y - previousY) / elapsedSeconds
        val jerkZ = (z - previousZ) / elapsedSeconds
        return sqrt(jerkX * jerkX + jerkY * jerkY + jerkZ * jerkZ)
    }

    private fun exponentialAverage(current: Double, sample: Double, elapsedSeconds: Double, halfLifeSeconds: Double): Double {
        val decay = 2.0.pow(-elapsedSeconds / halfLifeSeconds)
        return decay * current + (1.0 - decay) * sample
    }

    private fun updateMovementState(): RunMotionTransition? = when {
        isMoving && fastSignal * MOTION_RATIO < slowSignal -> {
            lastRunningSignal = slowSignal
            slowSignal = fastSignal
            isMoving = false
            RunMotionTransition.PAUSE
        }
        !isMoving && fastSignal > slowSignal * MOTION_RATIO -> {
            slowSignal = fastSignal
            isMoving = true
            RunMotionTransition.RESUME
        }
        !isMoving && fastSignal > lastRunningSignal -> {
            isMoving = true
            RunMotionTransition.RESUME
        }
        else -> null
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
        const val MIN_SAMPLE_INTERVAL_MS = 50L
        const val MAX_SAMPLE_GAP_MS = 200L
        const val MIN_SAMPLE_INTERVAL_SECONDS = 0.05
        const val FAST_SIGNAL_HALF_LIFE_SECONDS = 0.4
        const val SLOW_SIGNAL_HALF_LIFE_SECONDS = 10.0
        const val MIN_SLOW_SIGNAL = 50.0
        const val INITIAL_FAST_SIGNAL = 0.0
        const val INITIAL_LAST_RUNNING_SIGNAL = 150.0
        const val MOTION_RATIO = 3.0
    }
}
