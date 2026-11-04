package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.runs.components.RunSummaryShareCardContent
import com.example.gymtimer2.ui.screens.runs.components.RunSummaryShareMetric
import com.example.gymtimer2.ui.screens.runs.history.formatRunSummaryShareText
import org.junit.Assert.assertEquals
import org.junit.Test

class RunSummarySharingTest {
    @Test
    fun fallbackTextIncludesRunMetadataAndEverySummaryMetric() {
        val content = RunSummaryShareCardContent(
            appName = "Gym Timer",
            activityTitle = "Corrida matinal",
            activityDate = "02/11/2026",
            metrics = listOf(
                RunSummaryShareMetric("Distância", "5,2 km"),
                RunSummaryShareMetric("Tempo", "00:30:00"),
                RunSummaryShareMetric("Pace", "5:46 /km"),
                RunSummaryShareMetric("Velocidade", "10,4 km/h")
            ),
            footer = "Dados locais desta corrida"
        )

        assertEquals(
            """
                Corrida matinal
                02/11/2026
                Distância: 5,2 km
                Tempo: 00:30:00
                Pace: 5:46 /km
                Velocidade: 10,4 km/h
                Dados locais desta corrida
            """.trimIndent(),
            formatRunSummaryShareText(content)
        )
    }
}
