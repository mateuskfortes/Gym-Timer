package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.buildRunTrainingLogWeeks
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class RunTrainingLogTest {
    @Test
    fun groupsActivitiesIntoMondayBasedLocalWeeks() {
        val utc = TimeZone.getTimeZone("UTC")
        val now = timestamp(2026, Calendar.NOVEMBER, 2, 12, utc)
        val currentMonday = timestamp(2026, Calendar.NOVEMBER, 2, 8, utc)
        val previousSunday = timestamp(2026, Calendar.NOVEMBER, 1, 19, utc)
        val previousMonday = timestamp(2026, Calendar.OCTOBER, 26, 7, utc)
        val futureActivity = timestamp(2026, Calendar.NOVEMBER, 3, 8, utc)
        val weeks = buildRunTrainingLogWeeks(
            runs = listOf(
                run(1L, currentMonday, 4_000.0),
                run(2L, previousSunday, 3_000.0),
                run(3L, previousMonday, 5_000.0),
                run(4L, futureActivity, 8_000.0),
                run(5L, currentMonday + 60_000, 2_000.0, RunStatus.RUNNING)
            ),
            nowEpochMs = now,
            timeZone = utc
        )

        assertEquals(2, weeks.size)
        assertEquals(timestamp(2026, Calendar.NOVEMBER, 2, 0, utc), weeks[0].startEpochMs)
        assertEquals(4_000.0, weeks[0].distanceMeters, 0.0)
        assertEquals(60_000L, weeks[0].movingTimeMs)
        assertEquals(1, weeks[0].days.first().runs.size)
        assertEquals(timestamp(2026, Calendar.OCTOBER, 26, 0, utc), weeks[1].startEpochMs)
        assertEquals(8_000.0, weeks[1].distanceMeters, 0.0)
        assertEquals(120_000L, weeks[1].movingTimeMs)
        assertEquals(1, weeks[1].days.last().runs.size)
    }

    private fun run(
        id: Long,
        startedAtEpochMs: Long,
        distanceMeters: Double,
        status: RunStatus = RunStatus.COMPLETED
    ) = RunModel(
        id = id,
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = startedAtEpochMs + 60_000,
        durationMs = 60_000,
        distanceMeters = distanceMeters,
        status = status,
        currentSegment = 0
    )

    private fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        timeZone: TimeZone
    ) = Calendar.getInstance(timeZone).apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
