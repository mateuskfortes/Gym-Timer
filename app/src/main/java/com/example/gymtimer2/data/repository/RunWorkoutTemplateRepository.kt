package com.example.gymtimer2.data.repository

import com.google.gson.Gson
import com.example.gymtimer2.data.dao.RunWorkoutTemplateDao
import com.example.gymtimer2.data.entity.RunWorkoutTemplateEntity
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RunWorkoutTemplateRepository(private val dao: RunWorkoutTemplateDao) {
    private val gson = Gson()

    val templates: Flow<List<RunWorkoutTemplate>> = dao.observeAll().map { rows ->
        rows.mapNotNull(::toTemplate)
    }

    suspend fun save(id: Long?, plan: RunWorkoutPlan): Long? {
        val normalizedPlan = plan.normalizedOrNull() ?: return null
        return dao.save(
            RunWorkoutTemplateEntity(
                id = id ?: 0L,
                planJson = gson.toJson(normalizedPlan)
            )
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun toTemplate(entity: RunWorkoutTemplateEntity): RunWorkoutTemplate? {
        val plan = runCatching {
            gson.fromJson(entity.planJson, RunWorkoutPlan::class.java)?.normalizedOrNull()
        }.getOrNull() ?: return null
        return RunWorkoutTemplate(entity.id, plan)
    }
}
