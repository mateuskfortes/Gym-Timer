@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.ui.screens.runs.components.RunElevationProfileSection
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Rule
import org.junit.Test

/** Verifica quando o histórico apresenta o gráfico de altitude salvo. */
class RunElevationProfileTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsChartWhenTwoRoutePointsHaveAltitude() {
        composeRule.setContent {
            GymTimerTheme {
                RunElevationProfileSection(listOf(point(0.0, 100.0), point(0.001, 110.0)))
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Perfil de elevação", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Perfil de elevação", substring = true).assertIsDisplayed()
    }

    @Test
    fun hidesChartWhenRouteHasFewerThanTwoUsableAltitudePoints() {
        composeRule.setContent {
            GymTimerTheme {
                RunElevationProfileSection(listOf(point(0.0, null)))
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.run_elevation_profile_title))
            .assertDoesNotExist()
    }

    @Test
    fun selectingElevationOnChartShowsTheSelectedValue() {
        val points = listOf(point(0.0, 100.0), point(0.001, 110.0))
        composeRule.setContent {
            GymTimerTheme { RunElevationProfileSection(points) }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Perfil de elevação", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Perfil de elevação", substring = true).performTouchInput {
            click()
        }

        composeRule.onNodeWithContentDescription("Elevação de", substring = true).assertIsDisplayed()
    }

    private fun point(longitude: Double, altitudeMeters: Double?) = RunPointModel(
        id = 1,
        runId = 1,
        latitude = 0.0,
        longitude = longitude,
        recordedAtEpochMs = 1_000,
        accuracyMeters = 2f,
        segmentIndex = 0,
        altitudeMeters = altitudeMeters,
        verticalAccuracyMeters = 2f
    )
}
