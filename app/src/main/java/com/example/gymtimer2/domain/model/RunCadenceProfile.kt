package com.example.gymtimer2.domain.model

/** Um ponto do gráfico local de cadência ao longo da distância gravada. */
data class RunCadenceProfilePoint(
    override val distanceMeters: Double,
    val stepsPerMinute: Int,
    override val segmentIndex: Int
) : RunMetricProfilePoint {
    override val value: Double get() = stepsPerMinute.toDouble()
}

/** Cria uma amostra limitada de cadência usando somente pontos aceitos da corrida. */
fun calculateRunCadenceProfile(points: List<RunPointModel>): List<RunCadenceProfilePoint> {
    val acceptedPoints = points.filterNot(RunPointModel::isFiltered)
    var totalDistanceMeters = 0.0
    val profile = acceptedPoints.zipWithNext().mapNotNull { (previous, current) ->
        val interval = acceptedRunGpsInterval(previous, current) ?: return@mapNotNull null
        totalDistanceMeters += interval.distanceMeters
        val cadence = current.cadenceStepsPerMinute?.takeIf(::isValidRunCadence)
            ?: return@mapNotNull null
        RunCadenceProfilePoint(totalDistanceMeters, cadence, current.segmentIndex)
    }
    return downsampleRunMetricProfile(profile)
}
