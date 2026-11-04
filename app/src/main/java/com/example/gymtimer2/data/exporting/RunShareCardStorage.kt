package com.example.gymtimer2.data.exporting

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

internal class RunShareCardStorage(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.cacheDir, SHARE_DIRECTORY)
    private val authority = "${appContext.packageName}.runphoto.provider"

    fun save(bitmap: Bitmap): Uri? {
        if (!directory.exists() && !directory.mkdirs()) return null
        val image = File(directory, "run-share-${UUID.randomUUID()}.png")
        return try {
            val compressed = FileOutputStream(image).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (!compressed) {
                image.delete()
                return null
            }
            FileProvider.getUriForFile(appContext, authority, image)
        } catch (_: IOException) {
            image.delete()
            null
        } catch (_: IllegalArgumentException) {
            image.delete()
            null
        } catch (_: SecurityException) {
            image.delete()
            null
        }
    }

    private companion object {
        const val SHARE_DIRECTORY = "run_share_cards"
    }
}
