package com.example.gymtimer2.ui.screens.edit_exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.util.formatMillisToMinSec

@Composable
fun ExerciseChorusCard(
    chorus: ChorusModel,
    song: SongModel,
    playbackState: MusicPlaybackState,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRemove: () -> Unit
) {
    val isChorusPlaying = playbackState.isPlaying && playbackState.uri == song.uriString

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = chorus.name.ifBlank { "Refrão" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Começa em ${formatMillisToMinSec(chorus.startMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play/Stop button
                IconButton(
                    onClick = {
                        if (isChorusPlaying) {
                            onStop()
                        } else {
                            onPlay()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isChorusPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isChorusPlaying) "Parar refrão" else "Tocar refrão"
                    )
                }

                // Remove button
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remover refrão"
                    )
                }
            }
        }
    }
}

