package com.example.gymtimer2.data.entity

import androidx.room.ColumnInfo
import com.example.gymtimer2.domain.model.RunWeather

/** Colunas opcionais embutidas em uma corrida para preservar o histórico já existente. */
data class RunWeatherEntity(
    @ColumnInfo(name = "temperature_celsius")
    val temperatureCelsius: Double? = null,
    @ColumnInfo(name = "apparent_temperature_celsius")
    val apparentTemperatureCelsius: Double? = null,
    @ColumnInfo(name = "relative_humidity_percent")
    val relativeHumidityPercent: Double? = null,
    @ColumnInfo(name = "wind_speed_kmh")
    val windSpeedKmh: Double? = null,
    @ColumnInfo(name = "observed_at_epoch_ms")
    val observedAtEpochMs: Long? = null
)

fun RunWeatherEntity.toModelOrNull(): RunWeather? = temperatureCelsius?.let { temperature ->
    RunWeather(
        temperatureCelsius = temperature,
        apparentTemperatureCelsius = apparentTemperatureCelsius,
        relativeHumidityPercent = relativeHumidityPercent,
        windSpeedKmh = windSpeedKmh,
        observedAtEpochMs = observedAtEpochMs ?: return null
    )
}
