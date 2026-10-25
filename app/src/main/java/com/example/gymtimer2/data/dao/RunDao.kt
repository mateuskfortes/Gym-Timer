@file:Suppress("detekt:DocumentationOverPrivateFunction", "detekt:ComplexInterface")

package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
/** Consultas e gravações Room para sessões e pontos GPS. */
interface RunDao {
    /** Cria uma sessão antes de iniciar o serviço de GPS. */
    @Insert
    suspend fun insertRun(run: RunEntity): Long

    /** Atualiza estado, tempo e término da sessão. */
    @Update
    suspend fun updateRun(run: RunEntity)

    /** Salva uma nova coordenada da rota. */
    @Insert
    suspend fun insertPoint(point: RunPointEntity)

    /** Soma a distância aceita ao total da corrida. */
    @Query("UPDATE runs SET distance_meters = distance_meters + :distanceMeters WHERE id = :runId")
    suspend fun addDistance(runId: Long, distanceMeters: Double)

    /** Salva o ponto e atualiza a distância na mesma transação. */
    @Transaction
    suspend fun appendPoint(point: RunPointEntity, distanceMeters: Double) {
        insertPoint(point)
        addDistance(point.runId, distanceMeters)
    }

    /** Exclui apenas sessões concluídas; a chave estrangeira remove os pontos em cascata. */
    @Query("DELETE FROM runs WHERE id = :runId AND status = 'COMPLETED'")
    suspend fun deleteCompletedRun(runId: Long)

    /** Descarta apenas sessões abertas, removendo seu percurso em cascata. */
    @Query("DELETE FROM runs WHERE id = :runId AND status != 'COMPLETED'")
    suspend fun discardUnfinishedRun(runId: Long)

    /** Finaliza uma vez, preservando o maior tempo e os dados mais recentes da sessão. */
    @Query("UPDATE runs SET status = 'COMPLETED', duration_ms = MAX(duration_ms, :durationMs), finished_at_epoch_ms = :finishedAtEpochMs, title = :title WHERE id = :runId AND status != 'COMPLETED'")
    suspend fun finishRun(runId: Long, durationMs: Long, finishedAtEpochMs: Long, title: String?)

    /** Observa a única sessão que ainda não terminou. */
    @Query("SELECT * FROM runs WHERE status != 'COMPLETED' ORDER BY started_at_epoch_ms DESC LIMIT 1")
    fun observeActiveRun(): Flow<RunEntity?>

    /** Lista as sessões concluídas da mais nova para a mais antiga. */
    @Query("SELECT * FROM runs WHERE status = 'COMPLETED' ORDER BY started_at_epoch_ms DESC")
    fun observeCompletedRuns(): Flow<List<RunEntity>>

    /** Observa métricas de uma corrida escolhida. */
    @Query("SELECT * FROM runs WHERE id = :runId")
    fun observeRun(runId: Long): Flow<RunEntity?>

    /** Lê uma sessão antes de aplicar uma transição. */
    @Query("SELECT * FROM runs WHERE id = :runId")
    suspend fun getRun(runId: Long): RunEntity?

    /** Observa os pontos na ordem em que foram gravados. */
    @Query("SELECT * FROM run_points WHERE run_id = :runId ORDER BY recorded_at_epoch_ms, id")
    fun observePoints(runId: Long): Flow<List<RunPointEntity>>

    /** Lê o último ponto do trecho atual para evitar ligar trechos separados. */
    @Query("SELECT * FROM run_points WHERE run_id = :runId AND segment_index = :segmentIndex ORDER BY recorded_at_epoch_ms DESC, id DESC LIMIT 1")
    suspend fun getLastPoint(runId: Long, segmentIndex: Int): RunPointEntity?
}
