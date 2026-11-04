package com.example.gymtimer2.domain.model

/** Percurso importado para acompanhar durante uma gravação. */
data class PlannedRouteModel(
    val id: Long,
    val name: String,
    val distanceMeters: Double,
    val isFavorite: Boolean = false
)

data class LocalSegmentStart(
    val routeId: Long,
    val name: String,
    val distanceMeters: Double,
    val latitude: Double,
    val longitude: Double
)

/** Coordenada de um percurso planejado, sem métricas de gravação. */
data class PlannedRoutePoint(
    override val latitude: Double,
    override val longitude: Double,
    override val segmentIndex: Int,
    override val altitudeMeters: Double? = null
) : RoutePoint

/** Coordenada comum a uma rota gravada ou importada. */
interface RoutePoint {
    val latitude: Double
    val longitude: Double
    val segmentIndex: Int
    val altitudeMeters: Double? get() = null
    val verticalAccuracyMeters: Float? get() = null
    val isFiltered: Boolean get() = false
}

/** Rota validada antes de ser gravada no banco local. */
data class ImportedRoute(
    val name: String,
    val points: List<PlannedRoutePoint>,
    val distanceMeters: Double
)
