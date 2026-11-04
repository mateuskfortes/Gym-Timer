package com.example.gymtimer2.domain.model

data class RunStreetRoute(
    val points: List<PlannedRoutePoint>,
    val distanceMeters: Double,
    val instructions: List<RunStreetInstruction> = emptyList()
)

data class RunStreetInstruction(
    val text: String,
    val distanceAlongRouteMeters: Double
)

data class RunStreetGuidance(
    val instructionIndex: Int,
    val text: String,
    val distanceToInstructionMeters: Double
)

fun nextRunStreetGuidance(
    instructions: List<RunStreetInstruction>,
    distanceAlongRouteMeters: Double
): RunStreetGuidance? {
    if (!distanceAlongRouteMeters.isFinite() || distanceAlongRouteMeters < 0.0) return null
    val index = instructions.indexOfFirst {
        it.distanceAlongRouteMeters >= distanceAlongRouteMeters - PASSED_INSTRUCTION_TOLERANCE_METERS
    }
    if (index < 0) return null

    val instruction = instructions[index]
    return RunStreetGuidance(
        instructionIndex = index,
        text = instruction.text,
        distanceToInstructionMeters =
            (instruction.distanceAlongRouteMeters - distanceAlongRouteMeters).coerceAtLeast(0.0)
    )
}

private const val PASSED_INSTRUCTION_TOLERANCE_METERS = 20.0
