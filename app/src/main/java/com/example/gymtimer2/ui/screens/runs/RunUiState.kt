package com.example.gymtimer2.ui.screens.runs

import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel

/** Estado de leitura que a tela de corridas apresenta. */
data class RunUiState(
    /** Define se a sessão atual está aberta em tela cheia. */
    val isActiveRunOpen: Boolean = false,
    /** Sessão atual em andamento, pausada ou interrompida. */
    val activeRun: RunModel? = null,
    /** Coordenadas da sessão atual. */
    val activePoints: List<RunPointModel> = emptyList(),
    /** Corridas salvas no histórico. */
    val completedRuns: List<RunModel> = emptyList(),
    /** Corrida aberta para consulta detalhada. */
    val selectedRun: RunModel? = null,
    /** Coordenadas da corrida aberta para consulta. */
    val selectedPoints: List<RunPointModel> = emptyList()
)
