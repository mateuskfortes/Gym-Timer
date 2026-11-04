package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunGpsReading
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.currentPaceSecondsPerKm
import com.example.gymtimer2.domain.model.runGpsStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifica a classificação do sinal e os limites usados nas métricas GPS ao vivo. */
class RunGpsStatusTest {
    @Test
    /** Valida sessão, idade, precisão e estado do provedor GPS. */
    fun statusUsesFreshnessAccuracyAndTheCorrectRun() {
        val reading = RunGpsReading(1, 10_000, 5f, true)
        assertEquals(RunGpsStatus.GOOD, runGpsStatus(reading, 1, 20_000))
        assertEquals(RunGpsStatus.NO_SIGNAL, runGpsStatus(reading, 1, 25_001))
        assertEquals(RunGpsStatus.SEARCHING, runGpsStatus(reading, 2, 20_000))
        assertEquals(RunGpsStatus.GOOD, runGpsStatus(reading.copy(accuracyMeters = 30f), 1, 20_000))
        assertEquals(RunGpsStatus.GOOD, runGpsStatus(reading.copy(accuracyMeters = 49.9f), 1, 20_000))
        assertEquals(RunGpsStatus.WEAK, runGpsStatus(reading.copy(accuracyMeters = 50f), 1, 20_000))
        assertEquals(RunGpsStatus.WEAK, runGpsStatus(reading.copy(accuracyMeters = Float.NaN), 1, 20_000))
        assertEquals(RunGpsStatus.DISABLED, runGpsStatus(reading.copy(providerEnabled = false), 1, 20_000))
    }

    @Test
    /** Diferencia aquisição inicial de uma espera longa sem receber posição. */
    fun noFixTransitionsFromSearchingToNoSignalAfterTimeout() {
        val waiting = RunGpsReading(1, null, null, true, requestStartedAtElapsedMs = 10_000)

        assertEquals(RunGpsStatus.SEARCHING, runGpsStatus(waiting, 1, 24_999))
        assertEquals(RunGpsStatus.NO_SIGNAL, runGpsStatus(waiting, 1, 25_000))
        assertEquals(RunGpsStatus.SEARCHING, runGpsStatus(RunGpsReading(1, null, null, true), 1, 30_000))
    }

    @Test
    /** Ignora velocidade ausente, não positiva, antiga ou pertencente a outra sessão. */
    fun currentPaceUsesRecentGpsAndPositiveReportedSpeed() {
        val reading = RunGpsReading(4, 10_000, 5f, true, 2.5f)

        assertEquals(400.0, currentPaceSecondsPerKm(reading, 4, 12_000)!!, 0.0)
        assertNull(currentPaceSecondsPerKm(reading, 5, 12_000))
        assertNull(currentPaceSecondsPerKm(reading, 4, 30_001))
        assertEquals(400.0, currentPaceSecondsPerKm(reading, 4, 12_999)!!, 0.0)
        assertNull(currentPaceSecondsPerKm(reading, 4, 13_000))
        assertEquals(5_000.0, currentPaceSecondsPerKm(reading.copy(speedMetersPerSecond = 0.2f), 4, 12_000)!!, 0.1)
        assertEquals(50.0, currentPaceSecondsPerKm(reading.copy(speedMetersPerSecond = 20f), 4, 12_000)!!, 0.0)
        assertNull(currentPaceSecondsPerKm(reading.copy(speedMetersPerSecond = 0f), 4, 12_000))
        assertEquals(400.0, currentPaceSecondsPerKm(reading.copy(accuracyMeters = 40f), 4, 12_000)!!, 0.0)
        assertNull(currentPaceSecondsPerKm(reading.copy(accuracyMeters = 50f), 4, 12_000))
    }
}
