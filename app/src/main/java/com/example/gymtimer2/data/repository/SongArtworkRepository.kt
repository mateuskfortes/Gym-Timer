package com.example.gymtimer2.data.repository

import android.content.Context
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.LruCache
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import android.provider.MediaStore
import com.example.gymtimer2.domain.model.SongModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val PREVIEW_ARTWORK_SIZE_PX = 96
private const val FULL_ARTWORK_SIZE_PX = 256
private const val ARTWORK_CACHE_SIZE_KB = 8 * 1024
private const val MISSING_ARTWORK_CACHE_SIZE = 512
private const val MAX_CONCURRENT_ARTWORK_LOADS = 4
private const val MAX_CONCURRENT_PREVIEW_LOADS = 2

class SongArtworkRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val fullArtworkLoadLimit = Semaphore(MAX_CONCURRENT_ARTWORK_LOADS)
    private val previewArtworkLoadLimit = Semaphore(MAX_CONCURRENT_PREVIEW_LOADS)

    private val artworkCache = object : LruCache<String, Bitmap>(ARTWORK_CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }
    private val missingArtworkCache = LruCache<String, Boolean>(MISSING_ARTWORK_CACHE_SIZE)

    fun getBestCachedArtwork(song: SongModel): Bitmap? =
        getCachedArtwork(song, FULL_ARTWORK_SIZE_PX)
            ?: getCachedArtwork(song, PREVIEW_ARTWORK_SIZE_PX)

    fun hasFullArtwork(song: SongModel): Boolean =
        getCachedArtwork(song, FULL_ARTWORK_SIZE_PX) != null

    suspend fun loadPreviewArtwork(song: SongModel): Bitmap? =
        loadArtwork(song, PREVIEW_ARTWORK_SIZE_PX, previewArtworkLoadLimit)

    suspend fun loadFullArtwork(song: SongModel): Bitmap? =
        loadArtwork(song, FULL_ARTWORK_SIZE_PX, fullArtworkLoadLimit)

    private suspend fun loadArtwork(
        song: SongModel,
        targetSizePx: Int,
        loadLimit: Semaphore
    ): Bitmap? {
        val artworkKey = cacheKey(song)
        val sizedArtworkKey = cacheKey(song, targetSizePx)
        getCachedArtwork(song, targetSizePx)?.let { return it }
        if (hasNoArtwork(artworkKey)) return null

        return withContext(Dispatchers.IO) {
            val coroutineContext = currentCoroutineContext()
            loadLimit.withPermit {
                getCachedArtwork(song, targetSizePx)?.let { return@withPermit it }
                if (hasNoArtwork(artworkKey)) return@withPermit null

                coroutineContext.ensureActive()
                val result = extractArtwork(song, targetSizePx)
                coroutineContext.ensureActive()
                cacheResult(sizedArtworkKey, artworkKey, result)
            }
        }
    }

    private fun cacheKey(song: SongModel, targetSizePx: Int? = null): String {
        val artworkKey = song.albumId?.let { "album:$it" } ?: song.uriString
        return targetSizePx?.let { "$artworkKey:$it" } ?: artworkKey
    }

    private fun getCachedArtwork(song: SongModel, targetSizePx: Int): Bitmap? =
        artworkCache.get(cacheKey(song, targetSizePx))

    private fun hasNoArtwork(uri: String): Boolean = missingArtworkCache.get(uri) == true

    private fun cacheResult(
        sizedArtworkKey: String,
        artworkKey: String,
        result: ArtworkResult
    ): Bitmap? = when (result) {
        is ArtworkResult.Found -> result.bitmap.also { artworkCache.put(sizedArtworkKey, it) }
        ArtworkResult.Missing -> {
            missingArtworkCache.put(artworkKey, true)
            null
        }
        ArtworkResult.Unavailable -> null
    }

    private suspend fun extractArtwork(song: SongModel, targetSizePx: Int): ArtworkResult {
        val mediaUri = song.uriString.toUri()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadPlatformThumbnail(albumThumbnailUri(song) ?: mediaUri, targetSizePx)?.let {
                return ArtworkResult.Found(it)
            }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(applicationContext, mediaUri)
            val bytes = retriever.embeddedPicture ?: return ArtworkResult.Missing
            decodeArtwork(bytes, targetSizePx)?.let(ArtworkResult::Found) ?: ArtworkResult.Missing
        } catch (_: Exception) {
            ArtworkResult.Unavailable
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun albumThumbnailUri(song: SongModel) = song.albumId?.let { albumId ->
        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun loadPlatformThumbnail(uri: Uri, targetSizePx: Int): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }

            try {
                val thumbnail = applicationContext.contentResolver.loadThumbnail(
                    uri,
                    Size(targetSizePx, targetSizePx),
                    cancellationSignal
                )
                if (continuation.isActive) continuation.resume(thumbnail)
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private fun decodeArtwork(bytes: ByteArray, targetSizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > targetSizePx) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private sealed interface ArtworkResult {
        data class Found(val bitmap: Bitmap) : ArtworkResult
        object Missing : ArtworkResult
        object Unavailable : ArtworkResult
    }
}
