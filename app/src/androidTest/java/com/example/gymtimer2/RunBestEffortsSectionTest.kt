@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.ui.screens.runs.components.RunBestEffortsSection
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Rule
import org.junit.Test

/** Confirma que as marcas calculadas aparecem no detalhe da corrida. */
class RunBestEffortsSectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displaysCoveredBenchmarkDistances() {
        val points = listOf(
            point(1L, 0.0, 1_000L, 0),
            point(2L, 0.004, 41_000L, 0),
            point(3L, 0.004, 121_000L, 1),
            point(4L, 0.008, 161_000L, 1)
        )
        composeRule.setContent {
            GymTimerTheme {
                RunBestEffortsSection(
                    runId = 1L,
                    points = points,
                    overrides = emptyList(),
                    onSaveOverride = {},
                    onClearOverride = {}
                )
            }
        }

        composeRule.onNodeWithText("Melhores esforços").assertIsDisplayed()
        composeRule.onNodeWithText("400 m").assertIsDisplayed()
        composeRule.onNodeWithText("½ milha").assertIsDisplayed()
    }

    private fun point(id: Long, latitude: Double, timestampMs: Long, segmentIndex: Int) = RunPointModel(
        id = id,
        runId = 1L,
        latitude = latitude,
        longitude = 0.0,
        recordedAtEpochMs = timestampMs,
        accuracyMeters = 5f,
        segmentIndex = segmentIndex,
        elapsedRealtimeMs = timestampMs
    )
}
