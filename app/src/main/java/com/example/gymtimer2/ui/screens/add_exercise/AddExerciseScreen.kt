package com.example.gymtimer2.ui.screens.add_exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun AddExerciseScreen(
    modifier: Modifier = Modifier,
    goBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as GymApplication
    val repository = app.container.workoutRepository

    val viewModel: AddExerciseViewModel = viewModel(
        factory = AddExerciseViewModel.factory(repository)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.add_exercise_title),
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = viewModel.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_exercise_name_label)) },
            singleLine = true
        )

        OutlinedTextField(
            value = viewModel.weight,
            onValueChange = viewModel::onWeightChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_exercise_weight_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

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

        OutlinedTextField(
            value = viewModel.restSeconds,
            onValueChange = viewModel::onRestSecondsChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_exercise_rest_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.insertExercise(goBack) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.save_button))
            }
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