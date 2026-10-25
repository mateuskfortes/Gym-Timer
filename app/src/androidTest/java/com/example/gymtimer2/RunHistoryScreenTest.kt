@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.RunScreen
import com.example.gymtimer2.ui.screens.runs.RunUiState
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.theme.GymTimerTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Exercita confirmação, cancelamento e indicação da sessão aberta sem gravar dados reais. */
class RunHistoryScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val deletedIds = mutableListOf<Long>()
    private val selectedIds = mutableListOf<Long>()
    private var openedActiveRun = false

    /** Tocar no ícone não exclui; cancelar preserva a corrida e confirmar envia o ID correto. */
    @Test
    fun deletionRequiresConfirmationForSelectedRun() {
        val run = sampleRun()
        val otherRun = run.copy(id = 8L, startedAtEpochMs = run.startedAtEpochMs - 86_400_000L)
        showScreen(RunUiState(completedRuns = listOf(otherRun, run)))
        requestDeletion(run)
        composeRule.runOnIdle { assertEquals(emptyList<Long>(), deletedIds) }
        composeRule.onNodeWithText(text(R.string.cancel_button)).performClick()
        composeRule.onNodeWithText(text(R.string.run_delete_title)).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(emptyList<Long>(), deletedIds) }
        requestDeletion(run)
        composeRule.onNodeWithText(text(R.string.run_delete_action)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(run.id), deletedIds)
            assertEquals(emptyList<Long>(), selectedIds)
        }
    }

    /** Após remover o cartão, o vizinho deve passar por posições intermediárias até ocupar seu espaço. */
    @Test
    fun deletingRunAnimatesFollowingCardIntoVacatedSpace() {
        val firstRun = sampleRun()
        val followingRun = firstRun.copy(id = 10L, startedAtEpochMs = firstRun.startedAtEpochMs - 86_400_000L)
        val state = mutableStateOf(RunUiState(completedRuns = listOf(firstRun, followingRun)))
        showScreen { state.value }
        val firstDescription = deleteDescription(firstRun)
        val followingDescription = deleteDescription(followingRun)
        val targetTop = composeRule.onNodeWithContentDescription(firstDescription).fetchSemanticsNode().boundsInRoot.top
        val originalTop = composeRule.onNodeWithContentDescription(followingDescription).fetchSemanticsNode().boundsInRoot.top
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { state.value = RunUiState(completedRuns = listOf(followingRun)) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(64)
        val intermediateTop = composeRule.onNodeWithContentDescription(followingDescription)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue("O cartão deve subir gradualmente", intermediateTop > targetTop && intermediateTop < originalTop)
        composeRule.mainClock.advanceTimeBy(1_000)
        val finalTop = composeRule.onNodeWithContentDescription(followingDescription).fetchSemanticsNode().boundsInRoot.top
        assertEquals(targetTop, finalTop, 1f)
        composeRule.onNodeWithContentDescription(firstDescription).assertDoesNotExist()
    }

    /** A sessão aberta se destaca e permite voltar à corrida sem iniciar outra. */
    @Test
    fun activeSessionShowsStatusAndReopensRun() {
        showScreen(RunUiState(activeRun = sampleRun(RunStatus.RUNNING)))
        composeRule.onNodeWithText(text(R.string.run_status_recording)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.run_start_action)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_open_active_action)).performClick()
        composeRule.runOnIdle { assertEquals(true, openedActiveRun) }
    }

    /** Pausa e interrupção devem aparecer com o estado correto no mesmo cartão. */
    @Test
    fun activeSessionStatusUpdatesWithoutShowingRecordingForPausedRun() {
        val state = mutableStateOf(RunUiState(activeRun = sampleRun(RunStatus.PAUSED)))
        showScreen { state.value }
        composeRule.onNodeWithText(text(R.string.run_status_paused)).assertIsDisplayed()
        composeRule.runOnIdle { state.value = RunUiState(activeRun = sampleRun(RunStatus.INTERRUPTED)) }
        composeRule.onNodeWithText(text(R.string.run_status_interrupted)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.run_status_recording)).assertDoesNotExist()
    }

    /** O detalhe usa o mesmo modal e só exclui após confirmar. */
    @Test
    fun detailsDeleteUsesSameConfirmation() {
        val run = sampleRun()
        showScreen(RunUiState(completedRuns = listOf(run), selectedRun = run))
        composeRule.onNodeWithText(text(R.string.run_delete_action)).performClick()
        composeRule.onNodeWithText(text(R.string.run_delete_title)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList<Long>(), deletedIds) }
        composeRule.onNodeWithText(text(R.string.cancel_button)).performClick()
        composeRule.onNodeWithText(text(R.string.run_delete_action)).assertIsDisplayed()
    }

    /** O título salvo identifica a corrida tanto no histórico quanto no detalhe. */
    @Test
    fun savedTitleAppearsInHistoryAndDetails() {
        val run = sampleRun().copy(title = "Corrida no parque")
        val state = mutableStateOf(RunUiState(completedRuns = listOf(run)))
        showScreen { state.value }
        composeRule.onNodeWithText("Corrida no parque").assertIsDisplayed()
        composeRule.onNodeWithText(RunFormatting.formatDate(run.startedAtEpochMs)).assertIsDisplayed()
        composeRule.runOnIdle { state.value = state.value.copy(selectedRun = run) }
        composeRule.onNodeWithText("Corrida no parque").assertIsDisplayed()
    }

    /** Solicita a exclusão pelo controle acessível da corrida. */
    private fun requestDeletion(run: RunModel) {
        composeRule.onNodeWithContentDescription(deleteDescription(run)).performClick()
        composeRule.onNodeWithText(text(R.string.run_delete_title)).assertIsDisplayed()
    }

    /** Identifica o controle sem depender da posição do cartão na lista. */
    private fun deleteDescription(run: RunModel) = composeRule.activity.getString(
        R.string.run_delete_description, RunFormatting.displayTitle(run)
    )

    /** Apresenta um estado fixo para os casos sem atualização. */
    private fun showScreen(state: RunUiState) = showScreen { state }

    /** Injeta callbacks para verificar o comportamento sem iniciar serviço ou acessar o Room. */
    private fun showScreen(state: () -> RunUiState) {
        composeRule.setContent {
            GymTimerTheme {
                RunScreen(
                    state = state(), onStartRun = {}, onPauseRun = {}, onResumeRun = {},
                    onFinishRun = { _, _ -> }, onDiscardRun = {}, onSelectRun = { selectedIds.add(it) }, onDeleteRun = { deletedIds.add(it) },
                    onBackFromDetails = {}, onOpenActiveRun = { openedActiveRun = true }, onCloseActiveRun = {}
                )
            }
        }
    }

    /** Usa a mesma tradução da interface para localizar os controles. */
    private fun text(resourceId: Int) = composeRule.activity.getString(resourceId)

    /** Cria uma sessão isolada com métricas pequenas para caber na tela do teste. */
    private fun sampleRun(status: RunStatus = RunStatus.COMPLETED) = RunModel(
        id = 9L, startedAtEpochMs = 1_700_000_000_000L, finishedAtEpochMs = null,
        durationMs = 60_000L, distanceMeters = 200.0, status = status, currentSegment = 0
    )
}
