@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica filtros GPS e formatação sem depender de um dispositivo. */
class RunMetricsTest {
    @Test
    /** Confirma que um segmento com precisão válida soma sua distância geográfica. */
    fun acceptedGpsSegmentAddsGeographicDistance() {
        val previous = gpsPoint(longitude = 0.0, recordedAtMs = 1_000L)
        val current = gpsPoint(longitude = 0.0001, recordedAtMs = 6_000L)

        val distance = RunMetrics.acceptedDistanceMeters(previous, current)

        assertTrue(distance != null && distance in 10.0..12.0)
    }

    @Test
    /** Confirma que o filtro descarta leituras com precisão insuficiente. */
    fun rejectsLowAccuracyLocation() {
        val current = gpsPoint(longitude = 0.0001, accuracyMeters = 26f)

        assertNull(RunMetrics.acceptedDistanceMeters(null, current))
    }

    @Test
    /** Confirma que um salto incompatível com corrida não altera a distância. */
    fun rejectsImpossibleGpsJump() {
        val previous = gpsPoint(longitude = 0.0, recordedAtMs = 1_000L)
        val current = gpsPoint(longitude = 0.001, recordedAtMs = 2_000L)

        assertNull(RunMetrics.acceptedDistanceMeters(previous, current))
    }

    @Test
    /** Confirma o pace médio e a ausência de pace quando a distância é zero. */
    fun averagePaceUsesActiveDurationAndDistance() {
        val pace = RunMetrics.averagePaceSecondsPerKm(durationMs = 120_000L, distanceMeters = 1_000.0)

        assertEquals(120.0, pace!!, 0.0)
        assertEquals("2:00/km", RunMetrics.formatPace(pace))
        assertNull(RunMetrics.averagePaceSecondsPerKm(durationMs = 10_000L, distanceMeters = 0.0))
    }

    @Test
    /** Confirma os formatos de tempo abaixo e acima de uma hora. */
    fun formatsElapsedTimeWithHoursWhenNeeded() {
        assertEquals("02:05", RunMetrics.formatDuration(125_000L))
        assertEquals("1:02:05", RunMetrics.formatDuration(3_725_000L))
    }

    /** Cria uma posição GPS com valores controlados para cada cenário. */
    private fun gpsPoint(
        longitude: Double,
        recordedAtMs: Long = 1_000L,
        accuracyMeters: Float = 5f
    ) = GpsPoint(
        latitude = 0.0,
        longitude = longitude,
        recordedAtEpochMs = recordedAtMs,
        accuracyMeters = accuracyMeters
    )
}
