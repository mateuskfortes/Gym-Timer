@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymtimer2.domain.model.RunStreetGuidance
import com.example.gymtimer2.domain.model.RunStreetRoute
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.RunLiveSegment
import com.example.gymtimer2.ui.screens.runs.RunStreetReturnState
import com.example.gymtimer2.ui.screens.runs.RunUiState
import com.example.gymtimer2.ui.screens.runs.active.ActiveRunScreen
import com.example.gymtimer2.ui.screens.runs.active.RunActiveScreenActions
import com.example.gymtimer2.ui.screens.runs.active.RunSheetHeader
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica o painel recolhido e a saída segura da tela durante uma corrida. */
@RunWith(AndroidJUnit4::class)
class ActiveRunScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** A preferência mantém a tela ativa no painel e libera quando ele fecha. */
    @Test
    fun keepScreenAwakeFollowsTheActiveRunScreen() {
        val keepScreenAwake = mutableStateOf(true)
        var composeView: View? = null
        composeRule.setContent {
            GymTimerTheme {
                composeView = LocalView.current
                ActiveRunScreen(
                    state = activeRunState(pausedRun()),
                    actions = activeRunActions(),
                    keepScreenAwake = keepScreenAwake.value,
                )
            }
        }
        composeRule.waitForIdle()
        assertViewKeepsScreenOn(composeView, true)

        composeRule.runOnIdle { keepScreenAwake.value = false }
        composeRule.waitForIdle()
        assertViewKeepsScreenOn(composeView, false)
    }

    @Test
    fun touchingRunControlsRestoresDimmedWindowBrightness() {
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(pausedRun().copy(status = RunStatus.RUNNING)),
                    actions = activeRunActions(),
                    keepScreenAwake = true,
                    screenDimTimeoutMs = 100L
                )
            }
        }
        composeRule.waitUntil(2_000) {
            composeRule.activity.window.attributes.screenBrightness == DIMMED_SCREEN_BRIGHTNESS
        }

        composeRule.onNodeWithText("Pausar").performTouchInput { click() }

        composeRule.waitUntil(2_000) {
            composeRule.activity.window.attributes.screenBrightness == SYSTEM_SCREEN_BRIGHTNESS
        }
    }

    /** A preferência oculta o botão de volta sem remover as voltas salvas. */
    @Test
    fun manualLapButtonCanBeHidden() {
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(pausedRun().copy(status = RunStatus.RUNNING)),
                    actions = activeRunActions(onLap = null)
                )
            }
        }

        composeRule.onNodeWithText("Marcar volta").assertIsNotDisplayed()
    }

    /** Atualizar o tempo não deve expandir o painel que a pessoa recolheu. */
    @Test
    fun collapsedPanelStaysCollapsedWhenDurationChanges() {
        val run = mutableStateOf(pausedRun())
        var resumeCount = 0
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(run.value),
                    actions = activeRunActions(onResume = { resumeCount++ })
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

    /** O cabeçalho minimizado mantém as três métricas principais à vista. */
    @Test
    fun collapsedPanelHeaderShowsRunSummary() {
        composeRule.setContent {
            GymTimerTheme {
                RunSheetHeader(
                    run = pausedRun().copy(distanceMeters = 1_250.0),
                    expanded = false,
                    onToggle = {}
                )
            }
        }

        composeRule.onNodeWithText("Tempo ativo").assertIsDisplayed()
        composeRule.onNodeWithText("Distância").assertIsDisplayed()
        composeRule.onNodeWithText("Pace médio").assertIsDisplayed()
    }

    @Test
    fun collapsedPanelShowsGpsStatusWhenSignalIsUnavailable() {
        composeRule.setContent {
            GymTimerTheme {
                RunSheetHeader(
                    run = pausedRun(),
                    expanded = false,
                    onToggle = {},
                    gpsStatus = RunGpsStatus.NO_SIGNAL
                )
            }
        }

        composeRule.onNodeWithText("Sem sinal de GPS").assertIsDisplayed()
    }

    /** O modo de mapa em tela cheia oculta e restaura o painel da corrida. */
    @Test
    fun fullscreenMapCanHideAndRestoreRunPanel() {
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(pausedRun()),
                    actions = activeRunActions()
                )
            }
        }

        composeRule.onNodeWithText("Retomar").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ocultar dados e controles").performClick()
        composeRule.onNodeWithContentDescription("Mostrar dados e controles").assertIsDisplayed()
        composeRule.onNodeWithText("Retomar").assertIsNotDisplayed()

        composeRule.onNodeWithContentDescription("Mostrar dados e controles").performClick()
        composeRule.onNodeWithText("Retomar").assertIsDisplayed()
    }

    /** Voltar e cancelar a confirmação mantêm a sessão disponível para continuar. */
    @Test
    fun leavingScreenAndDismissingConfirmationDoNotFinishRun() {
        var finishCount = 0
        var backCount = 0
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(pausedRun().copy(distanceMeters = 1.0)),
                    actions = activeRunActions(
                        onFinish = { finishCount++ },
                        onBack = { backCount++ }
                    )
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

    /** Ao finalizar sem distância, a tela oferece retomar ou confirmar o descarte. */
    @Test
    fun finishingEmptyRunShowsResumeAndDiscardPrompt() {
        var finishCount = 0
        var discardCount = 0
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(pausedRun()),
                    actions = activeRunActions(
                        onDiscard = { discardCount++ },
                        onFinish = { finishCount++ }
                    )
                )
            }
        }
        composeRule.onNodeWithText("Finalizar").performClick()
        composeRule.onNodeWithText("Ainda não saiu do lugar?").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, finishCount)
            assertEquals(0, discardCount)
        }
    }

    /** Mostra o pace atual quando a corrida ativa possui velocidade GPS válida. */
    @Test
    fun runningScreenShowsCurrentPace() {
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(
                        run = pausedRun().copy(
                            status = RunStatus.RUNNING,
                            distanceMeters = 1_000.0,
                            paceTargetFasterSecondsPerKm = 350,
                            paceTargetSlowerSecondsPerKm = 450
                        ),
                        gpsStatus = RunGpsStatus.GOOD,
                        currentPaceSecondsPerKm = 400.0
                    ),
                    actions = activeRunActions()
                )
            }
        }

        composeRule.onNodeWithTag("run-stats-pager").performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("Pace atual").assertIsDisplayed()
        composeRule.onNodeWithText("6:40/km").assertIsDisplayed()
        composeRule.onNodeWithText("Dentro da faixa").assertIsDisplayed()
        composeRule.onNodeWithText("Velocidade atual").assertIsDisplayed()
    }

    @Test
    fun runningScreenShowsLocalSegmentProgressAndRecord() {
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(
                        run = pausedRun().copy(status = RunStatus.RUNNING),
                        localSegments = listOf(
                        RunLiveSegment(
                            name = "Parque",
                            progressMeters = 450.0,
                            distanceMeters = 1_000.0,
                            elapsedDurationMs = 20_000L,
                            bestDurationMs = 42_000L,
                            differenceFromBestMs = 1_000L,
                            isCompleted = false,
                            isNewRecord = false
                        ),
                        RunLiveSegment(
                            name = "Pista",
                            progressMeters = 0.0,
                            distanceMeters = 500.0,
                            elapsedDurationMs = 0L,
                            bestDurationMs = null,
                            differenceFromBestMs = null,
                            isCompleted = false,
                            isNewRecord = false,
                            distanceToStartMeters = 150.0
                        )
                    )
                    ),
                    actions = activeRunActions()
                )
            }
        }

        composeRule.onNodeWithText("Segmento local").assertIsDisplayed()
        composeRule.onNodeWithText("Parque").assertIsDisplayed()
        composeRule.onNodeWithText("Próximo segmento").assertIsDisplayed()
        composeRule.onNodeWithText("Pista").assertIsDisplayed()
        composeRule.onNodeWithText("Melhor tempo local: 00:42").assertIsDisplayed()
    }

    @Test
    fun runningScreenShowsTheNextStreetReturnInstruction() {
        composeRule.setContent {
            GymTimerTheme {
                ActiveRunScreen(
                    state = activeRunState(
                        run = pausedRun().copy(status = RunStatus.RUNNING),
                        streetReturnState = RunStreetReturnState(
                            route = RunStreetRoute(emptyList(), 100.0),
                            nextGuidance = RunStreetGuidance(
                                instructionIndex = 0,
                                text = "Vire à direita para a Rua A",
                                distanceToInstructionMeters = 42.0
                            )
                        )
                    ),
                    actions = activeRunActions()
                )
            }
        }

        composeRule.onNodeWithText("Próxima orientação de rua").assertIsDisplayed()
        composeRule.onNodeWithText("Vire à direita para a Rua A · Em 42 m").assertIsDisplayed()
    }

    private fun activeRunState(
        run: RunModel,
        points: List<RunPointModel> = emptyList(),
        gpsStatus: RunGpsStatus = RunGpsStatus.SEARCHING,
        currentPaceSecondsPerKm: Double? = null,
        localSegments: List<RunLiveSegment> = emptyList(),
        streetReturnState: RunStreetReturnState = RunStreetReturnState()
    ) = RunUiState(
        activeRun = run,
        activePoints = points,
        gpsStatus = gpsStatus,
        currentPaceSecondsPerKm = currentPaceSecondsPerKm,
        localSegments = localSegments,
        streetReturnState = streetReturnState
    )

    private fun activeRunActions(
        onPause: () -> Unit = {},
        onResume: () -> Unit = {},
        onFinish: (RunSaveDetails) -> Unit = {},
        onDiscard: () -> Unit = {},
        onBack: () -> Unit = {},
        onLap: (() -> Unit)? = {}
    ) = RunActiveScreenActions(
        onPause = onPause,
        onResume = onResume,
        onFinish = onFinish,
        onDiscard = onDiscard,
        onBack = onBack,
        onLap = onLap
    )

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

    private fun assertViewKeepsScreenOn(view: View?, expected: Boolean) {
        composeRule.runOnIdle {
            assertEquals(expected, view?.keepScreenOn)
        }
    }
}

private const val DIMMED_SCREEN_BRIGHTNESS = 0.1f
private const val SYSTEM_SCREEN_BRIGHTNESS = -1f
