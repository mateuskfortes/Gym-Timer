package com.example.gymtimer2

import com.example.gymtimer2.data.repository.ValhallaManeuver
import com.example.gymtimer2.data.repository.ValhallaRouteLeg
import com.example.gymtimer2.data.repository.buildRunStreetRoute
import com.example.gymtimer2.data.repository.buildRunStreetInstructions
import com.example.gymtimer2.domain.model.PlannedRoutePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ValhallaRouteRepositoryTest {
    @Test
    fun buildsStreetInstructionsAtTheStartOfEachManeuver() {
        val instructions = buildRunStreetInstructions(
            listOf(
                ValhallaManeuver("Caminhe pela Rua A", 0.05),
                ValhallaManeuver("Vire à direita para a Rua A", 0.4)
            )
        )

        assertEquals(1, instructions.size)
        assertEquals("Vire à direita para a Rua A", instructions.single().text)
        assertEquals(50.0, instructions.single().distanceAlongRouteMeters, 0.0)
    }

    @Test
    fun ignoresConnectorLegsBetweenSeparatePlannedRouteSegments() {
        val route = buildRunStreetRoute(
            legs = listOf(
                ValhallaRouteLeg(
                    points = listOf(point(0.0, 0.0), point(0.0, 0.001)),
                    maneuvers = listOf(ValhallaManeuver("Início", 0.05), ValhallaManeuver("Vire", 0.4))
                ),
                ValhallaRouteLeg(
                    points = listOf(point(0.0, 0.001), point(0.0, 1.0)),
                    maneuvers = listOf(ValhallaManeuver("Conector", 20.0))
                ),
                ValhallaRouteLeg(
                    points = listOf(point(1.0, 1.0), point(1.0, 1.001)),
                    maneuvers = listOf(ValhallaManeuver("Início", 0.02), ValhallaManeuver("Segunda rua", 0.1))
                )
            ),
            segmentCount = 2
        )

        assertEquals(listOf(0, 0, 1, 1), route.points.map { it.segmentIndex })
        assertEquals(2, route.instructions.size)
        assertEquals("Segunda rua", route.instructions.last().text)
        assertEquals(4, route.points.size)
        assert(route.distanceMeters in 200.0..250.0)
        assert(route.instructions.last().distanceAlongRouteMeters in 120.0..160.0)
    }

    private fun point(latitude: Double, longitude: Double) = PlannedRoutePoint(latitude, longitude, 0)
}
