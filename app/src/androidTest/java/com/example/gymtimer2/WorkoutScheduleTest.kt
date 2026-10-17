package com.example.gymtimer2

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutScheduleTest {
    @Test
    fun reorderMoveAndDeleteKeepDayRules() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = WorkoutRepository(
                context, database.exerciseDao(), database.songDao(), database.chorusDao(),
                database.exerciseChorusDao(), database.workoutDayDao(),
                database.exerciseScheduleDao(), database
            )
            val firstDay = repository.createDay("A")
            val secondDay = repository.createDay("B")
            val firstId = repository.insertExercise(exercise("First"), firstDay).toInt()
            val secondId = repository.insertExercise(exercise("Second"), firstDay).toInt()

            repository.reorderExercises(firstDay, listOf(secondId, firstId))
            assertEquals(listOf(secondId, firstId), repository.orderedExerciseIdsFrom(secondId))
            assertEquals(
                listOf(secondId, firstId),
                repository.scheduledExercisesForDay(firstDay).first().map { it.exercise.id }
            )
            assertEquals(emptyList<Int>(), repository.scheduledExercisesForDay(secondDay).first().map { it.exercise.id })

            repository.moveExercise(secondId, secondDay)
            assertEquals(listOf(firstId), repository.orderedExerciseIdsFrom(firstId))
            assertEquals(listOf(secondId), repository.orderedExerciseIdsFrom(secondId))
            assertEquals(listOf(firstId), repository.scheduledExercisesForDay(firstDay).first().map { it.exercise.id })
            assertEquals(listOf(secondId), repository.scheduledExercisesForDay(secondDay).first().map { it.exercise.id })
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.deleteDay(secondDay) }
            }

            repository.updateExerciseWeight(secondId, 25)
            assertEquals(25, repository.scheduledExercises.first()
                .first { it.exercise.id == secondId }.exercise.weight.value)

            repository.deleteExercise(repository.scheduledExercises.first()
                .first { it.exercise.id == secondId }.exercise)
            repository.deleteDay(secondDay)
            assertEquals(listOf("A"), repository.workoutDays.first().map { it.name })
        } finally {
            database.close()
        }
    }

    private fun exercise(name: String) = ExerciseModel(
        name = name,
        weight = WeightModel(10, WeightUnit.TOTAL_KG),
        restPeriod = 60000,
        chorusDelay = 0
    )
}
