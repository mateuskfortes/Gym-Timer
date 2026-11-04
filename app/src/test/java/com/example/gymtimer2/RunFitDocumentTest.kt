package com.example.gymtimer2

import com.example.gymtimer2.data.exporting.RunFitDocument
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.garmin.fit.Decode
import com.garmin.fit.Event
import com.garmin.fit.EventMesg
import com.garmin.fit.EventType
import com.garmin.fit.LapMesg
import com.garmin.fit.Mesg
import com.garmin.fit.MesgListener
import com.garmin.fit.MesgNum
import com.garmin.fit.RecordMesg
import com.garmin.fit.SessionMesg
import com.garmin.fit.Sport
import java.io.File
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunFitDocumentTest {
    @Test
    fun writesValidActivityWithFilteredPointsOmittedAndPauseSegmentsPreserved() {
        val startTimeMs = 1_790_000_000_000L
        val run = RunModel(
            id = 1,
            startedAtEpochMs = startTimeMs,
            finishedAtEpochMs = startTimeMs + 600_000L,
            durationMs = 300_000L,
            elapsedDurationMs = 600_000L,
            distanceMeters = 220.0,
            status = RunStatus.COMPLETED,
            currentSegment = 1,
            activityType = RunActivityType.RUN
        )
        val points = listOf(
            point(1, startTimeMs + 1_000L, 0.0, 0.0, segment = 0, cadenceStepsPerMinute = 180),
            point(2, startTimeMs + 2_000L, 0.0, 0.001, segment = 0),
            point(3, startTimeMs + 3_000L, 45.0, 45.0, segment = 1),
            point(4, startTimeMs + 4_000L, 45.0, 45.001, segment = 1),
            point(5, startTimeMs + 5_000L, 91.0, 0.0, segment = 1),
            point(6, startTimeMs + 6_000L, 0.0, 0.0, segment = 1, filtered = true)
        )
        val file = File.createTempFile("run-fit-test-", ".fit").apply { deleteOnExit() }

        RunFitDocument.write(run, points, file)

        val messages = decode(file)

        val session = SessionMesg(messages.single { it.num == MesgNum.SESSION })
        val records = messages.filter { it.num == MesgNum.RECORD }.map(::RecordMesg)
        val lap = LapMesg(messages.single { it.num == MesgNum.LAP })

        assertEquals(1, messages.count { it.num == MesgNum.ACTIVITY })
        assertEquals(1, messages.count { it.num == MesgNum.SESSION })
        assertEquals(1, messages.count { it.num == MesgNum.LAP })
        assertEquals(4, records.size)
        assertEquals(90.toShort(), records.first().cadence)
        assertEquals(Sport.RUNNING, session.sport)
        assertEquals(600f, session.totalElapsedTime, 0.01f)
        assertEquals(300f, session.totalTimerTime, 0.01f)
        assertEquals(220f, session.totalDistance, 0.01f)
        assertEquals(220f, records.last().distance, 0.02f)
        assertEquals(records[1].distance, records[2].distance, 0.02f)
        assertEquals(session.totalDistance, lap.totalDistance)
    }

    @Test
    fun writesManualLapsAndFinalPartialLap() {
        val startTimeMs = 1_790_000_000_000L
        val run = RunModel(
            id = 1,
            startedAtEpochMs = startTimeMs,
            finishedAtEpochMs = startTimeMs + 600_000L,
            durationMs = 300_000L,
            elapsedDurationMs = 600_000L,
            distanceMeters = 220.0,
            status = RunStatus.COMPLETED,
            currentSegment = 1
        )
        val file = File.createTempFile("run-fit-laps-test-", ".fit").apply { deleteOnExit() }

        RunFitDocument.write(
            run = run,
            points = listOf(
                point(1, startTimeMs + 1_000L, 0.0, 0.0, segment = 0),
                point(2, startTimeMs + 599_000L, 0.0, 0.001, segment = 0)
            ),
            destination = file,
            manualLaps = listOf(
                RunLap(1, 100_000L, 50.0, startTimeMs + 200_000L, 200_000L),
                RunLap(2, 250_000L, 120.0, startTimeMs + 500_000L, 500_000L)
            )
        )

        val laps = decode(file).filter { it.num == MesgNum.LAP }.map(::LapMesg)

        assertEquals(3, laps.size)
        assertEquals(100f, laps[0].totalTimerTime, 0.01f)
        assertEquals(200f, laps[0].totalElapsedTime, 0.01f)
        assertEquals(50f, laps[0].totalDistance, 0.01f)
        assertEquals(150f, laps[1].totalTimerTime, 0.01f)
        assertEquals(300f, laps[1].totalElapsedTime, 0.01f)
        assertEquals(70f, laps[1].totalDistance, 0.01f)
        assertEquals(50f, laps[2].totalTimerTime, 0.01f)
        assertEquals(100f, laps[2].totalElapsedTime, 0.01f)
        assertEquals(100f, laps[2].totalDistance, 0.01f)
    }

    @Test
    fun exportKeepsActivitySummaryWithoutLeakingFullyHiddenCoordinates() {
        val startTimeMs = 1_790_000_000_000L
        val run = RunModel(
            id = 1,
            startedAtEpochMs = startTimeMs,
            finishedAtEpochMs = startTimeMs + 20_000L,
            durationMs = 20_000L,
            distanceMeters = 200.0,
            status = RunStatus.COMPLETED,
            currentSegment = 0,
            startEndHideDistance = RunStartEndHideDistance.METERS_200
        )
        val file = File.createTempFile("run-fit-private-test-", ".fit").apply { deleteOnExit() }

        RunFitDocument.write(
            run = run,
            points = listOf(
                point(1, startTimeMs + 1_000L, 0.0, 0.0, segment = 0),
                point(
                    2,
                    startTimeMs + 10_000L,
                    0.0,
                    100.0 / METERS_PER_DEGREE_LONGITUDE,
                    segment = 0
                ),
                point(
                    3,
                    startTimeMs + 20_000L,
                    0.0,
                    200.0 / METERS_PER_DEGREE_LONGITUDE,
                    segment = 0
                )
            ),
            destination = file
        )

        val messages = decode(file)
        val records = messages.filter { it.num == MesgNum.RECORD }.map(::RecordMesg)
        val lap = LapMesg(messages.single { it.num == MesgNum.LAP })
        val session = SessionMesg(messages.single { it.num == MesgNum.SESSION })

        assertTrue(FileInputStream(file).use { Decode().checkFileIntegrity(it) })
        assertEquals(0, records.size)
        assertNull(lap.getStartPositionLat())
        assertNull(lap.getEndPositionLat())
        assertNull(session.getStartPositionLat())
        assertNull(session.getEndPositionLat())
    }

    @Test
    fun exportMarksPrivacyZoneCrossingAsATimerGap() {
        val startTimeMs = 1_790_000_000_000L
        val file = File.createTempFile("run-fit-zone-test-", ".fit").apply { deleteOnExit() }

        RunFitDocument.write(
            run = RunModel(
                id = 1,
                startedAtEpochMs = startTimeMs,
                finishedAtEpochMs = startTimeMs + 2_000L,
                durationMs = 2_000L,
                distanceMeters = 600.0,
                status = RunStatus.COMPLETED,
                currentSegment = 0
            ),
            points = listOf(
                point(1, startTimeMs + 1_000L, 0.0, -300.0 / METERS_PER_DEGREE_LONGITUDE, segment = 0),
                point(2, startTimeMs + 2_000L, 0.0, 300.0 / METERS_PER_DEGREE_LONGITUDE, segment = 0)
            ),
            destination = file,
            privacyZones = listOf(
                RunPrivacyZone(name = "Home", latitude = 0.0, longitude = 0.0, radiusMeters = 200)
            )
        )

        val messages = decode(file)
        val events = messages.filter { it.num == MesgNum.EVENT }.map(::EventMesg)

        assertEquals(listOf(Event.TIMER, Event.TIMER), events.map { it.event })
        assertEquals(listOf(EventType.STOP, EventType.START), events.map { it.eventType })
        assertEquals(2, messages.count { it.num == MesgNum.RECORD })
    }

    private fun decode(file: File): List<Mesg> {
        assertTrue(FileInputStream(file).use { Decode().checkFileIntegrity(it) })
        val messages = mutableListOf<Mesg>()
        assertTrue(FileInputStream(file).use { input ->
            Decode().read(input, MesgListener { message -> messages += message })
        })
        return messages
    }

    private fun point(
        id: Long,
        timestampMs: Long,
        latitude: Double,
        longitude: Double,
        segment: Int,
        filtered: Boolean = false,
        cadenceStepsPerMinute: Int? = null
    ) = RunPointModel(
        id = id,
        runId = 1,
        latitude = latitude,
        longitude = longitude,
        recordedAtEpochMs = timestampMs,
        accuracyMeters = 5f,
        segmentIndex = segment,
        cadenceStepsPerMinute = cadenceStepsPerMinute,
        isFiltered = filtered
    )

    private companion object {
        const val METERS_PER_DEGREE_LONGITUDE = 111_194.9266
    }
}
