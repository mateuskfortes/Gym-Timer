package com.example.gymtimer2.ui.screens.edit_chorus.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.ui.components.music.deleteChorusDialog
import com.example.gymtimer2.ui.screens.edit_chorus.EditSongChorusViewModel
import com.example.gymtimer2.util.formatMillisToMinSec

@Composable
fun ChorusCard(
    chorus: ChorusModel,
    playbackState: MusicPlaybackState,
    viewModel: EditSongChorusViewModel,
) {
    val durationMs = viewModel.songToEdit.durationMs?.takeIf { it > 0 }
        ?: maxOf(chorus.startMs + 1, 1L)
    val sliderValue = chorus.startMs.toFloat().coerceIn(0f, durationMs.toFloat())
    val isChorusPlaying = playbackState.isPlaying && playbackState.uri == viewModel.songToEdit.uriString
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chorus.name.ifBlank { "Refrão" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Começa em ${formatMillisToMinSec(chorus.startMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        if (isChorusPlaying) {
                            viewModel.stopPlayback()
                        } else {
                            viewModel.playChorus(chorus)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isChorusPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isChorusPlaying) "Parar refrão" else "Tocar a partir do refrão"
                    )
                }

                IconButton(onClick = { deleteChorusDialog(context, chorus) {viewModel.deleteChorus(chorus) }}) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Excluir refrão"
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { viewModel.setChorusToEdit(chorus) })
            ) {
                Slider(
                    value = sliderValue,
                    onValueChange = {},
                    enabled = false,
                    valueRange = 0f..durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
