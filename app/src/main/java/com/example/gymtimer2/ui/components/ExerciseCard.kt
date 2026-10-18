package com.example.gymtimer2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.util.formatMillisToMinSec

@Composable
fun ExerciseCard(
    exercise: ExerciseModel,
    modifier: Modifier,
    onEdit: (ExerciseModel) -> Unit,
    onDelete: (ExerciseModel) -> Unit,
    onStart: () -> Unit,
    dragHandle: @Composable () -> Unit = {},
    isDragged: Boolean = false
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(MaterialTheme.shapes.medium, 8.dp),
        border = if (isDragged) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isDragged) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 3.dp else 0.dp)
    ) {
        Row (
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Peso: ${exercise.weight.plainText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Descanso: ${formatMillisToMinSec(exercise.restPeriod)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Delay do refrão: ${formatMillisToMinSec(exercise.chorusDelay)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                dragHandle()
                IconActionButton(onClick = { onDelete(exercise) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.cd_delete_exercise)
                    )
                }

                IconActionButton(onClick = { onEdit(exercise) }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.cd_edit_exercise)
                    )
                }

                IconActionButton(onClick = onStart) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_start_exercise)
                    )
                }
            }
        }
    }
}
