package com.example.gymtimer2.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.gymtimer2.domain.model.SongModel

class SongRepository(
    private val context: Context
) {
    fun getSongs(): List<SongModel> {
        val result = mutableListOf<SongModel>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Sem título"
                val artist = cursor.getString(artistColumn) ?: "Artista desconhecido"
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val durationMs = loadSongDuration(uri.toString())

                result.add(
                    SongModel(
                        id = id,
                        title = title,
                        artist = artist,
                        uri = uri,
                        durationMs = durationMs
                    )
                )
            }
        }

        return result
    }

    private fun loadSongDuration(uriString: String): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uriString.toUri())
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}