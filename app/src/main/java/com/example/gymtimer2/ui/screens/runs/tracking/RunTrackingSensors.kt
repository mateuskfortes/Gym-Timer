package com.example.gymtimer2.ui.screens.runs.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.gymtimer2.domain.model.RUN_STEP_RATE_SAMPLE_INTERVAL_MS
import com.example.gymtimer2.domain.model.RunMotionDetector
import com.example.gymtimer2.domain.model.RunMotionTransition
import com.example.gymtimer2.domain.model.RunStepRateAccumulator
import com.example.gymtimer2.domain.model.RunStepRateReading
import com.example.gymtimer2.domain.model.RunStepRateSample
import com.example.gymtimer2.domain.model.shouldCollectRunStepRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Gerencia sensores usados pela gravação e entrega somente eventos já interpretados. */
internal class RunTrackingSensors(
    context: Context,
    private val scope: CoroutineScope,
    private val currentRunId: () -> Long?,
    private val onMotionTransition: suspend (RunMotionTransition, () -> Boolean) -> Unit,
    private val onStepRateSample: suspend (RunStepRateReading, RunStepRateSample) -> Unit,
    private val onStepRateCleared: () -> Unit
) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val motionDetector = RunMotionDetector()
    private val stepRateAccumulator = RunStepRateAccumulator()
    private val latestMotionSample = FloatArray(MOTION_AXES_COUNT)
    private var motionListener: SensorEventListener? = null
    private var motionSamplingJob: Job? = null
    private var motionGeneration = 0L
    private var hasLatestMotionSample = false
    private var stepRateListener: SensorEventListener? = null
    private var stepRateSamplingJob: Job? = null
    private var stepRateGeneration = 0L

    @Volatile
    var stepRateReading: RunStepRateReading? = null
        private set

    fun startMotion(initiallyMoving: Boolean = false) {
        if (motionListener != null) return
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        motionDetector.reset(initiallyMoving)
        val generation = ++motionGeneration
        val listener = createMotionListener(generation)
        if (!sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)) return

        motionListener = listener
        motionSamplingJob = scope.launch { sampleMotion(generation) }
    }

    fun stopMotion() {
        motionGeneration++
        motionSamplingJob?.cancel()
        motionSamplingJob = null
        motionListener?.let(sensorManager::unregisterListener)
        motionListener = null
        hasLatestMotionSample = false
        motionDetector.reset()
    }

    fun startStepRate(preferenceEnabled: Boolean) {
        stopStepRate()
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val permissionGranted = preferenceEnabled && hasActivityRecognitionPermission()
        if (!shouldCollectRunStepRate(preferenceEnabled, sensor != null, permissionGranted)) return
        val stepCounterSensor = sensor ?: return
        val generation = stepRateGeneration
        val listener = createStepRateListener(generation)

        try {
            if (!sensorManager.registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)) return
            stepRateListener = listener
            stepRateSamplingJob = scope.launch { sampleStepRate(generation) }
        } catch (_: SecurityException) {
            stopStepRate()
        }
    }

    fun stopStepRate() {
        stepRateGeneration++
        stepRateSamplingJob?.cancel()
        stepRateSamplingJob = null
        stepRateListener?.let(sensorManager::unregisterListener)
        stepRateListener = null
        stepRateAccumulator.reset()
        stepRateReading = null
        onStepRateCleared()
    }

    private fun createMotionListener(generation: Long) = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (generation != motionGeneration || event.values.size < MOTION_AXES_COUNT) return
            event.values.copyInto(latestMotionSample, endIndex = MOTION_AXES_COUNT)
            hasLatestMotionSample = true
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private suspend fun sampleMotion(generation: Long) {
        while (scope.isActive && generation == motionGeneration) {
            delay(MOTION_SAMPLE_INTERVAL_MS)
            sampleMotionIfReady(generation)
        }
    }

    private suspend fun sampleMotionIfReady(generation: Long) {
        if (!hasLatestMotionSample) return
        val transition = motionDetector.sample(
            SystemClock.elapsedRealtime(),
            latestMotionSample[0],
            latestMotionSample[1],
            latestMotionSample[2]
        ) ?: return
        onMotionTransition(transition) { generation == motionGeneration }
    }

    private fun createStepRateListener(generation: Long) = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (generation != stepRateGeneration) return
            val totalSteps = event.values.lastOrNull()?.takeIf { it.isFinite() && it >= 0f }?.toInt() ?: return
            stepRateAccumulator.recordCounterSample(event.timestamp / NANOS_PER_MILLISECOND, totalSteps)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private suspend fun sampleStepRate(generation: Long) {
        while (scope.isActive && generation == stepRateGeneration) {
            delay(RUN_STEP_RATE_SAMPLE_INTERVAL_MS)
            val runId = currentRunId() ?: return
            val elapsedMs = SystemClock.elapsedRealtime()
            val sample = stepRateAccumulator.sample(elapsedMs, System.currentTimeMillis())
            val reading = RunStepRateReading(runId, elapsedMs, sample.stepsPerMinute)
            onStepRateSample(reading, sample)
            stepRateReading = reading
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED

    private companion object {
        const val MOTION_AXES_COUNT = 3
        const val MOTION_SAMPLE_INTERVAL_MS = 50L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
