@file:Suppress("detekt:DocumentationOverPrivateFunction", "detekt:TooManyFunctions")

package com.example.gymtimer2.ui.screens.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import com.example.gymtimer2.data.repository.PlannedRouteRepository
import com.example.gymtimer2.data.repository.PlannedRouteImportException
import com.example.gymtimer2.data.repository.RunLocalSegmentSaver
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.data.repository.RunGearRepository
import com.example.gymtimer2.data.repository.RunExportRepository
import com.example.gymtimer2.data.repository.RunPrivacyZoneRepository
import com.example.gymtimer2.data.repository.RunWorkoutTemplateRepository
import com.example.gymtimer2.data.repository.ValhallaRouteRepository
import com.example.gymtimer2.data.repository.OpenStreetMapPoiRepository
import com.example.gymtimer2.data.preferences.AppSettingsRepository
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutTemplate
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunBestEffortRecords
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunBestEfforts
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.RunRouteProgressTracker
import com.example.gymtimer2.domain.model.RunLocalSegmentSelection
import com.example.gymtimer2.domain.model.RunGear
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.RunElevationProfilePoint
import com.example.gymtimer2.domain.model.elapsedDurationAt
import com.example.gymtimer2.domain.model.RunTurnInstruction
import com.example.gymtimer2.domain.model.runGpsStatus
import com.example.gymtimer2.domain.model.currentPaceSecondsPerKm
import com.example.gymtimer2.domain.model.currentRunStepRate
import com.example.gymtimer2.domain.model.isOffRunRoutePosition
import com.example.gymtimer2.domain.model.inferRunTurnInstructions
import com.example.gymtimer2.domain.model.calculateRunElevationProfile
import com.example.gymtimer2.domain.model.upcomingRunTurnGuidances
import com.example.gymtimer2.domain.model.nextRunStreetGuidance
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingService
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingState
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingServiceController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

