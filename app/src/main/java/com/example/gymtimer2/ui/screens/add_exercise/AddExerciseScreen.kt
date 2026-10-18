package com.example.gymtimer2.ui.screens.add_exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.WeightUnit
import com.example.gymtimer2.ui.components.WeightUnitChip
import com.example.gymtimer2.ui.components.WorkoutDaySelector
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.LabeledOutlinedTextField
import com.example.gymtimer2.ui.components.FormFieldLabel
import com.example.gymtimer2.ui.components.chorus.ExerciseChorusSection
import com.example.gymtimer2.ui.components.chorus.SelectChorusesModal

@Composable
fun AddExerciseScreen(
    modifier: Modifier = Modifier,
    initialDayId: Long? = null,
    goBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication

    val viewModel: AddExerciseViewModel = viewModel(
        factory = AddExerciseViewModel.factory(
            repository = app.container.workoutRepository,
            playerManager = app.musicPlayerManager
        )
    )

    val selectedChorusIds by viewModel.selectedChorusIds.collectAsState()
    val associatedChoruses by viewModel.associatedChoruses.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val days by viewModel.days.collectAsState(initial = emptyList())
    val allSongsWithChoruses by viewModel.allSongsWithChoruses.collectAsState()
    var showChorusModal by remember { mutableStateOf(false) }

    LaunchedEffect(days, initialDayId) {
        viewModel.selectInitialDay(days, initialDayId)
    }

    if (showChorusModal) {
        SelectChorusesModal(
            allSongsWithChoruses = allSongsWithChoruses,
            alreadySelectedChorusIds = selectedChorusIds,
            onConfirm = { selectedIds ->
                viewModel.addMultipleChorusesToExercise(selectedIds)
                showChorusModal = false
            },
            onDismiss = { showChorusModal = false }
        )
    }

    BackHandler { goBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.add_exercise_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FormFieldLabel(stringResource(R.string.workout_day_label))
                WorkoutDaySelector(days, viewModel.selectedDayId, viewModel::selectDay)
            }

            LabeledOutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.add_exercise_name_label)
            )

            LabeledOutlinedTextField(
                value = viewModel.weight,
                onValueChange = viewModel::onWeightChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.add_exercise_weight_label),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FormFieldLabel(stringResource(R.string.weight_unit_label))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeightUnitChip(
                        text = WeightUnit.TOTAL_KG.label,
                        selected = viewModel.weightUnit == WeightUnit.TOTAL_KG,
                        onClick = { viewModel.onWeightUnitChange(WeightUnit.TOTAL_KG) }
                    )

                    WeightUnitChip(
                        text = WeightUnit.MACHINE_STACK_UNITS.label,
                        selected = viewModel.weightUnit == WeightUnit.MACHINE_STACK_UNITS,
                        onClick = { viewModel.onWeightUnitChange(WeightUnit.MACHINE_STACK_UNITS) }
                    )
                }
            }

            LabeledOutlinedTextField(
                value = viewModel.restSeconds,
                onValueChange = viewModel::onRestSecondsChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.add_exercise_rest_label),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            LabeledOutlinedTextField(
                value = viewModel.chorusDelay,
                onValueChange = viewModel::onChorusDelayChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.add_exercise_chorus_delay_label),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            ExerciseChorusSection(
                choruses = associatedChoruses,
                playbackState = playbackState,
                onAddClick = { showChorusModal = true },
                onPlay = viewModel::playChorus,
                onStop = viewModel::stopPlayback,
                onRemove = viewModel::removeChorusFromExercise
            )
        }
        PrimaryButton(
            onClick = { viewModel.insertExercise(goBack) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Text(stringResource(R.string.save_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddExerciseScreenPreview() {
    AddExerciseScreen(
        goBack = {}
    )
}
