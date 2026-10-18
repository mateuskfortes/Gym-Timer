package com.example.gymtimer2.ui.screens.edit_chorus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.AppTextField
import com.example.gymtimer2.ui.components.IconActionButton
import com.example.gymtimer2.ui.components.music.SongCover
import com.example.gymtimer2.ui.components.music.MusicSeekBar
import com.example.gymtimer2.util.formatMillisToMinSec
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.SecondaryButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymtimer2.ui.screens.edit_chorus.components.ChorusCard
import com.example.gymtimer2.ui.screens.edit_chorus.components.ChorusEditorOverlay

@Composable
fun EditSongChorusScreen(
    modifier: Modifier = Modifier,
    songToEdit: SongModel,
    goBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication
    val viewModel: EditSongChorusViewModel = viewModel(
        factory = EditSongChorusViewModel.factory(
            repository = app.container.workoutRepository,
            playerManager = app.musicPlayerManager,
            songToEdit = songToEdit
        )
    )

    val playbackState by app.musicPlayerManager.playbackState.collectAsStateWithLifecycle()
    val choruses by viewModel.choruses.collectAsStateWithLifecycle()
    val chorusToEdit by viewModel.chorusToEdit.collectAsStateWithLifecycle()
    val song by viewModel.songToEditState.collectAsStateWithLifecycle()
    var showRenameDialog by remember { mutableStateOf(false) }
    var songNameDraft by remember(song.id) { mutableStateOf(song.title) }
    var isRenamingSong by remember { mutableStateOf(false) }
    var renameSongFailed by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRenamingSong) showRenameDialog = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = { Text(stringResource(R.string.edit_song_name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(
                        value = songNameDraft,
                        onValueChange = {
                            songNameDraft = it
                            renameSongFailed = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.song_name_label)) },
                        singleLine = true,
                        outlined = true
                    )
                    if (renameSongFailed) {
                        Text(
                            text = stringResource(R.string.edit_song_name_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    onClick = {
                        isRenamingSong = true
                        renameSongFailed = false
                        viewModel.renameSong(songNameDraft) { renamed ->
                            isRenamingSong = false
                            if (renamed) {
                                showRenameDialog = false
                            } else {
                                renameSongFailed = true
                            }
                        }
                    },
                    enabled = songNameDraft.isNotBlank() && !isRenamingSong
                ) {
                    Text(stringResource(R.string.save_button))
                }
            },
            dismissButton = {
                SecondaryButton(
                    onClick = { showRenameDialog = false },
                    enabled = !isRenamingSong
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    BackHandler { goBack() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ... existing header and controls code ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Editar Refrão",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SecondaryButton(onClick = goBack) {
                    Text("Voltar")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongCover(song, app.container.songArtworkRepository)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconActionButton(
                            onClick = {
                                songNameDraft = song.title
                                renameSongFailed = false
                                showRenameDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit_song_name)
                            )
                        }
                    }
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = song.durationMs?.let { formatMillisToMinSec(it) } ?: "Duração indisponível",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (playbackState.uri == song.uriString && playbackState.durationMs > 0) {
                MusicSeekBar(
                    playbackState = playbackState,
                    onSeek = viewModel::seekTo
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    onClick = {
                        if (playbackState.isPlaying && playbackState.uri == song.uriString) {
                            viewModel.stopPlayback()
                        } else {
                            viewModel.play()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (playbackState.isPlaying && playbackState.uri == song.uriString) {
                            "Parar música"
                        } else {
                            "Tocar música inteira"
                        }
                    )
                }
            }

            PrimaryButton(
                onClick = { viewModel.newChorus() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Adicionar refrão")
            }

            if (choruses.isEmpty() && chorusToEdit == null) {
                Text(
                    text = "Nenhum refrão cadastrado ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Only show saved choruses (id != 0)
                    items(choruses.filter { it.id != 0L }, key = { it.id }) { chorus ->
                        ChorusCard (
                            chorus = chorus,
                            song = song,
                            playbackState = playbackState,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }

        if (chorusToEdit != null) {
            ChorusEditorOverlay (
                chorus = chorusToEdit!!,
                song = song,
                playbackState = playbackState,
                viewModel = viewModel,
                artworkRepository = app.container.songArtworkRepository,
            )
        }
    }
}


