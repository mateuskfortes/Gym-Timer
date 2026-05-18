package com.example.gymtimer2.ui.screens.edit_exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.gymtimer2.domain.model.ChorusModel
import com.example.gymtimer2.domain.model.SongModel
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecionar Refrões",
                    style = MaterialTheme.typography.headlineSmall
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allSongsWithChoruses.forEach { (song, choruses) ->
                        if (choruses.isNotEmpty()) {
                            item {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                                )
                            }

                            items(
                                items = choruses,
                                key = { it.id }
                            ) { chorus ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
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
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "Começa em ${formatMillisToMinSec(chorus.startMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
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

