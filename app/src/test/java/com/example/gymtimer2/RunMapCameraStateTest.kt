@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.runs.map.RunMapCameraState
import com.example.gymtimer2.ui.screens.runs.map.RunMapCameraState.Action
import org.junit.Assert.assertEquals
import org.junit.Test

/** Reproduz os conflitos entre gestos, atualizações GPS e movimentos automáticos. */
class RunMapCameraStateTest {
    /** O enquadramento e o zoom padrão não podem ser repetidos após a primeira posição. */
    @Test
    fun initializesViewportOnlyOnce() {
        val state = RunMapCameraState()
        assertEquals(Action.INITIALIZE, state.nextAction(true, true, true))
        state.markInitialized()
        assertEquals(Action.KEEP_VIEWPORT, state.nextAction(true, false, false))
    }

    /** Seguir novas posições não deve redefinir o zoom. */
    @Test
    fun followsNewLocationsWithoutResettingZoom() {
        val state = initializedState()
        assertEquals(Action.CENTER_WITHOUT_ZOOM, state.nextAction(true, true, false))
    }

    /** Dados GPS ou mudanças do painel não podem disputar a câmera com um gesto ativo. */
    @Test
    fun ignoresAutomaticMovementDuringGestures() {
        val state = initializedState()
        state.onGestureStarted()
        assertEquals(Action.KEEP_VIEWPORT, state.nextAction(true, true, true))
    }

    /** Explorar outra área suspende a centralização, inclusive nas próximas leituras GPS. */
    @Test
    fun leavesOffCenterViewportUntouchedAfterGesture() {
        val state = initializedState()
        state.onGestureStarted()
        state.onCameraIdle(isCenteredOnUser = false)
        assertEquals(Action.KEEP_VIEWPORT, state.nextAction(true, true, false))
        assertEquals(Action.KEEP_VIEWPORT, state.nextAction(true, true, true))
    }

    /** Eventos de câmera feitos pelo app não devem reativar o acompanhamento manualmente suspenso. */
    @Test
    fun programmaticIdleDoesNotResumeFollowing() {
        val state = initializedState()
        state.onGestureStarted()
        state.onCameraIdle(isCenteredOnUser = false)
        state.onCameraIdle(isCenteredOnUser = true)
        assertEquals(Action.KEEP_VIEWPORT, state.nextAction(true, true, false))
    }

    /** Zoom com a posição ainda centralizada mantém o acompanhamento na escala escolhida. */
    @Test
    fun centeredZoomGestureKeepsFollowingWithoutResettingZoom() {
        val state = initializedState()
        state.onGestureStarted()
        state.onCameraIdle(isCenteredOnUser = true)
        assertEquals(Action.CENTER_WITHOUT_ZOOM, state.nextAction(true, true, false))
    }

    /** Após centralizar, as próximas leituras GPS preservam a escala sem repetir a inicialização. */
    @Test
    fun recenterResumesFollowingWithoutInitializingAgain() {
        val state = initializedState()
        state.onGestureStarted()
        state.onCameraIdle(isCenteredOnUser = false)
        state.onRecenter()
        assertEquals(Action.CENTER_WITHOUT_ZOOM, state.nextAction(true, true, false))
    }

    /** O histórico permanece no enquadramento escolhido após carregar a rota inicialmente. */
    @Test
    fun savedRouteUpdatesDoNotRefitViewport() {
        val state = initializedState()
        assertEquals(Action.KEEP_VIEWPORT, state.nextAction(false, true, false))
    }

    /** Abrir ou recolher o painel com acompanhamento ativo não deve aproximar a câmera. */
    @Test
    fun sheetPaddingChangesKeepUserZoom() {
        val state = initializedState()
        assertEquals(Action.CENTER_WITHOUT_ZOOM, state.nextAction(true, false, true))
    }

    /** Cria o estado após o primeiro enquadramento para verificar atualizações posteriores. */
    private fun initializedState() = RunMapCameraState().apply { markInitialized() }
}
