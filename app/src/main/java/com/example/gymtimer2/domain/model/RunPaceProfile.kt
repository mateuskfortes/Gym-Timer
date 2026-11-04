package com.example.gymtimer2.domain.model

/** Um ponto do gráfico local de pace ao longo da distância gravada. */
data class RunPaceProfilePoint(
    override val distanceMeters: Double,
    val secondsPerKm: Double,
    override val segmentIndex: Int
) : RunMetricProfilePoint {
    override val value: Double get() = secondsPerKm
}

/** Calcula o pace a partir dos pontos aceitos e mantém pausas como interrupções no gráfico. */
fun calculateRunPaceProfile(points: List<RunPointModel>): List<RunPaceProfilePoint> {
    val acceptedPoints = points.filterNot(RunPointModel::isFiltered)
    var totalDistanceMeters = 0.0
    val profile = acceptedPoints.zipWithNext().mapNotNull { (previous, current) ->
        acceptedRunGpsInterval(previous, current)?.let { interval ->
            totalDistanceMeters += interval.distanceMeters
            val paceSecondsPerKm = RunMetrics.averagePaceSecondsPerKm(interval.durationMs, interval.distanceMeters)
            paceSecondsPerKm?.let { RunPaceProfilePoint(totalDistanceMeters, it, current.segmentIndex) }
        }
    }
    return downsampleRunMetricProfile(profile)
}
