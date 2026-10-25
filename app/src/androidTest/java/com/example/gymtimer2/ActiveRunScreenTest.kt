@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.active.ActiveRunScreen
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica o painel recolhido e a saída segura da tela durante uma corrida. */
@RunWith(AndroidJUnit4::class)
class ActiveRunScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Atualizar o tempo não deve expandir o painel que a pessoa recolheu. */
    @Test
    fun collapsedPanelStaysCollapsedWhenDurationChanges() {
        val run = mutableStateOf(pausedRun())
        var resumeCount = 0
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    run = run.value,
                    points = emptyList(),
                    onPause = {},
                    onResume = { resumeCount++ },
                    onDiscard = {},
                    onFinish = {},
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Retomar").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Minimizar painel").performClick()
        composeRule.runOnIdle { run.value = run.value.copy(durationMs = 11_000L) }
        composeRule.onNodeWithContentDescription("Expandir painel").assertIsDisplayed()
        composeRule.onNodeWithText("Retomar").assertIsNotDisplayed()
        composeRule.onNodeWithContentDescription("Expandir painel").performClick()
        composeRule.onNodeWithText("Retomar").performClick()
        composeRule.runOnIdle { assertEquals(1, resumeCount) }
    }

    /** Voltar e cancelar a confirmação mantêm a sessão disponível para continuar. */
    @Test
    fun leavingScreenAndDismissingConfirmationDoNotFinishRun() {
        var finishCount = 0
        var backCount = 0
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    run = pausedRun(),
                    points = emptyList(),
                    onPause = {},
                    onResume = {},
                    onDiscard = {},
                    onFinish = { finishCount++ },
                    onBack = { backCount++ }
                )
            }
        }
        composeRule.onNodeWithText("Finalizar").performClick()
        composeRule.onNodeWithText("Finalizar corrida?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithContentDescription("Voltar ao histórico").performClick()
        composeRule.runOnIdle {
            assertEquals(0, finishCount)
            assertEquals(1, backCount)
        }
    }

    /** Cria uma sessão pausada sem coordenadas ou serviço de localização ativo. */
    private fun pausedRun() = RunModel(
        id = 1L,
        startedAtEpochMs = 1_700_000_000_000L,
        finishedAtEpochMs = null,
        durationMs = 10_000L,
        distanceMeters = 0.0,
        status = RunStatus.PAUSED,
        currentSegment = 0
    )
}
