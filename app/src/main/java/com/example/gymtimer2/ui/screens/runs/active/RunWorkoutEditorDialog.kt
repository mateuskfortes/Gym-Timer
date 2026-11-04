@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutSetPlan
import com.example.gymtimer2.domain.model.RunWorkoutStepPlan
import com.example.gymtimer2.domain.model.RunWorkoutTrigger
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.LocalRunDistanceUnit

@Composable
internal fun RunWorkoutEditorDialog(
    initialPlan: RunWorkoutPlan?,
    onSave: (RunWorkoutPlan) -> Unit,
    onDismiss: () -> Unit
) {
    val distanceUnit = LocalRunDistanceUnit.current
    var name by remember(initialPlan) { mutableStateOf(initialPlan?.name ?: "") }
    var sets by remember(initialPlan, distanceUnit) {
        mutableStateOf(
            initialPlan?.sets?.map { RunWorkoutSetDraft(it, distanceUnit) }
                ?: listOf(RunWorkoutSetDraft(distanceUnit))
        )
    }
    var paceEditingStep by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val plan = remember(name, sets) { workoutPlanOrNull(name, sets) }

    if (paceEditingStep == null) WorkoutPlanEditorDialog(
        name = name,
        sets = sets,
        distanceUnit = distanceUnit,
        plan = plan,
        onNameChange = { name = it },
        onSetsChange = { sets = it },
        onEditPace = { setIndex, stepIndex -> paceEditingStep = setIndex to stepIndex },
        onSave = onSave,
        onDismiss = onDismiss
    )
    val selectedStep = paceEditingStep?.let { (setIndex, stepIndex) -> sets.getOrNull(setIndex)?.steps?.getOrNull(stepIndex) }
    RunPaceTargetDialog(
        visible = paceEditingStep != null,
        initialTarget = selectedStep?.paceTarget,
        onConfirm = { target ->
            sets = sets.withPaceTarget(paceEditingStep, target)
            paceEditingStep = null
        },
        onDismiss = { paceEditingStep = null }
    )
}

@Composable
private fun WorkoutPlanEditorDialog(
    name: String,
    sets: List<RunWorkoutSetDraft>,
    distanceUnit: RunDistanceUnit,
    plan: RunWorkoutPlan?,
    onNameChange: (String) -> Unit,
    onSetsChange: (List<RunWorkoutSetDraft>) -> Unit,
    onEditPace: (Int, Int) -> Unit,
    onSave: (RunWorkoutPlan) -> Unit,
    onDismiss: () -> Unit
) = AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_workout_editor_title)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.run_workout_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                WorkoutSetList(sets, distanceUnit, onSetsChange, onEditPace)
            }
        },
        confirmButton = {
            TextButton(onClick = { plan?.let(onSave) }, enabled = plan != null) {
                Text(stringResource(R.string.run_workout_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.run_cancel_action))
            }
        }
    )

