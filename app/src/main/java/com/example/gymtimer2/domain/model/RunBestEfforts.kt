@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.domain.model

import kotlin.math.roundToLong
import java.util.Calendar

/** Distâncias padrão usadas para calcular as melhores marcas de uma corrida. */
enum class RunBenchmarkDistance(val distanceMeters: Double) {
    /** 400 metros. */
    FOUR_HUNDRED_METERS(400.0),
    /** 1 quilômetro. */
    ONE_KILOMETER(1_000.0),
    /** Meia milha. */
    HALF_MILE(804.672),
    /** 1 milha. */
    ONE_MILE(1_609.344),
    /** 2 milhas. */
    TWO_MILES(3_218.688),
    /** 5 quilômetros. */
    FIVE_KILOMETERS(5_000.0),
    /** 10 quilômetros. */
    TEN_KILOMETERS(10_000.0),
    /** 15 quilômetros. */
    FIFTEEN_KILOMETERS(15_000.0),
    /** 10 milhas. */
    TEN_MILES(16_093.44),
    /** 20 quilômetros. */
    TWENTY_KILOMETERS(20_000.0),
    /** Meia maratona. */
    HALF_MARATHON(21_097.5),
    /** 30 quilômetros. */
    THIRTY_KILOMETERS(30_000.0),
    /** Maratona. */
    MARATHON(42_195.0),
    /** 50 quilômetros. */
    FIFTY_KILOMETERS(50_000.0)
}

/** Melhor tempo aproximado da corrida para uma distância padrão. */
data class RunBestEffort(val distance: RunBenchmarkDistance, val durationMs: Long) {
    /** Pace em segundos por quilômetro. */
    val paceSecondsPerKm: Double
        get() = durationMs / distance.distanceMeters
}

/** Marca de uma corrida persistida, usada nos recordes do histórico local. */
data class RunBestEffortRecord(
    val runId: Long,
    val startedAtEpochMs: Long,
    val durationMs: Long
)

/** Melhores tempos por distância ao longo da vida e por ano. */
data class RunBestEffortRecords(
    val lifetime: Map<RunBenchmarkDistance, List<RunBestEffortRecord>> = emptyMap(),
    val annual: Map<Int, Map<RunBenchmarkDistance, List<RunBestEffortRecord>>> = emptyMap()
)

/** Tempo corrigido ou removido de uma marca específica de uma corrida. */
data class RunBestEffortOverride(
    val runId: Long,
    val distance: RunBenchmarkDistance,
    val durationMs: Long? = null,
    val isRemoved: Boolean = false
)

/** Converte uma entrada mm:ss ou hh:mm:ss para milissegundos. */
object RunBestEffortDuration {
    private const val MAX_DURATION_SECONDS = 48L * 60L * 60L

    /** Retorna nulo para formatos ou tempos inválidos. */
    fun parse(value: String): Long? {
        val numbers = parseFields(value) ?: return null
        val hasHours = numbers.size == 3
        val hours = if (hasHours) numbers[0] else 0L
        val minutes = if (hasHours) numbers[1] else numbers[0]
        val seconds = numbers.last()
        if (!hasValidRange(hours, minutes, seconds, hasHours)) return null

        val totalSeconds = hours * 3_600L + minutes * 60L + seconds
        return totalSeconds.takeIf { it in 1L..MAX_DURATION_SECONDS }?.times(1_000L)
    }

    private fun parseFields(value: String): List<Long>? {
        val fields = value.trim().split(':')
        if (fields.size !in 2..3) return null
        val numbers = fields.map { it.toLongOrNull() ?: return null }
        return numbers.takeIf { values -> values.all { it >= 0L } }
    }

    private fun hasValidRange(hours: Long, minutes: Long, seconds: Long, hasHours: Boolean): Boolean =
        hours <= 48L && seconds in 0L..59L &&
            if (hasHours) minutes in 0L..59L else minutes <= MAX_DURATION_SECONDS / 60L
}

/** Calcula as marcas mais rápidas a partir dos pontos GPS aceitos de uma sessão. */
object RunBestEfforts {
    /** Retorna uma marca para cada distância padrão coberta pela rota. */
    fun calculate(points: List<RunPointModel>): List<RunBestEffort> {
        val samples = cumulativeDistanceSamples(points)
        if (samples.size < 2) return emptyList()

        return RunBenchmarkDistance.entries.mapNotNull { distance ->
            bestEffort(samples, distance)
        }
    }

