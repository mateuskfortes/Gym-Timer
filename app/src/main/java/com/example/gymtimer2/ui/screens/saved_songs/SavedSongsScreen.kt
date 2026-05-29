package com.example.gymtimer2.ui.screens.saved_songs

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.music.MusicPermissionGate
import com.example.gymtimer2.ui.components.music.hasAudioPermission
import com.example.gymtimer2.ui.components.music.requiredAudioPermission
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
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
        onDispose {
            viewModel.stopPlayer()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopPlayer()
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val songs by viewModel.savedSongs.collectAsState(initial = emptyList())
    val playingSongId by viewModel.playingSongId.collectAsState()
    val playbackState by app.musicPlayerManager.playbackState.collectAsState()

    BackHandler { goBack() }

    Surface(modifier = modifier.fillMaxSize()) {
        MusicPermissionGate(
            hasPermission = hasPermission,
            onRequestPermission = { permissionLauncher.launch(requiredAudioPermission()) }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = onOpenSelection, modifier = Modifier.fillMaxWidth()) {
                    Text("Adicionar músicas")
                }

                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhuma música salva")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = songs,
                            key = { it.id }
                        ) { song ->
                            SongCard (
                                song = song,
                                isPlaying = playingSongId == song.id,
                                playbackState = if (playingSongId == song.id) playbackState else null,
                                onPlayClick = { viewModel.playSong(context, song) },
                                onStopClick = viewModel::stopPlayer,
                                onSeek = viewModel::seekTo,
                                onEdit = { onEditSong(song) },
                                onDelete = { viewModel.deleteSong(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