@Composable
private fun WorkoutSetList(
    sets: List<RunWorkoutSetDraft>,
    distanceUnit: RunDistanceUnit,
    onSetsChange: (List<RunWorkoutSetDraft>) -> Unit,
    onEditPace: (Int, Int) -> Unit
) {
    var stepPosition = 0
    val totalStepCount = sets.sumOf { it.steps.size }
    sets.forEachIndexed { setIndex, set ->
        WorkoutSetEditor(
            set = set,
            distanceUnit = distanceUnit,
            position = setIndex + 1,
            firstStepPosition = stepPosition + 1,
            canRemove = sets.size > 1,
            canAddStep = totalStepCount < MAX_WORKOUT_STEP_COUNT,
            onChange = { updated -> onSetsChange(sets.toMutableList().also { it[setIndex] = updated }) },
            onEditPace = { stepIndex -> onEditPace(setIndex, stepIndex) },
            onRemove = { onSetsChange(sets.filterIndexed { index, _ -> index != setIndex }) }
        )
        stepPosition += set.steps.size
    }
    OutlinedButton(
        onClick = { onSetsChange(sets + RunWorkoutSetDraft(distanceUnit)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = sets.size < MAX_WORKOUT_SET_COUNT && totalStepCount < MAX_WORKOUT_STEP_COUNT
    ) {
        Text(stringResource(R.string.run_workout_add_set))
    }
}

private fun workoutPlanOrNull(name: String, sets: List<RunWorkoutSetDraft>): RunWorkoutPlan? {
    val plannedSets = sets.mapNotNull(RunWorkoutSetDraft::toPlan)
    return RunWorkoutPlan(name, plannedSets).takeIf { plannedSets.size == sets.size }?.normalizedOrNull()
}

private fun List<RunWorkoutSetDraft>.withPaceTarget(
    selectedStep: Pair<Int, Int>?,
    target: RunPaceTarget?
): List<RunWorkoutSetDraft> {
    val (setIndex, stepIndex) = selectedStep ?: return this
    val set = getOrNull(setIndex) ?: return this
    val step = set.steps.getOrNull(stepIndex) ?: return this
    val updatedSet = set.copy(
        steps = set.steps.toMutableList().also { it[stepIndex] = step.copy(paceTarget = target) }
    )
    return toMutableList().also { it[setIndex] = updatedSet }
}

@Composable
private fun WorkoutSetEditor(
    set: RunWorkoutSetDraft,
    distanceUnit: RunDistanceUnit,
    position: Int,
    firstStepPosition: Int,
    canRemove: Boolean,
    canAddStep: Boolean,
    onChange: (RunWorkoutSetDraft) -> Unit,
    onEditPace: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.run_workout_set_number, position),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (canRemove) TextButton(onClick = onRemove) {
                Text(stringResource(R.string.run_workout_remove_set))
            }
        }
        OutlinedTextField(
            value = set.repetitions,
            onValueChange = { onChange(set.copy(repetitions = it)) },
            label = { Text(stringResource(R.string.run_workout_set_repetitions)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (set.steps.lastOrNull()?.isRest == true && (set.repetitions.toIntOrNull() ?: 0) > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = set.omitLastRecovery,
                    onCheckedChange = { onChange(set.copy(omitLastRecovery = it)) }
                )
                Text(stringResource(R.string.run_workout_omit_last_recovery))
            }
        }
        set.steps.forEachIndexed { index, step ->
            WorkoutStepEditor(
                step = step.copy(position = firstStepPosition + index),
                canRemove = set.steps.size > 1,
                onChange = { updated -> onChange(set.copy(steps = set.steps.toMutableList().also { it[index] = updated })) },
                onEditPace = { onEditPace(index) },
                onRemove = { onChange(set.copy(steps = set.steps.filterIndexed { stepIndex, _ -> stepIndex != index })) }
            )
        }
        OutlinedButton(
            onClick = { onChange(set.copy(steps = set.steps + RunWorkoutStepDraft(distanceUnit))) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canAddStep && set.steps.size < MAX_WORKOUT_STEP_COUNT
        ) {
            Text(stringResource(R.string.run_workout_add_step))
        }
    }
}

@Composable
private fun WorkoutStepEditor(
    step: RunWorkoutStepDraft,
    canRemove: Boolean,
    onChange: (RunWorkoutStepDraft) -> Unit,
    onEditPace: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.run_workout_step_number, step.position),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (canRemove) TextButton(onClick = onRemove) {
                Text(stringResource(R.string.run_workout_remove_step))
            }
        }
        OutlinedTextField(
            value = step.description,
            onValueChange = { onChange(step.copy(description = it)) },
            label = { Text(stringResource(R.string.run_workout_step_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RunWorkoutTrigger.entries.forEach { trigger ->
                FilterChip(
                    selected = step.trigger == trigger,
                    onClick = { onChange(step.withTrigger(trigger)) },
                    label = {
                        Text(stringResource(when (trigger) {
                            RunWorkoutTrigger.DURATION -> R.string.run_workout_trigger_time
                            RunWorkoutTrigger.DISTANCE -> R.string.run_workout_trigger_distance
                            RunWorkoutTrigger.ELEVATION -> R.string.run_workout_trigger_elevation
                            RunWorkoutTrigger.MANUAL -> R.string.run_workout_trigger_manual
                        }))
                    }
                )
            }
        }
        when (step.trigger) {
            RunWorkoutTrigger.DURATION -> OutlinedTextField(
                value = step.durationMinutes,
                onValueChange = { onChange(step.copy(durationMinutes = it)) },
                label = { Text(stringResource(R.string.run_workout_duration_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            RunWorkoutTrigger.DISTANCE -> OutlinedTextField(
                value = step.distanceValue,
                onValueChange = { onChange(step.copy(distanceValue = it)) },
                label = {
                    Text(stringResource(R.string.run_workout_distance_unit, RunFormatting.distanceUnitLabel()))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            RunWorkoutTrigger.ELEVATION -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = step.elevationChangeValue,
                    onValueChange = { onChange(step.copy(elevationChangeValue = it)) },
                    label = {
                        Text(
                            stringResource(
                                R.string.run_workout_elevation_change_unit,
                                RunFormatting.elevationUnitLabel()
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.run_workout_elevation_change_hint), style = MaterialTheme.typography.bodySmall)
            }
            RunWorkoutTrigger.MANUAL -> Unit
        }
        OutlinedButton(onClick = onEditPace, modifier = Modifier.fillMaxWidth()) {
            Text(step.paceTarget?.let { target ->
                stringResource(
                    R.string.run_workout_step_pace_target,
                    RunFormatting.pace(target.fasterSecondsPerKm.toDouble()),
                    RunFormatting.pace(target.slowerSecondsPerKm.toDouble())
                )
            } ?: stringResource(R.string.run_workout_step_pace_add))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = step.isRest, onCheckedChange = { onChange(step.copy(isRest = it)) })
            Text(stringResource(R.string.run_workout_rest_interval))
        }
    }
}

private data class RunWorkoutSetDraft(
    val repetitions: String = "1",
    val omitLastRecovery: Boolean = false,
    val steps: List<RunWorkoutStepDraft> = listOf(RunWorkoutStepDraft())
) {
    constructor(distanceUnit: RunDistanceUnit) : this(steps = listOf(RunWorkoutStepDraft(distanceUnit)))

    constructor(set: RunWorkoutSetPlan, distanceUnit: RunDistanceUnit) : this(
        repetitions = set.repetitions.toString(),
        omitLastRecovery = set.omitLastRecoveryStep,
        steps = set.steps.map { RunWorkoutStepDraft(it, distanceUnit) }
    )

    fun toPlan(): RunWorkoutSetPlan? {
        val repeatCount = repetitions.toIntOrNull() ?: return null
        val plannedSteps = steps.mapNotNull(RunWorkoutStepDraft::toPlan)
        if (plannedSteps.size != steps.size) return null
        return RunWorkoutSetPlan(plannedSteps, repeatCount, omitLastRecovery)
    }
}

private data class RunWorkoutStepDraft(
    val description: String = "",
    val trigger: RunWorkoutTrigger = RunWorkoutTrigger.DURATION,
    val durationMinutes: String = "5",
    val distanceValue: String = "0.4",
    val isRest: Boolean = false,
    val paceTarget: RunPaceTarget? = null,
    val elevationChangeValue: String = "10",
    val position: Int = 1,
    val distanceUnit: RunDistanceUnit = RunDistanceUnit.METRIC
) {
    constructor(distanceUnit: RunDistanceUnit) : this(
        distanceValue = RunFormatting.workoutDistanceInputValue(DEFAULT_STEP_DISTANCE_METERS, distanceUnit),
        elevationChangeValue = RunFormatting.workoutElevationInputValue(DEFAULT_STEP_ELEVATION_METERS, distanceUnit),
        distanceUnit = distanceUnit
    )

    constructor(step: RunWorkoutStepPlan, distanceUnit: RunDistanceUnit) : this(
        description = step.description,
        trigger = step.trigger,
        durationMinutes = step.durationTargetMs?.div(60_000.0)?.toString() ?: "5",
        distanceValue = RunFormatting.workoutDistanceInputValue(
            step.distanceTargetMeters ?: DEFAULT_STEP_DISTANCE_METERS,
            distanceUnit
        ),
        isRest = step.isRestInterval,
        paceTarget = step.paceTarget,
        elevationChangeValue = RunFormatting.workoutElevationInputValue(
            step.elevationChangeTargetMeters ?: DEFAULT_STEP_ELEVATION_METERS,
            distanceUnit
        ),
        distanceUnit = distanceUnit
    )

    fun withTrigger(value: RunWorkoutTrigger) = copy(
        trigger = value,
        durationMinutes = durationMinutes.ifBlank { "5" },
        distanceValue = distanceValue.ifBlank {
            RunFormatting.workoutDistanceInputValue(DEFAULT_STEP_DISTANCE_METERS, distanceUnit)
        },
        elevationChangeValue = elevationChangeValue.ifBlank {
            RunFormatting.workoutElevationInputValue(DEFAULT_STEP_ELEVATION_METERS, distanceUnit)
        }
    )

    fun toPlan(): RunWorkoutStepPlan? {
        val durationMs = durationMinutes.toDecimalOrNull()?.let { (it * 60_000).toLong() }
        val distance = distanceValue.toDecimalOrNull()?.times(distanceUnit.metersPerUnit)
        val elevationChange = elevationChangeValue.toDecimalOrNull()?.let(distanceUnit::metersFromElevation)
        return when (trigger) {
            RunWorkoutTrigger.DURATION -> RunWorkoutStepPlan(
                description, trigger, durationTargetMs = durationMs, isRestInterval = isRest,
                paceTarget = paceTarget
            )
            RunWorkoutTrigger.DISTANCE -> RunWorkoutStepPlan(
                description, trigger, distanceTargetMeters = distance, isRestInterval = isRest,
                paceTarget = paceTarget
            )
            RunWorkoutTrigger.ELEVATION -> RunWorkoutStepPlan(
                description, trigger, elevationChangeTargetMeters = elevationChange, isRestInterval = isRest,
                paceTarget = paceTarget
            )
            RunWorkoutTrigger.MANUAL -> RunWorkoutStepPlan(
                description, trigger, isRestInterval = isRest, paceTarget = paceTarget
            )
        }
    }
}

private fun String.toDecimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()

private const val DEFAULT_STEP_DISTANCE_METERS = 400.0
private const val DEFAULT_STEP_ELEVATION_METERS = 10.0
private const val MAX_WORKOUT_SET_COUNT = 10
private const val MAX_WORKOUT_STEP_COUNT = 20
