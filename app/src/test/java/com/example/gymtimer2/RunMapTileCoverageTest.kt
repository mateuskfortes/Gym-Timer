package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.runs.history.hasSufficientRunMapTileCoverage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMapTileCoverageTest {
    @Test
    fun acceptsFullyRenderedRasterSamples() {
        assertTrue(hasSufficientRunMapTileCoverage(IntArray(SAMPLE_COUNT) { OPAQUE_MAP_PIXEL }))
    }

    @Test
    fun rejectsTransparentSnapshotWithOnlyRoutePixels() {
        val routeOnlyPixels = IntArray(SAMPLE_COUNT)
        routeOnlyPixels[0] = OPAQUE_MAP_PIXEL

        assertFalse(hasSufficientRunMapTileCoverage(routeOnlyPixels))
    }

    @Test
    fun rejectsSnapshotWithMissingTileArea() {
        val pixels = IntArray(SAMPLE_COUNT) { OPAQUE_MAP_PIXEL }
        repeat(SAMPLE_COUNT / 16) { index -> pixels[index] = 0 }

        assertFalse(hasSufficientRunMapTileCoverage(pixels))
    }

    @Test
    fun allowsSmallTransparentRenderingEdges() {
        val pixels = IntArray(SAMPLE_COUNT) { OPAQUE_MAP_PIXEL }
        repeat(4) { index -> pixels[index] = 0 }

        assertTrue(hasSufficientRunMapTileCoverage(pixels))
    }

    private companion object {
        const val SAMPLE_COUNT = 16 * 16
        const val OPAQUE_MAP_PIXEL = -0x1000000
    }
}
