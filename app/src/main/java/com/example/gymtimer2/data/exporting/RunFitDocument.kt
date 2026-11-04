package com.example.gymtimer2.data.exporting

import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.distanceMetersBetweenCoordinates
import com.garmin.fit.ActivityMesg
import com.garmin.fit.DateTime
import com.garmin.fit.DeviceIndex
import com.garmin.fit.DeviceInfoMesg
import com.garmin.fit.Event
import com.garmin.fit.EventMesg
import com.garmin.fit.EventType
import com.garmin.fit.File
import com.garmin.fit.FileEncoder
import com.garmin.fit.FileIdMesg
import com.garmin.fit.Fit
import com.garmin.fit.LapMesg
import com.garmin.fit.Manufacturer
import com.garmin.fit.RecordMesg
import com.garmin.fit.SessionMesg
import com.garmin.fit.Sport
import com.garmin.fit.SubSport
import java.io.File as LocalFile
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

internal object RunFitDocument {
    const val MIME_TYPE = "application/octet-stream"
    const val FILE_EXTENSION = ".fit"

    fun hasExportablePoints(points: List<RunPointModel>): Boolean = points.any(RunPointModel::isExportable)

    fun fileName(title: String, fallbackName: String): String =
        RunExportFileName.create(title, fallbackName, FILE_EXTENSION)

    fun write(
        run: RunModel,
        points: List<RunPointModel>,
        destination: LocalFile,
        manualLaps: List<RunLap> = emptyList(),
        privacyZones: List<RunPrivacyZone> = emptyList()
    ) {
        val allExportablePoints = points.filter(RunPointModel::isExportable)
        val exportablePoints = RunExportPrivacy.visiblePoints(run, points, privacyZones)
            .filter(RunPointModel::isExportable)
            .sortedBy(RunPointModel::recordedAtEpochMs)
        require(run.distanceMeters.isFinite() && run.distanceMeters in 0.0..FIT_MAX_DISTANCE_METERS) {
            "run_distance_invalid"
        }

        val startTime = DateTime(Date(run.startedAtEpochMs))
        val lastPointTimestampMs = exportablePoints.lastOrNull()?.recordedAtEpochMs
        val runEndTimestampMs = run.finishedAtEpochMs ?: lastPointTimestampMs ?: run.startedAtEpochMs
        val endTimestampMs = maxOf(runEndTimestampMs, lastPointTimestampMs ?: runEndTimestampMs)
        val endTime = DateTime(Date(endTimestampMs))
        val elapsedDurationMs = maxOf(
            run.durationMs,
            run.elapsedDurationMs ?: (endTimestampMs - run.startedAtEpochMs).coerceAtLeast(0L)
        )
        val activeSeconds = run.durationMs.coerceAtLeast(0L) / MILLIS_PER_SECOND.toFloat()
        val elapsedSeconds = elapsedDurationMs / MILLIS_PER_SECOND.toFloat()
        val distanceMeters = run.distanceMeters.toFloat()
        val sport = run.activityType.fitSport()
        val subSport = run.activityType.fitSubSport()
        val laps = run.fitLaps(
            manualLaps = manualLaps,
            endTimestampMs = endTimestampMs,
            elapsedDurationMs = elapsedDurationMs,
            sport = sport,
            subSport = subSport,
            points = exportablePoints
        )
        val encoder = FileEncoder(destination, Fit.ProtocolVersion.V2_0)

        try {
            encoder.write(fileId(run, endTime))
            encoder.write(deviceInfo(endTime))
            writeRecords(
                encoder,
                exportablePoints,
                run.distanceMeters,
                scaleToActivityDistance = exportablePoints.size == allExportablePoints.size
            )
            laps.forEach(encoder::write)
            encoder.write(
                session(
                    startTime,
                    endTime,
                    elapsedSeconds,
                    activeSeconds,
                    distanceMeters,
                    sport,
                    subSport,
                    exportablePoints,
                    laps.size
                )
            )
            encoder.write(activity(endTime, activeSeconds, endTimestampMs))
        } finally {
            encoder.close()
        }
    }

