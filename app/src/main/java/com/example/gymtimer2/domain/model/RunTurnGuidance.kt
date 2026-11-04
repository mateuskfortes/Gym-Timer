@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.domain.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Direção estimada pela mudança de rumo entre segmentos consecutivos. */
enum class RunTurnDirection { LEFT, RIGHT, U_TURN }

/** Curva inferida e sua distância acumulada desde o início do percurso. */
data class RunTurnInstruction(
    val direction: RunTurnDirection,
    val distanceAlongRouteMeters: Double
)

/** Próxima curva e distância restante para mostrar ou anunciar. */
data class RunTurnGuidance(
    val instructionIndex: Int,
    val direction: RunTurnDirection,
    val distanceToTurnMeters: Double
)

/** Infere curvas de rotas GPX, que não trazem instruções de direção semânticas. */
fun inferRunTurnInstructions(points: List<RoutePoint>): List<RunTurnInstruction> {
    val samples = sampleRoute(points.filterNot(RoutePoint::isFiltered))
    val turns = mutableListOf<DetectedTurn>()
    for (index in 1 until samples.lastIndex) {
        detectTurnAt(samples, index)?.let { addTurn(turns, it) }
    }
    return turns.map(DetectedTurn::instruction)
}

/** Retorna a próxima curva usando a distância acumulada como aproximação do progresso. */
fun nextRunTurnGuidance(
    instructions: List<RunTurnInstruction>,
    distanceAlongRouteMeters: Double
): RunTurnGuidance? = upcomingRunTurnGuidances(instructions, distanceAlongRouteMeters, 1).firstOrNull()

/** Returns the next requested number of estimated turns, in route order. */
fun upcomingRunTurnGuidances(
    instructions: List<RunTurnInstruction>,
    distanceAlongRouteMeters: Double,
    limit: Int
): List<RunTurnGuidance> {
    if (!distanceAlongRouteMeters.isFinite() || distanceAlongRouteMeters < 0.0 || limit <= 0) return emptyList()
    return instructions.asSequence().mapIndexedNotNull { index, instruction ->
        if (instruction.distanceAlongRouteMeters < distanceAlongRouteMeters - PASSED_TURN_TOLERANCE_METERS) {
            null
        } else {
            RunTurnGuidance(
                instructionIndex = index,
                direction = instruction.direction,
                distanceToTurnMeters =
                    (instruction.distanceAlongRouteMeters - distanceAlongRouteMeters).coerceAtLeast(0.0)
            )
        }
    }.take(limit).toList()
}

/** Reduz pontos próximos e calcula a distância acumulada das amostras da rota. */
private fun sampleRoute(points: List<RoutePoint>): List<LocatedRoutePoint> {
    val samples = mutableListOf<LocatedRoutePoint>()
    var routeDistanceMeters = 0.0
    points.forEachIndexed { index, point ->
        val previous = points.getOrNull(index - 1)
        if (previous != null && previous.segmentIndex == point.segmentIndex) {
            routeDistanceMeters += distanceMetersBetweenCoordinates(
                previous.latitude,
                previous.longitude,
                point.latitude,
                point.longitude
            )
        }

        val lastSample = samples.lastOrNull()?.takeIf { it.point.segmentIndex == point.segmentIndex }
        val distanceFromLastSample = lastSample?.let {
            distanceMetersBetweenCoordinates(
                it.point.latitude,
                it.point.longitude,
                point.latitude,
                point.longitude
            )
        }
        if (shouldSamplePoint(points, index, distanceFromLastSample)) {
            samples += LocatedRoutePoint(point, routeDistanceMeters)
        }
    }
    return samples
}

