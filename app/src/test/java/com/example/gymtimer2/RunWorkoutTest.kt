package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutSetPlan
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.RunWorkoutStepPlan
import com.example.gymtimer2.domain.model.RunWorkoutTrigger
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.runElevationReadingOrNull
import com.example.gymtimer2.domain.model.advanceRunWorkout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica validação, repetição e transições dos treinos estruturados. */
class RunWorkoutTest {
    @Test
    fun planRepeatsEveryStepInASetBeforeStartingTheNextRepetition() {
        val plan = RunWorkoutPlan(
            name = "  Intervalado  ",
            sets = listOf(
                RunWorkoutSetPlan(
                    steps = listOf(
                        RunWorkoutStepPlan("Tiro", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = 400.0),
                        RunWorkoutStepPlan("Recuperação", RunWorkoutTrigger.DURATION, durationTargetMs = 60_000, isRestInterval = true)
                    ),
                    repetitions = 3
                )
            )
        )

        val normalized = plan.normalizedOrNull()

        assertEquals("Intervalado", normalized?.name)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), normalized?.expandedSteps()?.map { it.stepIndex })
        assertEquals(
            listOf("Tiro", "Recuperação", "Tiro", "Recuperação", "Tiro", "Recuperação"),
            normalized?.expandedSteps()?.map { it.description }
        )
        assertEquals(listOf(1, 1, 2, 2, 3, 3), normalized?.expandedSteps()?.map { it.repetition })
    }

    @Test
    fun planCanOmitOnlyTheLastRecoveryOfARepeatedSet() {
        val plan = RunWorkoutPlan(
            "Intervalado",
            listOf(
                RunWorkoutSetPlan(
                    steps = listOf(
                        RunWorkoutStepPlan("Tiro", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = 400.0),
                        RunWorkoutStepPlan("Recuperação", RunWorkoutTrigger.DURATION, durationTargetMs = 60_000, isRestInterval = true)
                    ),
                    repetitions = 3,
                    omitLastRecoveryStep = true
                )
            )
        )

        val expanded = plan.normalizedOrNull()?.expandedSteps().orEmpty()

        assertEquals(listOf("Tiro", "Recuperação", "Tiro", "Recuperação", "Tiro"), expanded.map { it.description })
        assertEquals(3, expanded.last().repetition)
    }

    @Test
    fun expandedStepsKeepPerStepPaceTargets() {
        val target = RunPaceTarget(300, 360)
        val plan = RunWorkoutPlan(
            "Intervalado",
            listOf(
                RunWorkoutSetPlan(
                    listOf(
                        RunWorkoutStepPlan("Tiro", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = 400.0, paceTarget = target)
                    )
                )
            )
        )

        assertEquals(target, plan.normalizedOrNull()?.expandedSteps()?.single()?.paceTarget)
    }

    @Test
    fun planRejectsMissingOrInvalidTargets() {
        assertNull(RunWorkoutPlan("Treino", emptyList()).normalizedOrNull())
        val invalidDistance = RunWorkoutPlan(
            "Treino",
            listOf(RunWorkoutSetPlan(listOf(
                RunWorkoutStepPlan("Corrida", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = Double.NaN)
            )))
        )

        assertNull(invalidDistance.normalizedOrNull())
    }

    @Test
    fun durationStepUsesActiveTimeAndStartsNextStepAtCurrentMetrics() {
        val initial = listOf(
            RunWorkoutStep(0, "Aquecimento", RunWorkoutTrigger.DURATION, durationTargetMs = 60_000, startedAtDurationMs = 0, startedAtDistanceMeters = 0.0),
            RunWorkoutStep(1, "Tiro", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = 400.0)
        )

        val early = advanceRunWorkout(initial, durationMs = 59_999, distanceMeters = 300.0)
        val transition = advanceRunWorkout(initial, durationMs = 60_000, distanceMeters = 320.0)

        assertTrue(early.completedSteps.isEmpty())
        assertEquals("Aquecimento", transition.completedSteps.single().description)
        assertEquals(60_000L, transition.steps[1].startedAtDurationMs)
        assertEquals(320.0, transition.steps[1].startedAtDistanceMeters!!, 0.0)
    }

    @Test
    fun distanceStepWaitsForDistanceAndManualStepNeedsExplicitAdvance() {
        val initial = listOf(
            RunWorkoutStep(0, "Corrida", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = 500.0, startedAtDurationMs = 0, startedAtDistanceMeters = 0.0),
            RunWorkoutStep(1, "Recuperação", RunWorkoutTrigger.MANUAL, isRestInterval = true)
        )

        val underDistance = advanceRunWorkout(initial, durationMs = 90_000, distanceMeters = 499.0)
        val reached = advanceRunWorkout(initial, durationMs = 90_000, distanceMeters = 500.0)
        val manual = advanceRunWorkout(reached.steps, durationMs = 90_000, distanceMeters = 500.0)

        assertTrue(underDistance.completedSteps.isEmpty())
        assertEquals(0, reached.completedSteps.single().stepIndex)
        assertTrue(manual.completedSteps.isEmpty())
        val advanced = advanceRunWorkout(reached.steps, 90_000, 500.0, manualAdvance = true)
        assertEquals(1, advanced.completedSteps.single().stepIndex)
        assertFalse(advanced.steps.last().completedAtDurationMs == null)
    }

    @Test
    fun elevationStepEndsAfterAReversalFromTheLatestExtreme() {
        val plan = RunWorkoutPlan(
            "Subidas",
            listOf(RunWorkoutSetPlan(listOf(
                RunWorkoutStepPlan("Elevação", RunWorkoutTrigger.ELEVATION, elevationChangeTargetMeters = 10.0),
                RunWorkoutStepPlan("Próxima subida", RunWorkoutTrigger.ELEVATION, elevationChangeTargetMeters = 10.0)
            )))
        )
        val steps = checkNotNull(plan.normalizedOrNull()).expandedSteps().map {
            it.copy(startedAtDurationMs = 0L, startedAtDistanceMeters = 0.0)
        }

        val initialized = advanceElevation(steps, 0L, 0.0, 100.0)
        val peak = advanceElevation(initialized.steps, 1_000L, 10.0, 120.0)
        val belowThreshold = advanceElevation(peak.steps, 2_000L, 20.0, 111.0)
        val firstReversal = advanceElevation(belowThreshold.steps, 3_000L, 30.0, 110.0)

        assertTrue(initialized.completedSteps.isEmpty())
        assertTrue(peak.completedSteps.isEmpty())
        assertTrue(belowThreshold.completedSteps.isEmpty())
        assertEquals(0, firstReversal.completedSteps.single().stepIndex)
        assertEquals(110.0, firstReversal.steps[1].minimumElevationMeters!!, 0.0)
        assertEquals(110.0, firstReversal.steps[1].maximumElevationMeters!!, 0.0)

        val valley = advanceElevation(firstReversal.steps, 4_000L, 40.0, 100.0)
        val secondReversal = advanceElevation(valley.steps, 5_000L, 50.0, 110.0)

        assertTrue(valley.completedSteps.isEmpty())
        assertEquals(1, secondReversal.completedSteps.single().stepIndex)
    }

    @Test
    fun delayedGpsStartsTheNextDistanceStepAtTheLatestAcceptedPoint() {
        val initial = listOf(
            RunWorkoutStep(0, "Corrida", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = 400.0, startedAtDurationMs = 0, startedAtDistanceMeters = 0.0),
            RunWorkoutStep(1, "Tiro", RunWorkoutTrigger.DISTANCE, distanceTargetMeters = 400.0)
        )

        val transition = advanceRunWorkout(initial, durationMs = 300_000, distanceMeters = 900.0)

        assertEquals(listOf(0), transition.completedSteps.map { it.stepIndex })
        assertEquals(900.0, transition.steps.last().startedAtDistanceMeters!!, 0.0)
    }

    private fun advanceElevation(
        steps: List<RunWorkoutStep>,
        durationMs: Long,
        distanceMeters: Double,
        altitudeMeters: Double
    ) = advanceRunWorkout(
        steps = steps,
        durationMs = durationMs,
        distanceMeters = distanceMeters,
        elevationReading = checkNotNull(runElevationReadingOrNull(altitudeMeters, 1f))
    )
}
