@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.data.repository

import android.net.Uri
import com.example.gymtimer2.data.dao.RunDao
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunMediaEntity
import com.example.gymtimer2.data.entity.toLocalSegmentEffort
import com.example.gymtimer2.data.entity.toLocalSegmentEffortEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.entity.RunStepRateEventEntity
import com.example.gymtimer2.data.entity.RunLapEntity
import com.example.gymtimer2.data.entity.toModel
import com.example.gymtimer2.data.entity.toModelOrNull
import com.example.gymtimer2.data.entity.toEntity
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunHeatmapCell
import com.example.gymtimer2.domain.model.RunStepRateSample
import com.example.gymtimer2.domain.model.RunElevationReading
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunMedia
import com.example.gymtimer2.domain.model.MAX_RUN_MEDIA_COUNT
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.RunWeather
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.isValidRunBodyMassKg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Converte modelos de corrida e coordena operações no DAO. */
@Suppress("detekt:TooManyFunctions")
class RunRepository(
    private val runDao: RunDao,
    private val mediaStorage: RunMediaStorage? = null
) {
    val bestEffortRepository = RunBestEffortRepository(runDao)

    /** Sessão atual, pausada ou interrompida. */
    val activeRun: Flow<RunModel?> = runDao.observeActiveRun().map { it?.toModel() }

    /** Histórico local em ordem decrescente de data. */
    val completedRuns: Flow<List<RunModel>> = runDao.observeCompletedRuns().map { runs ->
        runs.map { it.toModel() }
    }

    val completedLocalSegmentEfforts: Flow<List<RunLocalSegmentEffort>> =
        runDao.observeCompleted().map { efforts -> efforts.map { it.toLocalSegmentEffort() } }

    fun observeLocalSegmentEfforts(runId: Long): Flow<List<RunLocalSegmentEffort>> =
        runDao.observeForRun(runId).map { efforts -> efforts.map { it.toLocalSegmentEffort() } }

    suspend fun getLocalSegmentEfforts(runId: Long): List<RunLocalSegmentEffort> =
        runDao.getForRun(runId).map { it.toLocalSegmentEffort() }

    suspend fun saveLocalSegmentEffort(runId: Long, effort: RunLocalSegmentEffort) =
        runDao.saveWhileRunning(effort.toLocalSegmentEffortEntity(runId))

    /** Cria uma sessão antes de iniciar o serviço. */
    suspend fun startRun(
        startedAtEpochMs: Long,
        plannedRouteRunId: Long? = null,
        plannedRouteId: Long? = null,
        paceTarget: RunPaceTarget? = null,
        workoutPlan: RunWorkoutPlan? = null,
        activityType: RunActivityType = RunActivityType.RUN,
        bodyMassKgAtStart: Double? = null,
        startEndHideDistance: RunStartEndHideDistance = RunStartEndHideDistance.NONE
    ): Long {
        val normalizedWorkout = workoutPlan?.normalizedOrNull()
        require(workoutPlan == null || normalizedWorkout != null) { "Invalid run workout plan" }
        val steps = normalizedWorkout?.expandedSteps()?.mapIndexed { index, step ->
            step.copy(
                startedAtDurationMs = if (index == 0) 0L else null,
                startedAtDistanceMeters = if (index == 0) 0.0 else null
            ).toEntity(runId = 0L)
        }.orEmpty()
        return runDao.insertRunWithWorkout(
            run = RunEntity(
                startedAtEpochMs = startedAtEpochMs,
                plannedRouteRunId = plannedRouteRunId,
                plannedRouteId = plannedRouteId,
                paceTargetFasterSecondsPerKm = paceTarget?.fasterSecondsPerKm,
                paceTargetSlowerSecondsPerKm = paceTarget?.slowerSecondsPerKm,
                workoutTitle = normalizedWorkout?.name,
                activityType = activityType.name,
                bodyMassKgAtStart = bodyMassKgAtStart?.takeIf(::isValidRunBodyMassKg),
                startEndHideDistanceMeters = startEndHideDistance.meters
            ),
            steps = steps
        )
    }

    /** Marca que o serviço aceitou a sessão e começou a gravar. */
    suspend fun beginRun(
        runId: Long,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    ) = updateRunState(
        runId = runId,
        status = RunStatus.RUNNING,
        durationMs = null,
        elapsedDurationMs = elapsedDurationMs,
        elapsedRealtimeAnchorMs = elapsedRealtimeAnchorMs,
        elapsedEpochAnchorMs = elapsedEpochAnchorMs
    )

    /** Mantém o maior tempo já salvo para não voltar após uma emissão atrasada. */
    suspend fun updateDuration(
        runId: Long,
        durationMs: Long,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    ) = runDao.updateDuration(runId, durationMs, elapsedDurationMs, elapsedRealtimeAnchorMs, elapsedEpochAnchorMs)

    /** Persiste o silenciamento dos avisos de rota nesta corrida. */
    suspend fun muteRouteAlerts(runId: Long) = runDao.muteRouteAlerts(runId)

    /** Salva o progresso do segmento associado ao percurso escolhido na preparação. */
    suspend fun updateLocalSegmentProgress(
        runId: Long,
        progressMeters: Double,
        startedAtDurationMs: Long?,
        completedAtDurationMs: Long?,
        localSegmentRouteId: Long? = null
    ) = runDao.updateLocalSegmentProgress(
        runId,
        progressMeters,
        startedAtDurationMs,
        completedAtDurationMs,
        localSegmentRouteId
    )

    suspend fun getRun(runId: Long): RunModel? = runDao.getRun(runId)?.toModel()

    suspend fun updateWeather(runId: Long, weather: RunWeather) = runDao.updateWeather(
        runId = runId,
        temperatureCelsius = weather.temperatureCelsius,
        apparentTemperatureCelsius = weather.apparentTemperatureCelsius,
        relativeHumidityPercent = weather.relativeHumidityPercent,
        windSpeedKmh = weather.windSpeedKmh,
        observedAtEpochMs = weather.observedAtEpochMs
    )

    /** Avança etapas quando o marco foi atingido e retorna as concluídas nesta atualização. */
    suspend fun advanceWorkoutProgress(
        runId: Long,
        durationMs: Long,
        distanceMeters: Double,
        manualAdvance: Boolean = false,
        elevationReading: RunElevationReading? = null
    ): List<RunWorkoutStep> = runDao.advanceWorkoutProgress(
        runId, durationMs, distanceMeters, manualAdvance, elevationReading
    )
        .map { it.toModel() }

    suspend fun getActiveWorkoutStep(runId: Long): RunWorkoutStep? =
        runDao.getActiveWorkoutStep(runId)?.toModel()

    /** Observa o plano e o progresso salvos junto à corrida. */
    fun observeWorkoutSteps(runId: Long): Flow<List<RunWorkoutStep>> = runDao.observeWorkoutSteps(runId)
        .map { steps -> steps.map { it.toModel() } }

    suspend fun recordLap(
        runId: Long,
        durationMs: Long,
        recordedAtEpochMs: Long = System.currentTimeMillis(),
        elapsedDurationMs: Long? = null
    ) {
        val run = runDao.getRun(runId) ?: return
        if (run.status != RunStatus.RUNNING.name) return
        runDao.insertLap(
            RunLapEntity(
                runId = runId,
                durationMs = durationMs,
                elapsedDurationMs = elapsedDurationMs,
                distanceMeters = run.distanceMeters,
                recordedAtEpochMs = recordedAtEpochMs
            )
        )
    }

    fun observeLaps(runId: Long): Flow<List<RunLap>> = runDao.observeLaps(runId).map { laps ->
        laps.map { RunLap(it.id, it.durationMs, it.distanceMeters, it.recordedAtEpochMs, it.elapsedDurationMs) }
    }

    /** Salva o tempo ativo e muda a sessão para pausada. */
    suspend fun pauseRun(
        runId: Long,
        durationMs: Long,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    ) = updateRunState(
        runId = runId,
        status = RunStatus.PAUSED,
        durationMs = durationMs,
        elapsedDurationMs = elapsedDurationMs,
        elapsedRealtimeAnchorMs = elapsedRealtimeAnchorMs,
        elapsedEpochAnchorMs = elapsedEpochAnchorMs
    )

    suspend fun autoPauseRun(
        runId: Long,
        durationMs: Long,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    ) = updateRunState(
        runId = runId,
        status = RunStatus.AUTO_PAUSED,
        durationMs = durationMs,
        elapsedDurationMs = elapsedDurationMs,
        elapsedRealtimeAnchorMs = elapsedRealtimeAnchorMs,
        elapsedEpochAnchorMs = elapsedEpochAnchorMs
    )

    /** Retoma a sessão em um novo trecho para preservar as pausas na rota. */
    suspend fun resumeRun(
        runId: Long,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    ) = runDao.resumeRun(runId, elapsedDurationMs, elapsedRealtimeAnchorMs, elapsedEpochAnchorMs)

    /** Preserva a sessão e permite que a pessoa escolha como continuar. */
    suspend fun markInterrupted(runId: Long) = updateRunState(
        runId = runId,
        status = RunStatus.INTERRUPTED,
        durationMs = null
    )

    /** Finaliza uma corrida ou salva uma corrida parcial. */
    suspend fun finishRun(
        runId: Long,
        durationMs: Long,
        finishedAtEpochMs: Long,
        saveDetails: RunSaveDetails = RunSaveDetails(),
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    ) {
        val details = saveDetails.normalized()
        runDao.finishRun(
            runId,
            durationMs,
            finishedAtEpochMs,
            details.title,
            details.description,
            details.gearName,
            details.gearId,
            details.privateNotes,
            details.isCommute,
            details.perceivedExertion,
            details.activityType?.name,
            RunActivityTag.serialize(details.activityTags),
            elapsedDurationMs,
            elapsedRealtimeAnchorMs,
            elapsedEpochAnchorMs
        )
    }

    /** Atualiza os metadados locais de uma corrida salva. */
    suspend fun updateSaveDetails(runId: Long, saveDetails: RunSaveDetails): Boolean {
        val details = saveDetails.normalized()
        return runDao.updateSaveDetails(
            runId,
            details.title,
            details.description,
            details.gearName,
            details.gearId,
            details.privateNotes,
            details.isCommute,
            details.perceivedExertion,
            details.activityType?.name,
            RunActivityTag.serialize(details.activityTags)
        ) > 0
    }

    /** Descarta a sessão após o serviço encerrar as gravações pendentes. */
    suspend fun discardUnfinishedRun(runId: Long) = runDao.discardUnfinishedRun(runId)

    /** Salva cada posição e soma apenas a distância aceita ao total. */
    suspend fun appendLocation(
        runId: Long,
        point: GpsPoint,
        distanceMeters: Double,
        activeDurationMs: Long? = null
    ) {
        val run = runDao.getRun(runId) ?: return
        runDao.appendPoint(
            point = RunPointEntity(
                runId = runId,
                latitude = point.latitude,
                longitude = point.longitude,
                recordedAtEpochMs = point.recordedAtEpochMs,
                elapsedRealtimeMs = point.elapsedRealtimeMs,
                accuracyMeters = point.accuracyMeters,
                segmentIndex = run.currentSegment,
                altitudeMeters = point.altitudeMeters,
                verticalAccuracyMeters = point.verticalAccuracyMeters,
                speedMetersPerSecond = point.speedMetersPerSecond,
                speedAccuracyMetersPerSecond = point.speedAccuracyMetersPerSecond,
                cadenceStepsPerMinute = point.cadenceStepsPerMinute,
                isFiltered = point.isFiltered,
                activeDurationMs = activeDurationMs
            ),
            distanceMeters = distanceMeters
        )
    }

    /** Salva a cadência agregada sem depender do ritmo de atualização do GPS. */
    suspend fun appendStepRateSample(runId: Long, sample: RunStepRateSample) {
        runDao.insertStepRateEvent(
            RunStepRateEventEntity(
                runId = runId,
                stepsPerMinute = sample.stepsPerMinute,
                stepCount = sample.stepCount,
                timestampEpochMs = sample.timestampEpochMs,
                intervalDurationMs = sample.intervalDurationMs
            )
        )
    }

    /** Remove uma corrida passada e sua rota, preservando sessões ainda abertas. */
    suspend fun deleteCompletedRun(runId: Long): Boolean = withContext(Dispatchers.IO) {
        val mediaFileNames = runDao.getRunMedia(runId).map(RunMediaEntity::fileName)
        val deleted = runDao.deleteCompletedRun(runId) > 0
        if (deleted) mediaFileNames.forEach { mediaStorage?.delete(it) }
        deleted
    }

    fun observeRunMedia(runId: Long): Flow<List<RunMedia>> =
        runDao.observeRunMedia(runId).map { media -> media.map { it.toModel() } }

    suspend fun addRunMedia(runId: Long, selectedUris: List<Uri>) = withContext(Dispatchers.IO) {
        val storage = mediaStorage ?: return@withContext
        val run = runDao.getRun(runId) ?: return@withContext
        if (run.status != RunStatus.COMPLETED.name) return@withContext

        val remainingSlots = (MAX_RUN_MEDIA_COUNT - runDao.countRunMedia(runId)).coerceAtLeast(0)
        if (remainingSlots == 0) return@withContext

        selectedUris.distinct().take(remainingSlots).forEach { uri ->
            val storedMedia = storage.copyMedia(uri) ?: return@forEach
            var inserted = false
            try {
                inserted = runDao.insertRunMedia(
                    listOf(
                        RunMediaEntity(
                            runId = runId,
                            fileName = storedMedia.fileName,
                            isVideo = storedMedia.isVideo
                        )
                    )
                ).firstOrNull()?.let { it >= 0 } == true
            } finally {
                if (!inserted) storage.delete(storedMedia.fileName)
            }
        }
    }

    suspend fun deleteRunMedia(mediaId: Long) = withContext(Dispatchers.IO) {
        val fileName = runDao.getMediaFileName(mediaId) ?: return@withContext
        if (runDao.deleteRunMedia(mediaId) > 0) mediaStorage?.delete(fileName)
    }

    suspend fun setFeaturedRunMedia(runId: Long, mediaId: Long) = withContext(Dispatchers.IO) {
        val run = runDao.getRun(runId) ?: return@withContext
        if (run.status == RunStatus.COMPLETED.name) runDao.setFeaturedMedia(runId, mediaId)
    }

    /** Observa o traçado GPS de uma sessão. */
    fun observePoints(runId: Long): Flow<List<RunPointModel>> =
        runDao.observePoints(runId).map { points -> points.map { it.toModel() } }

    /** Observa células agregadas da semana para o mapa de atividade local. */
    fun observeWeeklyHeatmapCells(startEpochMs: Long, endEpochMs: Long): Flow<List<RunHeatmapCell>> =
        runDao.observeCompletedHeatmapCellsBetween(startEpochMs, endEpochMs).map { cells ->
            cells.map { cell -> RunHeatmapCell(cell.latitude, cell.longitude, cell.visitCount) }
        }

    /** Lê células agregadas de pontos aceitos em corridas concluídas. */
    suspend fun getCompletedRunHeatmapCells(): List<RunHeatmapCell> =
        runDao.getCompletedRunHeatmapCells().map { cell ->
            RunHeatmapCell(cell.latitude, cell.longitude, cell.visitCount)
        }

    /** Observa as amostras de cadência de uma sessão salva. */
    fun observeStepRateSamples(runId: Long): Flow<List<RunStepRateSample>> =
        runDao.observeStepRateEvents(runId).map { events ->
            events.map {
                RunStepRateSample(it.stepsPerMinute, it.stepCount, it.timestampEpochMs, it.intervalDurationMs)
            }
        }

    /** Observa métricas de uma sessão do histórico. */
    fun observeRun(runId: Long): Flow<RunModel?> = runDao.observeRun(runId).map { it?.toModel() }

    /** Aplica o corte local sem interromper sessões que usam esta rota. */
    suspend fun cropCompletedRun(runId: Long, startPointId: Long, endPointId: Long): RunCropOutcome =
        withContext(Dispatchers.IO) { runDao.cropCompletedRun(runId, startPointId, endPointId) }

    /** Atualiza um estado sem reduzir o tempo acumulado. */
    private suspend fun updateRunState(
        runId: Long,
        status: RunStatus,
        durationMs: Long?,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    ) {
        runDao.updateState(
            runId,
            status.name,
            durationMs,
            elapsedDurationMs,
            elapsedRealtimeAnchorMs,
            elapsedEpochAnchorMs
        )
    }

    /** Converte o estado de persistência em modelo de domínio. */
    private fun RunEntity.toModel() = RunModel(
        id = id,
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = finishedAtEpochMs,
        durationMs = durationMs,
        distanceMeters = distanceMeters,
        status = RunStatus.entries.firstOrNull { it.name == status } ?: RunStatus.INTERRUPTED,
        currentSegment = currentSegment,
        title = title,
        plannedRouteRunId = plannedRouteRunId,
        plannedRouteId = plannedRouteId,
        localSegmentRouteId = localSegmentRouteId,
        paceTargetFasterSecondsPerKm = paceTargetFasterSecondsPerKm,
        paceTargetSlowerSecondsPerKm = paceTargetSlowerSecondsPerKm,
        workoutTitle = workoutTitle,
        localSegmentProgressMeters = localSegmentProgressMeters,
        localSegmentStartedAtDurationMs = localSegmentStartedAtDurationMs,
        localSegmentCompletedAtDurationMs = localSegmentCompletedAtDurationMs,
        activityType = RunActivityType.entries.firstOrNull { it.name == activityType } ?: RunActivityType.RUN,
        routeAlertsMuted = routeAlertsMuted,
        description = description,
        gearName = gearName,
        gearId = gearId,
        privateNotes = privateNotes,
        isCommute = isCommute,
        perceivedExertion = perceivedExertion,
        activityTags = RunActivityTag.parse(activityTags),
        elapsedDurationMs = elapsedDurationMs,
        elapsedRealtimeAnchorMs = elapsedRealtimeAnchorMs,
        elapsedEpochAnchorMs = elapsedEpochAnchorMs,
        bodyMassKgAtStart = bodyMassKgAtStart,
        weather = weather?.toModelOrNull(),
        startEndHideDistance = RunStartEndHideDistance.fromMeters(startEndHideDistanceMeters)
    )

}
