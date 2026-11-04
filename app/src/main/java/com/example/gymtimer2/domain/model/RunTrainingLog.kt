package com.example.gymtimer2.domain.model

import java.util.Calendar
import java.util.TimeZone

/** Corridas registradas em um dia do calendário local. */
data class RunTrainingLogDay(
    val dateEpochMs: Long,
    val runs: List<RunModel>
)

/** Semana local de segunda a domingo, com a distância das atividades concluídas. */
data class RunTrainingLogWeek(
    val startEpochMs: Long,
    val days: List<RunTrainingLogDay>,
    val distanceMeters: Double,
    val movingTimeMs: Long
)

enum class RunTrainingLogMetric { DISTANCE, MOVING_TIME }

/** Agrupa o histórico local por semana sem consultar dados que o app não possui. */
fun buildRunTrainingLogWeeks(
    runs: List<RunModel>,
    nowEpochMs: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): List<RunTrainingLogWeek> {
    val runsByDay = runs.asSequence()
        .filter { it.status == RunStatus.COMPLETED && it.startedAtEpochMs <= nowEpochMs }
        .groupBy { startOfDay(it.startedAtEpochMs, timeZone) }
        .mapValues { (_, dayRuns) -> dayRuns.sortedByDescending(RunModel::startedAtEpochMs) }
    val oldestDay = runsByDay.keys.minOrNull() ?: return emptyList()
    val oldestWeek = startOfWeek(oldestDay, timeZone)
    val week = Calendar.getInstance(timeZone).apply {
        timeInMillis = startOfWeek(nowEpochMs, timeZone)
    }
    val result = mutableListOf<RunTrainingLogWeek>()

    while (week.timeInMillis >= oldestWeek) {
        val weekStart = week.timeInMillis
        val days = (0 until DAYS_PER_WEEK).map { offset ->
            val dayCalendar = (week.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            val dateEpochMs = dayCalendar.timeInMillis
            RunTrainingLogDay(dateEpochMs, runsByDay[dateEpochMs].orEmpty())
        }
        val distanceMeters = days.asSequence()
            .flatMap { it.runs.asSequence() }
            .sumOf { it.distanceMeters.takeIf { distance -> distance.isFinite() && distance > 0.0 } ?: 0.0 }
        val movingTimeMs = days.asSequence()
            .flatMap { it.runs.asSequence() }
            .sumOf { it.durationMs.coerceAtLeast(0L) }
        result += RunTrainingLogWeek(weekStart, days, distanceMeters, movingTimeMs)
        week.add(Calendar.DAY_OF_MONTH, -DAYS_PER_WEEK)
    }

    return result
}

private fun startOfDay(timestampMs: Long, timeZone: TimeZone): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = timestampMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun startOfWeek(timestampMs: Long, timeZone: TimeZone): Long {
    val week = Calendar.getInstance(timeZone).apply { timeInMillis = startOfDay(timestampMs, timeZone) }
    val daysAfterMonday = (week.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + DAYS_PER_WEEK) % DAYS_PER_WEEK
    week.add(Calendar.DAY_OF_MONTH, -daysAfterMonday)
    return week.timeInMillis
}

private const val DAYS_PER_WEEK = 7
