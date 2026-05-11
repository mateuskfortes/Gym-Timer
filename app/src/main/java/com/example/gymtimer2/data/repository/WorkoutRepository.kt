package com.example.gymtimer2.data.repository

import com.example.gymtimer2.data.dao.ExerciseDao
import com.example.gymtimer2.data.mapper.toEntity
import com.example.gymtimer2.data.mapper.toModel
import com.example.gymtimer2.domain.model.ExerciseModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(private val exerciseDao: ExerciseDao) {

    val allExercises: Flow<List<ExerciseModel>> =
        exerciseDao.getAllExercises().map { list ->
            list.map { it.toModel() }
        }
    suspend fun insertExercise(exercise: ExerciseModel) {
        exerciseDao.insert(exercise.toEntity())
    }

    suspend fun updateExercise(exercise: ExerciseModel) {
        exerciseDao.update(exercise.toEntity())
    }

    suspend fun deleteExercise(exercise: ExerciseModel) {
        exerciseDao.delete(exercise.toEntity())
    }
}