package com.example.gymtimer2

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.entity.RunEntity
import com.example.gymtimer2.data.entity.RunPointEntity
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.domain.model.RunStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica a exclusão real no Room, incluindo o percurso e a proteção de sessões abertas. */
@RunWith(AndroidJUnit4::class)
class RunDeletionTest {
    /** Remover uma corrida concluída deve remover seus pontos e preservar os demais registros. */
    @Test
    fun deletionCascadesOnlyToSelectedCompletedRun() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = database.runDao()
            val repository = RunRepository(dao)
            val removedId = dao.insertRun(RunEntity(startedAtEpochMs = 1L, status = "COMPLETED"))
            val retainedId = dao.insertRun(RunEntity(startedAtEpochMs = 2L, status = "COMPLETED"))
            for (id in listOf(removedId, retainedId)) {
                dao.insertPoint(RunPointEntity(runId = id, latitude = -23.55, longitude = -46.63,
                    recordedAtEpochMs = 3L, accuracyMeters = 4f, segmentIndex = 0))
            }
            repository.deleteCompletedRun(removedId)
            repository.deleteCompletedRun(removedId)
            assertNull(repository.getRun(removedId))
            assertEquals(emptyList<Any>(), repository.observePoints(removedId).first())
            assertEquals(listOf(retainedId), repository.completedRuns.first().map { it.id })
            assertEquals(1, repository.observePoints(retainedId).first().size)
        } finally {
            database.close()
        }
    }

    /** Nem uma chamada direta pode excluir sessões em andamento, pausadas ou interrompidas. */
    @Test
    fun deletionPreservesEveryUnfinishedStatus() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = RunRepository(database.runDao())
            for (status in RunStatus.entries.filter { it != RunStatus.COMPLETED }) {
                val id = database.runDao().insertRun(RunEntity(startedAtEpochMs = 1L, status = status.name))
                repository.deleteCompletedRun(id)
                assertNotNull(repository.getRun(id))
            }
        } finally {
            database.close()
        }
    }
}
