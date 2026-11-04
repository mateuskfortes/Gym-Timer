@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.data.repository

import com.example.gymtimer2.data.dao.RunBestEffortDao
import com.example.gymtimer2.data.entity.RunBestEffortOverrideEntity
import com.example.gymtimer2.data.entity.toModel
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunPointModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persistência de correções e pontos usados pelos recordes locais. */
class RunBestEffortRepository(private val dao: RunBestEffortDao) {
    /** Lê uma corrida por vez para limitar a memória usada no cálculo dos recordes. */
    // shortcut: one full activity remains in memory; use keyset paging if individual runs grow large.
    suspend fun getCompletedPoints(runId: Long): List<RunPointModel> =
        dao.getCompletedPoints(runId).map { it.toModel() }

    /** Observa as correções de melhores esforços de uma corrida. */
    fun observeOverrides(runId: Long): Flow<List<RunBestEffortOverride>> =
        dao.observeOverrides(runId).map { overrides -> overrides.mapNotNull { it.toBestEffortOverride() } }

    /** Observa correções para atualizar os recordes do histórico local. */
    fun observeAllOverrides(): Flow<List<RunBestEffortOverride>> =
        dao.observeAllOverrides().map { overrides -> overrides.mapNotNull { it.toBestEffortOverride() } }

    /** Salva ou remove dos rankings uma marca corrigida. */
    suspend fun saveOverride(override: RunBestEffortOverride) {
        dao.saveOverride(
            RunBestEffortOverrideEntity(
                runId = override.runId,
                distance = override.distance.name,
                durationMs = override.durationMs,
                isRemoved = override.isRemoved
            )
        )
    }

    /** Restaura o cálculo GPS original para uma distância da corrida. */
    suspend fun clearOverride(runId: Long, distance: RunBenchmarkDistance) {
        dao.deleteOverride(runId, distance.name)
    }
}

private fun RunBestEffortOverrideEntity.toBestEffortOverride(): RunBestEffortOverride? {
    val benchmark = RunBenchmarkDistance.entries.firstOrNull { it.name == distance } ?: return null
    return RunBestEffortOverride(runId, benchmark, durationMs, isRemoved)
}
