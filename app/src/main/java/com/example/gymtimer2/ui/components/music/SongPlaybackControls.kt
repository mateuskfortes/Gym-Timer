package com.example.gymtimer2.ui.components.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.ui.components.IconActionButton

private val PlaybackButtonSize = 48.dp
private val PlaybackButtonGap = 8.dp
// Align the button with the slider track, above the elapsed and total time labels.
private val PlaybackButtonProgressOffset = (-10).dp
private const val PlaybackAnimationDurationMs = 280

@Composable
fun SongPlaybackControls(
    playbackState: MusicPlaybackState,
    songUri: String,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSongPlaying = playbackState.isPlaying && playbackState.uri == songUri
    val showProgress = isSongPlaying && playbackState.durationMs > 0

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(PlaybackAnimationDurationMs))
    ) {
        val buttonOffset by animateDpAsState(
            targetValue = if (showProgress) {
                maxWidth - PlaybackButtonSize
            } else {
                (maxWidth - PlaybackButtonSize) / 2
            },
            animationSpec = tween(PlaybackAnimationDurationMs),
            label = "song_playback_button_position"
        )
        val buttonVerticalOffset by animateDpAsState(
            targetValue = if (showProgress) PlaybackButtonProgressOffset else 0.dp,
            animationSpec = tween(PlaybackAnimationDurationMs),
            label = "song_playback_button_vertical_position"
        )

        AnimatedVisibility(
            visible = showProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = PlaybackButtonSize + PlaybackButtonGap)
                .align(Alignment.CenterStart),
            enter = fadeIn(tween(PlaybackAnimationDurationMs)) +
                expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(PlaybackAnimationDurationMs)
                ),
            exit = fadeOut(tween(PlaybackAnimationDurationMs)) +
                shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = tween(PlaybackAnimationDurationMs)
                )
        ) {
            MusicSeekBar(
                playbackState = playbackState,
                onSeek = onSeek
            )
        }

        IconActionButton(
            onClick = if (isSongPlaying) onStop else onPlay,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = buttonOffset, y = buttonVerticalOffset)
                .size(PlaybackButtonSize)
        ) {
            Icon(
                imageVector = if (isSongPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isSongPlaying) R.string.stop_song_playback else R.string.play_song_playback
                ),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
