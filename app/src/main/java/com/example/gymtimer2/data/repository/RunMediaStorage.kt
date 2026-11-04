package com.example.gymtimer2.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal const val MAX_RUN_IMAGE_SIZE_BYTES = 20L * 1024 * 1024
internal const val MAX_RUN_VIDEO_SIZE_BYTES = 100L * 1024 * 1024

internal data class StoredRunMedia(val fileName: String, val isVideo: Boolean)

class RunMediaStorage(context: Context) {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val mediaDirectory = File(appContext.filesDir, "run_photos")
    private val captureAuthority = "${appContext.packageName}.runphoto.provider"

    fun createCaptureUri(): Uri? {
        if (!ensureMediaDirectory()) return null
        val file = File(mediaDirectory, "${UUID.randomUUID()}.capture.jpg")
        return try {
            if (!file.createNewFile()) return null
            FileProvider.getUriForFile(appContext, captureAuthority, file)
        } catch (_: IOException) {
            file.delete()
            null
        } catch (_: IllegalArgumentException) {
            file.delete()
            null
        } catch (_: SecurityException) {
            file.delete()
            null
        }
    }

    internal fun copyMedia(uri: Uri): StoredRunMedia? {
        val mimeType = mediaMimeType(uri) ?: return null
        val isVideo = mimeType.startsWith("video/")
        val isImage = mimeType.startsWith("image/")
        if ((!isImage && !isVideo) || !ensureMediaDirectory()) return null
        val extension = if (isVideo) ".video" else ".image"
        val maxSizeBytes = if (isVideo) MAX_RUN_VIDEO_SIZE_BYTES else MAX_RUN_IMAGE_SIZE_BYTES
        val file = File(mediaDirectory, "${UUID.randomUUID()}$extension")
        return copyToLocalFile(uri, file, maxSizeBytes)?.let { StoredRunMedia(it, isVideo) }
    }

    fun copyImage(uri: Uri): String? {
        if (mediaMimeType(uri)?.startsWith("image/") != true || !ensureMediaDirectory()) return null
        return copyToLocalFile(
            uri,
            File(mediaDirectory, "${UUID.randomUUID()}.image"),
            MAX_RUN_IMAGE_SIZE_BYTES
        )
    }

    fun copyBitmap(bitmap: Bitmap): String? {
        if (!ensureMediaDirectory()) return null
        val file = File(mediaDirectory, "${UUID.randomUUID()}.image")
        return try {
            FileOutputStream(file).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                    throw IOException("Unable to encode local image")
                }
            }
            if (file.length() > MAX_RUN_IMAGE_SIZE_BYTES) throw IOException("Image exceeds local storage limit")
            file.name
        } catch (_: IOException) {
            file.delete()
            null
        } catch (_: SecurityException) {
            file.delete()
            null
        }
    }

    private fun mediaMimeType(uri: Uri): String? = if (uri.scheme == "content") {
        runCatching { contentResolver.getType(uri) }.getOrNull()
    } else {
        null
    }

    private fun ensureMediaDirectory(): Boolean =
        runCatching { mediaDirectory.exists() || mediaDirectory.mkdirs() }.getOrDefault(false)

    private fun copyToLocalFile(uri: Uri, file: File, maxSizeBytes: Long): String? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            input.use { source ->
                FileOutputStream(file).use { output -> source.copyToLimited(output, maxSizeBytes) }
            }
            file.name
        } catch (_: IOException) {
            file.delete()
            null
        } catch (_: SecurityException) {
            file.delete()
            null
        } catch (_: IllegalArgumentException) {
            file.delete()
            null
        } catch (_: UnsupportedOperationException) {
            file.delete()
            null
        }
    }

    fun delete(fileName: String) {
        if ((fileName.endsWith(".image") || fileName.endsWith(".video")) && File(fileName).name == fileName) {
            runCatching { File(mediaDirectory, fileName).delete() }
        }
    }

    fun deleteCapture(uri: Uri) {
        if (uri.scheme != "content" || uri.authority != captureAuthority) return
        val fileName = uri.lastPathSegment ?: return
        if (fileName.endsWith(".capture.jpg") && File(fileName).name == fileName) {
            runCatching { File(mediaDirectory, fileName).delete() }
        }
    }
}

internal fun InputStream.copyToLimited(output: OutputStream, maxBytes: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copiedBytes = 0L
    while (true) {
        val bytesRead = read(buffer)
        if (bytesRead < 0) return
        copiedBytes += bytesRead
        if (copiedBytes > maxBytes) throw IOException("Media exceeds local storage limit")
        output.write(buffer, 0, bytesRead)
    }
}
