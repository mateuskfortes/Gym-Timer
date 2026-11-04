package com.example.gymtimer2.ui.screens.runs.active

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutTemplate
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunHeatmapCell
import com.example.gymtimer2.domain.model.RunMapBounds
import com.example.gymtimer2.domain.model.isRunRouteStartFarFromLocation
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.RunMapPoiUiState
import com.example.gymtimer2.ui.screens.runs.map.RunMap
import com.example.gymtimer2.ui.screens.runs.map.RunMapLayer
import com.example.gymtimer2.ui.screens.runs.permissions.rememberRunStartAction
import org.maplibre.android.geometry.LatLng

internal data class RunPreparationScreenState(
    val completedRuns: List<RunModel>,
    val plannedRoutes: List<PlannedRouteModel>,
    val selectedRouteRunId: Long?,
    val selectedRouteId: Long?,
    val plannedPoints: List<RoutePoint>,
    val manualLapsEnabled: Boolean,
    val paceTarget: RunPaceTarget?,
    val workoutPlan: RunWorkoutPlan?,
    val workoutTemplates: List<RunWorkoutTemplate>,
    val selectedWorkoutTemplateId: Long?,
    val routeImportFailed: Boolean,
    val mapPoiState: RunMapPoiUiState = RunMapPoiUiState(),
    val historyHeatmapCells: List<RunHeatmapCell> = emptyList(),
    val selectedMapLayers: Set<RunMapLayer>? = null
)

internal data class RunPreparationScreenActions(
    val onManualLapsChanged: (Boolean) -> Unit,
    val onSetRouteFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    val onSetPaceTarget: (RunPaceTarget?) -> Unit,
    val onSetWorkoutPlan: (RunWorkoutPlan?) -> Unit,
    val onSaveWorkoutTemplate: (Long?, RunWorkoutPlan) -> Unit,
    val onDeleteWorkoutTemplate: (Long) -> Unit,
    val onSelectRoute: (Long?, Long?) -> Unit,
    val onImportRoute: (Uri) -> Unit,
    val onDismissImportError: () -> Unit,
    val onStart: (RunActivityType) -> Unit,
    val onBack: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onMapPoiViewportChanged: (RunMapBounds?) -> Unit = {},
    val onSelectedMapLayersChanged: ((Set<RunMapLayer>) -> Unit)? = null
)

private data class RunPreparationDialogState(
    val showRoutePicker: MutableState<Boolean>,
    val showPaceTargetDialog: MutableState<Boolean>,
    val showWorkoutEditor: MutableState<Boolean>,
    val showWorkoutTemplates: MutableState<Boolean>,
    val editingWorkoutTemplateId: MutableState<Long?>,
    val showActivityTypeDialog: MutableState<Boolean>,
    val showBatterySaverWarning: MutableState<Boolean>,
    val showGpsWarning: MutableState<Boolean>,
    val showFarRouteWarning: MutableState<Boolean>,
    val routeSelectionAttempt: MutableIntState,
    val checkedRouteSelection: MutableState<String?>,
    val activityType: MutableState<RunActivityType>
)

@Composable
private fun rememberRunPreparationDialogState() = RunPreparationDialogState(
    showRoutePicker = rememberSaveable { mutableStateOf(false) },
    showPaceTargetDialog = rememberSaveable { mutableStateOf(false) },
    showWorkoutEditor = rememberSaveable { mutableStateOf(false) },
    showWorkoutTemplates = rememberSaveable { mutableStateOf(false) },
    editingWorkoutTemplateId = rememberSaveable { mutableStateOf<Long?>(null) },
    showActivityTypeDialog = rememberSaveable { mutableStateOf(false) },
    showBatterySaverWarning = rememberSaveable { mutableStateOf(false) },
    showGpsWarning = rememberSaveable { mutableStateOf(false) },
    showFarRouteWarning = rememberSaveable { mutableStateOf(false) },
    routeSelectionAttempt = rememberSaveable { mutableIntStateOf(0) },
    checkedRouteSelection = rememberSaveable { mutableStateOf<String?>(null) },
    activityType = rememberSaveable { mutableStateOf(RunActivityType.RUN) }
)