    /** Classifica o melhor esforço de cada corrida, limitando a três marcas vitalícias e dez por ano. */
    fun calculateRecords(
        runs: List<RunModel>,
        points: List<RunPointModel>,
        overrides: List<RunBestEffortOverride> = emptyList()
    ): RunBestEffortRecords {
        val runsById = runs.associateBy(RunModel::id)
        val overridesByRun = overrides.groupBy(RunBestEffortOverride::runId)
        val efforts = points.groupBy(RunPointModel::runId).flatMap { (runId, runPoints) ->
            val run = runsById[runId] ?: return@flatMap emptyList()
            val year = yearOf(run.startedAtEpochMs)
            val runOverrides = overridesByRun[runId].orEmpty().associateBy(RunBestEffortOverride::distance)
            calculate(runPoints).mapNotNull { effort ->
                val override = runOverrides[effort.distance]
                if (override?.isRemoved == true) return@mapNotNull null
                DatedRunEffort(
                    year = year,
                    distance = effort.distance,
                    record = RunBestEffortRecord(
                        runId,
                        run.startedAtEpochMs,
                        override?.durationMs ?: effort.durationMs
                    )
                )
            }
        }
        val lifetime = efforts.groupBy(DatedRunEffort::distance, DatedRunEffort::record)
            .mapValues { (_, records) -> rankedRecords(records, LIFETIME_RECORD_LIMIT) }
        val annual = efforts.groupBy(DatedRunEffort::year)
            .mapValues { (_, yearEfforts) ->
                yearEfforts.groupBy(DatedRunEffort::distance, DatedRunEffort::record)
                    .mapValues { (_, records) -> rankedRecords(records, ANNUAL_RECORD_LIMIT) }
            }
        return RunBestEffortRecords(lifetime, annual)
    }

    /** Combina resultados parciais sem alterar os limites dos rankings. */
    fun mergeRecords(first: RunBestEffortRecords, second: RunBestEffortRecords): RunBestEffortRecords {
        val lifetimeDistances = first.lifetime.keys + second.lifetime.keys
        val lifetime = lifetimeDistances.associateWith { distance ->
            rankedRecords(
                first.lifetime[distance].orEmpty() + second.lifetime[distance].orEmpty(),
                LIFETIME_RECORD_LIMIT
            )
        }
        val years = first.annual.keys + second.annual.keys
        val annual = years.associateWith { year ->
            val firstYear = first.annual[year].orEmpty()
            val secondYear = second.annual[year].orEmpty()
            (firstYear.keys + secondYear.keys).associateWith { distance ->
                rankedRecords(
                    firstYear[distance].orEmpty() + secondYear[distance].orEmpty(),
                    ANNUAL_RECORD_LIMIT
                )
            }
        }
        return RunBestEffortRecords(lifetime, annual)
    }

    private fun rankedRecords(records: List<RunBestEffortRecord>, limit: Int): List<RunBestEffortRecord> =
        records.distinctBy(RunBestEffortRecord::runId)
            .sortedWith(
                compareBy(
                    RunBestEffortRecord::durationMs,
                    RunBestEffortRecord::startedAtEpochMs,
                    RunBestEffortRecord::runId
                )
            )
            .take(limit)

    private fun yearOf(epochMs: Long): Int = Calendar.getInstance().run {
        timeInMillis = epochMs
        get(Calendar.YEAR)
    }

    private fun cumulativeDistanceSamples(points: List<RunPointModel>): List<DistanceSample> {
        val validPoints = points.filter { point ->
            !point.isFiltered && point.asGpsPoint(point.recordedAtEpochMs).hasValidAccuracy()
        }
        val track = DistanceTrack(usesElapsedRealtime(validPoints))
        validPoints.forEach(track::add)
        return track.samples
    }

    private fun bestEffort(
        samples: List<DistanceSample>,
        benchmark: RunBenchmarkDistance
    ): RunBestEffort? = fastestDurationMs(samples, benchmark.distanceMeters)?.let { durationMs ->
        RunBestEffort(benchmark, durationMs)
    }

