package com.example.gymtimer2.ui.screens.runs.history

/** Rejects missing raster areas while allowing a few transparent edge samples. */
internal fun hasSufficientRunMapTileCoverage(sampledPixelsArgb: IntArray): Boolean {
    if (sampledPixelsArgb.isEmpty()) return false

    val opaqueSamples = sampledPixelsArgb.count { pixel -> (pixel ushr 24) >= MIN_OPAQUE_TILE_ALPHA }
    return opaqueSamples * 100 >= sampledPixelsArgb.size * MIN_OPAQUE_TILE_PERCENT
}

private const val MIN_OPAQUE_TILE_ALPHA = 240
private const val MIN_OPAQUE_TILE_PERCENT = 98
