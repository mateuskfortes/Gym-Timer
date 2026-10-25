package com.example.gymtimer2.ui.screens.runs.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formata datas e rótulos compartilhados pelas telas de corrida. */
internal object RunFormatting {
    /** Mostra o instante salvo no formato usado pelo aplicativo. */
    fun formatDate(timestampMs: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(timestampMs))

    /** Usa a data como identificação das corridas salvas sem título. */
    fun displayTitle(run: RunModel): String = run.title ?: formatDate(run.startedAtEpochMs)

    /** Traduz o estado da sessão sem incluir regras de gravação na interface. */
    @Composable
    fun statusLabel(status: RunStatus): String = when (status) {
        RunStatus.STARTING, RunStatus.RUNNING -> stringResource(R.string.run_status_recording)
        RunStatus.PAUSED -> stringResource(R.string.run_status_paused)
        RunStatus.INTERRUPTED -> stringResource(R.string.run_status_interrupted)
        RunStatus.COMPLETED -> stringResource(R.string.run_status_completed)
    }
}
