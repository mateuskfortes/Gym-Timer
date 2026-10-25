package com.example.gymtimer2.ui.screens.runs.map

/** Regras da câmera, independentes do SDK, para preservar a navegação manual. */
internal class RunMapCameraState {
    private var initialized = false
    private var followingUser = true

    /** Impede movimentos automáticos enquanto um gesto está em andamento. */
    var isGestureInProgress = false
        private set

    /** Distingue o enquadramento inicial dos movimentos que devem preservar o zoom. */
    enum class Action { INITIALIZE, CENTER_WITHOUT_ZOOM, KEEP_VIEWPORT }

    /** Decide se uma atualização realmente precisa mover a câmera. */
    fun nextAction(followLatestPoint: Boolean, positionChanged: Boolean, paddingChanged: Boolean): Action = when {
        isGestureInProgress -> Action.KEEP_VIEWPORT
        !initialized -> Action.INITIALIZE
        followingUser && (followLatestPoint || paddingChanged) && (positionChanged || paddingChanged) ->
            Action.CENTER_WITHOUT_ZOOM
        else -> Action.KEEP_VIEWPORT
    }

    /** O zoom padrão e o enquadramento da rota são aplicados apenas na primeira posição. */
    fun markInitialized() {
        initialized = true
    }

    /** Entrega o controle ao usuário antes de receber novas coordenadas GPS. */
    fun onGestureStarted() {
        isGestureInProgress = true
        followingUser = false
    }

    /** Um zoom que mantém a posição centralizada continua seguindo sem mudar a escala. */
    fun onCameraIdle(isCenteredOnUser: Boolean) {
        if (!isGestureInProgress) return
        isGestureInProgress = false
        followingUser = isCenteredOnUser
    }

    /** O botão de centralizar retoma o acompanhamento após restaurar a escala da corrida. */
    fun onRecenter() {
        followingUser = true
        isGestureInProgress = false
    }
}
