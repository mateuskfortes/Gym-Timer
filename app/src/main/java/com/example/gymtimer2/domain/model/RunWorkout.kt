package com.example.gymtimer2.domain.model

import kotlin.math.max

/** Gatilho que encerra uma etapa do treino de corrida. */
enum class RunWorkoutTrigger { DURATION, DISTANCE, ELEVATION, MANUAL }

/** Etapa configurável antes da corrida. Distâncias estão em metros e tempos em milissegundos. */
data class RunWorkoutStepPlan(
    val description: String,
    val trigger: RunWorkoutTrigger,
    val durationTargetMs: Long? = null,
    val distanceTargetMeters: Double? = null,
    val elevationChangeTargetMeters: Double? = null,
    val isRestInterval: Boolean = false,
    val paceTarget: RunPaceTarget? = null
)

/** Bloco ordenado do treino, repetido como uma unidade. */
data class RunWorkoutSetPlan(
    val steps: List<RunWorkoutStepPlan>,
    val repetitions: Int = 1,
    val omitLastRecoveryStep: Boolean = false
)

/** Treino local que será copiado para a corrida iniciada. */
data class RunWorkoutPlan(val name: String, val sets: List<RunWorkoutSetPlan>) {
    /** Normaliza campos e recusa valores que não podem ser executados com segurança. */
    fun normalizedOrNull(): RunWorkoutPlan? {
        val cleanName = name.trim().take(MAX_WORKOUT_NAME_LENGTH)
        if (cleanName.isEmpty() || sets.isEmpty() || sets.size > MAX_WORKOUT_SET_COUNT) return null
        val cleanSets = sets.map { it.normalizedOrNull() ?: return null }
        val plannedStepCount = cleanSets.sumOf { it.expandedStepCount() }
        if (plannedStepCount !in 1..MAX_EXPANDED_STEP_COUNT) return null
        if (cleanSets.sumOf { it.steps.size } > MAX_PLAN_STEP_COUNT) return null
        return copy(name = cleanName, sets = cleanSets)
    }

    /** Expande blocos em etapas planas para retomar o treino com o estado salvo no Room. */
    fun expandedSteps(): List<RunWorkoutStep> = buildList {
        sets.forEach { set ->
            repeat(set.repetitions) { repetitionIndex ->
                val steps = if (set.shouldOmitFinalRecovery(repetitionIndex)) set.steps.dropLast(1) else set.steps
                steps.forEach { planStep ->
                    add(
                        RunWorkoutStep(
                            stepIndex = size,
                            description = planStep.description,
                            trigger = planStep.trigger,
                            durationTargetMs = planStep.durationTargetMs,
                            distanceTargetMeters = planStep.distanceTargetMeters,
                            elevationChangeTargetMeters = planStep.elevationChangeTargetMeters,
                            isRestInterval = planStep.isRestInterval,
                            repetition = repetitionIndex + 1,
                            repetitionCount = set.repetitions,
                            paceTarget = planStep.paceTarget
                        )
                    )
                }
            }
        }
    }
}

private fun RunWorkoutSetPlan.normalizedOrNull(): RunWorkoutSetPlan? {
    if (repetitions !in 1..MAX_SET_REPETITIONS || steps.isEmpty()) return null
    val cleanSteps = steps.map { it.normalizedOrNull() ?: return null }
    val cleanSet = copy(
        steps = cleanSteps,
        omitLastRecoveryStep = omitLastRecoveryStep && repetitions > 1 && cleanSteps.last().isRestInterval
    )
    return cleanSet.takeIf { it.expandedStepCount() > 0 }
}

private fun RunWorkoutSetPlan.expandedStepCount(): Int =
    repetitions * steps.size - if (omitLastRecoveryStep && steps.last().isRestInterval) 1 else 0

private fun RunWorkoutSetPlan.shouldOmitFinalRecovery(repetitionIndex: Int): Boolean =
    omitLastRecoveryStep && repetitionIndex == repetitions - 1 && steps.last().isRestInterval

/** Etapa da sessão com os marcos de execução persistidos localmente. */
data class RunWorkoutStep(
    val stepIndex: Int,
    val description: String,
    val trigger: RunWorkoutTrigger,
    val durationTargetMs: Long? = null,
    val distanceTargetMeters: Double? = null,
    val elevationChangeTargetMeters: Double? = null,
    val isRestInterval: Boolean = false,
    val repetition: Int = 1,
    val repetitionCount: Int = 1,
    val paceTarget: RunPaceTarget? = null,
    val startedAtDurationMs: Long? = null,
    val startedAtDistanceMeters: Double? = null,
    val completedAtDurationMs: Long? = null,
    val completedAtDistanceMeters: Double? = null,
    val minimumElevationMeters: Double? = null,
    val minimumElevationAtDurationMs: Long? = null,
    val maximumElevationMeters: Double? = null,
    val maximumElevationAtDurationMs: Long? = null
)