    private fun fileId(run: RunModel, endTime: DateTime) = FileIdMesg().apply {
        type = File.ACTIVITY
        manufacturer = Manufacturer.DEVELOPMENT
        product = PRODUCT_ID
        serialNumber = run.id.coerceAtLeast(0L)
        timeCreated = endTime
    }

    private fun deviceInfo(endTime: DateTime) = DeviceInfoMesg().apply {
        deviceIndex = DeviceIndex.CREATOR
        manufacturer = Manufacturer.DEVELOPMENT
        product = PRODUCT_ID
        productName = PRODUCT_NAME
        timestamp = endTime
    }

    private fun writeRecords(
        encoder: FileEncoder,
        points: List<RunPointModel>,
        totalDistanceMeters: Double,
        scaleToActivityDistance: Boolean
    ) {
        val measuredDistanceMeters = points.zipWithNext().sumOf { (first, second) ->
            if (first.segmentIndex == second.segmentIndex) {
                distanceMetersBetweenCoordinates(first.latitude, first.longitude, second.latitude, second.longitude)
            } else {
                0.0
            }
        }
        val distanceScale = if (scaleToActivityDistance && measuredDistanceMeters > 0.0) {
            totalDistanceMeters / measuredDistanceMeters
        } else {
            1.0
        }
        var accumulatedDistanceMeters = 0.0
        var previousPoint: RunPointModel? = null

        points.forEach { point ->
            previousPoint?.takeIf { it.segmentIndex != point.segmentIndex }?.let { previous ->
                encoder.write(timerEvent(previous.recordedAtEpochMs, EventType.STOP))
                encoder.write(timerEvent(previous.recordedAtEpochMs, EventType.START))
            }
            previousPoint?.takeIf { it.segmentIndex == point.segmentIndex }?.let { previous ->
                accumulatedDistanceMeters += distanceMetersBetweenCoordinates(
                    previous.latitude,
                    previous.longitude,
                    point.latitude,
                    point.longitude
                )
            }
            encoder.write(point.toFitRecord(accumulatedDistanceMeters * distanceScale))
            previousPoint = point
        }
    }

    private fun timerEvent(timestampMs: Long, type: EventType) = EventMesg().apply {
        timestamp = DateTime(Date(timestampMs))
        event = Event.TIMER
        eventType = type
    }

    private fun RunPointModel.toFitRecord(distanceMeters: Double) = RecordMesg().apply {
        timestamp = DateTime(Date(recordedAtEpochMs))
        positionLat = latitude.toFitSemicircles()
        positionLong = longitude.toFitSemicircles()
        setDistance(distanceMeters.toFloat())
        altitudeMeters?.takeIf { it.isFinite() && it in FIT_ALTITUDE_RANGE }?.let { altitude = it.toFloat() }
        speedMetersPerSecond?.takeIf { it.isFinite() && it in FIT_SPEED_RANGE }?.let { speed = it }
        cadenceStepsPerMinute?.takeIf { it in MIN_CADENCE..MAX_CADENCE }?.let {
            cadence = (it / 2).toShort()
        }
        accuracyMeters.takeIf { it.isFinite() && it in 1f..MAX_GPS_ACCURACY_METERS }?.let {
            gpsAccuracy = it.roundToInt().toShort()
        }
    }

