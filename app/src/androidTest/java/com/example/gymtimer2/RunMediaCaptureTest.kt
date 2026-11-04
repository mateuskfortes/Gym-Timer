package com.example.gymtimer2

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.repository.RunMediaStorage
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMediaCaptureTest {
    @Test
    fun selectedVideoIsCopiedToPrivateStorageAndCanBeDeleted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "run_photos").apply { mkdirs() }
        val source = File(directory, "${UUID.randomUUID()}.mp4").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val storage = RunMediaStorage(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.runphoto.provider",
            source
        )
        var copiedFileName: String? = null
        try {
            val storedMedia = storage.copyMedia(uri)
            assertNotNull(storedMedia)
            requireNotNull(storedMedia)
            copiedFileName = storedMedia.fileName
            val copy = File(directory, storedMedia.fileName)
            assertTrue(storedMedia.isVideo)
            assertTrue(copy.exists())
            assertEquals(source.readBytes().toList(), copy.readBytes().toList())
            storage.delete(storedMedia.fileName)
            assertFalse(copy.exists())
        } finally {
            copiedFileName?.let(storage::delete)
            source.delete()
        }
    }

    @Test
    fun croppedBitmapIsStoredAsAPrivateImageAndCanBeRemoved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = RunMediaStorage(context)
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        val fileName = storage.copyBitmap(bitmap)
        assertNotNull(fileName)
        requireNotNull(fileName)
        val file = File(context.filesDir, "run_photos/$fileName")

        try {
            assertTrue(file.exists())
            assertTrue(file.length() > 0L)
            storage.delete(fileName)
            assertFalse(file.exists())
        } finally {
            storage.delete(fileName)
            bitmap.recycle()
        }
    }

    @Test
    fun captureUriIsPrivateAndTemporaryFileCanBeRemoved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = RunMediaStorage(context)
        val uri = storage.createCaptureUri()
        assertNotNull(uri)
        requireNotNull(uri)

        try {
            assertEquals("${context.packageName}.runphoto.provider", uri.authority)
            context.contentResolver.openOutputStream(uri)?.use { it.write(byteArrayOf(1)) }
                ?: error("Capture URI must be writable")

            storage.deleteCapture(Uri.parse("content://untrusted/${uri.lastPathSegment}"))
            context.contentResolver.openInputStream(uri)?.close()

            storage.deleteCapture(uri)
            assertThrows(FileNotFoundException::class.java) {
                context.contentResolver.openInputStream(uri)
            }
        } finally {
            storage.deleteCapture(uri)
        }
    }
}
