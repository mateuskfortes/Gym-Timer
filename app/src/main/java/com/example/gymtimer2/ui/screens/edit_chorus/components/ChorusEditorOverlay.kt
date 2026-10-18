package com.example.gymtimer2.ui.screens.edit_chorus.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.MusicPlaybackState
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.data.repository.SongArtworkRepository
import com.example.gymtimer2.ui.components.music.SongCover
import com.example.gymtimer2.ui.screens.edit_chorus.EditSongChorusViewModel
import com.example.gymtimer2.util.formatMillisToMinSec
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.AppTextField
import com.example.gymtimer2.ui.components.SecondaryButton
import com.example.gymtimer2.ui.components.softCardShadow

private val START_MARKER_AREA_HEIGHT = 48.dp
private val START_MARKER_WIDTH = 12.dp
private val START_MARKER_HEIGHT = 12.dp
private val START_MARKER_GAP = 2.dp
private val PLAYBACK_TRACK_HEIGHT = 16.dp
private val SLIDER_THUMB_WIDTH = 4.dp
private val PLAYBACK_THUMB_HEIGHT = 24.dp
private const val PLAYBACK_TRACK_VERTICAL_OFFSET_PX = 2

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChorusEditorOverlay(
    chorus: ChorusModel,
    song: SongModel,
    playbackState: MusicPlaybackState,
    viewModel: EditSongChorusViewModel,
    artworkRepository: SongArtworkRepository,
    onPlay: () -> Unit,
) {
    val maxDuration = song.durationMs?.takeIf { it > 0 } ?: maxOf(chorus.startMs + 1, 1L)

    val isPreviewPlaying = playbackState.isPlaying && playbackState.uri == song.uriString
    val progressSliderInteractionSource = remember { MutableInteractionSource() }

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
                .heightIn(max = 620.dp)
                .softCardShadow(RoundedCornerShape(24.dp), 14.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    SongCover(song, artworkRepository)

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

                AppTextField(
                    value = chorus.name,
                    onValueChange = viewModel::updateChorusName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nome do refrão") },
                    singleLine = true
                )

                PrimaryButton(
                    onClick = {
                        if (isPreviewPlaying) {
                            viewModel.stopPlayback()
                        } else {
                            onPlay()
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

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(START_MARKER_AREA_HEIGHT)
                    ) {
                        val markerProgress = (chorus.startMs / maxDuration.toFloat()).coerceIn(0f, 1f)
                        val markerOffsetY = (START_MARKER_AREA_HEIGHT - PLAYBACK_TRACK_HEIGHT) / 2 -
                            START_MARKER_GAP - START_MARKER_HEIGHT
                        val trackWidth = maxWidth - SLIDER_THUMB_WIDTH
                        val markerOffsetX = trackWidth * markerProgress +
                            SLIDER_THUMB_WIDTH / 2 - START_MARKER_WIDTH / 2
                        val markerColor = Color.Black
                        val progressPositionMs = if (isPreviewPlaying) {
                            playbackState.currentPositionMs.toLong()
                        } else {
                            chorus.startMs
                        }

                        Slider(
                            value = progressPositionMs.toFloat().coerceIn(0f, maxDuration.toFloat()),
                            onValueChange = {
                                viewModel.updateChorusStart(it.toLong())
                                viewModel.seekTo(it.toInt())
                            },
                            valueRange = 0f..maxDuration.toFloat(),
                            modifier = Modifier.fillMaxSize(),
                            interactionSource = progressSliderInteractionSource,
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = progressSliderInteractionSource,
                                    thumbSize = DpSize(
                                        width = SLIDER_THUMB_WIDTH,
                                        height = PLAYBACK_THUMB_HEIGHT
                                    )
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.offset {
                                        IntOffset(x = 0, y = PLAYBACK_TRACK_VERTICAL_OFFSET_PX)
                                    }
                                )
                            }
                        )
                        StartMarkerTriangle(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = markerOffsetX, y = markerOffsetY),
                            pointsDown = true,
                            color = markerColor
                        )
                        StartMarkerTriangle(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = markerOffsetX, y = -markerOffsetY),
                            pointsDown = false,
                            color = markerColor
                        )
                    }
                    Text(
                        text = "Início selecionado: ${formatMillisToMinSec(chorus.startMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = chorus.volume ?: 0f,
                        valueRange = 0f..viewModel.playerManager.audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat(),
                        onValueChange = viewModel::updateChorusVolume,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        onClick = {
                            viewModel.setChorusToEdit(null)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    PrimaryButton(
                        onClick = {
                            viewModel.saveUpdatedChorus()
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

@Composable
private fun StartMarkerTriangle(
    modifier: Modifier = Modifier,
    pointsDown: Boolean,
    color: Color
) {
    Canvas(modifier = modifier.size(START_MARKER_WIDTH, START_MARKER_HEIGHT)) {
        val path = Path().apply {
            if (pointsDown) {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            } else {
                moveTo(0f, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width / 2f, 0f)
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}
