package com.example.gymtimer2.ui.screens.song_list

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.data.repository.SongRepository
import com.example.gymtimer2.player.MusicPlayerManager
import com.example.gymtimer2.ui.screens.song_list.components.SongPermissionContent
import com.example.gymtimer2.ui.screens.song_list.components.SongCard

@Composable
fun MusicListScreen(
    onOpenOverlayClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current

    val viewModel: MusicListViewModel = viewModel(
        factory = MusicListViewModel.factory(
            repository = SongRepository(context.applicationContext),
            playerManager = MusicPlayerManager(context.applicationContext)
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkAudioPermission(context)
    }

    LaunchedEffect(Unit) {
        viewModel.checkAudioPermission(context)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            !uiState.hasPermission -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SongPermissionContent(
                        onRequestPermission = { permissionLauncher.launch(permission) }
                    )
                }
            }

            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.error ?: "Erro")
                }
            }

            uiState.songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma música encontrada")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.songs,
                        key = { it.id }
                    ) { song ->
                        SongCard (
                            song = song,
                            isPlaying = uiState.playingSongId == song.id,
                            onPlayClick = { viewModel.playSong(song) },
                            onOpenOverlayClick = { onOpenOverlayClick(80L) }
                        )
                    }
                }
            }
        }
    }
}