/** Exibe o mapa e as métricas vazias antes de criar uma sessão de corrida. */
@Composable
internal fun RunPreparationScreen(
    screenState: RunPreparationScreenState,
    screenActions: RunPreparationScreenActions,
    modifier: Modifier = Modifier
) {
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val dialogs = rememberRunPreparationDialogState()
    val context = LocalContext.current
    val gpsStatus = rememberRunPreparationGpsStatus()
    val startRun = rememberRunStartAction(
        context = context,
        onStartRun = { screenActions.onStart(dialogs.activityType.value) },
        onMessage = {},
        requestOptionalPermissions = false
    )
    val startRunWithGpsWarning = {
        if (gpsStatus == RunGpsStatus.NO_SIGNAL) dialogs.showGpsWarning.value = true else startRun()
    }
    val selectedRouteTitle = screenState.completedRuns.firstOrNull { it.id == screenState.selectedRouteRunId }
        ?.let(RunActivityFormatting::displayTitle)
        ?: screenState.plannedRoutes.firstOrNull { it.id == screenState.selectedRouteId }?.name
    val selectedRouteKey = screenState.selectedRouteRunId?.let { "run:$it" } ?: screenState.selectedRouteId?.let { "gpx:$it" }
    val panelBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + PREPARATION_PANEL_HEIGHT
    val closePreparation = {
        dialogs.activityType.value = RunActivityType.RUN
        screenActions.onBack()
    }
    BackHandler(onBack = closePreparation)
    RunPreparationMapAndPanel(
        modifier = modifier,
        historyHeatmapCells = screenState.historyHeatmapCells,
        panelBottomPadding = panelBottomPadding,
        selectedRouteTitle = selectedRouteTitle,
        plannedPoints = screenState.plannedPoints,
        selectedRouteKey = selectedRouteKey,
        mapPoiState = screenState.mapPoiState,
        onMapPoiViewportChanged = screenActions.onMapPoiViewportChanged,
        selectedMapLayers = screenState.selectedMapLayers,
        onSelectedMapLayersChanged = screenActions.onSelectedMapLayersChanged,
        onInitialLocationChanged = { currentLocation = it },
        onBack = closePreparation,
        onOpenSettings = screenActions.onOpenSettings,
        gpsStatus = gpsStatus,
        paceTarget = screenState.paceTarget,
        workoutTitle = screenState.workoutPlan?.name,
        activityType = dialogs.activityType.value,
        manualLapsEnabled = screenState.manualLapsEnabled,
        onManualLapsChanged = screenActions.onManualLapsChanged,
        onChooseRoute = { dialogs.showRoutePicker.value = true },
        onEditPaceTarget = { dialogs.showPaceTargetDialog.value = true },
        onEditWorkout = { dialogs.showWorkoutTemplates.value = true },
        onEditActivityType = { dialogs.showActivityTypeDialog.value = true },
        onStart = {
            dispatchRunStart(
                activityType = dialogs.activityType.value,
                isBatterySaverEnabled = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true,
                onStart = { startRunWithGpsWarning() },
                onShowBatterySaverWarning = { dialogs.showBatterySaverWarning.value = true }
            )
        }
    )
    RunPreparationOverlays(
        screenState = screenState,
        screenActions = screenActions,
        dialogState = dialogs,
        currentLocation = currentLocation,
        context = context,
        startRun = startRun,
        startRunWithGpsWarning = startRunWithGpsWarning
    )
}

