package com.example.gymtimer2

import com.example.gymtimer2.data.exporting.RunGpxDocument
import com.example.gymtimer2.data.importing.GpxRouteParser
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunPrivacyZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class RunGpxDocumentTest {
    @Test
    fun serializesValidPointsWithSeparateSegmentsAndEscapedMetadata() {
        val gpx = RunGpxDocument.serialize(
            run = completedRun(title = "Parque & <pista>\u0001"),
            activityTypeLabel = "Corrida > rua",
            points = listOf(
                point(id = 1, segment = 0, timestamp = 1_234, latitude = -23.5, longitude = -46.6, altitude = 760.0),
                point(id = 2, segment = 0, timestamp = 2_000, latitude = -23.501, longitude = -46.601),
                point(id = 3, segment = 1, timestamp = 3_000, latitude = -23.502, longitude = -46.602),
                point(id = 4, segment = 1, timestamp = 4_000, latitude = 95.0, longitude = 0.0),
                point(id = 5, segment = 1, timestamp = 5_000, latitude = 0.0, longitude = 0.0, filtered = true)
            )
        )

        assertTrue(gpx.contains("Parque &amp; &lt;pista&gt;"))
        assertFalse(gpx.contains("\u0001"))
        assertTrue(gpx.contains("Corrida &gt; rua"))
        assertTrue(gpx.contains("<ele>760.0</ele>"))
        assertTrue(gpx.contains("1970-01-01T00:00:01.234Z"))
        assertEquals(2, "<trkseg>".toRegex().findAll(gpx).count())
        assertFalse(gpx.contains("95.0"))
        assertFalse(gpx.contains("00:00:05.000Z"))

        val importedRoute = GpxRouteParser().parse(ByteArrayInputStream(gpx.toByteArray()), "fallback.gpx")
        assertEquals(listOf(0, 0, 1), importedRoute.points.map { it.segmentIndex })
    }

    @Test
    fun fileNameRemovesUnsafeCharactersAndUsesFallbackForBlankTitles() {
        assertEquals("treino-dia.gpx", RunGpxDocument.fileName(" treino:/dia ", "corrida"))
        assertEquals("corrida.gpx", RunGpxDocument.fileName("  ", "corrida"))
    }

    @Test
    fun exportHidesRunExtremitiesButKeepsAValidGpxWithoutVisiblePoints() {
        val gpx = RunGpxDocument.serialize(
            run = completedRun("corrida").copy(
                startEndHideDistance = RunStartEndHideDistance.METERS_200
            ),
            activityTypeLabel = "Corrida",
            points = listOf(
                point(1, 0, 1_000, 0.0, 0.0),
                point(2, 0, 2_000, 0.0, 100.0 / METERS_PER_DEGREE_LONGITUDE),
                point(3, 0, 3_000, 0.0, 200.0 / METERS_PER_DEGREE_LONGITUDE)
            )
        )

        assertTrue(gpx.contains("<trk>"))
        assertFalse(gpx.contains("lon=\"0.0\""))
        assertFalse(gpx.contains("<trkpt"))
    }

    @Test
    fun exportDoesNotConnectRouteAcrossPrivacyZone() {
        val gpx = RunGpxDocument.serialize(
            run = completedRun("corrida"),
            activityTypeLabel = "Corrida",
            points = listOf(
                point(1, 0, 1_000, 0.0, -300.0 / METERS_PER_DEGREE_LONGITUDE),
                point(2, 0, 2_000, 0.0, 300.0 / METERS_PER_DEGREE_LONGITUDE)
            ),
            privacyZones = listOf(
                RunPrivacyZone(name = "Home", latitude = 0.0, longitude = 0.0, radiusMeters = 200)
            )
        )

        assertEquals(2, "<trkseg>".toRegex().findAll(gpx).count())
        assertEquals(2, "<trkpt ".toRegex().findAll(gpx).count())
    }

    private fun completedRun(title: String) = RunModel(
        id = 1,
        startedAtEpochMs = 0,
        finishedAtEpochMs = 5_000,
        durationMs = 5_000,
        distanceMeters = 10.0,
        status = RunStatus.COMPLETED,
        currentSegment = 1,
        title = title
    )

    private fun point(
        id: Long,
        segment: Int,
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        filtered: Boolean = false
    ) = RunPointModel(
        id = id,
        runId = 1,
        latitude = latitude,
        longitude = longitude,
        recordedAtEpochMs = timestamp,
        accuracyMeters = 5f,
        segmentIndex = segment,
        altitudeMeters = altitude,
        isFiltered = filtered
    )

    private companion object {
        const val METERS_PER_DEGREE_LONGITUDE = 111_194.9266
    }
}
