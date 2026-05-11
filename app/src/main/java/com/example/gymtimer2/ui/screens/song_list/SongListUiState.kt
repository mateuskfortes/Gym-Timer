package com.example.gymtimer2.ui.screens.song_list

import com.example.gymtimer2.domain.model.SongModel

data class SongListUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasPermission: Boolean = false,
    val songs: List<SongModel> = emptyList(),
    val selectedSongIds: Set<Long> = emptySet(),
    val playingSongId: Long? = null,
    val error: String? = null
)