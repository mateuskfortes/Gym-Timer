package com.example.gymtimer2.data.repository

import com.example.gymtimer2.data.dao.RunPrivacyZoneDao
import com.example.gymtimer2.data.entity.toEntity
import com.example.gymtimer2.data.entity.toModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import kotlinx.coroutines.flow.map

class RunPrivacyZoneRepository(private val dao: RunPrivacyZoneDao) {
    val zones = dao.observeAll().map { entities -> entities.map { it.toModel() } }

    suspend fun getAll(): List<RunPrivacyZone> = dao.getAll().map { it.toModel() }

    suspend fun save(zone: RunPrivacyZone) {
        val normalized = zone.copy(name = zone.name.trim())
        require(normalized.name.isNotEmpty() && normalized.name.length <= RunPrivacyZone.MAX_NAME_LENGTH)
        require(normalized.latitude.isFinite() && normalized.latitude in -90.0..90.0)
        require(normalized.longitude.isFinite() && normalized.longitude in -180.0..180.0)
        require(normalized.radiusMeters in RunPrivacyZone.RADIUS_OPTIONS_METERS)

        if (normalized.id == NEW_ZONE_ID) {
            dao.insert(normalized.toEntity())
        } else {
            check(dao.update(normalized.toEntity()) == 1) { "privacy_zone_not_found" }
        }
    }

    suspend fun delete(id: Long) {
        dao.get(id)?.let { dao.delete(it) }
    }

    private companion object {
        const val NEW_ZONE_ID = 0L
    }
}
