package com.example.gymtimer2.ui.screens.saved_songs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.IconActionButton
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.ScreenTitle
import com.example.gymtimer2.ui.components.music.MusicPermissionGate
import com.example.gymtimer2.ui.components.music.deleteSongDialog
import com.example.gymtimer2.ui.components.music.rememberAudioPermissionRequest
import com.example.gymtimer2.util.hasAudioPermission
import com.example.gymtimer2.ui.screens.saved_songs.components.SongCard

@Composable
fun SavedSongsScreen(
    modifier: Modifier = Modifier,
    onOpenSelection: () -> Unit = {},
    onEditSong: (SongModel) -> Unit = {},
    goBack: () -> Unit = { }
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication
    var hasPermission by remember { mutableStateOf(hasAudioPermission(context)) }

    val requestAudioPermission = rememberAudioPermissionRequest {
        hasPermission = hasAudioPermission(context)
    }

    LaunchedEffect(Unit) {
        hasPermission = hasAudioPermission(context)
    }

    val viewModel: SavedSongsViewModel = viewModel(
        factory = SavedSongsViewModel.factory(
            repository = app.container.workoutRepository,
            playerManager = app.musicPlayerManager
        )
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopPlayer()
                }
                Lifecycle.Event.ON_RESUME -> {
                    hasPermission = hasAudioPermission(context)
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPlayer()
        }
    }

    val songs by viewModel.savedSongs.collectAsState(initial = emptyList())
    val playbackState by app.musicPlayerManager.playbackState.collectAsState()

    BackHandler { goBack() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        MusicPermissionGate(
            hasPermission = hasPermission,
            onRequestPermission = { requestAudioPermission {} }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconActionButton(onClick = goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_music_categories)
                        )
                    }
                    ScreenTitle(
                        text = stringResource(R.string.music_library_title),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(onClick = onOpenSelection, modifier = Modifier.fillMaxWidth()) {
                    Text("Adicionar músicas")
                }

                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhuma música salva")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = songs,
                            key = { it.id }
                        ) { song ->
                            SongCard (
                                song = song,
                                isPlaying = playbackState.isPlaying && playbackState.uri == song.uriString,
                                playbackState = playbackState.takeIf { it.uri == song.uriString },
                                onPlayClick = {
                                    requestAudioPermission { viewModel.playSong(context, song) }
                                },
                                onStopClick = viewModel::stopPlayer,
                                onSeek = viewModel::seekTo,
                                onEdit = { onEditSong(song) },
                                onDelete = {
                                    deleteSongDialog(context, song){
                                        viewModel.deleteSong(song)
                                    }
                                },
                                artworkRepository = app.container.songArtworkRepository
                            )
                        }
                    }
                }
            }
        }
    }
}