    private fun fastestDurationMs(samples: List<DistanceSample>, distanceMeters: Double): Long? {
        var fastestDurationMs: Long? = null
        var startIndex = 0
        var previousEndDistanceMeters = -1.0

        // shortcut: effort ends use GPS fixes; interpolate ends if sparse sampling distorts short marks.
        for (endIndex in 1 until samples.size) {
            val end = samples[endIndex]
            if (end.distanceMeters > previousEndDistanceMeters) {
                previousEndDistanceMeters = end.distanceMeters
                val startDistanceMeters = end.distanceMeters - distanceMeters
                if (startDistanceMeters >= 0.0) {
                    startIndex = latestStartIndex(samples, endIndex, startIndex, startDistanceMeters)
                    val durationMs = windowDurationMs(samples, startIndex, endIndex, startDistanceMeters)
                    if (durationMs != null) {
                        fastestDurationMs = minOf(fastestDurationMs ?: Long.MAX_VALUE, durationMs)
                    }
                }
            }
        }
        return fastestDurationMs
    }

    private fun latestStartIndex(
        samples: List<DistanceSample>,
        endIndex: Int,
        startIndex: Int,
        targetDistanceMeters: Double
    ): Int {
        var index = startIndex
        while (index + 1 < endIndex && samples[index + 1].distanceMeters <= targetDistanceMeters) index++
        return index
    }

    private fun windowDurationMs(
        samples: List<DistanceSample>,
        startIndex: Int,
        endIndex: Int,
        startDistanceMeters: Double
    ): Long? {
        val start = samples[startIndex]
        val startTimeMs = if (start.distanceMeters == startDistanceMeters) {
            start.timestampMs
        } else {
            interpolateTimeMs(start, samples[startIndex + 1], startDistanceMeters)
        }
        return (samples[endIndex].timestampMs - startTimeMs).takeIf { it > 0L }
    }

    private fun usesElapsedRealtime(points: List<RunPointModel>): Boolean {
        if (points.any { it.elapsedRealtimeMs == null }) return false
        return points.zipWithNext().all { (first, second) ->
            second.elapsedRealtimeMs!! >= first.elapsedRealtimeMs!!
        }
    }

    private fun interpolateTimeMs(
        start: DistanceSample,
        end: DistanceSample,
        distanceMeters: Double
    ): Long {
        val fraction = (distanceMeters - start.distanceMeters) / (end.distanceMeters - start.distanceMeters)
        return start.timestampMs + ((end.timestampMs - start.timestampMs) * fraction).roundToLong()
    }

    private data class DistanceSample(val distanceMeters: Double, val timestampMs: Long)

    private class DistanceTrack(private val useElapsedRealtime: Boolean) {
        val samples = mutableListOf<DistanceSample>()
        private var previousPoint: RunPointModel? = null
        private var previousGpsPoint: GpsPoint? = null
        private var previousTimestampMs: Long? = null
        private var distanceMeters = 0.0

        fun add(point: RunPointModel) {
            val timestampMs = point.bestEffortTimestamp(useElapsedRealtime)
            if (previousTimestampMs != null && timestampMs <= previousTimestampMs!!) return

            val gpsPoint = point.asGpsPoint(timestampMs)
            val distanceDelta = distanceDelta(point, gpsPoint) ?: return
            distanceMeters += distanceDelta
            samples += DistanceSample(distanceMeters, timestampMs)
            previousPoint = point
            previousGpsPoint = gpsPoint
            previousTimestampMs = timestampMs
        }

        private fun distanceDelta(point: RunPointModel, gpsPoint: GpsPoint): Double? {
            val previous = previousPoint ?: return 0.0
            if (previous.segmentIndex != point.segmentIndex) return 0.0
            return RunMetrics.acceptedDistanceMeters(previousGpsPoint, gpsPoint)
        }
    }
}

private const val LIFETIME_RECORD_LIMIT = 3
private const val ANNUAL_RECORD_LIMIT = 10

private data class DatedRunEffort(
    val year: Int,
    val distance: RunBenchmarkDistance,
    val record: RunBestEffortRecord
)

private fun RunPointModel.bestEffortTimestamp(useElapsedRealtime: Boolean): Long =
    if (useElapsedRealtime) checkNotNull(elapsedRealtimeMs) else recordedAtEpochMs

private fun RunPointModel.asGpsPoint(timestampMs: Long) = GpsPoint(
    latitude = latitude,
    longitude = longitude,
    recordedAtEpochMs = timestampMs,
    accuracyMeters = accuracyMeters,
    elapsedRealtimeMs = timestampMs
)
