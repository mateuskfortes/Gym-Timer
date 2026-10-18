package com.example.gymtimer2.ui.screens.local_songs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.data.repository.SongArtworkRepository
import com.example.gymtimer2.ui.components.music.MusicSeekBar
import com.example.gymtimer2.ui.components.music.SongCover
import com.example.gymtimer2.ui.components.softCardShadow
import com.example.gymtimer2.ui.components.IconActionButton
import com.example.gymtimer2.ui.components.AppCheckbox
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SongCard(
    song: SongModel,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    selected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    playbackStateFlow: StateFlow<MusicPlaybackState>? = null,
    onSeek: (Int) -> Unit,
    artworkRepository: SongArtworkRepository,
    shouldLoadArtwork: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth().softCardShadow(MaterialTheme.shapes.medium, 7.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onSelectionChange != null) {
                    AppCheckbox(
                        checked = selected,
                        onCheckedChange = onSelectionChange
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                SongCover(
                    song = song,
                    artworkRepository = artworkRepository,
                    shouldLoadArtwork = shouldLoadArtwork
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                IconActionButton (onClick = {
                    if (isPlaying) {
                        onStopClick()
                    } else {
                        onPlayClick()
                    }

                }) {
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pausar música"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Tocar música"
                        )
                    }
                }
            }

            if (isPlaying && playbackStateFlow != null) {
                val playbackState by playbackStateFlow.collectAsState()
                MusicSeekBar(
                    playbackState = playbackState,
                    onSeek = onSeek
                )
            }
        }
    }
}
