package com.example.gymtimer2.ui.components.chorus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.SecondaryButton

@Composable
fun ExerciseChorusSection(
    choruses: List<Pair<SongModel, ChorusModel>>,
    playbackState: MusicPlaybackState,
    onAddClick: () -> Unit,
    onPlay: (SongModel, ChorusModel) -> Unit,
    onStop: () -> Unit,
    onRemove: (Long) -> Unit,
    addEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.chorus_section_title, choruses.size),
                style = MaterialTheme.typography.titleMedium
            )

            SecondaryButton(onClick = onAddClick, enabled = addEnabled) {
                Text("+ ${stringResource(R.string.add_chorus_button)}")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            choruses.forEach { (song, chorus) ->
                key(chorus.id) {
                    ExerciseChorusCard(
                        chorus = chorus,
                        song = song,
                        playbackState = playbackState.takeIf { it.uri == song.uriString }
                            ?: MusicPlaybackState(),
                        onPlay = { onPlay(song, chorus) },
                        onStop = onStop,
                        onRemove = { onRemove(chorus.id) }
                    )
                }
            }
        }
    }
}
