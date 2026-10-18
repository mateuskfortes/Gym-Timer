package com.example.gymtimer2.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.ScheduledExerciseModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ReorderableExerciseList(
    exercises: List<ScheduledExerciseModel>,
    onReorder: (List<Int>) -> Unit,
    onEdit: (ScheduledExerciseModel) -> Unit,
    onDelete: (ScheduledExerciseModel) -> Unit,
    onStart: (ScheduledExerciseModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayed by remember { mutableStateOf(exercises) }
    var dragStartOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        displayed = displayed.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(exercises) {
        if (dragStartOrder.isEmpty()) displayed = exercises
    }

    if (displayed.isEmpty()) {
        Text(stringResource(R.string.no_exercise_in_day), modifier = modifier.padding(top = 24.dp))
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        userScrollEnabled = dragStartOrder.isEmpty(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(displayed, key = { it.exercise.id }) { scheduled ->
            ReorderableItem(reorderableState, key = scheduled.exercise.id) { isDragging ->
                val itemScope = this
                ExerciseCard(
                    exercise = scheduled.exercise,
                    modifier = Modifier.zIndex(if (isDragging) 1f else 0f),
                    isDragged = isDragging,
                    onEdit = { onEdit(scheduled) },
                    onDelete = { onDelete(scheduled) },
                    onStart = { onStart(scheduled) },
                    dragHandle = {
                        Box(
                            modifier = with(itemScope) {
                                Modifier.size(48.dp).draggableHandle(
                                    onDragStarted = {
                                        dragStartOrder = displayed.map { it.exercise.id }
                                    },
                                    onDragStopped = {
                                        val orderedIds = displayed.map { it.exercise.id }
                                        if (orderedIds != dragStartOrder) onReorder(orderedIds)
                                        dragStartOrder = emptyList()
                                    }
                                )
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = stringResource(R.string.reorder_exercise),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}
