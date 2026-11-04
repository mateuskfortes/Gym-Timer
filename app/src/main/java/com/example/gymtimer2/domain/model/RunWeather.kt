package com.example.gymtimer2.domain.model

/** Snapshot local das condições reportadas na primeira localização aceita da corrida. */
data class RunWeather(
    val temperatureCelsius: Double,
    val apparentTemperatureCelsius: Double?,
    val relativeHumidityPercent: Double?,
    val windSpeedKmh: Double?,
    val observedAtEpochMs: Long
)
