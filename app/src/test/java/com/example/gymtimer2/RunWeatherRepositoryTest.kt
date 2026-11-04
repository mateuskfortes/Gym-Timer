package com.example.gymtimer2

import com.example.gymtimer2.data.repository.parseOpenMeteoCurrentWeather
import com.example.gymtimer2.data.repository.roundWeatherCoordinate
import com.example.gymtimer2.domain.model.RunWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunWeatherRepositoryTest {
    @Test
    fun parsesCurrentConditionsAndConvertsUnixSecondsToMilliseconds() {
        val weather = parseOpenMeteoCurrentWeather(
            """{"current":{"time":1793577600,"temperature_2m":18.5,"relative_humidity_2m":61,"apparent_temperature":17.8,"wind_speed_10m":9.2}}"""
        )

        assertEquals(RunWeather(18.5, 17.8, 61.0, 9.2, 1_793_577_600_000L), weather)
    }

    @Test
    fun rejectsMissingOrInvalidRequiredWeatherValues() {
        assertNull(parseOpenMeteoCurrentWeather("{"))
        assertNull(parseOpenMeteoCurrentWeather("""{"current":{"time":1793577600}}"""))
        assertNull(
            parseOpenMeteoCurrentWeather(
                """{"current":{"time":1793577600,"temperature_2m":100}}"""
            )
        )
    }

    @Test
    fun ignoresInvalidOptionalMeasurements() {
        val weather = parseOpenMeteoCurrentWeather(
            """{"current":{"time":1793577600,"temperature_2m":18.5,"relative_humidity_2m":130,"wind_speed_10m":-1}}"""
        )

        assertEquals(RunWeather(18.5, null, null, null, 1_793_577_600_000L), weather)
    }

    @Test
    fun roundsCoordinatesToOneDecimalPlaceBeforeSendingThem() {
        assertEquals(-23.6, roundWeatherCoordinate(-23.56), 0.0)
        assertEquals(-46.6, roundWeatherCoordinate(-46.63), 0.0)
    }
}
