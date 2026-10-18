package com.example.gymtimer2.ui.screens.edit_exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.ScheduledExerciseModel
import com.example.gymtimer2.ui.components.WorkoutDaySelector
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.ScreenTitle
import com.example.gymtimer2.ui.components.AppTextField
import com.example.gymtimer2.ui.components.SecondaryButton
import com.example.gymtimer2.ui.components.chorus.ExerciseChorusCard
import com.example.gymtimer2.ui.components.chorus.SelectChorusesModal

@Composable
fun EditExerciseScreen(
    modifier: Modifier = Modifier,
    scheduledExercise: ScheduledExerciseModel,
    goBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication
    val repository = app.container.workoutRepository
    val playerManager = app.musicPlayerManager

    val viewModel: EditExerciseViewModel = viewModel(
        factory = EditExerciseViewModel.factory(
            repository = repository,
            playerManager = playerManager
        )
    )

    val playbackState by viewModel.playbackState.collectAsState()
    val associatedChoruses by viewModel.associatedChoruses.collectAsState()
    val allSongsWithChoruses by viewModel.allSongsWithChoruses.collectAsState()
    val selectedChorusIds by viewModel.selectedChorusIds.collectAsState()
    val days by viewModel.days.collectAsState(initial = emptyList())

    var showSelectModal by remember { mutableStateOf(false) }

    LaunchedEffect(scheduledExercise.exercise.id) {
        viewModel.loadExercise(scheduledExercise)
    }

    if (showSelectModal) {
        SelectChorusesModal(
            allSongsWithChoruses = allSongsWithChoruses,
            alreadySelectedChorusIds = selectedChorusIds,
            onConfirm = { selectedIds ->
                viewModel.addMultipleChorusesToExercise(selectedIds)
            },
            onDismiss = { showSelectModal = false }
        )
    }

    BackHandler { goBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(stringResource(R.string.edit_exercise_title))

        Text(stringResource(R.string.workout_day_label))
        WorkoutDaySelector(days, viewModel.selectedDayId, viewModel::selectDay)

        AppTextField(
            value = viewModel.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_exercise_name_label)) },
            singleLine = true,
            outlined = true
        )

        AppTextField(
            value = viewModel.weight,
            onValueChange = viewModel::onWeightChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(
                R.string.add_exercise_weight_label) + " (${scheduledExercise.exercise.weight.unit.label})"
            ) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            outlined = true
        )

        AppTextField(
            value = viewModel.restSeconds,
            onValueChange = viewModel::onRestSecondsChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_exercise_rest_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            outlined = true
        )

        AppTextField(
            value = viewModel.chorusDelay,
            onValueChange = viewModel::onChorusDelayChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Delay do refrão (s)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            outlined = true
        )

        // Choruses section
        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Refrões (${associatedChoruses.size})",
                style = MaterialTheme.typography.titleMedium
            )

            SecondaryButton(
                onClick = { showSelectModal = true }
            ) {
                Text("+ Adicionar")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = associatedChoruses,
                key = { (_, chorus) -> chorus.id }
            ) { (song, chorus) ->
                ExerciseChorusCard(
                    chorus = chorus,
                    song = song,
                    playbackState = if (playbackState.uri == song.uriString) playbackState else com.example.gymtimer2.domain.model.MusicPlaybackState(),
                    onPlay = { viewModel.playChorus(song, chorus) },
                    onStop = viewModel::stopPlayback,
                    onRemove = { viewModel.removeChorusFromExercise(chorus.id) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                onClick = { viewModel.discardChanges(goBack) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel_button))
            }

            PrimaryButton(
                onClick = {
                    viewModel.updateExercise(goBack)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.save_button))
            }
        }
    }
}
