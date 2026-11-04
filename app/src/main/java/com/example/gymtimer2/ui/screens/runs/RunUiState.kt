package com.example.gymtimer2.ui.screens.runs

import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunTurnGuidance
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutTemplate
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.RunStepRateSample
import com.example.gymtimer2.domain.model.RunStreetGuidance
import com.example.gymtimer2.domain.model.RunStreetRoute
import com.example.gymtimer2.domain.model.RunMapPoi
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.RunBestEffortRecords
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunHeatmapCell
import com.example.gymtimer2.domain.model.RunGear
import com.example.gymtimer2.domain.model.RunElevationProfilePoint
import com.example.gymtimer2.domain.model.RunMedia
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunRoutePosition
import com.example.gymtimer2.domain.model.RunPrivacyZone

/** Estado de leitura que a tela de corridas apresenta. */
data class RunUiState(
    /** Define se a sessão atual está aberta em tela cheia. */
    val isActiveRunOpen: Boolean = false,
    /** Define se a tela de preparação para iniciar uma corrida está aberta. */
    val isRunPreparationOpen: Boolean = false,
    /** Sessão atual em andamento, pausada ou interrompida. */
    val activeRun: RunModel? = null,
    /** Coordenadas da sessão atual. */
    val activePoints: List<RunPointModel> = emptyList(),
    /** Células agregadas de corridas concluídas recentes para o mapa de atividade local. */
    val weeklyHeatmapCells: List<RunHeatmapCell> = emptyList(),
    /** Células agregadas de todas as rotas concluídas para o mapa de histórico local. */
    val historyHeatmapCells: List<RunHeatmapCell> = emptyList(),
    /** Enquadramento salvo para a sessão ativa, se existir. */
    val activeMapCameraSnapshot: RunMapCameraSnapshot? = null,
    /** Corrida salva selecionada como alvo para uma nova gravação. */
    val plannedRouteRunId: Long? = null,
    /** Percurso GPX selecionado; nulo quando o alvo vem do histórico. */
    val plannedRouteId: Long? = null,
    /** Coordenadas da rota-alvo, quando uma rota está selecionada. */
    val plannedRoutePoints: List<RoutePoint> = emptyList(),
    /** Posição GPS projetada no percurso selecionado, quando há uma leitura confiável. */
    val plannedRoutePosition: RunRoutePosition? = null,
    /** Percursos GPX importados e guardados no dispositivo. */
    val plannedRoutes: List<PlannedRouteModel> = emptyList(),
    /** Meta opcional de pace escolhida na preparação da próxima corrida. */
    val selectedPaceTarget: RunPaceTarget? = null,
    /** Treino intervalado selecionado para a próxima corrida. */
    val selectedWorkoutPlan: RunWorkoutPlan? = null,
    /** Modelos locais que podem ser reutilizados na preparação de uma corrida. */
    val workoutTemplates: List<RunWorkoutTemplate> = emptyList(),
    /** ID do modelo escolhido, ou nulo quando a corrida não usa um modelo salvo. */
    val selectedWorkoutTemplateId: Long? = null,
    /** Etapas persistidas da corrida atual. */
    val activeWorkoutSteps: List<RunWorkoutStep> = emptyList(),
    /** Etapas persistidas da corrida aberta no histórico. */
    val selectedWorkoutSteps: List<RunWorkoutStep> = emptyList(),
    /** Indica uma falha de importação para que a tela apresente uma mensagem. */
    val routeImportFailed: Boolean = false,
    /** Indica que a última posição válida está longe da rota-alvo. */
    val isOffPlannedRoute: Boolean = false,
    /** Indica que o aviso atual foi silenciado até a posição voltar à rota. */
    val isRouteAlertsMutedUntilReturn: Boolean = false,
    /** Marcos manuais da sessão atual. */
    val activeLaps: List<RunLap> = emptyList(),
    /** Qualidade da última leitura GPS recebida. */
    val gpsStatus: RunGpsStatus = RunGpsStatus.SEARCHING,
    /** Próxima mudança de direção inferida do percurso selecionado. */
    val nextTurnGuidance: RunTurnGuidance? = null,
    /** Próximas mudanças de direção visíveis conforme as preferências de navegação. */
    val upcomingTurnGuidances: List<RunTurnGuidance> = emptyList(),
    /** Pace atual em segundos por quilômetro, ou nulo sem leitura confiável. */
    val currentPaceSecondsPerKm: Double? = null,
    /** Cadência atual em passos por minuto, ou nulo sem sensor ativo. */
    val currentCadenceStepsPerMinute: Int? = null,
    /** Corridas salvas no histórico. */
    val completedRuns: List<RunModel> = emptyList(),
    /** Equipamentos locais disponíveis ao salvar ou editar uma corrida. */
    val runGears: List<RunGear> = emptyList(),
    /** Melhores esforços calculados a partir das corridas salvas localmente. */
    val bestEffortRecords: RunBestEffortRecords = RunBestEffortRecords(),
    /** Correções de melhores esforços da corrida aberta no histórico. */
    val selectedBestEffortOverrides: List<RunBestEffortOverride> = emptyList(),
    /** Corrida aberta para consulta detalhada. */
    val selectedRun: RunModel? = null,
    /** Coordenadas da corrida aberta para consulta. */
    val selectedPoints: List<RunPointModel> = emptyList(),
    /** Amostras de cadência da corrida aberta para consulta. */
    val selectedStepRateSamples: List<RunStepRateSample> = emptyList(),
    /** Zonas que ocultam coordenadas em compartilhamentos e exportações. */
    val privacyZones: List<RunPrivacyZone> = emptyList(),
    /** Mídia local anexada à corrida aberta no histórico. */
    val selectedRunMedia: List<RunMedia> = emptyList(),
    /** Marcos manuais da corrida aberta para consulta. */
    val selectedLaps: List<RunLap> = emptyList(),
    /** Tentativas locais ativas ou segmentos próximos do início. */
    val localSegments: List<RunLiveSegment> = emptyList(),
    /** Tentativas persistidas da corrida aberta no histórico. */
    val selectedLocalSegmentEfforts: List<RunLocalSegmentEffort> = emptyList(),
    val plannedRouteGuidance: RunPlannedRouteGuidanceState = RunPlannedRouteGuidanceState(),
    val streetReturnState: RunStreetReturnState = RunStreetReturnState(),
    val mapPoiState: RunMapPoiUiState = RunMapPoiUiState()
) {
    /** Identifica a origem do percurso para reenquadrar o mapa quando a seleção muda. */
    val plannedRouteKey: String?
        get() = plannedRouteRunId?.let { "run:$it" } ?: plannedRouteId?.let { "gpx:$it" }

    /** Velocidade atual derivada da mesma leitura GPS usada no pace. */
    val currentSpeedKmPerHour: Double?
        get() = currentPaceSecondsPerKm?.let { 3_600.0 / it }
}

data class RunMapPoiUiState(
    val points: List<RunMapPoi> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val hasError: Boolean = false,
    val needsZoom: Boolean = false
)

/** Progresso do percurso escolhido e comparação com o melhor tempo local. */
data class RunLiveSegment(
    val name: String?,
    val progressMeters: Double,
    val distanceMeters: Double,
    val elapsedDurationMs: Long,
    val bestDurationMs: Long?,
    val differenceFromBestMs: Long?,
    val isCompleted: Boolean,
    val isNewRecord: Boolean,
    val distanceToStartMeters: Double? = null,
    val elevationProfile: List<RunElevationProfilePoint> = emptyList(),
    val routeId: Long? = null
)

data class RunStreetReturnState(
    val route: RunStreetRoute? = null,
    val nextGuidance: RunStreetGuidance? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)

data class RunPlannedRouteGuidanceState(
    val route: RunStreetRoute? = null,
    val nextGuidance: RunStreetGuidance? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)
