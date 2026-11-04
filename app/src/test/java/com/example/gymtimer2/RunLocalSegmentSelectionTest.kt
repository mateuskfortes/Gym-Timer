package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunLocalSegmentSelection
import com.example.gymtimer2.domain.model.RunPointModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunLocalSegmentSelectionTest {
    @Test
    fun keepsPausedTrackSectionsSeparate() {
        val tracks = RunLocalSegmentSelection.tracks(
            listOf(
                point(1, 0.0, 0),
                point(2, 0.001, 0),
                point(3, 0.002, 0),
                point(4, 0.003, 0),
                point(5, 10.0, 2),
                point(6, 10.001, 2),
                point(7, 10.002, 2),
                point(8, 10.003, 2)
            )
        )

        assertEquals(listOf(0, 2), tracks.map { it.segmentIndex })
        assertTrue(tracks.all { track -> track.points.all { point -> point.segmentIndex == track.segmentIndex } })
    }

    @Test
    fun selectsTheNearestGpsPointsInsideTheRequestedRange() {
        val track = RunLocalSegmentSelection.tracks(
            (0..4).map { point(it + 1L, it * 0.001, 0) }
        ).single()

        val selected = RunLocalSegmentSelection.pointsBetween(track, 120.0, 335.0)

        assertEquals(listOf(2L, 3L, 4L), selected.map(RunPointModel::id))
    }

    @Test
    fun rejectsRangesThatCannotProduceACompetitiveSegment() {
        val track = RunLocalSegmentSelection.tracks(
            (0..3).map { point(it + 1L, it * 0.001, 0) }
        ).single()

        assertTrue(RunLocalSegmentSelection.pointsBetween(track, 0.0, 80.0).isEmpty())
        assertTrue(RunLocalSegmentSelection.pointsBetween(track, 300.0, 100.0).isEmpty())
        assertTrue(RunLocalSegmentSelection.pointsBetween(track, 0.0, track.distanceMeters + 1.0).isEmpty())
    }

    private fun point(id: Long, latitude: Double, segmentIndex: Int) = RunPointModel(
        id = id,
        runId = 1L,
        latitude = latitude,
        longitude = 0.0,
        recordedAtEpochMs = id * 1_000L,
        accuracyMeters = 5f,
        segmentIndex = segmentIndex
    )
}
