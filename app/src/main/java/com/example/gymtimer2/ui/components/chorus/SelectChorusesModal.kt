package com.example.gymtimer2.ui.components.chorus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.components.AppCheckbox
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.SecondaryButton
import com.example.gymtimer2.ui.components.softCardShadow
import com.example.gymtimer2.util.formatMillisToMinSec

@Composable
fun SelectChorusesModal(
    allSongsWithChoruses: List<Pair<SongModel, List<ChorusModel>>>,
    alreadySelectedChorusIds: Set<Long>,
    onConfirm: (selectedChorusIds: Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedChorusIds by remember {
        mutableStateOf(alreadySelectedChorusIds)
    }
    val availableSongsWithChoruses = remember(allSongsWithChoruses, alreadySelectedChorusIds) {
        allSongsWithChoruses.mapNotNull { (song, choruses) ->
            val availableChoruses = choruses.filterNot { it.id in alreadySelectedChorusIds }
            availableChoruses.takeIf { it.isNotEmpty() }?.let { song to it }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .softCardShadow(MaterialTheme.shapes.extraLarge, 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecionar Refrões",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableSongsWithChoruses.forEach { (song, choruses) ->
                        item(key = "song_${song.id}") {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }

                        items(
                            items = choruses,
                            key = { it.id }
                        ) { chorus ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppCheckbox(
                                    checked = chorus.id in selectedChorusIds,
                                    onCheckedChange = { isChecked ->
                                        selectedChorusIds = if (isChecked) {
                                            selectedChorusIds + chorus.id
                                        } else {
                                            selectedChorusIds - chorus.id
                                        }
                                    }
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chorus.name.ifBlank { "Refrão" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Começa em ${formatMillisToMinSec(chorus.startMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    PrimaryButton(
                        onClick = {
                            onConfirm(selectedChorusIds)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Adicionar")
                    }
                }
            }
        }
    }
}

