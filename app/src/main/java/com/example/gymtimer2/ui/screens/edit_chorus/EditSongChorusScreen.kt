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
import com.example.gymtimer2.ui.components.ScreenTitle
import com.example.gymtimer2.ui.components.music.SongCover
import com.example.gymtimer2.ui.components.music.SongPlaybackControls
import com.example.gymtimer2.ui.components.music.rememberAudioPermissionRequest
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
    val runWithAudioPermission = rememberAudioPermissionRequest()

    val playbackState by app.musicPlayerManager.playbackState.collectAsStateWithLifecycle()
    val choruses by viewModel.choruses.collectAsStateWithLifecycle()
    val chorusToEdit by viewModel.chorusToEdit.collectAsStateWithLifecycle()
    val song by viewModel.songToEditState.collectAsStateWithLifecycle()
    var showSongDetailsDialog by remember { mutableStateOf(false) }
    var songTitleDraft by remember(song.id) { mutableStateOf(song.title) }
    var artistDraft by remember(song.id) { mutableStateOf(song.artist) }
    var isUpdatingSongDetails by remember { mutableStateOf(false) }
    var songDetailsUpdateFailed by remember { mutableStateOf(false) }

    if (showSongDetailsDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isUpdatingSongDetails) showSongDetailsDialog = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = { Text(stringResource(R.string.edit_song_details_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(
                        value = songTitleDraft,
                        onValueChange = {
                            songTitleDraft = it
                            songDetailsUpdateFailed = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.song_name_label)) },
                        singleLine = true,
                        outlined = true
                    )
                    AppTextField(
                        value = artistDraft,
                        onValueChange = {
                            artistDraft = it
                            songDetailsUpdateFailed = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.song_artist_label)) },
                        singleLine = true,
                        outlined = true
                    )
                    if (songDetailsUpdateFailed) {
                        Text(
                            text = stringResource(R.string.edit_song_details_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    onClick = {
                        isUpdatingSongDetails = true
                        songDetailsUpdateFailed = false
                        viewModel.updateSongDetails(songTitleDraft, artistDraft) { updated ->
                            isUpdatingSongDetails = false
                            if (updated) {
                                showSongDetailsDialog = false
                            } else {
                                songDetailsUpdateFailed = true
                            }
                        }
                    },
                    enabled = songTitleDraft.isNotBlank() && !isUpdatingSongDetails
                ) {
                    Text(stringResource(R.string.save_button))
                }
            },
            dismissButton = {
                SecondaryButton(
                    onClick = { showSongDetailsDialog = false },
                    enabled = !isUpdatingSongDetails
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
                    ScreenTitle("Editar refrão")
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
                                songTitleDraft = song.title
                                artistDraft = song.artist
                                songDetailsUpdateFailed = false
                                showSongDetailsDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit_song_details)
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

            SongPlaybackControls(
                playbackState = playbackState,
                songUri = song.uriString,
                onPlay = { runWithAudioPermission { viewModel.play() } },
                onStop = viewModel::stopPlayback,
                onSeek = viewModel::seekTo,
            )

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
                            onPlay = {
                                runWithAudioPermission { viewModel.playChorus(chorus) }
                            },
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
                onPlay = {
                    runWithAudioPermission(viewModel::playCurrentChorusPreview)
                },
            )
        }
    }
}


