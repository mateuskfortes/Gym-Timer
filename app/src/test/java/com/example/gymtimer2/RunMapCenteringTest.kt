package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.runs.map.isRunMapPositionCentered
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Confirma que a escala global não mascara o deslocamento geográfico da câmera. */
class RunMapCenteringTest {
    /** Mesmo poucos pixels representam uma região distante quando o mapa está muito afastado. */
    @Test
    fun distantPositionIsNotCenteredEvenWhenItLooksCloseInPixels() {
        assertFalse(isRunMapPositionCentered(distancePx = 1.0, distanceMeters = 10_000.0, tolerancePx = 32.0))
    }

    /** Pequenas diferenças de posição dentro das duas tolerâncias não fazem o botão oscilar. */
    @Test
    fun nearbyPositionInsideVisualToleranceIsCentered() {
        assertTrue(isRunMapPositionCentered(distancePx = 5.0, distanceMeters = 10.0, tolerancePx = 32.0))
    }

    /** Com zoom alto, estar perto em metros não basta se o ponto estiver fora do centro visual. */
    @Test
    fun nearbyPositionOutsideVisualToleranceIsNotCentered() {
        assertFalse(isRunMapPositionCentered(distancePx = 40.0, distanceMeters = 5.0, tolerancePx = 32.0))
    }

    /** Leituras inválidas nunca devem esconder a ação de centralizar. */
    @Test
    fun invalidDistanceIsNotCentered() {
        assertFalse(isRunMapPositionCentered(distancePx = Double.NaN, distanceMeters = 0.0, tolerancePx = 32.0))
    }
}
