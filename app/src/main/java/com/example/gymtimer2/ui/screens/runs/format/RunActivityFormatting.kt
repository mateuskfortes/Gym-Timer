package com.example.gymtimer2.ui.screens.runs.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formata datas, tipos e estados exibidos no histórico de corridas. */
internal object RunActivityFormatting {
    /** Mostra o instante salvo no formato usado pelo aplicativo. */
    fun formatDate(timestampMs: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(timestampMs))

    /** Usa a data como identificação das corridas salvas sem título. */
    fun displayTitle(run: RunModel): String = run.title ?: formatDate(run.startedAtEpochMs)

    /** Retorna o rótulo localizado do tipo de corrida salvo. */
    @Composable
    fun activityTypeLabel(activityType: RunActivityType): String = stringResource(activityTypeResource(activityType))

    fun activityTypeResource(activityType: RunActivityType): Int = when (activityType) {
        RunActivityType.RUN -> R.string.run_activity_type
        RunActivityType.TRAIL_RUN -> R.string.run_activity_type_trail
        RunActivityType.VIRTUAL_RUN -> R.string.run_activity_type_virtual
        RunActivityType.WALK -> R.string.run_activity_type_walk
        RunActivityType.HIKE -> R.string.run_activity_type_hike
        RunActivityType.WHEELCHAIR -> R.string.run_activity_type_wheelchair
    }

    /** Retorna o rótulo localizado de uma tag de corrida. */
    @Composable
    fun activityTagLabel(tag: RunActivityTag): String = stringResource(activityTagResource(tag))

    fun activityTagResource(tag: RunActivityTag): Int = when (tag) {
        RunActivityTag.RACE -> R.string.run_activity_tag_race
        RunActivityTag.LONG_RUN -> R.string.run_activity_tag_long_run
        RunActivityTag.WORKOUT -> R.string.run_activity_tag_workout
        RunActivityTag.COMPETITION -> R.string.run_activity_tag_competition
        RunActivityTag.RECOVERY -> R.string.run_activity_tag_recovery
        RunActivityTag.WITH_PET -> R.string.run_activity_tag_with_pet
        RunActivityTag.WITH_KID -> R.string.run_activity_tag_with_kid
        RunActivityTag.FOR_A_CAUSE -> R.string.run_activity_tag_for_a_cause
    }

    /** Retorna o rótulo localizado da distância padrão de uma melhor marca. */
    fun bestEffortDistanceResource(distance: RunBenchmarkDistance): Int = when (distance) {
        RunBenchmarkDistance.FOUR_HUNDRED_METERS -> R.string.run_best_effort_400_meters
        RunBenchmarkDistance.ONE_KILOMETER -> R.string.run_best_effort_1_kilometer
        RunBenchmarkDistance.HALF_MILE -> R.string.run_best_effort_half_mile
        RunBenchmarkDistance.ONE_MILE -> R.string.run_best_effort_1_mile
        RunBenchmarkDistance.TWO_MILES -> R.string.run_best_effort_2_miles
        RunBenchmarkDistance.FIVE_KILOMETERS -> R.string.run_best_effort_5_kilometers
        RunBenchmarkDistance.TEN_KILOMETERS -> R.string.run_best_effort_10_kilometers
        RunBenchmarkDistance.FIFTEEN_KILOMETERS -> R.string.run_best_effort_15_kilometers
        RunBenchmarkDistance.TEN_MILES -> R.string.run_best_effort_10_miles
        RunBenchmarkDistance.TWENTY_KILOMETERS -> R.string.run_best_effort_20_kilometers
        RunBenchmarkDistance.HALF_MARATHON -> R.string.run_best_effort_half_marathon
        RunBenchmarkDistance.THIRTY_KILOMETERS -> R.string.run_best_effort_30_kilometers
        RunBenchmarkDistance.MARATHON -> R.string.run_best_effort_marathon
        RunBenchmarkDistance.FIFTY_KILOMETERS -> R.string.run_best_effort_50_kilometers
    }

    /** Traduz o estado da sessão sem incluir regras de gravação na interface. */
    @Composable
    fun statusLabel(status: RunStatus): String = when (status) {
        RunStatus.STARTING, RunStatus.RUNNING -> stringResource(R.string.run_status_recording)
        RunStatus.PAUSED -> stringResource(R.string.run_status_paused)
        RunStatus.AUTO_PAUSED -> stringResource(R.string.run_status_auto_paused)
        RunStatus.INTERRUPTED -> stringResource(R.string.run_status_interrupted)
        RunStatus.COMPLETED -> stringResource(R.string.run_status_completed)
    }
}
