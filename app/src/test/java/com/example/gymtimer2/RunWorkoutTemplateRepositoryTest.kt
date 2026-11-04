package com.example.gymtimer2

import com.example.gymtimer2.data.dao.RunWorkoutTemplateDao
import com.example.gymtimer2.data.entity.RunWorkoutTemplateEntity
import com.example.gymtimer2.data.repository.RunWorkoutTemplateRepository
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutSetPlan
import com.example.gymtimer2.domain.model.RunWorkoutStepPlan
import com.example.gymtimer2.domain.model.RunWorkoutTemplate
import com.example.gymtimer2.domain.model.RunWorkoutTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunWorkoutTemplateRepositoryTest {
    @Test
    fun savesAndRestoresWorkoutTemplateWithoutLosingPlanFields() = runBlocking {
        val repository = RunWorkoutTemplateRepository(InMemoryRunWorkoutTemplateDao())
        val plan = RunWorkoutPlan(
            name = "  Intervalado  ",
            sets = listOf(
                RunWorkoutSetPlan(
                    steps = listOf(
                        RunWorkoutStepPlan(
                            description = "Ritmo forte",
                            trigger = RunWorkoutTrigger.DISTANCE,
                            distanceTargetMeters = 400.0,
                            paceTarget = RunPaceTarget(270, 300)
                        )
                    ),
                    repetitions = 4
                )
            )
        )

        val id = checkNotNull(repository.save(null, plan))
        assertEquals(
            RunWorkoutTemplate(id, plan.copy(name = "Intervalado")),
            repository.templates.first().single()
        )
        assertEquals(id, repository.save(id, plan.copy(name = "Quatro tiros")))
        assertEquals("Quatro tiros", repository.templates.first().single().plan.name)

        repository.delete(id)
        assertTrue(repository.templates.first().isEmpty())
    }

    private class InMemoryRunWorkoutTemplateDao : RunWorkoutTemplateDao {
        private val rows = MutableStateFlow(emptyList<RunWorkoutTemplateEntity>())

        override fun observeAll(): Flow<List<RunWorkoutTemplateEntity>> = rows

        override suspend fun save(template: RunWorkoutTemplateEntity): Long {
            val id = template.id.takeIf { it != 0L } ?: ((rows.value.maxOfOrNull { it.id } ?: 0L) + 1)
            rows.value = rows.value.filterNot { it.id == id } + template.copy(id = id)
            return id
        }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }
}
