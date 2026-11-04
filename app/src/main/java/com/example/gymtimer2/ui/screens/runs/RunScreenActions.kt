package com.example.gymtimer2.ui.screens.runs

import android.net.Uri
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.RunGear
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunMapBounds
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.ui.screens.runs.RunViewModel

/** Actions for the run screens, grouped at the root UI boundary. */
data class RunScreenActions(
    val onPrepareRun: () -> Unit,
    val onBeginRun: (RunActivityType) -> Unit,
    val onCloseRunPreparation: () -> Unit,
    val onPauseRun: (Long) -> Unit,
    val onMarkLap: (Long) -> Unit,
    val onResumeRun: (Long) -> Unit,
    val onFinishRun: (Long, RunSaveDetails) -> Unit,
    val onDiscardRun: (Long) -> Unit,
    val onSelectRun: (Long) -> Unit,
    val onSelectPlannedRoute: (Long?, Long?) -> Unit,
    val onSetPlannedRouteFavorite: (Long, Boolean) -> Unit,
    val onSetPaceTarget: (RunPaceTarget?) -> Unit,
    val onSetWorkoutPlan: (RunWorkoutPlan?) -> Unit,
    val onSaveWorkoutTemplate: (Long?, RunWorkoutPlan) -> Unit,
    val onDeleteWorkoutTemplate: (Long) -> Unit,
    val onAdvanceWorkout: (Long) -> Unit,
    val onMuteRouteAlerts: (Long, Boolean) -> Unit,
    val onImportRoute: (Uri) -> Unit,
    val onDismissRouteImportError: () -> Unit,
    val onDeleteRun: (Long) -> Unit,
    val onUpdateRunSaveDetails: (Long, RunSaveDetails) -> Unit,
    val onAddRunMedia: (Long, List<Uri>) -> Unit,
    val onDeleteRunMedia: (Long) -> Unit,
    val onSetFeaturedRunMedia: (Long, Long) -> Unit,
    val onSaveBestEffortOverride: (RunBestEffortOverride) -> Unit,
    val onClearBestEffortOverride: (Long, RunBenchmarkDistance) -> Unit,
    val onBackFromRunDetails: () -> Unit,
    val onOpenActiveRun: () -> Unit,
    val onCloseActiveRun: () -> Unit,
    val onCalculateStreetReturn: () -> Unit,
    val onClearStreetReturn: () -> Unit,
    val onCalculatePlannedRouteGuidance: (List<RoutePoint>) -> Unit,
    val onMapPoiViewportChanged: (RunMapBounds?) -> Unit,
    val onSaveActiveRunMapCamera: (RunMapCameraSnapshot) -> Unit,
    val onSaveLocalSegment: suspend (Long, String, Int, Double, Double) -> Boolean,
    val onCropRun: suspend (Long, Long, Long) -> RunCropOutcome,
    val onExportRunGpx: suspend (Uri, RunModel, String, List<RunPointModel>) -> Unit,
    val onExportRunFit: suspend (Uri, RunModel, List<RunPointModel>, List<RunLap>) -> Unit,
    val gearCatalog: RunGearCatalog
)

fun RunViewModel.screenActions(runGears: List<RunGear>) = RunScreenActions(
    onPrepareRun = ::openRunPreparation,
    onBeginRun = ::startRun,
    onCloseRunPreparation = ::closeRunPreparation,
    onPauseRun = ::pauseRun,
    onMarkLap = ::markLap,
    onResumeRun = ::resumeRun,
    onFinishRun = ::finishRun,
    onDiscardRun = ::discardRun,
    onSelectRun = ::selectRun,
    onSelectPlannedRoute = ::selectPlannedRoute,
    onSetPlannedRouteFavorite = ::setPlannedRouteFavorite,
    onSetPaceTarget = ::setPaceTarget,
    onSetWorkoutPlan = ::setWorkoutPlan,
    onSaveWorkoutTemplate = ::saveWorkoutTemplate,
    onDeleteWorkoutTemplate = ::deleteWorkoutTemplate,
    onAdvanceWorkout = ::advanceWorkout,
    onMuteRouteAlerts = ::muteRouteAlerts,
    onImportRoute = ::importGpxRoute,
    onDismissRouteImportError = ::dismissRouteImportError,
    onDeleteRun = ::deleteCompletedRun,
    onUpdateRunSaveDetails = ::updateRunSaveDetails,
    onAddRunMedia = ::addRunMedia,
    onDeleteRunMedia = ::deleteRunMedia,
    onSetFeaturedRunMedia = ::setFeaturedRunMedia,
    onSaveBestEffortOverride = ::saveBestEffortOverride,
    onClearBestEffortOverride = ::clearBestEffortOverride,
    onBackFromRunDetails = { selectRun(null) },
    onOpenActiveRun = { setActiveRunOpen(true) },
    onCloseActiveRun = { setActiveRunOpen(false) },
    onCalculateStreetReturn = ::calculateStreetReturnRoute,
    onClearStreetReturn = ::clearStreetReturnRoute,
    onCalculatePlannedRouteGuidance = ::calculatePlannedRouteGuidance,
    onMapPoiViewportChanged = onMapPoiViewportChanged,
    onSaveActiveRunMapCamera = ::saveActiveRunMapCameraSnapshot,
    onSaveLocalSegment = ::saveRunAsLocalSegment,
    onCropRun = ::cropCompletedRun,
    onExportRunGpx = ::exportRunGpx,
    onExportRunFit = ::exportRunFit,
    gearCatalog = RunGearCatalog(
        items = runGears,
        create = ::createRunGear,
        setDefault = ::setDefaultRunGear,
        setRetired = ::setRunGearRetired,
        rename = ::renameRunGear,
        setPhoto = ::setRunGearPhoto,
        removePhoto = ::removeRunGearPhoto,
        createPhotoCapture = ::createRunGearPhotoCapture,
        deletePhotoCapture = ::deleteRunGearPhotoCapture
    )
)
