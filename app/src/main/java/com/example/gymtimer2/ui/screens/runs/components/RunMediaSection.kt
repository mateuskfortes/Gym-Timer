package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.MAX_RUN_MEDIA_COUNT
import com.example.gymtimer2.domain.model.RunMedia

@Composable
internal fun RunMediaSection(
    media: List<RunMedia>,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
    onSetFeatured: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.run_media_title, media.size, MAX_RUN_MEDIA_COUNT),
                style = MaterialTheme.typography.titleSmall
            )
            if (media.size < MAX_RUN_MEDIA_COUNT) {
                TextButton(onClick = onAdd) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Text(stringResource(R.string.run_media_add))
                }
            }
        }
        if (media.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(media, key = { _, item -> item.id }) { index, item ->
                    RunMediaThumbnail(item, index, onRemove, onSetFeatured)
                }
            }
        }
        Text(
            stringResource(R.string.run_media_local_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
