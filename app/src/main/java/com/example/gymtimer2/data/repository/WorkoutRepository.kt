package com.example.gymtimer2.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.gymtimer2.data.dao.ChorusDao
import com.example.gymtimer2.data.dao.ExerciseChorusDao
import com.example.gymtimer2.data.dao.ExerciseDao
import com.example.gymtimer2.data.dao.SongDao
import com.example.gymtimer2.data.entity.ExerciseChorusEntity
import com.example.gymtimer2.data.mapper.toEntity
import com.example.gymtimer2.data.mapper.toModel
import com.example.gymtimer2.data.relation.ExerciseWithChorusesRelation
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.ChorusWithSongModel
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.ExerciseWithChorusesModel
import com.example.gymtimer2.domain.model.SongModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val songDao: SongDao,
    private val chorusDao: ChorusDao,
    private val exerciseChorusDao: ExerciseChorusDao
) {

    val allExercises: Flow<List<ExerciseModel>> =
        exerciseDao.getAllExercises().map { list ->
            list.map { it.toModel() }
        }

    val savedSongs: Flow<List<SongModel>> = songDao.getAllSongs().map { songs ->
        songs.map { it.toModel() }
    }

    fun getSavedSongsIds(): Flow<List<Long>> {
        return savedSongs.map{ songs ->
            songs.map { it.id }
        }
    }

    suspend fun insertExercise(exercise: ExerciseModel): Long {
        return exerciseDao.insert(exercise.toEntity())
    }

    suspend fun updateExercise(exercise: ExerciseModel) {
        exerciseDao.update(exercise.toEntity())
    }

    suspend fun deleteExercise(exercise: ExerciseModel) {
        exerciseDao.delete(exercise.toEntity())
    }

    // Returns a list of songs available on the device, not saved in the app's database
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
                        uriString = uri.toString(),
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

    suspend fun saveSongs(songs: List<SongModel>) {
        songs.forEach { song ->
            songDao.insert(song.toEntity())
        }
    }

    suspend fun deleteSong(song: SongModel) {
        songDao.delete(song.toEntity())
    }

    fun getChorusesBySongId(songId: Long): Flow<List<ChorusModel>> {
        return chorusDao.getChorusesBySongId(songId).map { choruses ->
            choruses.map { it.toModel() }
        }
    }

    fun getAllChoruses(): Flow<List<ChorusModel>> {
        return chorusDao.getAllChoruses().map { choruses ->
            choruses.map { it.toModel() }
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

    suspend fun addChorusToExercise(exerciseId: Int, chorusId: Long) {
        exerciseChorusDao.insert(
            ExerciseChorusEntity(
                exerciseId = exerciseId,
                chorusId = chorusId
            )
        )
    }

    suspend fun removeChorusFromExercise(exerciseId: Int, chorusId: Long) {
        exerciseChorusDao.deleteByIds(exerciseId, chorusId)
    }

    fun getChorusesByExerciseId(exerciseId: Int): Flow<List<ChorusModel>> {
        return exerciseChorusDao.getChorusesByExerciseId(exerciseId).map { choruses ->
            choruses.map { it.toModel() }
        }
    }

    fun getChorusesWithSongsByExerciseId(exerciseId: Int): Flow<List<ChorusWithSongModel>> {
        return chorusDao.getChorusesWithSongsByExerciseId(exerciseId).map { list ->
            list.map { it.toModel() }
        }
    }

    fun getExerciseWithChorusesByExerciseId(exerciseId: Int): Flow<ExerciseWithChorusesModel> {
        return chorusDao.getExerciseWithChorusesByExerciseId(exerciseId).map { it.toModel() }
    }
}