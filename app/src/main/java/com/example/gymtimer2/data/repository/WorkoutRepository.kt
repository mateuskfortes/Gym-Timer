package com.example.gymtimer2.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.gymtimer2.data.dao.ChorusDao
import com.example.gymtimer2.data.dao.ExerciseChorusDao
import com.example.gymtimer2.data.dao.ExerciseDao
import com.example.gymtimer2.data.dao.ExerciseScheduleDao
import com.example.gymtimer2.data.dao.SongDao
import com.example.gymtimer2.data.dao.WorkoutDayDao
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.entity.ExerciseChorusEntity
import com.example.gymtimer2.data.entity.ExerciseScheduleEntity
import com.example.gymtimer2.data.entity.WorkoutDayEntity
import com.example.gymtimer2.data.mapper.toEntity
import com.example.gymtimer2.data.mapper.toModel
import com.example.gymtimer2.data.relation.ExerciseWithChorusesRelation
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.ChorusWithSongModel
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.ExerciseWithChorusesModel
import com.example.gymtimer2.domain.model.ScheduledExerciseModel
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.domain.model.WorkoutDayModel
import com.example.gymtimer2.domain.model.WorkoutScheduleError
import com.example.gymtimer2.domain.model.WorkoutScheduleException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val songDao: SongDao,
    private val chorusDao: ChorusDao,
    private val exerciseChorusDao: ExerciseChorusDao,
    private val workoutDayDao: WorkoutDayDao,
    private val exerciseScheduleDao: ExerciseScheduleDao,
    private val database: WorkoutDatabase
) {

    val allExercises: Flow<List<ExerciseModel>> =
        exerciseDao.getAllExercises().map { list ->
            list.map { it.toModel() }
        }

    val workoutDays: Flow<List<WorkoutDayModel>> = workoutDayDao.observeDays().map { days ->
        days.map { WorkoutDayModel(it.id, it.name) }
    }

    val scheduledExercises: Flow<List<ScheduledExerciseModel>> =
        exerciseScheduleDao.observeScheduledExercises().map { rows ->
            rows.map { row -> ScheduledExerciseModel(row.exercise.toModel(), row.dayId, row.sortOrder) }
        }

    fun scheduledExercisesForDay(dayId: Long): Flow<List<ScheduledExerciseModel>> =
        exerciseScheduleDao.observeScheduledExercisesForDay(dayId).map { rows ->
            rows.map { row -> ScheduledExerciseModel(row.exercise.toModel(), row.dayId, row.sortOrder) }
        }

    val savedSongs: Flow<List<SongModel>> = songDao.getAllSongs().map { songs ->
        songs.map { it.toModel() }
    }

    fun getSavedSongsIds(): Flow<List<Long>> {
        return savedSongs.map{ songs ->
            songs.map { it.id }
        }
    }

    suspend fun insertExercise(exercise: ExerciseModel, dayId: Long): Long = database.withTransaction {
        requireSchedule(workoutDayDao.getById(dayId) != null, WorkoutScheduleError.DAY_NOT_FOUND)
        val position = exerciseScheduleDao.countInDay(dayId)
        val id = exerciseDao.insert(exercise.toEntity())
        exerciseScheduleDao.insert(ExerciseScheduleEntity(id.toInt(), dayId, position))
        id
    }

    suspend fun updateExercise(exercise: ExerciseModel) {
        exerciseDao.update(exercise.toEntity())
    }

    suspend fun deleteExercise(exercise: ExerciseModel) = database.withTransaction {
        val dayId = exerciseScheduleDao.dayIdForExercise(exercise.id)
        exerciseDao.delete(exercise.toEntity())
        if (dayId != null) normalizeOrder(dayId)
    }

    suspend fun createDay(name: String): Long = database.withTransaction {
        val cleanName = name.trim()
        requireSchedule(cleanName.isNotEmpty(), WorkoutScheduleError.DAY_NAME_REQUIRED)
        requireSchedule(workoutDayDao.countByName(cleanName) == 0, WorkoutScheduleError.DUPLICATE_DAY_NAME)
        workoutDayDao.insert(WorkoutDayEntity(name = cleanName))
    }

    suspend fun renameDay(dayId: Long, name: String) = database.withTransaction {
        val day = workoutDayDao.getById(dayId)
            ?: throw WorkoutScheduleException(WorkoutScheduleError.DAY_NOT_FOUND)
        val cleanName = name.trim()
        requireSchedule(cleanName.isNotEmpty(), WorkoutScheduleError.DAY_NAME_REQUIRED)
        if (!day.name.equals(cleanName, ignoreCase = true)) {
            requireSchedule(workoutDayDao.countByName(cleanName) == 0, WorkoutScheduleError.DUPLICATE_DAY_NAME)
        }
        workoutDayDao.update(day.copy(name = cleanName))
    }

    suspend fun deleteDay(dayId: Long) = database.withTransaction {
        val day = workoutDayDao.getById(dayId) ?: return@withTransaction
        requireSchedule(workoutDayDao.count() > 1, WorkoutScheduleError.LAST_DAY)
        requireSchedule(exerciseScheduleDao.countInDay(dayId) == 0, WorkoutScheduleError.DAY_NOT_EMPTY)
        workoutDayDao.delete(day)
    }

    suspend fun moveExercise(exerciseId: Int, targetDayId: Long) = database.withTransaction {
        requireSchedule(workoutDayDao.getById(targetDayId) != null, WorkoutScheduleError.DAY_NOT_FOUND)
        val sourceDayId = exerciseScheduleDao.dayIdForExercise(exerciseId)
            ?: throw WorkoutScheduleException(WorkoutScheduleError.EXERCISE_NOT_FOUND)
        if (sourceDayId == targetDayId) return@withTransaction
        exerciseScheduleDao.move(exerciseId, targetDayId, exerciseScheduleDao.countInDay(targetDayId))
        normalizeOrder(sourceDayId)
    }

    suspend fun reorderExercises(dayId: Long, orderedIds: List<Int>) = database.withTransaction {
        val currentIds = exerciseScheduleDao.orderedExerciseIds(dayId)
        requireSchedule(
            currentIds.size == orderedIds.size && currentIds.toSet() == orderedIds.toSet(),
            WorkoutScheduleError.INVALID_ORDER
        )
        orderedIds.forEachIndexed { index, exerciseId ->
            exerciseScheduleDao.updateOrder(exerciseId, index)
        }
    }

    suspend fun updateExerciseWeight(exerciseId: Int, weight: Int) {
        requireSchedule(weight >= 0, WorkoutScheduleError.INVALID_WEIGHT)
        requireSchedule(exerciseDao.updateWeight(exerciseId, weight) == 1, WorkoutScheduleError.EXERCISE_NOT_FOUND)
    }

    suspend fun orderedExerciseIdsFrom(exerciseId: Int): List<Int> = database.withTransaction {
        val dayId = exerciseScheduleDao.dayIdForExercise(exerciseId) ?: return@withTransaction emptyList()
        exerciseScheduleDao.orderedExerciseIds(dayId).dropWhile { it != exerciseId }
    }

    private suspend fun normalizeOrder(dayId: Long) {
        exerciseScheduleDao.orderedExerciseIds(dayId).forEachIndexed { index, exerciseId ->
            exerciseScheduleDao.updateOrder(exerciseId, index)
        }
    }

    private fun requireSchedule(condition: Boolean, error: WorkoutScheduleError) {
        if (!condition) throw WorkoutScheduleException(error)
    }

    // Returns a list of songs available on the device, not saved in the app's database
    fun getSongs(): List<SongModel> {
        val result = mutableListOf<SongModel>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
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
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Sem título"
                val artist = cursor.getString(artistColumn) ?: "Artista desconhecido"
                val albumId = if (cursor.isNull(albumIdColumn)) null else {
                    cursor.getLong(albumIdColumn).takeIf { it > 0L }
                }
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val durationMs = if (cursor.isNull(durationColumn)) null else cursor.getLong(durationColumn)

                result.add(
                    SongModel(
                        id = id,
                        title = title,
                        artist = artist,
                        uriString = uri.toString(),
                        durationMs = durationMs,
                        albumId = albumId
                    )
                )
            }
        }

        return result
    }

    suspend fun saveSongs(songs: List<SongModel>) {
        songs.forEach { song ->
            songDao.insert(song.toEntity())
        }
    }

    suspend fun deleteSong(song: SongModel) {
        songDao.delete(song.toEntity())
    }

    suspend fun updateSongDetails(songId: Long, title: String, artist: String): Boolean {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty())
        return songDao.updateDetails(songId, normalizedTitle, artist.trim()) == 1
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

    suspend fun replaceChorusesForExercise(exerciseId: Int, chorusIds: Set<Long>) = database.withTransaction {
        exerciseChorusDao.deleteByExerciseId(exerciseId)
        exerciseChorusDao.insertAll(
            chorusIds.map { chorusId ->
                ExerciseChorusEntity(exerciseId = exerciseId, chorusId = chorusId)
            }
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

    fun getExerciseWithChorusesByExerciseId(exerciseId: Int): Flow<ExerciseWithChorusesModel?> {
        return chorusDao.getExerciseWithChorusesByExerciseId(exerciseId).map { it?.toModel() }
    }
}
