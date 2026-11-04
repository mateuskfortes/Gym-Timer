package com.example.gymtimer2.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.gymtimer2.data.dao.RunGearDao
import com.example.gymtimer2.domain.model.RunGear
import com.example.gymtimer2.domain.model.RunSaveDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RunGearRepository(
    private val dao: RunGearDao,
    private val mediaStorage: RunMediaStorage? = null
) {
    private val photoMutex = Mutex()

    val gears: Flow<List<RunGear>> = dao.observeGears().map { rows ->
        rows.map { RunGear(it.id, it.name, it.distanceMeters, it.isDefault, it.isRetired, it.photoFileName) }
    }

    suspend fun getOrCreate(name: String): RunGear {
        val normalizedName = name.trim().take(RunSaveDetails.MAX_GEAR_NAME_LENGTH)
        require(normalizedName.isNotBlank()) { "Gear name must not be blank" }
        val id = dao.getOrCreate(normalizedName)
        return RunGear(id, normalizedName, 0.0, false)
    }

    suspend fun setDefault(gearId: Long) = dao.setDefault(gearId)

    suspend fun setRetired(gearId: Long, retired: Boolean) = dao.setRetired(gearId, retired)

    suspend fun rename(gearId: Long, name: String): Boolean {
        val normalizedName = name.trim().take(RunSaveDetails.MAX_GEAR_NAME_LENGTH)
        require(normalizedName.isNotBlank()) { "Gear name must not be blank" }
        return dao.rename(gearId, normalizedName)
    }

    suspend fun setPhoto(gearId: Long, uri: Uri?) = savePhoto(gearId, uri != null) { storage ->
        uri?.let(storage::copyImage)
    }

    suspend fun setPhoto(gearId: Long, bitmap: Bitmap) = savePhoto(gearId, hasNewPhoto = true) { storage ->
        storage.copyBitmap(bitmap)
    }

    private suspend fun savePhoto(
        gearId: Long,
        hasNewPhoto: Boolean,
        createFile: (RunMediaStorage) -> String?
    ) = photoMutex.withLock {
        withContext(Dispatchers.IO) {
            val storage = mediaStorage ?: return@withContext
            val newFileName = createFile(storage)
            if (hasNewPhoto && newFileName == null) return@withContext
            persistPhoto(gearId, newFileName, storage)
        }
    }

    private suspend fun persistPhoto(gearId: Long, newFileName: String?, storage: RunMediaStorage) {
        var saved = false
        try {
            val oldFileName = dao.getPhotoFileName(gearId)
            if (dao.updatePhotoFileName(gearId, newFileName) == 0) return
            saved = true
            if (oldFileName != newFileName) oldFileName?.let(storage::delete)
        } finally {
            if (!saved) newFileName?.let(storage::delete)
        }
    }

    suspend fun createPhotoCapture(): Uri? = withContext(Dispatchers.IO) {
        mediaStorage?.createCaptureUri()
    }

    suspend fun deletePhotoCapture(uri: Uri) {
        withContext(NonCancellable + Dispatchers.IO) { mediaStorage?.deleteCapture(uri) }
    }
}