/** Resultado de avanço do treino; concluídas permite anunciar cada transição uma vez. */
data class RunWorkoutTransition(
    val steps: List<RunWorkoutStep>,
    val completedSteps: List<RunWorkoutStep>
)

/** Avança as etapas vencidas usando tempo, distância, elevação ou ação manual. */
fun advanceRunWorkout(
    steps: List<RunWorkoutStep>,
    durationMs: Long,
    distanceMeters: Double,
    manualAdvance: Boolean = false,
    elevationReading: RunElevationReading? = null
): RunWorkoutTransition {
    if (durationMs < 0L || !distanceMeters.isFinite() || distanceMeters < 0.0) {
        return RunWorkoutTransition(steps, emptyList())
    }
    val updated = steps.sortedBy { it.stepIndex }.toMutableList()
    val completed = mutableListOf<RunWorkoutStep>()
    var allowManualAdvance = manualAdvance
    var currentIndex = updated.indexOfFirst { it.startedAtDurationMs != null && it.completedAtDurationMs == null }
    while (currentIndex >= 0) {
        val progress = updated[currentIndex].advance(
            durationMs = durationMs,
            distanceMeters = distanceMeters,
            manualAdvance = allowManualAdvance,
            elevationReading = elevationReading
        )
        updated[currentIndex] = progress.step
        if (!progress.reached) break
        val current = progress.step.copy(
            completedAtDurationMs = durationMs,
            completedAtDistanceMeters = distanceMeters
        )
        updated[currentIndex] = current
        completed += current
        currentIndex = updated.indexOfFirst { it.stepIndex == current.stepIndex + 1 }
        if (currentIndex >= 0) {
            updated[currentIndex] = updated[currentIndex].copy(
                startedAtDurationMs = durationMs,
                startedAtDistanceMeters = distanceMeters
            ).startElevationTracking(elevationReading, durationMs)
            allowManualAdvance = false
        }
    }
    return RunWorkoutTransition(updated, completed)
}

private fun RunWorkoutStepPlan.normalizedOrNull(): RunWorkoutStepPlan? {
    val cleanDescription = description.trim().take(MAX_STEP_DESCRIPTION_LENGTH)
    if (cleanDescription.isEmpty()) return null
    if (!hasValidTarget()) return null
    return copy(description = cleanDescription)
}

private fun RunWorkoutStepPlan.hasValidTarget(): Boolean = when (trigger) {
    RunWorkoutTrigger.DURATION -> durationTargetMs.isValidDuration() && distanceTargetMeters == null && elevationChangeTargetMeters == null
    RunWorkoutTrigger.DISTANCE -> distanceTargetMeters.isValidDistance() && durationTargetMs == null && elevationChangeTargetMeters == null
    RunWorkoutTrigger.ELEVATION -> elevationChangeTargetMeters.isValidElevationChange() && durationTargetMs == null && distanceTargetMeters == null
    RunWorkoutTrigger.MANUAL -> durationTargetMs == null && distanceTargetMeters == null && elevationChangeTargetMeters == null
}

private fun Long?.isValidDuration(): Boolean = this != null && this in MIN_STEP_DURATION_MS..MAX_STEP_DURATION_MS

private fun Double?.isValidDistance(): Boolean = this != null && this.isFinite() && this in MIN_STEP_DISTANCE_METERS..MAX_STEP_DISTANCE_METERS

private fun Double?.isValidElevationChange(): Boolean = this != null && this.isFinite() && this in MIN_ELEVATION_CHANGE_METERS..MAX_ELEVATION_CHANGE_METERS

private fun RunWorkoutStep.advance(
    durationMs: Long,
    distanceMeters: Double,
    manualAdvance: Boolean,
    elevationReading: RunElevationReading?
): RunWorkoutStepProgress {
    if (completedAtDurationMs != null) return RunWorkoutStepProgress(this, false)
    return when (trigger) {
        RunWorkoutTrigger.DURATION -> RunWorkoutStepProgress(
            this,
            startedAtDurationMs?.let { durationMs - it >= (durationTargetMs ?: Long.MAX_VALUE) } == true
        )
        RunWorkoutTrigger.DISTANCE -> RunWorkoutStepProgress(
            this,
            startedAtDistanceMeters?.let { distanceMeters - it >= (distanceTargetMeters ?: Double.POSITIVE_INFINITY) } == true
        )
        RunWorkoutTrigger.ELEVATION -> advanceElevation(elevationReading, durationMs)
        RunWorkoutTrigger.MANUAL -> RunWorkoutStepProgress(this, manualAdvance)
    }
}

