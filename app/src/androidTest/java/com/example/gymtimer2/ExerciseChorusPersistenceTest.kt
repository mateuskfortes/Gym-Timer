package com.example.gymtimer2

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.domain.model.WeightModel
import com.example.gymtimer2.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseChorusPersistenceTest {
    @Test
    fun replacingExerciseChorusesPersistsDeselection() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = WorkoutRepository(
                context, database.exerciseDao(), database.songDao(), database.chorusDao(),
                database.exerciseChorusDao(), database.workoutDayDao(),
                database.exerciseScheduleDao(), database
            )
            repository.saveSongs(
                listOf(SongModel(id = 7, title = "Song", artist = "Artist", uriString = "content://song/7"))
            )
            val exerciseId = repository.insertExercise(
                ExerciseModel(
                    name = "Exercise",
                    weight = WeightModel(10, WeightUnit.TOTAL_KG),
                    restPeriod = 60_000,
                    chorusDelay = 0
                ),
                repository.createDay("Day")
            ).toInt()
            val firstChorusId = repository.insertChorus(ChorusModel(songId = 7, startMs = 0))
            val retainedChorusId = repository.insertChorus(ChorusModel(songId = 7, startMs = 10_000))
            repository.addChorusToExercise(exerciseId, firstChorusId)
            repository.addChorusToExercise(exerciseId, retainedChorusId)

            repository.replaceChorusesForExercise(exerciseId, setOf(retainedChorusId))

            val savedChorusIds = repository.getChorusesByExerciseId(exerciseId).first().map { it.id }
            assertEquals(listOf(retainedChorusId), savedChorusIds)
        } finally {
            database.close()
        }
    }
}
