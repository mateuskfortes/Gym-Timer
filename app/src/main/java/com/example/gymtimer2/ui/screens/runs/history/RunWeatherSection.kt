package com.example.gymtimer2.ui.screens.runs.history

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunWeather
import com.example.gymtimer2.ui.screens.runs.components.RunMetric
import com.example.gymtimer2.ui.screens.runs.format.LocalRunTemperatureUnit
import java.util.Date

@Composable
internal fun RunWeatherSection(weather: RunWeather?) {
    if (weather == null) return
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val temperatureUnit = LocalRunTemperatureUnit.current
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.run_weather_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.run_weather_time, DateFormat.getTimeFormat(context).format(Date(weather.observedAtEpochMs))),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunMetric(
            label = stringResource(R.string.run_weather_temperature),
            value = stringResource(
                R.string.run_weather_temperature_value,
                temperatureUnit.fromCelsius(weather.temperatureCelsius),
                temperatureUnit.symbol
            ),
            modifier = Modifier.weight(1f)
        )
        RunMetric(
            label = stringResource(R.string.run_weather_apparent_temperature),
            value = weather.apparentTemperatureCelsius?.let {
                stringResource(
                    R.string.run_weather_temperature_value,
                    temperatureUnit.fromCelsius(it),
                    temperatureUnit.symbol
                )
            } ?: "--",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunMetric(
            label = stringResource(R.string.run_weather_humidity),
            value = weather.relativeHumidityPercent?.let {
                stringResource(R.string.run_weather_humidity_value, it)
            } ?: "--",
            modifier = Modifier.weight(1f)
        )
        RunMetric(
            label = stringResource(R.string.run_weather_wind),
            value = weather.windSpeedKmh?.let {
                stringResource(R.string.run_weather_wind_value, it)
            } ?: "--",
            modifier = Modifier.weight(1f)
        )
    }
    TextButton(onClick = { uriHandler.openUri(OPEN_METEO_ATTRIBUTION_URL) }) {
        Text(stringResource(R.string.run_weather_attribution))
    }
}

private const val OPEN_METEO_ATTRIBUTION_URL = "https://open-meteo.com/"
