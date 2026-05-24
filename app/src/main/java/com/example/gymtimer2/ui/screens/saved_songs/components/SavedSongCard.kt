package com.example.gymtimer2.ui.screens.saved_songs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.ui.components.music.MusicSeekBar
import com.example.gymtimer2.ui.components.music.SongCover

@Composable
fun SongCard(
    song: SongModel,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    selected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    playbackState: MusicPlaybackState? = null,
    onSeek: (Int) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    Checkbox(
                        checked = selected,
                        onCheckedChange = onSelectionChange
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                SongCover(song.uriString)

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column {
                    IconButton (onClick = {
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

                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar música"
                            )
                        }
                    }
                    if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Excluir música"
                        )
                    }
                }
                }
            }

            if (isPlaying && playbackState != null) {
                MusicSeekBar(
                    playbackState = playbackState,
                    onSeek = onSeek
                )
            }
        }
    }
}