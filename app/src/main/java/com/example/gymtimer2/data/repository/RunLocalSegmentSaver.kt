package com.example.gymtimer2.data.repository

import com.example.gymtimer2.domain.model.RunLocalSegmentSelection
import com.example.gymtimer2.domain.model.RunStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Salva um trecho selecionado de uma corrida concluída como rota local. */
class RunLocalSegmentSaver(
    private val runRepository: RunRepository,
    private val plannedRouteRepository: PlannedRouteRepository
) {
    suspend fun save(
        runId: Long,
        name: String,
        segmentIndex: Int,
        startDistanceMeters: Double,
        endDistanceMeters: Double
    ): Boolean {
        val run = runRepository.getRun(runId) ?: return false
        if (run.status != RunStatus.COMPLETED) return false
        return try {
            val points = runRepository.observePoints(runId).first()
            val segmentPoints = withContext(Dispatchers.Default) {
                val track = RunLocalSegmentSelection.tracks(points)
                    .firstOrNull { it.segmentIndex == segmentIndex } ?: return@withContext emptyList()
                RunLocalSegmentSelection.pointsBetween(track, startDistanceMeters, endDistanceMeters)
            }
            if (segmentPoints.isEmpty()) return false
            plannedRouteRepository.saveLocalSegment(name, segmentPoints)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}
