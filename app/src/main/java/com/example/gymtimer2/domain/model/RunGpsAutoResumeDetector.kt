package com.example.gymtimer2.domain.model

/** Detecta retomada por GPS após uma pausa automática quando o acelerômetro não a detectou. */
class RunGpsAutoResumeDetector {
    private val gpsFilter = RunGpsFilter()
    private val acceptedPoints = ArrayDeque<GpsPoint>()
    private var pauseAnchor: GpsPoint? = null
    private var resumeDetected = false

    /** Reinicia a janela usando o último ponto válido antes da pausa. */
    fun reset(anchor: GpsPoint?) {
        pauseAnchor = anchor
        acceptedPoints.clear()
        gpsFilter.reset(anchor)
        resumeDetected = false
    }

    /** Retorna true uma vez quando três pontos confirmam movimento sustentado. */
    fun shouldResume(point: GpsPoint): Boolean {
        val anchor = pauseAnchor ?: return false
        if (resumeDetected) return false
        val filterResult = gpsFilter.filter(point)
        if (filterResult.resetsAutoResumeWindow) acceptedPoints.clear()
        if (filterResult.distanceMeters == null) return false

        acceptedPoints.addLast(point)
        if (acceptedPoints.size > REQUIRED_POINT_COUNT) acceptedPoints.removeFirst()
        if (acceptedPoints.size < REQUIRED_POINT_COUNT) return false

        return confirmsSustainedMovement(anchor)
    }

    private fun confirmsSustainedMovement(anchor: GpsPoint): Boolean {
        val first = acceptedPoints.first()
        val last = acceptedPoints.last()
        val anchorTimeMs = anchor.elapsedRealtimeMs ?: return false
        val firstTimeMs = first.elapsedRealtimeMs ?: return false
        val lastTimeMs = last.elapsedRealtimeMs ?: return false
        val elapsedMs = lastTimeMs - firstTimeMs
        if (firstTimeMs - anchorTimeMs <= MIN_PAUSE_WINDOW_MS || elapsedMs <= 0L) return false

        val movedFromAnchorMeters = RunMetrics.distanceMeters(anchor, last)
        val averageSpeedMetersPerSecond =
            RunMetrics.distanceMeters(first, last) / (elapsedMs / MILLIS_PER_SECOND)
        if (movedFromAnchorMeters < MIN_MOVEMENT_METERS) return false
        if (averageSpeedMetersPerSecond <= MIN_RESUME_SPEED_METERS_PER_SECOND) return false

        resumeDetected = true
        return true
    }

    private companion object {
        const val REQUIRED_POINT_COUNT = 3
        const val MIN_PAUSE_WINDOW_MS = 2_000L
        const val MIN_MOVEMENT_METERS = 3.0
        const val MIN_RESUME_SPEED_METERS_PER_SECOND = 2.2
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
