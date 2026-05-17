package com.example.gymtimer2.ui.screens.local_songs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.ui.components.music.MusicPermissionGate
import com.example.gymtimer2.ui.screens.local_songs.components.SongCard
import com.example.gymtimer2.ui.components.music.requiredAudioPermission

@Composable
fun LocalSongsScreen(
    modifier: Modifier = Modifier,
    onOpenSavedSongs: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication

    val viewModel: LocalSongsViewModel = viewModel(
        factory = LocalSongsViewModel.factory(
            repository = app.container.songRepository,
            savedSongRepository = app.container.savedSongRepository,
            playerManager = app.musicPlayerManager
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val playbackState by app.musicPlayerManager.playbackState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkAudioPermission(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
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
            viewModel.stopPlayer()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAudioPermission(context)
    }

    Surface(modifier = modifier.fillMaxSize()) {
        MusicPermissionGate(
            hasPermission = uiState.hasPermission,
            onRequestPermission = { permissionLauncher.launch(requiredAudioPermission()) }
        ) {
            Scaffold(
                bottomBar = {
                    Button(
                        onClick = viewModel::saveSelectedSongs,
                        enabled = uiState.selectedSongIds.isNotEmpty() && !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(if (uiState.isSaving) "Salvando..." else "Save Selection")
                    }
                }
            ) { innerPadding ->
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.error != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(uiState.error ?: "Erro")
                        }
                    }

                    uiState.songs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhuma música encontrada")
                        }
                    }

                    else -> {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            Button(onClick = onOpenSavedSongs, modifier = Modifier.fillMaxWidth()) {
                                Text("View saved songs")
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.songs,
                                    key = { it.id }
                                ) { song ->
                                    SongCard(
                                        song = song,
                                        isPlaying = uiState.playingSongId == song.id,
                                        selected = song.id in uiState.selectedSongIds,
                                        onSelectionChange = { viewModel.toggleSongSelection(song.id) },
                                        onPlayClick = { viewModel.playSong(context, song) },
                                        onStopClick = viewModel::stopPlayer,
                                        playbackState = if (uiState.playingSongId == song.id) playbackState else null,
                                        onSeek = viewModel::seekTo,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
