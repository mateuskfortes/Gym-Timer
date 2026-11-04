package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunStreetInstruction
import com.example.gymtimer2.domain.model.nextRunStreetGuidance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunStreetGuidanceTest {
    @Test
    fun returnsNextStreetInstructionAndKeepsItVisibleJustAfterTheTurn() {
        val instructions = listOf(
            RunStreetInstruction("Vire à direita para a Rua A", 100.0),
            RunStreetInstruction("Vire à esquerda para a Rua B", 300.0)
        )

        val next = nextRunStreetGuidance(instructions, 30.0)

        assertEquals("Vire à direita para a Rua A", next?.text)
        assertEquals(70.0, next?.distanceToInstructionMeters ?: -1.0, 0.0)
        assertEquals("Vire à esquerda para a Rua B", nextRunStreetGuidance(instructions, 130.0)?.text)
        assertNull(nextRunStreetGuidance(instructions, Double.NaN))
    }

}
