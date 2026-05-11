package com.example.gymtimer2.data.repository

import com.example.gymtimer2.data.dao.SongDao
import com.example.gymtimer2.data.mapper.toDomain
import com.example.gymtimer2.data.mapper.toEntity
import com.example.gymtimer2.domain.model.SongModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedSongRepository(
    private val songDao: SongDao
) {
    val savedSongs: Flow<List<SongModel>> = songDao.getAllSongs().map { songs ->
        songs.map { it.toDomain() }
    }

    suspend fun saveSongs(songs: List<SongModel>) {
        songs.forEach { song ->
            songDao.insert(song.toEntity())
        }
    }
}