    private fun summaryLap(
        startTime: DateTime,
        endTime: DateTime,
        elapsedSeconds: Float,
        activeSeconds: Float,
        distanceMeters: Float,
        sport: Sport,
        subSport: SubSport,
        points: List<RunPointModel>
    ) = LapMesg().apply {
        messageIndex = 0
        timestamp = endTime
        this.startTime = startTime
        totalElapsedTime = elapsedSeconds
        totalTimerTime = activeSeconds
        totalDistance = distanceMeters
        this.sport = sport
        this.subSport = subSport
        points.firstOrNull()?.let { first ->
            startPositionLat = first.latitude.toFitSemicircles()
            startPositionLong = first.longitude.toFitSemicircles()
        }
        points.lastOrNull()?.let { last ->
            endPositionLat = last.latitude.toFitSemicircles()
            endPositionLong = last.longitude.toFitSemicircles()
        }
    }

    private fun RunModel.fitLaps(
        manualLaps: List<RunLap>,
        endTimestampMs: Long,
        elapsedDurationMs: Long,
        sport: Sport,
        subSport: SubSport,
        points: List<RunPointModel>
    ): List<LapMesg> {
        val laps = mutableListOf<LapMesg>()
        var previousTimestampMs = startedAtEpochMs
        var previousDurationMs = 0L
        var previousElapsedDurationMs = 0L
        var previousDistanceMeters = 0.0

        manualLaps.forEach { manualLap ->
            val timestampMs = manualLap.recordedAtEpochMs ?: return@forEach
            if (!manualLap.isValidForFitLap(
                    timestampMs,
                    previousTimestampMs,
                    endTimestampMs,
                    previousDurationMs,
                    durationMs,
                    previousDistanceMeters,
                    distanceMeters
                )
            ) return@forEach

            val elapsedAtLapMs = manualLap.elapsedDurationMs
                ?: (timestampMs - startedAtEpochMs).coerceAtLeast(0L)
            laps += createLap(
                index = laps.size,
                startTimestampMs = previousTimestampMs,
                endTimestampMs = timestampMs,
                elapsedDurationMs = (elapsedAtLapMs - previousElapsedDurationMs).coerceAtLeast(0L),
                activeDurationMs = manualLap.durationMs - previousDurationMs,
                distanceMeters = manualLap.distanceMeters - previousDistanceMeters,
                sport = sport,
                subSport = subSport
            )
            previousTimestampMs = timestampMs
            previousDurationMs = manualLap.durationMs
            previousElapsedDurationMs = elapsedAtLapMs
            previousDistanceMeters = manualLap.distanceMeters
        }

        if (laps.isEmpty()) {
            return listOf(
                summaryLap(
                    DateTime(Date(startedAtEpochMs)),
                    DateTime(Date(endTimestampMs)),
                    elapsedDurationMs / MILLIS_PER_SECOND.toFloat(),
                    durationMs.coerceAtLeast(0L) / MILLIS_PER_SECOND.toFloat(),
                    distanceMeters.toFloat(),
                    sport,
                    subSport,
                    points
                )
            )
        }

        val remainingActiveMs = (durationMs - previousDurationMs).coerceAtLeast(0L)
        val remainingDistanceMeters = (distanceMeters - previousDistanceMeters).coerceAtLeast(0.0)
        if (remainingActiveMs > 0L || remainingDistanceMeters > 0.0) {
            laps += createLap(
                index = laps.size,
                startTimestampMs = previousTimestampMs,
                endTimestampMs = endTimestampMs,
                elapsedDurationMs = (elapsedDurationMs - previousElapsedDurationMs).coerceAtLeast(0L),
                activeDurationMs = remainingActiveMs,
                distanceMeters = remainingDistanceMeters,
                sport = sport,
                subSport = subSport
            )
        }
        return laps
    }

    private fun RunLap.isValidForFitLap(
        timestampMs: Long,
        previousTimestampMs: Long,
        endTimestampMs: Long,
        previousDurationMs: Long,
        runDurationMs: Long,
        previousDistanceMeters: Double,
        runDistanceMeters: Double
    ): Boolean = timestampMs > previousTimestampMs && timestampMs <= endTimestampMs &&
        durationMs in previousDurationMs..runDurationMs && distanceMeters.isFinite() &&
        distanceMeters in previousDistanceMeters..runDistanceMeters

