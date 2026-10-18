package com.example.gymtimer2.ui.screens.exercise_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.ScheduledExerciseModel
import com.example.gymtimer2.domain.model.WorkoutScheduleError
import com.example.gymtimer2.ui.components.ExerciseCard
import com.example.gymtimer2.ui.components.LoadingScreen
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.IconActionButton
import com.example.gymtimer2.ui.components.AppTextField
import com.example.gymtimer2.ui.components.SecondaryButton
import com.example.gymtimer2.ui.components.softCardShadow
import com.example.gymtimer2.ui.components.ReorderableExerciseList
import com.example.gymtimer2.ui.components.WorkoutDaySelector
import com.example.gymtimer2.ui.components.deleteExerciseDialog

@Composable
fun ExerciseListScreen(
    modifier: Modifier = Modifier,
    selectedDayId: Long?,
    onSelectDay: (Long) -> Unit,
    onOpenOverlayClick: (Int) -> Unit,
    onEditExercise: (ScheduledExerciseModel) -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as GymApplication).container.workoutRepository
    val viewModel: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.factory(repository))
    val days by viewModel.days.collectAsState(initial = emptyList())
    val dayExercises by viewModel.dayExercises.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedDay = days.firstOrNull { it.id == selectedDayId }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(days, selectedDayId) {
        if (days.isNotEmpty() && selectedDay == null) onSelectDay(days.first().id)
    }

    LaunchedEffect(selectedDayId) {
        viewModel.selectDay(selectedDayId)
    }

    if (showCreateDialog) {
        DayNameDialog(
            title = stringResource(R.string.create_day),
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createDay(name, onSelectDay)
                showCreateDialog = false
            }
        )
    }
    if (showRenameDialog && selectedDay != null) {
        DayNameDialog(
            title = stringResource(R.string.rename_day),
            initialName = selectedDay.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                viewModel.renameDay(selectedDay.id, name)
                showRenameDialog = false
            }
        )
    }
    if (showDeleteDialog && selectedDay != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            modifier = Modifier.softCardShadow(MaterialTheme.shapes.extraLarge, 12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = { Text(stringResource(R.string.delete_day)) },
            text = { Text(stringResource(R.string.delete_day_confirmation, selectedDay.name)) },
            confirmButton = {
                SecondaryButton(onClick = {
                    viewModel.deleteDay(selectedDay.id)
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.delete_day)) }
            },
            dismissButton = {
                SecondaryButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconActionButton(onClick = { viewModel.clearError(); showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_day))
            }
            IconActionButton(onClick = { viewModel.clearError(); showRenameDialog = true }, enabled = selectedDay != null) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename_day))
            }
            IconActionButton(
                onClick = { viewModel.clearError(); showDeleteDialog = true },
                enabled = selectedDay != null && days.size > 1 &&
                    (dayExercises as? DayExercisesUiState.Loaded)?.let {
                        it.dayId == selectedDay.id && it.exercises.isEmpty()
                    } == true
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_day))
            }
        }
        WorkoutDaySelector(days, selectedDayId, onSelectDay)
        error?.let { reason ->
            val message = when (reason) {
                WorkoutScheduleError.DAY_NOT_FOUND -> R.string.day_not_found
                WorkoutScheduleError.DAY_NAME_REQUIRED -> R.string.day_name_required
                WorkoutScheduleError.DUPLICATE_DAY_NAME -> R.string.duplicate_day_name
                WorkoutScheduleError.LAST_DAY -> R.string.last_day_required
                WorkoutScheduleError.DAY_NOT_EMPTY -> R.string.day_not_empty
                WorkoutScheduleError.EXERCISE_NOT_FOUND -> R.string.exercise_not_found
                WorkoutScheduleError.INVALID_ORDER -> R.string.invalid_exercise_order
                WorkoutScheduleError.INVALID_WEIGHT -> R.string.invalid_weight
                WorkoutScheduleError.SAVE_FAILED -> R.string.schedule_save_failed
            }
            Text(stringResource(message))
        }
        if (selectedDay != null) {
            when (val state = dayExercises) {
                is DayExercisesUiState.Loaded -> {
                    if (state.dayId == selectedDay.id) {
                        key(selectedDay.id) {
                            ReorderableExerciseList(
                                exercises = state.exercises,
                                onReorder = { orderedIds -> viewModel.reorder(selectedDay.id, orderedIds) },
                                onEdit = onEditExercise,
                                onDelete = { exercise ->
                                    deleteExerciseDialog(context, exercise.exercise) {
                                        viewModel.deleteExercise(exercise.exercise)
                                    }
                                },
                                onStart = { onOpenOverlayClick(it.exercise.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        LoadingScreen(modifier = Modifier.weight(1f))
                    }
                }
                is DayExercisesUiState.Failed -> {
                    if (state.dayId == selectedDay.id) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.load_day_failed))
                        }
                    } else {
                        LoadingScreen(modifier = Modifier.weight(1f))
                    }
                }
                DayExercisesUiState.Idle, is DayExercisesUiState.Loading -> {
                    LoadingScreen(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.softCardShadow(MaterialTheme.shapes.extraLarge, 12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text(title) },
        text = {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.day_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )
        },
        confirmButton = {
            PrimaryButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            SecondaryButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}
