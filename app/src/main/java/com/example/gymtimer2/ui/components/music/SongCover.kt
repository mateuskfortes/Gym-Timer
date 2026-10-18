package com.example.gymtimer2.ui.components.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtimer2.data.repository.SongArtworkRepository
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.AppSurface

@Composable
fun SongCover(
    song: SongModel,
    artworkRepository: SongArtworkRepository,
    shouldLoadArtwork: Boolean = true
) {
    val bitmap by produceState(
        initialValue = artworkRepository.getBestCachedArtwork(song),
        key1 = song.uriString,
        key2 = artworkRepository,
        key3 = shouldLoadArtwork
    ) {
        if (shouldLoadArtwork && !artworkRepository.hasFullArtwork(song)) {
            artworkRepository.loadFullArtwork(song)?.let { value = it }
        } else if (!shouldLoadArtwork && value == null) {
            value = artworkRepository.loadPreviewArtwork(song)
        }
    }
    val shape = RoundedCornerShape(14.dp)
    val displayedBitmap = bitmap

    AppSurface(
        modifier = Modifier.size(64.dp),
        shape = shape,
        elevation = 4.dp,
        contentAlignment = Alignment.Center
    ) {
        if (displayedBitmap != null) {
            Image(
                bitmap = displayedBitmap.asImageBitmap(),
                contentDescription = "Capa da música",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("♪", fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
