package com.example.gymtimer2.ui.components.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.util.formatMillisToMinSec

@Composable
fun MusicSeekBar(
    playbackState: MusicPlaybackState,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playbackState.durationMs <= 0) return

    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember(playbackState.durationMs) {
        mutableFloatStateOf(playbackState.currentPositionMs.toFloat())
    }

    LaunchedEffect(playbackState.currentPositionMs, playbackState.durationMs) {
        if (!isDragging) {
            sliderPosition = playbackState.currentPositionMs.toFloat()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Slider(
            value = sliderPosition.coerceIn(0f, playbackState.durationMs.toFloat()),
            onValueChange = {
                isDragging = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                onSeek(sliderPosition.toInt())
                isDragging = false
            },
            valueRange = 0f..playbackState.durationMs.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMillisToMinSec(sliderPosition.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMillisToMinSec(playbackState.durationMs.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

