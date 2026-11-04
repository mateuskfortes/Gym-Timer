package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Transaction
import com.example.gymtimer2.data.entity.RunGearEntity

@Dao
interface RunGearDao : RunGearQueriesDao, RunGearCommandsDao {

    @Transaction
    suspend fun setDefault(gearId: Long) {
        require(existsActive(gearId)) { "Gear is missing or retired" }
        clearDefault()
        markDefault(gearId)
    }

    @Transaction
    suspend fun setRetired(gearId: Long, retired: Boolean) {
        require(exists(gearId)) { "Gear does not exist" }
        updateRetired(gearId, retired)
        if (retired) clearGearDefault(gearId)
    }

    @Transaction
    suspend fun rename(gearId: Long, name: String): Boolean {
        require(exists(gearId)) { "Gear does not exist" }
        val existingId = findIdByName(name)
        if (existingId != null && existingId != gearId) return false
        updateName(gearId, name)
        return true
    }

    @Transaction
    suspend fun getOrCreate(name: String): Long {
        findIdByName(name)?.let { existingId ->
            if (isRetired(existingId) == true) setRetired(existingId, false)
            return existingId
        }
        val id = insert(RunGearEntity(name = name))
        val savedId = if (id == -1L) checkNotNull(findIdByName(name)) else id
        if (!hasDefault()) setDefault(savedId)
        return savedId
    }
}