/** Coordena a gravação, a recuperação e a consulta do histórico de corridas. */
@OptIn(ExperimentalCoroutinesApi::class)
class RunViewModel(
    private val repository: RunRepository,
    private val plannedRouteRepository: PlannedRouteRepository,
    private val valhallaRouteRepository: ValhallaRouteRepository,
    private val openStreetMapPoiRepository: OpenStreetMapPoiRepository,
    private val serviceController: RunTrackingServiceController,
    private val runGearRepository: RunGearRepository,
    private val runExportRepository: RunExportRepository,
    private val appSettingsRepository: AppSettingsRepository? = null,
    private val runPrivacyZoneRepository: RunPrivacyZoneRepository? = null,
    private val runWorkoutTemplateRepository: RunWorkoutTemplateRepository? = null
) : ViewModel() {
    private val activeRunOpen = MutableStateFlow(false)
    private val runPreparationOpen = MutableStateFlow(false)
    private val localSegmentSaver = RunLocalSegmentSaver(repository, plannedRouteRepository)
    private val selectedPlannedRouteRunId = MutableStateFlow<Long?>(null)
    private val selectedPlannedRouteId = MutableStateFlow<Long?>(null)
    private val selectedPaceTarget = MutableStateFlow<RunPaceTarget?>(null)
    private val selectedWorkoutPlan = MutableStateFlow<RunWorkoutPlan?>(null)
    private val selectedWorkoutTemplateId = MutableStateFlow<Long?>(null)
    private val workoutTemplates = (runWorkoutTemplateRepository?.templates ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val routeImportFailure = MutableStateFlow<PlannedRouteImportException?>(null)
    private val streetReturnState = MutableStateFlow(RunStreetReturnState())
    private val mapPoiController = RunMapPoiController(viewModelScope, openStreetMapPoiRepository)
    val privacyZones: StateFlow<List<RunPrivacyZone>> = (
        runPrivacyZoneRepository?.zones ?: flowOf<List<RunPrivacyZone>>(emptyList())
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val onMapPoiViewportChanged = mapPoiController.onViewportChanged
    private var streetReturnRequest: Job? = null
    private val plannedRouteGuidance = MutableStateFlow(RunPlannedRouteGuidanceState())
    private var plannedRouteGuidanceRequest: Job? = null
    private val selectedRunId = MutableStateFlow<Long?>(null)
    private var runStartInProgress = false

    private val uiStateObserver = RunUiStateObserver(
        RunUiStateObserverSources(
            scope = viewModelScope,
            repository = repository,
            plannedRouteRepository = plannedRouteRepository,
            runGearRepository = runGearRepository,
            appSettingsRepository = appSettingsRepository,
            privacyZones = privacyZones,
            mapPoiController = mapPoiController,
            workoutTemplates = workoutTemplates,
            selections = RunUiStateSelections(
                activeRunOpen = activeRunOpen,
                runPreparationOpen = runPreparationOpen,
                selectedPlannedRouteRunId = selectedPlannedRouteRunId,
                selectedPlannedRouteId = selectedPlannedRouteId,
                selectedPaceTarget = selectedPaceTarget,
                selectedWorkoutPlan = selectedWorkoutPlan,
                selectedWorkoutTemplateId = selectedWorkoutTemplateId,
                routeImportFailure = routeImportFailure,
                streetReturnState = streetReturnState,
                plannedRouteGuidance = plannedRouteGuidance,
                selectedRunId = selectedRunId
            )
        )
    )
    val uiState = uiStateObserver.uiState

    private var mapCameraSnapshotClosedRunId: Long? = null

    init {
        recoverInterruptedRun()
    }

    /** Abre a prévia do mapa sem criar uma corrida ou iniciar o GPS de gravação. */
    fun openRunPreparation() {
        clearStreetReturnRoute()
        clearPlannedRouteGuidance()
        selectedPlannedRouteRunId.value = null
        selectedPlannedRouteId.value = null
        selectedPaceTarget.value = null
        selectedWorkoutPlan.value = null
        selectedWorkoutTemplateId.value = null
        routeImportFailure.value = null
        runPreparationOpen.value = true
    }

    /** Fecha a prévia sem criar uma sessão. */
    fun closeRunPreparation() {
        clearStreetReturnRoute()
        clearPlannedRouteGuidance()
        selectedPlannedRouteRunId.value = null
        selectedPlannedRouteId.value = null
        selectedPaceTarget.value = null
        selectedWorkoutPlan.value = null
        selectedWorkoutTemplateId.value = null
        routeImportFailure.value = null
        runPreparationOpen.value = false
    }

    /** Seleciona ou remove o percurso que será acompanhado durante a gravação. */
    fun selectPlannedRoute(runId: Long?, routeId: Long?) {
        clearPlannedRouteGuidance()
        selectedPlannedRouteRunId.value = runId
        selectedPlannedRouteId.value = routeId
    }

    fun setPlannedRouteFavorite(routeId: Long, isFavorite: Boolean) {
        viewModelScope.launch { plannedRouteRepository.setFavorite(routeId, isFavorite) }
    }

    fun calculatePlannedRouteGuidance(points: List<RoutePoint>) {
        if (plannedRouteGuidance.value.isLoading || plannedRouteGuidance.value.route != null) return
        plannedRouteGuidanceRequest?.cancel()
        plannedRouteGuidanceRequest = viewModelScope.launch {
            plannedRouteGuidance.value = RunPlannedRouteGuidanceState(isLoading = true)
            try {
                val route = valhallaRouteRepository.instructionsForRoute(points)
                plannedRouteGuidance.value = if (route.instructions.isEmpty()) {
                    RunPlannedRouteGuidanceState(hasError = true)
                } else {
                    RunPlannedRouteGuidanceState(route = route)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                plannedRouteGuidance.value = RunPlannedRouteGuidanceState(hasError = true)
            }
        }
    }

    private fun clearPlannedRouteGuidance() {
        plannedRouteGuidanceRequest?.cancel()
        plannedRouteGuidanceRequest = null
        plannedRouteGuidance.value = RunPlannedRouteGuidanceState()
    }

    fun setPaceTarget(target: RunPaceTarget?) {
        selectedPaceTarget.value = target
    }

    fun setWorkoutPlan(plan: RunWorkoutPlan?) {
        val normalizedPlan = plan?.normalizedOrNull()
        selectedWorkoutPlan.value = normalizedPlan
        selectedWorkoutTemplateId.value = normalizedPlan?.let { selectedPlan ->
            workoutTemplates.value.firstOrNull { it.plan == selectedPlan }?.id
        }
    }

    fun saveWorkoutTemplate(id: Long?, plan: RunWorkoutPlan) {
        val repository = runWorkoutTemplateRepository ?: return
        val normalizedPlan = plan.normalizedOrNull() ?: return
        viewModelScope.launch {
            val savedId = repository.save(id, normalizedPlan) ?: return@launch
            selectedWorkoutPlan.value = normalizedPlan
            selectedWorkoutTemplateId.value = savedId
        }
    }

    fun deleteWorkoutTemplate(id: Long) {
        val repository = runWorkoutTemplateRepository ?: return
        viewModelScope.launch {
            repository.delete(id)
            if (selectedWorkoutTemplateId.value == id) {
                selectedWorkoutPlan.value = null
                selectedWorkoutTemplateId.value = null
            }
        }
    }

    /** Importa GPX no banco local e seleciona o novo percurso. */
    fun importGpxRoute(uri: Uri) {
        routeImportFailure.value = null
        viewModelScope.launch {
            try {
                val routeId = plannedRouteRepository.importGpx(uri)
                routeImportFailure.value = null
                selectPlannedRoute(null, routeId)
            } catch (exception: PlannedRouteImportException) {
                routeImportFailure.value = exception
            }
        }
    }

    /** Exporta os pontos locais para o destino GPX escolhido pela pessoa. */
    suspend fun exportRunGpx(
        uri: Uri,
        run: RunModel,
        activityTypeLabel: String,
        points: List<RunPointModel>
    ) = runExportRepository.exportGpx(
        uri,
        run,
        activityTypeLabel,
        points,
        runPrivacyZoneRepository?.getAll().orEmpty()
    )

    suspend fun exportRunFit(uri: Uri, run: RunModel, points: List<RunPointModel>, laps: List<RunLap>) =
        runExportRepository.exportFit(uri, run, points, laps, runPrivacyZoneRepository?.getAll().orEmpty())

    fun savePrivacyZone(zone: RunPrivacyZone) {
        runPrivacyZoneRepository?.let { repository ->
            viewModelScope.launch { repository.save(zone) }
        }
    }

    fun deletePrivacyZone(zoneId: Long) {
        runPrivacyZoneRepository?.let { repository ->
            viewModelScope.launch { repository.delete(zoneId) }
        }
    }

    fun dismissRouteImportError() {
        routeImportFailure.value = null
    }

    /** Cria uma sessão local e pede ao serviço para iniciar o GPS. */
    fun startRun(activityType: RunActivityType) {
        if (runStartInProgress) return
        runStartInProgress = true
        mapCameraSnapshotClosedRunId = null
        appSettingsRepository?.clearRunMapCameraSnapshot()
        viewModelScope.launch {
            try {
                val runId = repository.startRun(
                    startedAtEpochMs = System.currentTimeMillis(),
                    plannedRouteRunId = selectedPlannedRouteRunId.value,
                    plannedRouteId = selectedPlannedRouteId.value,
                    paceTarget = selectedPaceTarget.value,
                    workoutPlan = selectedWorkoutPlan.value,
                    activityType = activityType,
                    bodyMassKgAtStart = appSettingsRepository?.settings?.value?.runBodyMassKg,
                    startEndHideDistance = appSettingsRepository?.settings?.value?.runStartEndHideDistance
                        ?: RunStartEndHideDistance.NONE
                )
                startService(runId, serviceController::start)
                runPreparationOpen.value = false
                selectedPaceTarget.value = null
                selectedWorkoutPlan.value = null
                selectedWorkoutTemplateId.value = null
                setActiveRunOpen(true)
            } finally {
                runStartInProgress = false
            }
        }
    }

    /** Abre ou fecha a sessão atual sem interromper o serviço GPS. */
    fun setActiveRunOpen(open: Boolean) {
        if (open) selectedRunId.value = null else clearStreetReturnRoute()
        activeRunOpen.value = open
    }

    fun saveActiveRunMapCameraSnapshot(snapshot: RunMapCameraSnapshot) {
        if (snapshot.runId != uiState.value.activeRun?.id || snapshot.runId == mapCameraSnapshotClosedRunId) return
        appSettingsRepository?.setRunMapCameraSnapshot(snapshot)
    }

    fun calculateStreetReturnRoute() {
        val state = uiState.value
        val acceptedPoints = state.activePoints.filterNot { it.isFiltered }
        val start = acceptedPoints.firstOrNull()
        val current = acceptedPoints.lastOrNull()
        if (state.activeRun == null || state.gpsStatus != RunGpsStatus.GOOD) {
            streetReturnState.value = RunStreetReturnState(hasError = true)
            return
        }
        if (start == null || current == null) {
            streetReturnState.value = RunStreetReturnState(hasError = true)
            return
        }

        streetReturnRequest?.cancel()
        streetReturnRequest = viewModelScope.launch {
            streetReturnState.value = RunStreetReturnState(isLoading = true)
            try {
                val route = valhallaRouteRepository.routeToStart(current, start)
                streetReturnState.value = RunStreetReturnState(route = route)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                streetReturnState.value = RunStreetReturnState(hasError = true)
            }
        }
    }

    fun clearStreetReturnRoute() {
        streetReturnRequest?.cancel()
        streetReturnRequest = null
        streetReturnState.value = RunStreetReturnState()
    }

    /** Retoma uma sessão pausada ou interrompida em um novo trecho de rota. */
    fun resumeRun(runId: Long) {
        viewModelScope.launch { startService(runId, serviceController::resume) }
    }

    /** Pausa o GPS e mantém a sessão disponível para retomada. */
    fun pauseRun(runId: Long) {
        serviceController.pause(runId)
    }

    fun markLap(runId: Long) = serviceController.lap(runId)

    fun advanceWorkout(runId: Long) = serviceController.advanceWorkout(runId)

    fun muteRouteAlerts(runId: Long, untilRouteReturn: Boolean) =
        serviceController.muteRouteAlerts(runId, untilRouteReturn)

    /** Finaliza a sessão pelo serviço ou salva a última versão interrompida. */
    fun finishRun(runId: Long, saveDetails: RunSaveDetails) {
        mapCameraSnapshotClosedRunId = runId
        viewModelScope.launch {
            if (RunTrackingService.isTracking(runId)) {
                serviceController.finish(runId, saveDetails)
            } else {
                val run = repository.getRun(runId) ?: return@launch
                repository.advanceWorkoutProgress(runId, run.durationMs, run.distanceMeters)
                val nowEpochMs = System.currentTimeMillis()
                val nowElapsedRealtimeMs = SystemClock.elapsedRealtime()
                repository.finishRun(
                    runId,
                    run.durationMs,
                    nowEpochMs,
                    saveDetails,
                    run.elapsedDurationAt(nowElapsedRealtimeMs, nowEpochMs),
                    nowElapsedRealtimeMs,
                    nowEpochMs
                )
            }
            appSettingsRepository?.clearRunMapCameraSnapshot()
            setActiveRunOpen(false)
        }
    }

    /** Salva os metadados editados de uma corrida concluída. */
    fun updateRunSaveDetails(runId: Long, saveDetails: RunSaveDetails) {
        viewModelScope.launch { repository.updateSaveDetails(runId, saveDetails) }
    }

    fun addRunMedia(runId: Long, uris: List<Uri>) {
        viewModelScope.launch { repository.addRunMedia(runId, uris) }
    }

    fun deleteRunMedia(mediaId: Long) {
        viewModelScope.launch { repository.deleteRunMedia(mediaId) }
    }

    fun setFeaturedRunMedia(runId: Long, mediaId: Long) {
        viewModelScope.launch { repository.setFeaturedRunMedia(runId, mediaId) }
    }

    fun createRunGear(name: String, onCreated: (RunGear) -> Unit) {
        viewModelScope.launch {
            onCreated(runGearRepository.getOrCreate(name))
        }
    }

    fun setDefaultRunGear(gearId: Long) {
        viewModelScope.launch { runGearRepository.setDefault(gearId) }
    }

    fun setRunGearRetired(gearId: Long, retired: Boolean) {
        viewModelScope.launch { runGearRepository.setRetired(gearId, retired) }
    }

    fun renameRunGear(gearId: Long, name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(runGearRepository.rename(gearId, name)) }
    }

    fun setRunGearPhoto(gearId: Long, bitmap: Bitmap) {
        viewModelScope.launch { runGearRepository.setPhoto(gearId, bitmap) }
    }

    fun removeRunGearPhoto(gearId: Long) {
        viewModelScope.launch { runGearRepository.setPhoto(gearId, null) }
    }

    fun createRunGearPhotoCapture(onCreated: (Uri?) -> Unit) {
        viewModelScope.launch { onCreated(runGearRepository.createPhotoCapture()) }
    }

    fun deleteRunGearPhotoCapture(uri: Uri) {
        viewModelScope.launch { runGearRepository.deletePhotoCapture(uri) }
    }

    /** Guarda uma correção manual ou remove uma marca dos recordes locais. */
    fun saveBestEffortOverride(override: RunBestEffortOverride) {
        viewModelScope.launch { repository.bestEffortRepository.saveOverride(override) }
    }

    /** Restaura o tempo calculado por GPS para uma marca da corrida. */
    fun clearBestEffortOverride(runId: Long, distance: RunBenchmarkDistance) {
        viewModelScope.launch { repository.bestEffortRepository.clearOverride(runId, distance) }
    }

    /** Encaminha o descarte ao serviço ou remove uma sessão interrompida sem serviço ativo. */
    fun discardRun(runId: Long) {
        mapCameraSnapshotClosedRunId = runId
        viewModelScope.launch {
            if (RunTrackingService.isTracking(runId)) {
                serviceController.discard(runId)
            } else {
                repository.discardUnfinishedRun(runId)
            }
            appSettingsRepository?.clearRunMapCameraSnapshot()
            setActiveRunOpen(false)
        }
    }

    /** Abre o detalhe de uma corrida salva. */
    fun selectRun(runId: Long?) {
        selectedRunId.value = runId
    }

    /** Encaminha o corte local e aguarda a transação Room concluir. */
    suspend fun cropCompletedRun(runId: Long, startPointId: Long, endPointId: Long): RunCropOutcome =
        repository.cropCompletedRun(runId, startPointId, endPointId)

    suspend fun saveRunAsLocalSegment(
        runId: Long,
        name: String,
        segmentIndex: Int,
        startDistanceMeters: Double,
        endDistanceMeters: Double
    ): Boolean = localSegmentSaver.save(runId, name, segmentIndex, startDistanceMeters, endDistanceMeters)

    /** Exclui pelo repositório e fecha o detalhe somente se ele pertence à corrida removida. */
    fun deleteCompletedRun(runId: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteCompletedRun(runId)
            if (deleted && selectedRunId.value == runId) selectedRunId.value = null
        }
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
            plannedRouteRepository: PlannedRouteRepository,
            valhallaRouteRepository: ValhallaRouteRepository,
            openStreetMapPoiRepository: OpenStreetMapPoiRepository,
            serviceController: RunTrackingServiceController,
            runGearRepository: RunGearRepository,
            runExportRepository: RunExportRepository,
            appSettingsRepository: AppSettingsRepository? = null,
            runPrivacyZoneRepository: RunPrivacyZoneRepository? = null,
            runWorkoutTemplateRepository: RunWorkoutTemplateRepository? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RunViewModel(
                    repository,
                    plannedRouteRepository,
                    valhallaRouteRepository,
                    openStreetMapPoiRepository,
                    serviceController,
                    runGearRepository,
                    runExportRepository,
                    appSettingsRepository,
                    runPrivacyZoneRepository,
                    runWorkoutTemplateRepository
                )
            }
        }

        /** Informa se a sessão pode estar gravando após uma parada do processo. */
        private fun RunStatus.needsRecovery(): Boolean =
            this == RunStatus.STARTING || this == RunStatus.RUNNING || this == RunStatus.AUTO_PAUSED
    }

}
