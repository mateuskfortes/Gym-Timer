package com.example.gymtimer2.data.repository

import com.example.gymtimer2.data.dao.ExerciseChorusDao
import com.example.gymtimer2.data.entity.ExerciseChorusEntity
import com.example.gymtimer2.data.mapper.toDomain
import com.example.gymtimer2.data.mapper.toModel
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.ExerciseModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseChorusRepository(
    private val exerciseChorusDao: ExerciseChorusDao
) {

    suspend fun addChorusToExercise(exerciseId: Int, chorusId: Long) {
        exerciseChorusDao.insert(
            ExerciseChorusEntity(
                exerciseId = exerciseId,
                chorusId = chorusId
            )
        )
    }

    suspend fun removeChorusFromExercise(exerciseId: Int, chorusId: Long) {
        exerciseChorusDao.deleteByIds(exerciseId, chorusId)
    }

    fun getChorusesByExerciseId(exerciseId: Int): Flow<List<ChorusModel>> {
        return exerciseChorusDao.getChorusesByExerciseId(exerciseId).map { choruses ->
            choruses.map { it.toDomain() }
        }
    }

    fun getExercisesByChorusId(chorusId: Long): Flow<List<ExerciseModel>> {
        return exerciseChorusDao.getExercisesByChorusId(chorusId).map { exercises ->
            exercises.map { it.toModel() }
        }
    }

    suspend fun removeAllChorusesFromExercise(exerciseId: Int) {
        exerciseChorusDao.deleteByExerciseId(exerciseId)
    }

    suspend fun removeAllExercisesFromChorus(chorusId: Long) {
        exerciseChorusDao.deleteByChorusId(chorusId)
    }
}

