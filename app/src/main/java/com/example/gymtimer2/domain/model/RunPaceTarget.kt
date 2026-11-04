package com.example.gymtimer2.domain.model

/** Faixa opcional de pace para acompanhar durante uma corrida. */
data class RunPaceTarget(
    val fasterSecondsPerKm: Int,
    val slowerSecondsPerKm: Int
) {
    init {
        require(isValidRange(fasterSecondsPerKm, slowerSecondsPerKm))
    }

    fun classify(currentSecondsPerKm: Double?): RunPaceGuidance = when {
        currentSecondsPerKm == null || !currentSecondsPerKm.isFinite() -> RunPaceGuidance.WAITING
        currentSecondsPerKm < fasterSecondsPerKm -> RunPaceGuidance.FASTER
        currentSecondsPerKm > slowerSecondsPerKm -> RunPaceGuidance.SLOWER
        else -> RunPaceGuidance.ON_TARGET
    }

    companion object {
        fun parse(
            fasterPace: String,
            slowerPace: String,
            distanceUnit: RunDistanceUnit = RunDistanceUnit.METRIC
        ): RunPaceTarget? {
            val fasterSeconds = parsePaceSeconds(fasterPace)
                ?.let(distanceUnit::paceSecondsPerKilometer) ?: return null
            val slowerSeconds = parsePaceSeconds(slowerPace)
                ?.let(distanceUnit::paceSecondsPerKilometer) ?: return null
            if (!isValidRange(fasterSeconds, slowerSeconds)) return null
            return RunPaceTarget(fasterSeconds, slowerSeconds)
        }

        fun isValidRange(fasterSecondsPerKm: Int, slowerSecondsPerKm: Int): Boolean =
            fasterSecondsPerKm in 1..MAX_PACE_SECONDS_PER_KM &&
                slowerSecondsPerKm in (fasterSecondsPerKm + 1)..MAX_PACE_SECONDS_PER_KM

        private fun parsePaceSeconds(value: String): Int? {
            val parts = value.trim().split(':')
            if (parts.size != 2) return null
            val minutes = parts[0].toIntOrNull() ?: return null
            val seconds = parts[1].toIntOrNull() ?: return null
            if (minutes !in 0..59 || seconds !in 0..59) return null
            return (minutes * 60 + seconds).takeIf { it > 0 }
        }
    }
}

private const val MAX_PACE_SECONDS_PER_KM = 3_599

enum class RunPaceGuidance {
    WAITING,
    FASTER,
    ON_TARGET,
    SLOWER
}
