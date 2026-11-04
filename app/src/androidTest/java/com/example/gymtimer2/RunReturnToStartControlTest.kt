@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.gymtimer2.ui.screens.runs.active.RunReturnToStartControl
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Confere que a solicitação pública só acontece após mostrar o aviso de privacidade. */
class RunReturnToStartControlTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun streetRouteRequiresExplicitChoiceAfterPrivacyNotice() {
        var routeRequested = false
        composeRule.setContent {
            GymTimerTheme {
                RunReturnToStartControl(
                    isReturning = false,
                    hasRecordedPath = true,
                    canCalculateStreetRoute = true,
                    isLoading = false,
                    hasError = false,
                    streetRouteDistanceMeters = null,
                    onStartRecorded = {},
                    onCalculateStreet = { routeRequested = true },
                    onStop = {}
                )
            }
        }

        val startAction = composeRule.activity.getString(R.string.run_return_start_action)
        val privacyNotice = composeRule.activity.getString(R.string.run_return_dialog_message)
        val streetAction = composeRule.activity.getString(R.string.run_return_street_action)
        composeRule.onNodeWithText(startAction).performClick()
        composeRule.onNodeWithText(privacyNotice).assertIsDisplayed()
        composeRule.runOnIdle { assertFalse(routeRequested) }
        composeRule.onNodeWithText(streetAction).performClick()
        composeRule.runOnIdle { assertTrue(routeRequested) }
    }
}
