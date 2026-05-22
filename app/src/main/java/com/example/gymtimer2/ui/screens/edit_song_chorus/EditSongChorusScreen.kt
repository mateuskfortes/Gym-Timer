package com.example.gymtimer2.ui.screens.edit_song_chorus

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.music.SongCover
import com.example.gymtimer2.ui.components.music.MusicSeekBar
import com.example.gymtimer2.util.formatMillisToMinSec
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import com.example.gymtimer2.ui.screens.edit_song_chorus.components.ChorusCard
import com.example.gymtimer2.ui.screens.edit_song_chorus.components.ChorusEditorOverlay

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
            playerManager = app.musicPlayerManager
        )
    )
    val playbackState by app.musicPlayerManager.playbackState.collectAsState()

    val chorusesFlow = remember(songToEdit.id) { viewModel.choruses(songToEdit.id) }
    val choruses by chorusesFlow.collectAsState(initial = emptyList())

    var chorusToEdit by remember(songToEdit.id) { mutableStateOf<ChorusModel?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

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
                        text = songToEdit.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = goBack) {
                    Text("Voltar")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongCover(songToEdit.uri)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = songToEdit.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = songToEdit.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = songToEdit.durationMs?.let { formatMillisToMinSec(it) } ?: "Duração indisponível",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (playbackState.uri == songToEdit.uri.toString() && playbackState.durationMs > 0) {
                MusicSeekBar(
                    playbackState = playbackState,
                    onSeek = viewModel::seekTo
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (playbackState.isPlaying && playbackState.uri == songToEdit.uri.toString()) {
                            viewModel.stopPlayback()
                        } else {
                            viewModel.playFullSong(songToEdit)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (playbackState.isPlaying && playbackState.uri == songToEdit.uri.toString()) {
                            "Parar música"
                        } else {
                            "Tocar música inteira"
                        }
                    )
                }
            }

            Button(
                onClick = { chorusToEdit = viewModel.createNewChorus(songToEdit) },
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
                            songDurationMs = songToEdit.durationMs,
                            songUri = songToEdit.uri.toString(),
                            playbackState = playbackState,
                            onPlay = { viewModel.playChorus(songToEdit, chorus) },
                            onStop = { viewModel.stopPlayback() },
                            onEdit = { chorusToEdit = chorus },
                            onDelete = {
                                viewModel.deleteChorus(chorus) {
                                    if (chorusToEdit?.id == chorus.id) {
                                        chorusToEdit = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (chorusToEdit != null) {
            ChorusEditorOverlay (
                chorus = chorusToEdit!!,
                song = songToEdit,
                playbackState = playbackState,
                onPreview = { startMs -> viewModel.playChorusPreview(songToEdit, startMs) },
                onStop = viewModel::stopPlayback,
                onCancel = { chorusToEdit = null },
                onSave = { updatedChorus ->
                    viewModel.saveChorus(
                        chorus = updatedChorus,
                        songDurationMs = songToEdit.durationMs,
                        onDone = { chorusToEdit = null }
                    )
                }
            )
        }
    }
}


