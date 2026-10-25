@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingService
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingServiceController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Coordena a gravação, a recuperação e a consulta do histórico de corridas. */
@OptIn(ExperimentalCoroutinesApi::class)
class RunViewModel(
    private val repository: RunRepository,
    private val serviceController: RunTrackingServiceController
) : ViewModel() {
    private val activeRunOpen = MutableStateFlow(false)
    private val selectedRunId = MutableStateFlow<Long?>(null)

    private val activePoints = repository.activeRun.flatMapLatest { run ->
        run?.let { repository.observePoints(it.id) } ?: flowOf(emptyList())
    }
    private val selectedRun = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observeRun) ?: flowOf(null)
    }
    private val selectedPoints = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observePoints) ?: flowOf(emptyList())
    }

    /** Junta os fluxos da corrida atual, do histórico e do detalhe escolhido. */
    val uiState = combine(
        repository.activeRun,
        activePoints,
        repository.completedRuns,
        selectedRun,
        selectedPoints
    ) { activeRun, route, history, detail, detailRoute ->
        RunUiState(
            activeRun = activeRun,
            activePoints = route,
            completedRuns = history,
            selectedRun = detail,
            selectedPoints = detailRoute
        )
    }.combine(activeRunOpen) { state, isOpen ->
        state.copy(isActiveRunOpen = isOpen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunUiState())

    init {
        recoverInterruptedRun()
    }

    /** Cria uma sessão local e pede ao serviço para iniciar o GPS. */
    fun startRun() {
        viewModelScope.launch {
            val runId = repository.startRun(System.currentTimeMillis())
            startService(runId, serviceController::start)
            openActiveRun()
        }
    }

    /** Abre a sessão atual sem alterar seu estado de gravação. */
    fun openActiveRun() {
        selectedRunId.value = null
        activeRunOpen.value = true
    }

    /** Volta ao histórico sem interromper o serviço GPS. */
    fun closeActiveRun() {
        activeRunOpen.value = false
    }

    /** Retoma uma sessão pausada ou interrompida em um novo trecho de rota. */
    fun resumeRun(runId: Long) {
        viewModelScope.launch { startService(runId, serviceController::resume) }
    }

    /** Pausa o GPS e mantém a sessão disponível para retomada. */
    fun pauseRun(runId: Long) {
        serviceController.pause(runId)
    }

    /** Finaliza a sessão pelo serviço ou salva a última versão interrompida. */
    fun finishRun(runId: Long, title: String?) {
        viewModelScope.launch {
            if (RunTrackingService.isTracking(runId)) {
                serviceController.finish(runId, title)
            } else {
                val run = repository.getRun(runId) ?: return@launch
                repository.finishRun(runId, run.durationMs, System.currentTimeMillis(), title)
            }
            closeActiveRun()
        }
    }

    /** Encaminha o descarte ao serviço ou remove uma sessão interrompida sem serviço ativo. */
    fun discardRun(runId: Long) {
        viewModelScope.launch {
            if (RunTrackingService.isTracking(runId)) {
                serviceController.discard(runId)
            } else {
                repository.discardUnfinishedRun(runId)
            }
            closeActiveRun()
        }
    }

    /** Abre o detalhe de uma corrida salva. */
    fun selectRun(runId: Long) {
        selectedRunId.value = runId
    }

    /** Exclui pelo repositório e fecha o detalhe somente se ele pertence à corrida removida. */
    fun deleteCompletedRun(runId: Long) {
        viewModelScope.launch {
            repository.deleteCompletedRun(runId)
            if (selectedRunId.value == runId) selectedRunId.value = null
        }
    }

    /** Fecha o detalhe e volta para o histórico. */
    fun clearSelectedRun() {
        selectedRunId.value = null
    }

    /** Marca como interrompida uma sessão sem serviço ativo após reiniciar o app. */
    private fun recoverInterruptedRun() {
        viewModelScope.launch {
            repository.activeRun.collect { run ->
                if (run == null || !run.status.needsRecovery()) return@collect
                delay(RECOVERY_CHECK_DELAY_MS)
                val latestRun = repository.getRun(run.id) ?: return@collect
                val isUnclaimed = latestRun.status.needsRecovery() && !RunTrackingService.isTracking(run.id)
                if (isUnclaimed) repository.markInterrupted(run.id)
            }
        }
    }

    /** Inicia o serviço e preserva a sessão se o Android recusar a inicialização. */
    private suspend fun startService(runId: Long, command: (Long) -> Unit) {
        try {
            command(runId)
        } catch (_: Exception) {
            repository.markInterrupted(runId)
        }
    }

    companion object {
        private const val RECOVERY_CHECK_DELAY_MS = 3_000L

        /** Cria o ViewModel com o repositório e o controlador do serviço do app. */
        fun factory(
            repository: RunRepository,
            serviceController: RunTrackingServiceController
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RunViewModel(repository, serviceController) }
        }

        /** Informa se a sessão pode estar gravando após uma parada do processo. */
        private fun RunStatus.needsRecovery(): Boolean =
            this == RunStatus.STARTING || this == RunStatus.RUNNING
    }
}
