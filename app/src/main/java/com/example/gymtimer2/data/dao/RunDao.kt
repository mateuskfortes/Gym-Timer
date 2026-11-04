@file:Suppress("detekt:DocumentationOverPrivateFunction", "detekt:ComplexInterface", "detekt:TooManyFunctions")

package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunHeatmapCellEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.entity.RunLapEntity
import com.example.gymtimer2.data.entity.toLocalSegmentEffort
import com.example.gymtimer2.data.entity.toLocalSegmentEffortEntity
import com.example.gymtimer2.data.entity.toModel
import com.example.gymtimer2.data.entity.toEntity
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.calculateRunActivityCrop
import com.example.gymtimer2.data.entity.RunWorkoutStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
/** Consultas e gravações Room para sessões e pontos GPS. */
interface RunDao : RunWorkoutDao, RunStepRateDao, RunRouteAlertDao, RunBestEffortDao, RunMediaDao,
    RunLocalSegmentEffortDao {
    /** Cria uma sessão antes de iniciar o serviço de GPS. */
    @Insert
    suspend fun insertRun(run: RunEntity): Long

    /** Cria a corrida e seu snapshot de treino na mesma transação. */
    @Transaction
    suspend fun insertRunWithWorkout(run: RunEntity, steps: List<RunWorkoutStepEntity>): Long {
        val runId = insertRun(run)
        if (steps.isNotEmpty()) insertWorkoutSteps(steps.map { it.copy(runId = runId) })
        return runId
    }

    /** Atualiza somente o tempo para não sobrescrever pontos gravados em paralelo. */
    @Query(
        "UPDATE runs SET duration_ms = MAX(duration_ms, :durationMs), " +
            "elapsed_duration_ms = CASE WHEN :elapsedDurationMs IS NULL THEN elapsed_duration_ms " +
            "ELSE MAX(COALESCE(elapsed_duration_ms, 0), :elapsedDurationMs) END, " +
            "elapsed_realtime_anchor_ms = CASE WHEN :elapsedDurationMs >= COALESCE(elapsed_duration_ms, 0) " +
            "THEN COALESCE(:elapsedRealtimeAnchorMs, elapsed_realtime_anchor_ms) ELSE elapsed_realtime_anchor_ms END, " +
            "elapsed_epoch_anchor_ms = CASE WHEN :elapsedDurationMs >= COALESCE(elapsed_duration_ms, 0) " +
            "THEN COALESCE(:elapsedEpochAnchorMs, elapsed_epoch_anchor_ms) ELSE elapsed_epoch_anchor_ms END " +
            "WHERE id = :runId AND status != 'COMPLETED'"
    )
    suspend fun updateDuration(
        runId: Long,
        durationMs: Long,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    )

    /** Grava somente o snapshot do clima, preservando métricas atualizadas em paralelo. */
    @Query(
        "UPDATE runs SET weather_temperature_celsius = :temperatureCelsius, " +
            "weather_apparent_temperature_celsius = :apparentTemperatureCelsius, " +
            "weather_relative_humidity_percent = :relativeHumidityPercent, " +
            "weather_wind_speed_kmh = :windSpeedKmh, " +
            "weather_observed_at_epoch_ms = :observedAtEpochMs WHERE id = :runId"
    )
    suspend fun updateWeather(
        runId: Long,
        temperatureCelsius: Double,
        apparentTemperatureCelsius: Double?,
        relativeHumidityPercent: Double?,
        windSpeedKmh: Double?,
        observedAtEpochMs: Long
    )

    /** Altera o estado sem substituir as demais métricas. */
    @Query(
        "UPDATE runs SET status = :status, " +
            "duration_ms = MAX(duration_ms, COALESCE(:durationMs, duration_ms)), " +
            "elapsed_duration_ms = CASE WHEN :elapsedDurationMs IS NULL THEN elapsed_duration_ms " +
            "ELSE MAX(COALESCE(elapsed_duration_ms, 0), :elapsedDurationMs) END, " +
            "elapsed_realtime_anchor_ms = CASE WHEN :elapsedDurationMs >= COALESCE(elapsed_duration_ms, 0) " +
            "THEN COALESCE(:elapsedRealtimeAnchorMs, elapsed_realtime_anchor_ms) ELSE elapsed_realtime_anchor_ms END, " +
            "elapsed_epoch_anchor_ms = CASE WHEN :elapsedDurationMs >= COALESCE(elapsed_duration_ms, 0) " +
            "THEN COALESCE(:elapsedEpochAnchorMs, elapsed_epoch_anchor_ms) ELSE elapsed_epoch_anchor_ms END " +
            "WHERE id = :runId AND status != 'COMPLETED'"
    )
    suspend fun updateState(
        runId: Long,
        status: String,
        durationMs: Long?,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    )

    /** Abre um trecho novo sem alterar a distância e o tempo acumulados. */
    @Query(
        "UPDATE runs SET status = 'RUNNING', current_segment = current_segment + 1, " +
            "elapsed_duration_ms = COALESCE(:elapsedDurationMs, elapsed_duration_ms), " +
            "elapsed_realtime_anchor_ms = COALESCE(:elapsedRealtimeAnchorMs, elapsed_realtime_anchor_ms), " +
            "elapsed_epoch_anchor_ms = COALESCE(:elapsedEpochAnchorMs, elapsed_epoch_anchor_ms) " +
            "WHERE id = :runId AND status IN ('PAUSED', 'AUTO_PAUSED', 'INTERRUPTED')"
    )
    suspend fun resumeRun(
        runId: Long,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    )

    /** Salva uma nova coordenada da rota. */
    @Insert
    suspend fun insertPoint(point: RunPointEntity)

    @Insert
    suspend fun insertLap(lap: RunLapEntity)

    @Insert
    suspend fun insertLaps(laps: List<RunLapEntity>)

    @Query("DELETE FROM run_laps WHERE run_id = :runId")
    suspend fun deleteLaps(runId: Long)

    @Query("SELECT * FROM run_laps WHERE run_id = :runId ORDER BY id")
    suspend fun getLaps(runId: Long): List<RunLapEntity>

    @Query("SELECT * FROM run_laps WHERE run_id = :runId ORDER BY id")
    fun observeLaps(runId: Long): Flow<List<RunLapEntity>>

    /** Soma a distância aceita ao total da corrida. */
    @Query("UPDATE runs SET distance_meters = distance_meters + :distanceMeters WHERE id = :runId")
    suspend fun addDistance(runId: Long, distanceMeters: Double)

    /** Persiste o progresso sem substituir o início ou o fim já gravados. */
    @Query(
        "UPDATE runs SET " +
            "local_segment_route_id = COALESCE(local_segment_route_id, :localSegmentRouteId), " +
            "local_segment_progress_meters = MAX(local_segment_progress_meters, :progressMeters), " +
            "local_segment_started_at_duration_ms = COALESCE(local_segment_started_at_duration_ms, :startedAtDurationMs), " +
            "local_segment_completed_at_duration_ms = COALESCE(local_segment_completed_at_duration_ms, :completedAtDurationMs) " +
            "WHERE id = :runId AND status = 'RUNNING'"
    )
    suspend fun updateLocalSegmentProgress(
        runId: Long,
        progressMeters: Double,
        startedAtDurationMs: Long?,
        completedAtDurationMs: Long?,
        localSegmentRouteId: Long?
    )

    /** Salva o ponto e atualiza a distância na mesma transação. */
    @Transaction
    suspend fun appendPoint(point: RunPointEntity, distanceMeters: Double) {
        insertPoint(point)
        addDistance(point.runId, distanceMeters)
    }

    /** Exclui apenas sessões concluídas; a chave estrangeira remove os pontos em cascata. */
    @Query("DELETE FROM runs WHERE id = :runId AND status = 'COMPLETED' AND NOT EXISTS (SELECT 1 FROM runs active_run WHERE active_run.planned_route_run_id = :runId AND active_run.status != 'COMPLETED')")
    suspend fun deleteCompletedRun(runId: Long): Int

    /** Descarta apenas sessões abertas, removendo seu percurso em cascata. */
    @Query("DELETE FROM runs WHERE id = :runId AND status != 'COMPLETED'")
    suspend fun discardUnfinishedRun(runId: Long)

    /** Finaliza uma vez, preservando o maior tempo e os dados mais recentes da sessão. */
    @Query(
        "UPDATE runs SET status = 'COMPLETED', duration_ms = MAX(duration_ms, :durationMs), " +
            "elapsed_duration_ms = CASE WHEN :elapsedDurationMs IS NULL THEN elapsed_duration_ms " +
            "ELSE MAX(COALESCE(elapsed_duration_ms, 0), :elapsedDurationMs) END, " +
            "elapsed_realtime_anchor_ms = CASE WHEN :elapsedDurationMs >= COALESCE(elapsed_duration_ms, 0) " +
            "THEN COALESCE(:elapsedRealtimeAnchorMs, elapsed_realtime_anchor_ms) ELSE elapsed_realtime_anchor_ms END, " +
            "elapsed_epoch_anchor_ms = CASE WHEN :elapsedDurationMs >= COALESCE(elapsed_duration_ms, 0) " +
            "THEN COALESCE(:elapsedEpochAnchorMs, elapsed_epoch_anchor_ms) ELSE elapsed_epoch_anchor_ms END, " +
            "finished_at_epoch_ms = :finishedAtEpochMs, title = :title, description = :description, gear_name = :gearName, gear_id = :gearId, " +
            "private_notes = :privateNotes, " +
            "is_commute = :isCommute, perceived_exertion = :perceivedExertion, " +
            "activity_type = COALESCE(:activityType, activity_type), activity_tags = :activityTags " +
            "WHERE id = :runId AND status != 'COMPLETED'"
    )
    suspend fun finishRun(
        runId: Long,
        durationMs: Long,
        finishedAtEpochMs: Long,
        title: String?,
        description: String?,
        gearName: String?,
        gearId: Long?,
        privateNotes: String?,
        isCommute: Boolean,
        perceivedExertion: Int?,
        activityType: String?,
        activityTags: String,
        elapsedDurationMs: Long? = null,
        elapsedRealtimeAnchorMs: Long? = null,
        elapsedEpochAnchorMs: Long? = null
    )

    /** Atualiza os metadados somente de uma corrida já concluída. */
    @Query(
        "UPDATE runs SET title = :title, description = :description, gear_name = :gearName, gear_id = :gearId, private_notes = :privateNotes, " +
            "is_commute = :isCommute, " +
            "perceived_exertion = :perceivedExertion, activity_type = COALESCE(:activityType, activity_type), " +
            "activity_tags = :activityTags " +
            "WHERE id = :runId AND status = 'COMPLETED'"
    )
    suspend fun updateSaveDetails(
        runId: Long,
        title: String?,
        description: String?,
        gearName: String?,
        gearId: Long?,
        privateNotes: String?,
        isCommute: Boolean,
        perceivedExertion: Int?,
        activityType: String?,
        activityTags: String
    ): Int

    /** Aplica o corte e atualiza os dados dependentes numa única transação. */
    @Transaction
    suspend fun cropCompletedRun(runId: Long, startPointId: Long, endPointId: Long): RunCropOutcome {
        val run = getRun(runId) ?: return RunCropOutcome.RUN_NOT_COMPLETED
        if (run.status != RunStatus.COMPLETED.name) return RunCropOutcome.RUN_NOT_COMPLETED
        if (isUsedByActiveRun(runId)) return RunCropOutcome.ACTIVE_ROUTE_IN_USE

        val points = getRunPoints(runId).map { it.toModel() }
        val crop = calculateRunActivityCrop(
            run = RunModel(
                id = run.id,
                startedAtEpochMs = run.startedAtEpochMs,
                finishedAtEpochMs = run.finishedAtEpochMs,
                durationMs = run.durationMs,
                distanceMeters = run.distanceMeters,
                status = RunStatus.COMPLETED,
                currentSegment = run.currentSegment,
                elapsedDurationMs = run.elapsedDurationMs
            ),
            points = points,
            startPointId = startPointId,
            endPointId = endPointId,
            laps = getLaps(runId).map { lap ->
                RunLap(
                    id = lap.id,
                    durationMs = lap.durationMs,
                    distanceMeters = lap.distanceMeters,
                    recordedAtEpochMs = lap.recordedAtEpochMs,
                    elapsedDurationMs = lap.elapsedDurationMs
                )
            },
            workoutSteps = getWorkoutSteps(runId).map { it.toModel() },
            localSegmentEfforts = getForRun(runId).map { it.toLocalSegmentEffort() }
        ) ?: return RunCropOutcome.INVALID_SELECTION

        val updated = updateRunForCrop(
            runId = runId,
            startedAtEpochMs = crop.startedAtEpochMs,
            finishedAtEpochMs = crop.finishedAtEpochMs,
            durationMs = crop.durationMs,
            elapsedDurationMs = crop.elapsedDurationMs,
            distanceMeters = crop.distanceMeters
        )
        if (updated == 0) return RunCropOutcome.ACTIVE_ROUTE_IN_USE

        deletePointsOutsideRange(runId, startPointId, endPointId)
        deleteLaps(runId)
        if (crop.laps.isNotEmpty()) {
            insertLaps(crop.laps.map { lap ->
                RunLapEntity(
                    id = lap.id,
                    runId = runId,
                    durationMs = lap.durationMs,
                    elapsedDurationMs = lap.elapsedDurationMs,
                    distanceMeters = lap.distanceMeters,
                    recordedAtEpochMs = lap.recordedAtEpochMs
                )
            })
        }
        deleteWorkoutSteps(runId)
        if (crop.workoutSteps.isNotEmpty()) insertWorkoutSteps(crop.workoutSteps.map { it.toEntity(runId) })
        deleteStepRateEventsOutsideRange(runId, crop.startedAtEpochMs, crop.finishedAtEpochMs)
        trimStepRateIntervalsAtRangeStart(runId, crop.startedAtEpochMs, crop.finishedAtEpochMs)
        deleteForRun(runId)
        crop.localSegmentEfforts.forEach { upsert(it.toLocalSegmentEffortEntity(runId)) }
        deleteOverrides(runId)
        return RunCropOutcome.SAVED
    }

    /** Observa a única sessão que ainda não terminou. */
    @Query("SELECT * FROM runs WHERE status != 'COMPLETED' ORDER BY started_at_epoch_ms DESC LIMIT 1")
    fun observeActiveRun(): Flow<RunEntity?>

    /** Lista as sessões concluídas da mais nova para a mais antiga. */
    @Query("SELECT * FROM runs WHERE status = 'COMPLETED' ORDER BY started_at_epoch_ms DESC")
    fun observeCompletedRuns(): Flow<List<RunEntity>>

    /** Observa células agregadas dos pontos aceitos de corridas concluídas no intervalo. */
    @Query(
        "SELECT ROUND(run_points.latitude, 4) AS latitude, ROUND(run_points.longitude, 4) AS longitude, " +
            "COUNT(DISTINCT runs.id) AS visitCount FROM run_points INNER JOIN runs ON runs.id = run_points.run_id " +
            "WHERE runs.status = 'COMPLETED' AND runs.started_at_epoch_ms BETWEEN :startEpochMs AND :endEpochMs " +
            "AND run_points.is_filtered = 0 " +
            "GROUP BY ROUND(run_points.latitude, 4), ROUND(run_points.longitude, 4)"
    )
    fun observeCompletedHeatmapCellsBetween(
        startEpochMs: Long,
        endEpochMs: Long
    ): Flow<List<RunHeatmapCellEntity>>

    // shortcut: cells use degrees, switch to a projected grid if consistent meter resolution becomes necessary.
    /** Agrupa pontos aceitos de todas as corridas concluídas em células de aproximadamente 10 metros. */
    @Query(
        "SELECT ROUND(run_points.latitude, 4) AS latitude, " +
            "ROUND(run_points.longitude, 4) AS longitude, COUNT(DISTINCT runs.id) AS visitCount " +
            "FROM run_points INNER JOIN runs ON runs.id = run_points.run_id " +
            "WHERE runs.status = 'COMPLETED' AND run_points.is_filtered = 0 " +
            "GROUP BY ROUND(run_points.latitude, 4), ROUND(run_points.longitude, 4)"
    )
    suspend fun getCompletedRunHeatmapCells(): List<RunHeatmapCellEntity>

    /** Observa métricas de uma corrida escolhida. */
    @Query("SELECT * FROM runs WHERE id = :runId")
    fun observeRun(runId: Long): Flow<RunEntity?>

    /** Lê uma sessão antes de aplicar uma transição. */
    @Query("SELECT * FROM runs WHERE id = :runId")
    suspend fun getRun(runId: Long): RunEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM runs WHERE planned_route_run_id = :runId AND status != 'COMPLETED')")
    suspend fun isUsedByActiveRun(runId: Long): Boolean

    /** Observa os pontos na ordem em que foram gravados. */
    @Query("SELECT * FROM run_points WHERE run_id = :runId ORDER BY id")
    fun observePoints(runId: Long): Flow<List<RunPointEntity>>

    @Query("SELECT * FROM run_points WHERE run_id = :runId ORDER BY id")
    suspend fun getRunPoints(runId: Long): List<RunPointEntity>

    @Query("DELETE FROM run_points WHERE run_id = :runId AND (id < :startPointId OR id > :endPointId)")
    suspend fun deletePointsOutsideRange(runId: Long, startPointId: Long, endPointId: Long)

    @Query(
        "UPDATE runs SET started_at_epoch_ms = :startedAtEpochMs, finished_at_epoch_ms = :finishedAtEpochMs, " +
            "duration_ms = :durationMs, elapsed_duration_ms = :elapsedDurationMs, " +
            "elapsed_realtime_anchor_ms = NULL, elapsed_epoch_anchor_ms = NULL, distance_meters = :distanceMeters, " +
            "local_segment_route_id = NULL, local_segment_progress_meters = 0.0, " +
            "local_segment_started_at_duration_ms = NULL, local_segment_completed_at_duration_ms = NULL " +
            "WHERE id = :runId AND status = 'COMPLETED' " +
            "AND NOT EXISTS (SELECT 1 FROM runs active_run WHERE active_run.planned_route_run_id = :runId " +
            "AND active_run.status != 'COMPLETED')"
    )
    suspend fun updateRunForCrop(
        runId: Long,
        startedAtEpochMs: Long,
        finishedAtEpochMs: Long,
        durationMs: Long,
        elapsedDurationMs: Long,
        distanceMeters: Double
    ): Int

    /** Lê o último ponto do trecho atual para evitar ligar trechos separados. */
    @Query("SELECT * FROM run_points WHERE run_id = :runId AND segment_index = :segmentIndex ORDER BY id DESC LIMIT 1")
    suspend fun getLastPoint(runId: Long, segmentIndex: Int): RunPointEntity?
}
