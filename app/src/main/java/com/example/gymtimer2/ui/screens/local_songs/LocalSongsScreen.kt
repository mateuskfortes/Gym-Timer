package com.example.gymtimer2.ui.screens.local_songs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.components.LoadingScreen
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.SecondaryButton
import com.example.gymtimer2.ui.components.music.MusicPermissionGate
import com.example.gymtimer2.ui.components.music.rememberAudioPermissionRequest
import com.example.gymtimer2.ui.screens.local_songs.components.SongCard

@Composable
fun LocalSongsScreen(
    modifier: Modifier = Modifier,
    goBack: () -> Unit = { }
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication

    val viewModel: LocalSongsViewModel = viewModel(
        factory = LocalSongsViewModel.factory(
            repository = app.container.workoutRepository,
            playerManager = app.musicPlayerManager
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current.density
    val scrollResistance = remember(density) { ScrollResistanceConnection(density) }
    val listState = rememberLazyListState()
    val isScrolling by remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }

    val requestAudioPermission = rememberAudioPermissionRequest {
        viewModel.checkAudioPermission(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopPlayer()
                }
                Lifecycle.Event.ON_RESUME -> viewModel.checkAudioPermission(context)
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPlayer()
            viewModel.stopObservingSongs()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAudioPermission(context)
    }

    BackHandler { goBack() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        MusicPermissionGate(
            hasPermission = uiState.hasPermission,
            onRequestPermission = { requestAudioPermission {} }
        ) {
            Scaffold(
                bottomBar = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(
                            onClick = goBack,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Voltar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        PrimaryButton (
                            onClick = {viewModel.saveSelectedSongs(goBack)},
                            enabled = uiState.selectedSongIds.isNotEmpty() && !uiState.isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(stringResource(if (uiState.isSaving) R.string.saving_selection else R.string.save_selection))
                        }
                    }
                }
            ) { innerPadding ->
                when {
                    uiState.isLoading -> {
                        LoadingScreen(modifier = Modifier.padding(innerPadding))
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .nestedScroll(scrollResistance)
                                .padding(
                                    PaddingValues(
                                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                                        bottom = innerPadding.calculateBottomPadding(),
                                        top = 0.dp
                                    )
                                )
                                .fillMaxSize(),
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
                                    onPlayClick = {
                                        requestAudioPermission { viewModel.playSong(context, song) }
                                    },
                                    onStopClick = viewModel::stopPlayer,
                                    playbackStateFlow = if (uiState.playingSongId == song.id) {
                                        app.musicPlayerManager.playbackState
                                    } else null,
                                    onSeek = viewModel::seekTo,
                                    artworkRepository = app.container.songArtworkRepository,
                                    shouldLoadArtwork = !isScrolling,
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}
