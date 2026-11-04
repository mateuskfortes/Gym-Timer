package com.example.gymtimer2.domain.model

import kotlin.math.roundToInt

/** Units used to display running metrics while values remain stored in SI units. */
enum class RunDistanceUnit(val metersPerUnit: Double, val paceSuffix: String) {
    METRIC(1_000.0, "km"),
    IMPERIAL(1_609.344, "mi");

    fun distanceFromMeters(distanceMeters: Double): Double = distanceMeters / metersPerUnit

    fun paceSecondsPerUnit(secondsPerKilometer: Double): Double =
        secondsPerKilometer * metersPerUnit / 1_000.0

    fun paceSecondsPerKilometer(secondsPerUnit: Int): Int =
        (secondsPerUnit * 1_000.0 / metersPerUnit).roundToInt()

    fun speedFromKilometersPerHour(speedKilometersPerHour: Double): Double =
        speedKilometersPerHour * 1_000.0 / metersPerUnit

    fun feetFromMeters(distanceMeters: Double): Double = distanceMeters * METERS_TO_FEET

    fun metersFromElevation(elevationValue: Double): Double =
        if (this == IMPERIAL) elevationValue / METERS_TO_FEET else elevationValue

    fun elevationFromMeters(elevationMeters: Double): Double =
        if (this == IMPERIAL) feetFromMeters(elevationMeters) else elevationMeters

    companion object {
        fun fromStoredValue(value: String?): RunDistanceUnit =
            entries.firstOrNull { it.name == value } ?: METRIC

        private const val METERS_TO_FEET = 3.280839895
    }
}
