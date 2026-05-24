package com.example.gymtimer2.ui.screens.exercise_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.GymApplication
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.ChorusWithSongModel
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.ui.components.ExerciseCard
import com.example.gymtimer2.ui.components.deleteExerciseDialog

@Composable
fun ExerciseListScreen(
    modifier: Modifier = Modifier,
    onOpenOverlayClick: (ExerciseModel, List<ChorusWithSongModel>) -> Unit,
    onEditExercise: (ExerciseModel) -> Unit,

    // Just used to see preview
    exercisesPreview: List<ExerciseModel>? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication
    val repository = app.container.workoutRepository

    val viewModel: ExerciseListViewModel = viewModel(
        factory = ExerciseListViewModel.factory(repository)
    )

    val exercisesDelegated by viewModel.allExercises.collectAsState(initial = emptyList())
    val exercises  = exercisesPreview ?: exercisesDelegated

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (exercises.isEmpty()) {
            Text(
                text = stringResource(R.string.no_exercise_registered),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = exercises,
                    key = { exercise -> exercise.id }
                ) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        modifier = Modifier,
                        onEdit = onEditExercise,
                        onDelete = {
                            deleteExerciseDialog(
                                context,
                                exercise
                            ) {
                                viewModel.deleteExercise(exercise)
                            }

                        },
                        onStart = {
                            viewModel.startExercise(
                                exercise = exercise,
                                onOpen = onOpenOverlayClick
                            )
                        }
                    )
                }
            }
        }
    }
}
