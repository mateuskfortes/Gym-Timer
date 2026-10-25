package com.example.gymtimer2

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.domain.model.RunStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifica salvamento com título e descarte sem afetar corridas concluídas. */
class RunPersistenceTest {
    /** A finalização conserva métricas e rota e impede que comandos repetidos substituam o título. */
    @Test
    fun finishingPersistsTitleAndRetainsRoute() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            val id = repository.startRun(1L)
            repository.updateDuration(id, 8_000L)
            database.runDao().insertPoint(RunPointEntity(runId = id, latitude = -23.55, longitude = -46.63,
                recordedAtEpochMs = 2L, accuracyMeters = 4f, segmentIndex = 0))
            repository.finishRun(id, 5_000L, 3L, "  Corrida no parque  ")
            repository.finishRun(id, 4_000L, 4L, "Outro título")
            val saved = checkNotNull(repository.getRun(id))
            assertEquals("Corrida no parque", saved.title)
            assertEquals(RunStatus.COMPLETED, saved.status)
            assertEquals(8_000L, saved.durationMs)
            assertEquals(1, repository.observePoints(id).first().size)
            repository.discardUnfinishedRun(id)
            assertNotNull(repository.getRun(id))
        } finally {
            database.close()
        }
    }

    /** Descartar cada estado aberto remove os pontos e não cria uma entrada no histórico. */
    @Test
    fun discardingUnfinishedRunsRemovesTheirRoutes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = database.runDao()
            val repository = RunRepository(dao)
            for (status in RunStatus.entries.filter { it != RunStatus.COMPLETED }) {
                val id = repository.startRun(1L)
                dao.updateRun(checkNotNull(dao.getRun(id)).copy(status = status.name))
                dao.insertPoint(RunPointEntity(runId = id, latitude = -23.55, longitude = -46.63,
                    recordedAtEpochMs = 2L, accuracyMeters = 4f, segmentIndex = 0))
                repository.discardUnfinishedRun(id)
                assertNull(repository.getRun(id))
                assertEquals(0, repository.observePoints(id).first().size)
            }
            assertEquals(0, repository.completedRuns.first().size)
        } finally {
            database.close()
        }
    }
}