private fun RunWorkoutStep.advanceElevation(
    reading: RunElevationReading?,
    durationMs: Long
): RunWorkoutStepProgress {
    return when {
        reading == null || startedAtDurationMs == null -> RunWorkoutStepProgress(this, false)
        minimumElevationMeters == null || maximumElevationMeters == null ->
            RunWorkoutStepProgress(initializeElevationExtremes(reading, durationMs), false)
        else -> progressElevationChange(reading, durationMs)
    }
}

private fun RunWorkoutStep.initializeElevationExtremes(
    reading: RunElevationReading,
    durationMs: Long
): RunWorkoutStep = copy(
    minimumElevationMeters = reading.altitudeMeters,
    minimumElevationAtDurationMs = durationMs,
    maximumElevationMeters = reading.altitudeMeters,
    maximumElevationAtDurationMs = durationMs
)

private fun RunWorkoutStep.progressElevationChange(
    reading: RunElevationReading,
    durationMs: Long
): RunWorkoutStepProgress {
    val extremes = updateElevationExtremes(reading, durationMs)
    val updated = copy(
        minimumElevationMeters = extremes.minimumMeters,
        minimumElevationAtDurationMs = extremes.minimumAtDurationMs,
        maximumElevationMeters = extremes.maximumMeters,
        maximumElevationAtDurationMs = extremes.maximumAtDurationMs
    )
    return RunWorkoutStepProgress(
        updated,
        extremes.changeFromExtreme(reading.altitudeMeters) >= (elevationChangeTargetMeters ?: Double.POSITIVE_INFINITY)
    )
}

private fun RunWorkoutStep.updateElevationExtremes(
    reading: RunElevationReading,
    durationMs: Long
): RunElevationExtremes {
    val minimum = checkNotNull(minimumElevationMeters)
    val maximum = checkNotNull(maximumElevationMeters)
    val noiseThreshold = max(MIN_ELEVATION_EXTREME_CHANGE_METERS, reading.accuracyMeters.toDouble() * 2)
    val minimumChanged = reading.altitudeMeters < minimum - noiseThreshold
    val maximumChanged = reading.altitudeMeters > maximum + noiseThreshold
    return RunElevationExtremes(
        minimumMeters = if (minimumChanged) reading.altitudeMeters else minimum,
        minimumAtDurationMs = if (minimumChanged) durationMs else minimumElevationAtDurationMs ?: durationMs,
        maximumMeters = if (maximumChanged) reading.altitudeMeters else maximum,
        maximumAtDurationMs = if (maximumChanged) durationMs else maximumElevationAtDurationMs ?: durationMs
    )
}

private fun RunWorkoutStep.startElevationTracking(
    reading: RunElevationReading?,
    durationMs: Long
): RunWorkoutStep {
    if (trigger != RunWorkoutTrigger.ELEVATION || reading == null) return this
    return copy(
        minimumElevationMeters = reading.altitudeMeters,
        minimumElevationAtDurationMs = durationMs,
        maximumElevationMeters = reading.altitudeMeters,
        maximumElevationAtDurationMs = durationMs
    )
}

private data class RunWorkoutStepProgress(val step: RunWorkoutStep, val reached: Boolean)

private data class RunElevationExtremes(
    val minimumMeters: Double,
    val minimumAtDurationMs: Long,
    val maximumMeters: Double,
    val maximumAtDurationMs: Long
) {
    fun changeFromExtreme(altitudeMeters: Double): Double =
        if (minimumAtDurationMs > maximumAtDurationMs) {
            altitudeMeters - minimumMeters
        } else {
            maximumMeters - altitudeMeters
        }
}

private const val MAX_WORKOUT_NAME_LENGTH = 80
private const val MAX_STEP_DESCRIPTION_LENGTH = 60
private const val MAX_PLAN_STEP_COUNT = 20
private const val MAX_EXPANDED_STEP_COUNT = 60
private const val MAX_WORKOUT_SET_COUNT = 10
private const val MAX_SET_REPETITIONS = 20
private const val MIN_STEP_DURATION_MS = 1_000L
private const val MAX_STEP_DURATION_MS = 10_800_000L
private const val MIN_STEP_DISTANCE_METERS = 10.0
private const val MAX_STEP_DISTANCE_METERS = 100_000.0
private const val MIN_ELEVATION_CHANGE_METERS = 3.0
private const val MAX_ELEVATION_CHANGE_METERS = 10_000.0
private const val MIN_ELEVATION_EXTREME_CHANGE_METERS = 3.0
