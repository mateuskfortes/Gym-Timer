package com.example.gymtimer2

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.data.database.WorkoutDatabase
import com.example.gymtimer2.data.repository.WorkoutRepository
import com.example.gymtimer2.domain.model.SongModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongRenameTest {
    @Test
    fun renameSongPersistsTrimmedTitle() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = WorkoutRepository(
                context, database.exerciseDao(), database.songDao(), database.chorusDao(),
                database.exerciseChorusDao(), database.workoutDayDao(),
                database.exerciseScheduleDao(), database
            )
            repository.saveSongs(
                listOf(SongModel(id = 7, title = "Original", artist = "Artist", uriString = "content://song/7"))
            )

            assertTrue(repository.renameSong(7, "  Nome atualizado  "))
            assertEquals("Nome atualizado", repository.savedSongs.first().single().title)
        } finally {
            database.close()
        }
    }
}