    private fun createLap(
        index: Int,
        startTimestampMs: Long,
        endTimestampMs: Long,
        elapsedDurationMs: Long,
        activeDurationMs: Long,
        distanceMeters: Double,
        sport: Sport,
        subSport: SubSport
    ) = LapMesg().apply {
        messageIndex = index
        startTime = DateTime(Date(startTimestampMs))
        timestamp = DateTime(Date(endTimestampMs))
        totalElapsedTime = maxOf(elapsedDurationMs, activeDurationMs) / MILLIS_PER_SECOND.toFloat()
        totalTimerTime = activeDurationMs / MILLIS_PER_SECOND.toFloat()
        totalDistance = distanceMeters.toFloat()
        this.sport = sport
        this.subSport = subSport
    }

    private fun session(
        startTime: DateTime,
        endTime: DateTime,
        elapsedSeconds: Float,
        activeSeconds: Float,
        distanceMeters: Float,
        sport: Sport,
        subSport: SubSport,
        points: List<RunPointModel>,
        lapCount: Int
    ) = SessionMesg().apply {
        messageIndex = 0
        timestamp = endTime
        this.startTime = startTime
        totalElapsedTime = elapsedSeconds
        totalTimerTime = activeSeconds
        totalDistance = distanceMeters
        if (activeSeconds > 0f) avgSpeed = distanceMeters / activeSeconds
        maxSpeed = points.mapNotNull(RunPointModel::speedMetersPerSecond)
            .filter { it.isFinite() && it in FIT_SPEED_RANGE }
            .maxOrNull()
        this.sport = sport
        this.subSport = subSport
        firstLapIndex = 0
        numLaps = lapCount
        points.firstOrNull()?.let { first ->
            startPositionLat = first.latitude.toFitSemicircles()
            startPositionLong = first.longitude.toFitSemicircles()
        }
        points.lastOrNull()?.let { last ->
            endPositionLat = last.latitude.toFitSemicircles()
            endPositionLong = last.longitude.toFitSemicircles()
        }
    }

    private fun activity(endTime: DateTime, activeSeconds: Float, endTimestampMs: Long) = ActivityMesg().apply {
        timestamp = endTime
        totalTimerTime = activeSeconds
        numSessions = 1
        localTimestamp = endTime.timestamp +
            TimeZone.getDefault().getOffset(endTimestampMs) / MILLIS_PER_SECOND
    }

    private fun RunActivityType.fitSport(): Sport = when (this) {
        RunActivityType.RUN, RunActivityType.TRAIL_RUN, RunActivityType.VIRTUAL_RUN -> Sport.RUNNING
        RunActivityType.WALK -> Sport.WALKING
        RunActivityType.HIKE -> Sport.HIKING
        RunActivityType.WHEELCHAIR -> Sport.WHEELCHAIR_PUSH_RUN
    }

    private fun RunActivityType.fitSubSport(): SubSport = when (this) {
        RunActivityType.TRAIL_RUN -> SubSport.TRAIL
        RunActivityType.VIRTUAL_RUN -> SubSport.VIRTUAL_ACTIVITY
        else -> SubSport.GENERIC
    }

    private fun Double.toFitSemicircles(): Int = (this * SEMICIRCLES_PER_DEGREE).roundToInt()

    private const val PRODUCT_ID = 0
    private const val PRODUCT_NAME = "Gym Timer"
    private const val MILLIS_PER_SECOND = 1_000L
    private const val FIT_MAX_DISTANCE_METERS = 42_949_672.95
    private const val SEMICIRCLES_PER_DEGREE = 2_147_483_648.0 / 180.0
    private const val MIN_CADENCE = 30
    private const val MAX_CADENCE = 300
    private const val MAX_GPS_ACCURACY_METERS = 254f
    private val FIT_ALTITUDE_RANGE = -500.0..12_606.8
    private val FIT_SPEED_RANGE = 0f..65.535f
}
