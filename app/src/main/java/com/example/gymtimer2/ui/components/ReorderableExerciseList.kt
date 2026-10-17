package com.example.gymtimer2.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.ScheduledExerciseModel
import kotlinx.coroutines.launch

@Composable
fun ReorderableExerciseList(
    exercises: List<ScheduledExerciseModel>,
    onReorder: (List<Int>) -> Unit,
    onEdit: (ScheduledExerciseModel) -> Unit,
    onDelete: (ScheduledExerciseModel) -> Unit,
    onStart: (ScheduledExerciseModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayed by remember { mutableStateOf(exercises) }
    var draggedId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(exercises) {
        if (draggedId == null) displayed = exercises
    }

    if (displayed.isEmpty()) {
        Text(stringResource(R.string.no_exercise_in_day), modifier = modifier.padding(top = 24.dp))
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        userScrollEnabled = draggedId == null,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(displayed, key = { it.exercise.id }) { scheduled ->
            val id = scheduled.exercise.id
            ExerciseCard(
                exercise = scheduled.exercise,
                modifier = Modifier.zIndex(if (draggedId == id) 1f else 0f).graphicsLayer {
                    translationY = if (draggedId == id) dragOffset else 0f
                },
                onEdit = { onEdit(scheduled) },
                onDelete = { onDelete(scheduled) },
                onStart = { onStart(scheduled) },
                dragHandle = {
                    Box(
                        modifier = Modifier.size(48.dp).pointerInput(id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedId = id
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    val orderedIds = displayed.map { it.exercise.id }
                                    if (draggedId != null && orderedIds != exercises.map { it.exercise.id }) {
                                        onReorder(orderedIds)
                                    }
                                    draggedId = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffset = 0f
                                    displayed = exercises
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val layout = listState.layoutInfo
                                    val source = layout.visibleItemsInfo.firstOrNull { it.key == id }
                                        ?: return@detectDragGestures
                                    val center = source.offset + source.size / 2 + dragOffset
                                    val target = layout.visibleItemsInfo.firstOrNull { item ->
                                        item.key != id && center.toInt() in item.offset..(item.offset + item.size)
                                    }
                                    if (target != null) {
                                        val from = displayed.indexOfFirst { it.exercise.id == id }
                                        val to = displayed.indexOfFirst { it.exercise.id == target.key }
                                        if (from >= 0 && to >= 0) {
                                            displayed = displayed.toMutableList().apply { add(to, removeAt(from)) }
                                            dragOffset += source.offset - target.offset
                                        }
                                    }
                                    when {
                                        center > layout.viewportEndOffset - source.size -> scope.launch { listState.scrollBy(24f) }
                                        center < layout.viewportStartOffset + source.size -> scope.launch { listState.scrollBy(-24f) }
                                    }
                                }
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = stringResource(R.string.reorder_exercise),
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}