@Composable
private fun RunPreparationOverlays(
    screenState: RunPreparationScreenState,
    screenActions: RunPreparationScreenActions,
    dialogState: RunPreparationDialogState,
    currentLocation: LatLng?,
    context: Context,
    startRun: () -> Unit,
    startRunWithGpsWarning: () -> Unit
) {
    val selectedRouteKey = screenState.selectedRouteRunId?.let { "run:$it" }
        ?: screenState.selectedRouteId?.let { "gpx:$it" }
    val routeSelectionKey = selectedRouteKey?.let { "$it:${dialogState.routeSelectionAttempt.intValue}" }
    val availableRoutes = screenState.completedRuns.filter { it.distanceMeters.isFinite() && it.distanceMeters > 0.0 }
    val routePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(screenActions.onImportRoute)
    }

    RunPreparationRouteEffects(
        selectedRouteRunId = screenState.selectedRouteRunId,
        selectedRouteId = screenState.selectedRouteId,
        routeSelectionKey = routeSelectionKey,
        checkedRouteSelection = dialogState.checkedRouteSelection.value,
        plannedPoints = screenState.plannedPoints,
        currentLocation = currentLocation,
        onHideRoutePicker = { dialogState.showRoutePicker.value = false },
        onClearRouteCheck = {
            dialogState.checkedRouteSelection.value = null
            dialogState.showFarRouteWarning.value = false
        },
        onRouteChecked = { key, isFar ->
            dialogState.checkedRouteSelection.value = key
            dialogState.showFarRouteWarning.value = isFar
        }
    )
    RunPreparationRouteDialogs(
        showRoutePicker = dialogState.showRoutePicker.value,
        routes = availableRoutes,
        plannedRoutes = screenState.plannedRoutes,
        onSetRouteFavorite = screenActions.onSetRouteFavorite,
        onSelectRoute = { runId, routeId ->
            dialogState.routeSelectionAttempt.intValue++
            screenActions.onSelectRoute(runId, routeId)
            dialogState.showRoutePicker.value = false
        },
        onImportRoute = {
            routePicker.launch(arrayOf("application/gpx+xml", "application/gpx", "application/xml", "text/xml", "*/*"))
        },
        onDismissRoutePicker = { dialogState.showRoutePicker.value = false },
        showFarRouteWarning = dialogState.showFarRouteWarning.value,
        onUseRoute = { dialogState.showFarRouteWarning.value = false },
        onSwitchRoute = {
            dialogState.showFarRouteWarning.value = false
            dialogState.routeSelectionAttempt.intValue++
            screenActions.onSelectRoute(null, null)
            dialogState.showRoutePicker.value = true
        }
    )
    RunPaceTargetDialog(
        visible = dialogState.showPaceTargetDialog.value,
        initialTarget = screenState.paceTarget,
        onConfirm = {
            screenActions.onSetPaceTarget(it)
            dialogState.showPaceTargetDialog.value = false
        },
        onDismiss = { dialogState.showPaceTargetDialog.value = false }
    )
    RunPreparationActivityTypeOverlay(
        isVisible = dialogState.showActivityTypeDialog.value,
        activityType = dialogState.activityType.value,
        onSelect = {
            dialogState.activityType.value = it
            dialogState.showActivityTypeDialog.value = false
        },
        onDismiss = { dialogState.showActivityTypeDialog.value = false }
    )
    RunPreparationDialogs(
        showBatterySaverWarning = dialogState.showBatterySaverWarning.value,
        onContinueWithBatterySaver = {
            dialogState.showBatterySaverWarning.value = false
            startRunWithGpsWarning()
        },
        onDismissBatterySaverWarning = { dialogState.showBatterySaverWarning.value = false },
        onOpenBatterySaverSettings = {
            dialogState.showBatterySaverWarning.value = false
            openBatterySaverSettings(context)
        },
        showGpsWarning = dialogState.showGpsWarning.value,
        onContinueWithoutGps = {
            dialogState.showGpsWarning.value = false
            startRun()
        },
        onDismissGpsWarning = { dialogState.showGpsWarning.value = false },
        showWorkoutEditor = dialogState.showWorkoutEditor.value,
        workoutPlan = screenState.workoutTemplates.firstOrNull {
            it.id == dialogState.editingWorkoutTemplateId.value
        }?.plan,
        onSaveWorkout = { plan ->
            screenActions.onSaveWorkoutTemplate(dialogState.editingWorkoutTemplateId.value, plan)
            dialogState.showWorkoutEditor.value = false
        },
        onDismissWorkoutEditor = { dialogState.showWorkoutEditor.value = false },
        routeImportFailed = screenState.routeImportFailed,
        onDismissImportError = screenActions.onDismissImportError
    )
    RunPreparationWorkoutTemplatesOverlay(
        isVisible = dialogState.showWorkoutTemplates.value,
        templates = screenState.workoutTemplates,
        selectedTemplateId = screenState.selectedWorkoutTemplateId,
        onSelect = {
            screenActions.onSetWorkoutPlan(it?.plan)
            dialogState.showWorkoutTemplates.value = false
        },
        onCreate = {
            dialogState.editingWorkoutTemplateId.value = null
            dialogState.showWorkoutTemplates.value = false
            dialogState.showWorkoutEditor.value = true
        },
        onEdit = {
            dialogState.editingWorkoutTemplateId.value = it.id
            dialogState.showWorkoutTemplates.value = false
            dialogState.showWorkoutEditor.value = true
        },
        onDelete = screenActions.onDeleteWorkoutTemplate,
        onDismiss = { dialogState.showWorkoutTemplates.value = false }
    )
}

