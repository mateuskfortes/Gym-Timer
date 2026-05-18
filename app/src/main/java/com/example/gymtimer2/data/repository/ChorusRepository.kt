package com.example.gymtimer2.data.repository

import com.example.gymtimer2.data.dao.ChorusDao
import com.example.gymtimer2.data.mapper.toDomain
import com.example.gymtimer2.data.mapper.toEntity
import com.example.gymtimer2.domain.model.ChorusModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChorusRepository(
    private val chorusDao: ChorusDao
) {
    fun getChorusesBySongId(songId: Long): Flow<List<ChorusModel>> {
        return chorusDao.getChorusesBySongId(songId).map { choruses ->
            choruses.map { it.toDomain() }
        }
    }

    fun getAllChoruses(): Flow<List<ChorusModel>> {
        return chorusDao.getAllChoruses().map { choruses ->
            choruses.map { it.toDomain() }
        }
    }

    suspend fun insertChorus(chorus: ChorusModel): Long {
        return chorusDao.insert(chorus.toEntity())
    }

    suspend fun updateChorus(chorus: ChorusModel) {
        chorusDao.update(chorus.toEntity())
    }

    suspend fun deleteChorus(chorus: ChorusModel) {
        chorusDao.delete(chorus.toEntity())
    }

    suspend fun deleteChorusesBySongId(songId: Long) {
        chorusDao.deleteChorusesBySongId(songId)
    }
}