/** Sempre mantém limites de segmento e ignora duplicatas antes de aplicar o espaçamento. */
private fun shouldSamplePoint(
    points: List<RoutePoint>,
    index: Int,
    distanceFromLastSample: Double?
): Boolean {
    if (distanceFromLastSample != null && distanceFromLastSample < MIN_DISTINCT_SAMPLE_METERS) return false
    val point = points[index]
    val previous = points.getOrNull(index - 1)
    val next = points.getOrNull(index + 1)
    val isSegmentBoundary = previous?.segmentIndex != point.segmentIndex || next?.segmentIndex != point.segmentIndex
    if (isSegmentBoundary) return true
    return distanceFromLastSample == null || distanceFromLastSample >= ROUTE_SAMPLE_SPACING_METERS
}

/** Calcula a curva entre três pontos do mesmo segmento da rota. */
private fun detectTurnAt(samples: List<LocatedRoutePoint>, index: Int): DetectedTurn? {
    val previous = samples[index - 1]
    val current = samples[index]
    val next = samples[index + 1]
    if (previous.point.segmentIndex != current.point.segmentIndex ||
        current.point.segmentIndex != next.point.segmentIndex
    ) return null

    val angle = routeTurnAngle(previous.point, current.point, next.point) ?: return null
    if (abs(angle) < MIN_TURN_ANGLE_DEGREES) return null

    val direction = when {
        abs(angle) >= U_TURN_ANGLE_DEGREES -> RunTurnDirection.U_TURN
        angle > 0.0 -> RunTurnDirection.RIGHT
        else -> RunTurnDirection.LEFT
    }
    return DetectedTurn(RunTurnInstruction(direction, current.distanceAlongRouteMeters), abs(angle))
}

/** Calcula a diferença entre os rumos de entrada e saída de uma curva. */
private fun routeTurnAngle(start: RoutePoint, corner: RoutePoint, end: RoutePoint): Double? {
    val incoming = bearingDegrees(start, corner) ?: return null
    val outgoing = bearingDegrees(corner, end) ?: return null
    return ((outgoing - incoming + 540.0) % 360.0) - 180.0
}

/** Calcula o rumo inicial entre duas coordenadas válidas. */
private fun bearingDegrees(start: RoutePoint, end: RoutePoint): Double? {
    if (!start.hasValidCoordinates() || !end.hasValidCoordinates()) return null
    val latitude1 = Math.toRadians(start.latitude)
    val latitude2 = Math.toRadians(end.latitude)
    val longitudeDelta = Math.toRadians(end.longitude - start.longitude)
    val y = sin(longitudeDelta) * cos(latitude2)
    val x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longitudeDelta)
    return Math.toDegrees(atan2(y, x)).takeIf(Double::isFinite)
}

/** Mantém uma instrução por curva quando amostras vizinhas detectam o mesmo giro. */
private fun addTurn(turns: MutableList<DetectedTurn>, candidate: DetectedTurn) {
    val previous = turns.lastOrNull()
    if (previous != null && previous.instruction.direction == candidate.instruction.direction &&
        candidate.instruction.distanceAlongRouteMeters - previous.instruction.distanceAlongRouteMeters <
        TURN_CLUSTER_DISTANCE_METERS
    ) {
        if (candidate.angleDegrees > previous.angleDegrees) turns[turns.lastIndex] = candidate
        return
    }
    turns += candidate
}

/** Rejeita coordenadas inválidas antes de calcular o rumo. */
private fun RoutePoint.hasValidCoordinates(): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0

private data class LocatedRoutePoint(val point: RoutePoint, val distanceAlongRouteMeters: Double)

private data class DetectedTurn(val instruction: RunTurnInstruction, val angleDegrees: Double)

// shortcut: estima curvas com amostras de 8 m; usar metadados de direção quando uma rota os fornecer.
private const val ROUTE_SAMPLE_SPACING_METERS = 8.0
private const val MIN_DISTINCT_SAMPLE_METERS = 1.0
private const val MIN_TURN_ANGLE_DEGREES = 45.0
private const val U_TURN_ANGLE_DEGREES = 150.0
private const val TURN_CLUSTER_DISTANCE_METERS = 35.0
private const val PASSED_TURN_TOLERANCE_METERS = 20.0