@Composable
private fun RunPreparationRouteDialogs(
    showRoutePicker: Boolean,
    routes: List<RunModel>,
    plannedRoutes: List<PlannedRouteModel>,
    onSetRouteFavorite: (Long, Boolean) -> Unit,
    onSelectRoute: (Long?, Long?) -> Unit,
    onImportRoute: () -> Unit,
    onDismissRoutePicker: () -> Unit,
    showFarRouteWarning: Boolean,
    onUseRoute: () -> Unit,
    onSwitchRoute: () -> Unit
) {
    if (showRoutePicker) {
        RunRoutePickerDialog(
            routes = routes,
            plannedRoutes = plannedRoutes,
            onSetRouteFavorite = onSetRouteFavorite,
            onSelectRoute = onSelectRoute,
            onImportRoute = onImportRoute,
            onDismiss = onDismissRoutePicker
        )
    }
    if (showFarRouteWarning) {
        RunRouteFarDistanceDialog(onUseRoute = onUseRoute, onSwitchRoute = onSwitchRoute)
    }
}

@Composable
private fun RunPreparationActivityTypeOverlay(
    isVisible: Boolean,
    activityType: RunActivityType,
    onSelect: (RunActivityType) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return
    RunActivityTypeDialog(activityType, onSelect, onDismiss)
}

