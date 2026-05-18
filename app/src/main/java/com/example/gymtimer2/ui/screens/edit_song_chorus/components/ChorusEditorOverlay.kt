package com.example.gymtimer2.ui.screens.edit_song_chorus.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.music.SongCover
import com.example.gymtimer2.util.formatMillisToMinSec

@Composable
fun ChorusEditorOverlay(
    chorus: ChorusModel,
    song: SongModel,
    playbackState: MusicPlaybackState,
    onPreview: (Long) -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onSave: (ChorusModel) -> Unit
) {
    val maxDuration = song.durationMs?.takeIf { it > 0 } ?: maxOf(chorus.startMs + 1, 1L)

    var name by remember(chorus.id) { mutableStateOf(chorus.name) }
    var startMs by remember(chorus.id) { mutableFloatStateOf(chorus.startMs.toFloat()) }

    val isPreviewPlaying = playbackState.isPlaying && playbackState.uri == song.uri.toString()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongCover(song.uri)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nome do refrão") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (isPreviewPlaying) {
                            onStop()
                        } else {
                            onPreview(startMs.toLong())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isPreviewPlaying) "Parar refrão" else "Ouvir refrão")
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Escolha o início do refrão", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = startMs.coerceIn(0f, maxDuration.toFloat()),
                        onValueChange = { startMs = it },
                        valueRange = 0f..maxDuration.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Início selecionado: ${formatMillisToMinSec(startMs.toLong())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            // Revert to original values before canceling
                            onCancel()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            // Only save if there are actual changes
                            val updatedChorus = chorus.copy(name = name, startMs = startMs.toLong())
                            onSave(updatedChorus)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}