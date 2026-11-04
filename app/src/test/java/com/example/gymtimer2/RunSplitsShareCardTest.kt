package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunSplit
import com.example.gymtimer2.ui.screens.runs.components.runSplitPaceBarWidthRatios
import com.example.gymtimer2.ui.screens.runs.components.runSplitSharePages
import com.example.gymtimer2.ui.screens.runs.history.formatRunSplitsShareText
import org.junit.Assert.assertEquals
import org.junit.Test

class RunSplitsShareCardTest {
    @Test
    fun shareTextKeepsTitleRowsAndGpsNoteInOrder() {
        val text = formatRunSplitsShareText(
            title = "Parciais da corrida por km",
            rows = listOf("1 km · 05:20", "2 km · 05:12"),
            estimateNote = "Tempos estimados a partir do GPS."
        )

        assertEquals(
            """
                Parciais da corrida por km
                1 km · 05:20
                2 km · 05:12
                Tempos estimados a partir do GPS.
            """.trimIndent(),
            text
        )
    }

    @Test
    fun sharePagesKeepEverySplitInOrder() {
        val splits = (1..19).map { RunSplit(it, it * 1_000L) }

        val pages = runSplitSharePages(splits)

        assertEquals(listOf(8, 8, 3), pages.map { it.size })
        assertEquals(splits, pages.flatten())
    }

    @Test
    fun fasterSplitUsesLongerBarAndEqualPacesUseFullBars() {
        val variedPaces = runSplitPaceBarWidthRatios(listOf(RunSplit(1, 200_000), RunSplit(2, 400_000)))
        assertEquals(1f, variedPaces[0], 0f)
        assertEquals(0.35f, variedPaces[1], 0.0001f)
        assertEquals(listOf(1f, 1f), runSplitPaceBarWidthRatios(listOf(RunSplit(1, 200_000), RunSplit(2, 200_000))))
    }
}
