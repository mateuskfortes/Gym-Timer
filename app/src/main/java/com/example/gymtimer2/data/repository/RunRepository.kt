@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.data.repository

import com.example.gymtimer2.data.dao.RunDao
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunTitle
import com.example.gymtimer2.domain.model.RunStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Converte modelos de corrida e coordena operações no DAO. */
class RunRepository(private val runDao: RunDao) {
    /** Sessão atual, pausada ou interrompida. */
    val activeRun: Flow<RunModel?> = runDao.observeActiveRun().map { it?.toModel() }

    /** Histórico local em ordem decrescente de data. */
    val completedRuns: Flow<List<RunModel>> = runDao.observeCompletedRuns().map { runs ->
        runs.map { it.toModel() }
    }

    /** Cria uma sessão antes de iniciar o serviço. */
    suspend fun startRun(startedAtEpochMs: Long): Long = runDao.insertRun(
        RunEntity(startedAtEpochMs = startedAtEpochMs)
    )

    /** Marca que o serviço aceitou a sessão e começou a gravar. */
    suspend fun beginRun(runId: Long) = updateRunState(
        runId = runId,
        status = RunStatus.RUNNING,
        durationMs = null
    )

    /** Mantém o maior tempo já salvo para não voltar após uma emissão atrasada. */
    suspend fun updateDuration(runId: Long, durationMs: Long) {
        val run = runDao.getRun(runId) ?: return
        runDao.updateRun(run.copy(durationMs = durationMs.coerceAtLeast(run.durationMs)))
    }

    suspend fun getRun(runId: Long): RunModel? = runDao.getRun(runId)?.toModel()

    /** Salva o tempo ativo e muda a sessão para pausada. */
    suspend fun pauseRun(runId: Long, durationMs: Long) = updateRunState(
        runId = runId,
        status = RunStatus.PAUSED,
        durationMs = durationMs
    )

    /** Retoma a sessão em um novo trecho para preservar as pausas na rota. */
    suspend fun resumeRun(runId: Long) {
        val run = runDao.getRun(runId) ?: return
        runDao.updateRun(run.copy(status = RunStatus.RUNNING.name, currentSegment = run.currentSegment + 1))
    }

    /** Preserva a sessão e permite que a pessoa escolha como continuar. */
    suspend fun markInterrupted(runId: Long) = updateRunState(
        runId = runId,
        status = RunStatus.INTERRUPTED,
        durationMs = null
    )

    /** Finaliza uma corrida ou salva uma corrida parcial. */
    suspend fun finishRun(runId: Long, durationMs: Long, finishedAtEpochMs: Long, title: String? = null) {
        runDao.finishRun(runId, durationMs, finishedAtEpochMs, RunTitle.normalize(title))
    }

    /** Descarta a sessão após o serviço encerrar as gravações pendentes. */
    suspend fun discardUnfinishedRun(runId: Long) = runDao.discardUnfinishedRun(runId)

    /** Insere um ponto aceito e soma sua distância ao total. */
    suspend fun appendLocation(runId: Long, point: GpsPoint, distanceMeters: Double) {
        val run = runDao.getRun(runId) ?: return
        runDao.appendPoint(
            point = RunPointEntity(
                runId = runId,
                latitude = point.latitude,
                longitude = point.longitude,
                recordedAtEpochMs = point.recordedAtEpochMs,
                accuracyMeters = point.accuracyMeters,
                segmentIndex = run.currentSegment
            ),
            distanceMeters = distanceMeters
        )
    }

    /** Remove uma corrida passada e sua rota, preservando sessões ainda abertas. */
    suspend fun deleteCompletedRun(runId: Long) = runDao.deleteCompletedRun(runId)

    /** Observa o traçado GPS de uma sessão. */
    fun observePoints(runId: Long): Flow<List<RunPointModel>> =
        runDao.observePoints(runId).map { points -> points.map { it.toModel() } }

    /** Observa métricas de uma sessão do histórico. */
    fun observeRun(runId: Long): Flow<RunModel?> = runDao.observeRun(runId).map { it?.toModel() }

    /** Atualiza um estado sem reduzir o tempo acumulado. */
    private suspend fun updateRunState(runId: Long, status: RunStatus, durationMs: Long?) {
        val run = runDao.getRun(runId) ?: return
        runDao.updateRun(
            run.copy(
                status = status.name,
                durationMs = durationMs?.coerceAtLeast(run.durationMs) ?: run.durationMs
            )
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
        title = title
    )

    /** Converte uma coordenada salva em modelo de domínio. */
    private fun RunPointEntity.toModel() = RunPointModel(
        id = id,
        runId = runId,
        latitude = latitude,
        longitude = longitude,
        recordedAtEpochMs = recordedAtEpochMs,
        accuracyMeters = accuracyMeters,
        segmentIndex = segmentIndex
    )
}
