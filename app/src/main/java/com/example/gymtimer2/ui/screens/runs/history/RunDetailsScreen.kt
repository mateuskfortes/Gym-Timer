package com.example.gymtimer2.ui.screens.runs.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunMedia
import com.example.gymtimer2.domain.model.RunLocalSegmentSelection
import com.example.gymtimer2.ui.screens.runs.RunGearCatalog
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.data.exporting.RunGpxDocument
import com.example.gymtimer2.data.exporting.RunFitDocument
import com.example.gymtimer2.domain.model.RunLocalSegmentTrack
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.RunStepRateSample
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.ui.screens.runs.components.RunSplitsSection
import com.example.gymtimer2.ui.screens.runs.components.RunShareCardColors
import com.example.gymtimer2.ui.screens.runs.components.RunLapsSection
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.components.RunWorkoutSection
import com.example.gymtimer2.ui.screens.runs.format.LocalRunDistanceUnit
import kotlinx.coroutines.launch

/** Mostra o mapa e as métricas de uma corrida escolhida no histórico. */
@Composable
internal fun RunDetailsScreen(
    run: RunModel,
    points: List<RunPointModel>,
    stepRateSamples: List<RunStepRateSample>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdateSaveDetails: (RunSaveDetails) -> Unit = {},
    modifier: Modifier = Modifier,
    laps: List<RunLap> = emptyList(),
    workoutSteps: List<RunWorkoutStep> = emptyList(),
    bestEffortOverrides: List<RunBestEffortOverride> = emptyList(),
    media: List<RunMedia> = emptyList(),
    onAddMedia: (List<Uri>) -> Unit = {},
    onRemoveMedia: (Long) -> Unit = {},
    onSetFeaturedMedia: (Long) -> Unit = {},
    onSaveBestEffortOverride: (RunBestEffortOverride) -> Unit = {},
    onClearBestEffortOverride: (RunBenchmarkDistance) -> Unit = {},
    gearCatalog: RunGearCatalog = RunGearCatalog(),
    onSaveLocalSegment: suspend (Long, String, Int, Double, Double) -> Boolean = { _, _, _, _, _ -> false },
    onExportRunGpx: (Uri) -> Unit = {},
    onExportRunFit: (Uri) -> Unit = {},
    onLocalSegmentSaved: () -> Unit = {},
    localSegmentEfforts: List<RunLocalSegmentEffort> = emptyList(),
    plannedRoutes: List<PlannedRouteModel> = emptyList(),
    privacyZones: List<RunPrivacyZone> = emptyList(),
    isRouteInUse: Boolean = false,
    onCropRun: suspend (Long, Long) -> RunCropOutcome = { _, _ -> RunCropOutcome.RUN_NOT_COMPLETED }
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    val distanceUnit = LocalRunDistanceUnit.current
    val shareablePoints = remember(run, points, privacyZones) {
        runMapShareablePoints(run, points, privacyZones)
    }
    val canShareMap = remember(shareablePoints) { runMapShareRegion(shareablePoints) != null }
    val shareCardColors = RunShareCardColors(
        background = MaterialTheme.colorScheme.background.toArgb(),
        surface = MaterialTheme.colorScheme.surface.toArgb(),
        text = MaterialTheme.colorScheme.onSurface.toArgb(),
        secondaryText = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        accent = MaterialTheme.colorScheme.primary.toArgb(),
        divider = MaterialTheme.colorScheme.outlineVariant.toArgb()
    )
    var isEditingDetails by rememberSaveable(run.id) { mutableStateOf(false) }
    var isCreatingLocalSegment by rememberSaveable(run.id) { mutableStateOf(false) }
    var isCropping by rememberSaveable(run.id) { mutableStateOf(false) }
    val selectableSegmentTracks = remember(points) { RunLocalSegmentSelection.tracks(points) }
    val gpxExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(RunGpxDocument.MIME_TYPE)
    ) { uri -> uri?.let(onExportRunGpx) }
    val fitExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(RunFitDocument.MIME_TYPE)
    ) { uri -> uri?.let(onExportRunFit) }
    val defaultGpxFileName = stringResource(R.string.run_export_gpx_default_filename)
    val defaultFitFileName = stringResource(R.string.run_export_fit_default_filename)
    Column(modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        RunDetailsHeader(
            run = run,
            points = points,
            canShareMap = canShareMap,
            onBack = onBack,
            onShareMap = {
                shareScope.launch { shareRunMap(context, run, shareablePoints, distanceUnit, shareCardColors) }
            }
        )
        RunSavedMetadata(run)
        RunDetailsMediaSection(
            media = media,
            onAdd = onAddMedia,
            onRemove = onRemoveMedia,
            onSetFeatured = onSetFeaturedMedia
        )
        RunDetailsActions(
            canExport = RunGpxDocument.hasExportablePoints(points),
            canCreateLocalSegment = selectableSegmentTracks.isNotEmpty(),
            canCrop = points.size >= 2,
            onShareSummary = {
                shareScope.launch { shareRunSummary(context, run, distanceUnit, shareCardColors) }
            },
            onEditDetails = { isEditingDetails = true },
            onExportGpx = {
                gpxExporter.launch(RunGpxDocument.fileName(RunActivityFormatting.displayTitle(run), defaultGpxFileName))
            },
            onExportFit = {
                fitExporter.launch(RunFitDocument.fileName(RunActivityFormatting.displayTitle(run), defaultFitFileName))
            },
            onCreateLocalSegment = { isCreatingLocalSegment = true },
            onCrop = { isCropping = true }
        )
        RunDetailsMetricsSection(
            run = run,
            points = points,
            stepRateSamples = stepRateSamples,
            bestEffortOverrides = bestEffortOverrides,
            onSaveBestEffortOverride = onSaveBestEffortOverride,
            onClearBestEffortOverride = onClearBestEffortOverride,
            localSegmentEfforts = localSegmentEfforts,
            plannedRoutes = plannedRoutes
        )
        RunWeatherSection(run.weather)
        Spacer(Modifier.height(14.dp))
        RunLapsSection(laps)
        RunWorkoutSection(run.workoutTitle, workoutSteps, isRunning = false)
        RunSplitsSection(points, onShareSplits = { splits ->
            shareScope.launch {
                shareRunSplits(context, run, splits, distanceUnit, shareCardColors)
            }
        })
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.run_delete_action), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(20.dp))
    }
    if (isEditingDetails) {
        RunEditDetailsDialog(
            run = run,
            gearCatalog = gearCatalog,
            onSave = {
                onUpdateSaveDetails(it)
                isEditingDetails = false
            },
            onDismiss = { isEditingDetails = false }
        )
    }
    RunLocalSegmentDialogHost(
        isVisible = isCreatingLocalSegment,
        run = run,
        tracks = selectableSegmentTracks,
        onSave = onSaveLocalSegment,
        onSaved = onLocalSegmentSaved,
        onDismiss = { isCreatingLocalSegment = false }
    )
    if (isCropping && points.size >= 2) {
        RunActivityCropDialog(
            run = run,
            points = points,
            laps = laps,
            workoutSteps = workoutSteps,
            localSegmentEfforts = localSegmentEfforts,
            isRouteInUse = isRouteInUse,
            onSave = onCropRun,
            onDismiss = { isCropping = false }
        )
    }
}

@Composable
private fun RunLocalSegmentDialogHost(
    isVisible: Boolean,
    run: RunModel,
    tracks: List<RunLocalSegmentTrack>,
    onSave: suspend (Long, String, Int, Double, Double) -> Boolean,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible || tracks.isEmpty()) return
    RunLocalSegmentDialog(run, tracks, onDismiss, onSave, onSaved)
}

@Composable
private fun RunSavedMetadata(run: RunModel) {
    val context = LocalContext.current
    if (run.isCommute) {
        Text(
            stringResource(R.string.run_commute_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (run.activityTags.isNotEmpty()) {
        Text(
            run.activityTags.joinToString(" · ") { tag ->
                context.getString(RunActivityFormatting.activityTagResource(tag))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    run.description?.let { description ->
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
    run.gearName?.let { gearName ->
        Text(
            stringResource(R.string.run_gear_summary, gearName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    run.privateNotes?.let { notes ->
        Text(
            stringResource(R.string.run_private_notes_summary, notes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    run.perceivedExertion?.let { exertion ->
        Text(
            stringResource(R.string.run_perceived_exertion_summary, exertion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
