package com.example.gymtimer2.data.repository

import com.example.gymtimer2.BuildConfig
import com.example.gymtimer2.domain.model.RunWeather
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.round

class RunWeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(WEATHER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
) {
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): RunWeather? = withContext(Dispatchers.IO) {
        if (!latitude.isValidLatitude() || !longitude.isValidLongitude()) return@withContext null
        try {
            client.newCall(createRequest(latitude, longitude)).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                parseOpenMeteoCurrentWeather(readResponseBody(response))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun createRequest(latitude: Double, longitude: Double): Request {
        val url = FORECAST_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", roundWeatherCoordinate(latitude).toString())
            .addQueryParameter("longitude", roundWeatherCoordinate(longitude).toString())
            .addQueryParameter("current", CURRENT_WEATHER_VARIABLES)
            .addQueryParameter("wind_speed_unit", "kmh")
            .addQueryParameter("timeformat", "unixtime")
            .addQueryParameter("timezone", "auto")
            .build()
        return Request.Builder()
            .url(url)
            .header("User-Agent", "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/json")
            .build()
    }

    private companion object {
        // shortcut: the free endpoint is non-commercial; use an authorized paid endpoint before monetization.
        const val FORECAST_ENDPOINT = "https://api.open-meteo.com/v1/forecast"
        const val WEATHER_TIMEOUT_SECONDS = 10L
        const val CURRENT_WEATHER_VARIABLES =
            "temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m"
    }
}

internal fun roundWeatherCoordinate(value: Double): Double = round(value * 10.0) / 10.0

internal fun parseOpenMeteoCurrentWeather(json: String): RunWeather? {
    return try {
        val current = JsonParser.parseString(json).asObjectOrNull()?.get("current")?.asObjectOrNull() ?: return null
        val temperature = current.number("temperature_2m")?.takeIf { it in MIN_TEMPERATURE_C..MAX_TEMPERATURE_C }
            ?: return null
        val observedAtEpochMs = current.integer("time")?.let { seconds ->
            Math.multiplyExact(seconds, 1_000L)
        } ?: return null
        RunWeather(
            temperatureCelsius = temperature,
            apparentTemperatureCelsius = current.number("apparent_temperature")
                ?.takeIf { it in MIN_TEMPERATURE_C..MAX_TEMPERATURE_C },
            relativeHumidityPercent = current.number("relative_humidity_2m")?.takeIf { it in 0.0..100.0 },
            windSpeedKmh = current.number("wind_speed_10m")?.takeIf { it in 0.0..400.0 },
            observedAtEpochMs = observedAtEpochMs
        )
    } catch (_: RuntimeException) {
        null
    }
}

private fun readResponseBody(response: Response): String {
    val source = response.body?.source() ?: throw IOException("Open-Meteo returned an empty response")
    source.request(MAX_RESPONSE_BYTES + 1L)
    if (source.buffer.size > MAX_RESPONSE_BYTES) throw IOException("Open-Meteo response exceeded the allowed size")
    return source.readUtf8()
}

private fun JsonElement.asObjectOrNull(): JsonObject? = takeIf { isJsonObject }?.asJsonObject

private fun JsonObject.number(name: String): Double? = get(name)
    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
    ?.asDouble
    ?.takeIf(Double::isFinite)

private fun JsonObject.integer(name: String): Long? = get(name)
    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
    ?.asString
    ?.toLongOrNull()

private fun Double.isValidLatitude(): Boolean = isFinite() && this in -90.0..90.0

private fun Double.isValidLongitude(): Boolean = isFinite() && this in -180.0..180.0

private const val MAX_RESPONSE_BYTES = 64 * 1024L
private const val MIN_TEMPERATURE_C = -100.0
private const val MAX_TEMPERATURE_C = 70.0