@Composable
private fun RunPreparationWorkoutTemplatesOverlay(
    isVisible: Boolean,
    templates: List<RunWorkoutTemplate>,
    selectedTemplateId: Long?,
    onSelect: (RunWorkoutTemplate?) -> Unit,
    onCreate: () -> Unit,
    onEdit: (RunWorkoutTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return
    RunWorkoutTemplatesDialog(
        templates = templates,
        selectedTemplateId = selectedTemplateId,
        onSelect = onSelect,
        onCreate = onCreate,
        onEdit = onEdit,
        onDelete = onDelete,
        onDismiss = onDismiss
    )
}

@Composable
private fun RunPreparationMapAndPanel(
    modifier: Modifier,
    historyHeatmapCells: List<RunHeatmapCell>,
    panelBottomPadding: Dp,
    selectedRouteTitle: String?,
    plannedPoints: List<RoutePoint>,
    selectedRouteKey: String?,
    mapPoiState: RunMapPoiUiState,
    onMapPoiViewportChanged: (RunMapBounds?) -> Unit,
    selectedMapLayers: Set<RunMapLayer>?,
    onSelectedMapLayersChanged: ((Set<RunMapLayer>) -> Unit)?,
    onInitialLocationChanged: (LatLng?) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    gpsStatus: RunGpsStatus,
    paceTarget: RunPaceTarget?,
    workoutTitle: String?,
    activityType: RunActivityType,
    manualLapsEnabled: Boolean,
    onManualLapsChanged: (Boolean) -> Unit,
    onChooseRoute: () -> Unit,
    onEditPaceTarget: () -> Unit,
    onEditWorkout: () -> Unit,
    onEditActivityType: () -> Unit,
    onStart: () -> Unit
) {
    Box(modifier.fillMaxSize()) {
        RunMap(
            points = emptyList(),
            historyHeatmapCells = historyHeatmapCells,
            followLatestPoint = false,
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            bottomPadding = panelBottomPadding,
            showRecenterControl = true,
            showRouteOverviewControl = selectedRouteTitle != null,
            onInitialLocationChanged = onInitialLocationChanged,
            plannedPoints = plannedPoints,
            plannedRouteKey = selectedRouteKey,
            poiState = mapPoiState,
            onPoiViewportChanged = onMapPoiViewportChanged,
            selectedLayers = selectedMapLayers,
            onSelectedLayersChanged = onSelectedMapLayersChanged
        )
        RunBackButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(16.dp))
        RunPreparationSettingsButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
        )
        RunPreparationPanel(
            gpsStatus = gpsStatus,
            selectedRouteTitle = selectedRouteTitle,
            paceTarget = paceTarget,
            workoutTitle = workoutTitle,
            activityType = activityType,
            manualLapsEnabled = manualLapsEnabled,
            onManualLapsChanged = onManualLapsChanged,
            onChooseRoute = onChooseRoute,
            onEditPaceTarget = onEditPaceTarget,
            onEditWorkout = onEditWorkout,
            onEditActivityType = onEditActivityType,
            onStart = onStart,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RunPreparationRouteEffects(
    selectedRouteRunId: Long?,
    selectedRouteId: Long?,
    routeSelectionKey: String?,
    checkedRouteSelection: String?,
    plannedPoints: List<RoutePoint>,
    currentLocation: LatLng?,
    onHideRoutePicker: () -> Unit,
    onClearRouteCheck: () -> Unit,
    onRouteChecked: (String, Boolean) -> Unit
) {
    LaunchedEffect(selectedRouteRunId, selectedRouteId) {
        if (selectedRouteRunId != null || selectedRouteId != null) onHideRoutePicker()
    }
    LaunchedEffect(routeSelectionKey, plannedPoints, currentLocation) {
        val selectionKey = routeSelectionKey ?: run {
            onClearRouteCheck()
            return@LaunchedEffect
        }
        if (selectionKey == checkedRouteSelection) return@LaunchedEffect
        val location = currentLocation ?: return@LaunchedEffect
        if (plannedPoints.isEmpty()) return@LaunchedEffect

        val isFar = isRunRouteStartFarFromLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            route = plannedPoints
        )
        onRouteChecked(selectionKey, isFar)
    }
}

private fun openBatterySaverSettings(context: Context) {
    val batterySaverSettings = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    val settingsIntent = if (batterySaverSettings.resolveActivity(context.packageManager) != null) {
        batterySaverSettings
    } else {
        Intent(Settings.ACTION_SETTINGS)
    }
    context.startActivity(settingsIntent)
}

/** Escolhe entre iniciar a sessão e pedir confirmação quando a economia está ativa. */
internal fun dispatchRunStart(
    activityType: RunActivityType,
    isBatterySaverEnabled: Boolean,
    onStart: (RunActivityType) -> Unit,
    onShowBatterySaverWarning: () -> Unit
) {
    if (isBatterySaverEnabled) onShowBatterySaverWarning() else onStart(activityType)
}

private val PREPARATION_PANEL_HEIGHT = 490.dp
