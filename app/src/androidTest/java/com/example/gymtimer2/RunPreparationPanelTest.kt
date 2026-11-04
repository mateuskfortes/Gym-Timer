package com.example.gymtimer2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymtimer2.ui.screens.runs.active.RunPreparationPanel
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Confirma que a preparação mostra localização e inicia somente após a ação explícita. */
@RunWith(AndroidJUnit4::class)
class RunPreparationPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** A tela oferece uma ação acessível e só chama o início após o toque. */
    @Test
    fun startActionBeginsRunOnClick() {
        var startCount = 0
        composeRule.setContent {
            GymTimerTheme {
                RunPreparationPanel(
                    gpsStatus = RunGpsStatus.SEARCHING,
                    selectedRouteTitle = null,
                    paceTarget = null,
                    onChooseRoute = {},
                    onEditPaceTarget = {},
                    onStart = { startCount++ }
                )
            }
        }

        composeRule.onNodeWithText("Procurando sinal de GPS").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Iniciar Corrida").performClick()
        composeRule.runOnIdle { assertEquals(1, startCount) }
    }

    @Test
    fun paceTargetActionOpensItsEditor() {
        var editCount = 0
        composeRule.setContent {
            GymTimerTheme {
                RunPreparationPanel(
                    gpsStatus = RunGpsStatus.GOOD,
                    selectedRouteTitle = null,
                    paceTarget = RunPaceTarget(330, 360),
                    onChooseRoute = {},
                    onEditPaceTarget = { editCount++ },
                    onStart = {}
                )
            }
        }

        composeRule.onNodeWithText("5:30/km – 6:00/km").performClick()
        composeRule.runOnIdle { assertEquals(1, editCount) }
    }
}
