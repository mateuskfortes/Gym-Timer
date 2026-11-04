@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunActivityTag

/** Envia comandos do ViewModel ao serviço de localização. */
class RunTrackingServiceController(context: Context) {
    private val appContext = context.applicationContext

    /** Inicia uma nova sessão enquanto a tela está visível. */
    fun start(runId: Long) = startForeground(RunTrackingService.ACTION_START, runId)

    /** Inicia a gravação de um trecho depois de uma pausa. */
    fun resume(runId: Long) = startForeground(RunTrackingService.ACTION_RESUME, runId)

    /** Para atualizações GPS e conserva os dados já salvos. */
    fun pause(runId: Long) {
        clearCrashRecoveryCandidate(runId)
        send(RunTrackingService.ACTION_PAUSE, runId)
    }

    fun lap(runId: Long) = send(RunTrackingService.ACTION_LAP, runId)

    fun advanceWorkout(runId: Long) = send(RunTrackingService.ACTION_ADVANCE_WORKOUT, runId)

    /** Silencia o aviso atual até voltar à rota ou durante toda a corrida. */
    fun muteRouteAlerts(runId: Long, untilRouteReturn: Boolean) = send(
        if (untilRouteReturn) RunTrackingService.ACTION_MUTE_ROUTE_ALERTS_ONCE
        else RunTrackingService.ACTION_MUTE_ROUTE_ALERTS_FOR_RUN,
        runId
    )

    /** Finaliza uma sessão que ainda tem serviço ativo. */
    fun finish(runId: Long, saveDetails: RunSaveDetails = RunSaveDetails()) {
        clearCrashRecoveryCandidate(runId)
        appContext.startService(
            intent(RunTrackingService.ACTION_FINISH, runId)
                .putExtra(RunTrackingService.EXTRA_TITLE, saveDetails.title)
                .putExtra(RunTrackingService.EXTRA_DESCRIPTION, saveDetails.description)
                .putExtra(RunTrackingService.EXTRA_GEAR_NAME, saveDetails.gearName)
                .putExtra(RunTrackingService.EXTRA_GEAR_ID, saveDetails.gearId ?: -1L)
                .putExtra(RunTrackingService.EXTRA_PRIVATE_NOTES, saveDetails.privateNotes)
                .putExtra(RunTrackingService.EXTRA_IS_COMMUTE, saveDetails.isCommute)
                .putExtra(RunTrackingService.EXTRA_PERCEIVED_EXERTION, saveDetails.perceivedExertion ?: -1)
                .putExtra(RunTrackingService.EXTRA_ACTIVITY_TYPE, saveDetails.activityType?.name)
                .putExtra(RunTrackingService.EXTRA_ACTIVITY_TAGS, RunActivityTag.serialize(saveDetails.activityTags))
        )
    }

    /** Pede ao serviço para parar as gravações antes de excluir a sessão aberta. */
    fun discard(runId: Long) {
        clearCrashRecoveryCandidate(runId)
        send(RunTrackingService.ACTION_DISCARD, runId)
    }

    /** Inicia o serviço do tipo localização antes de enviar o comando. */
    private fun startForeground(action: String, runId: Long) {
        if (action == RunTrackingService.ACTION_START || action == RunTrackingService.ACTION_RESUME) {
            RunTrackingState.crashRecoverableRunId = runId
        }
        try {
            ContextCompat.startForegroundService(appContext, intent(action, runId))
        } catch (error: SecurityException) {
            clearCrashRecoveryCandidate(runId)
            throw error
        } catch (error: IllegalStateException) {
            clearCrashRecoveryCandidate(runId)
            throw error
        }
    }

    private fun clearCrashRecoveryCandidate(runId: Long) {
        if (RunTrackingState.crashRecoverableRunId == runId) {
            RunTrackingState.crashRecoverableRunId = null
        }
    }

    /** Envia uma ação para o serviço que já está em primeiro plano. */
    private fun send(action: String, runId: Long) {
        appContext.startService(intent(action, runId))
    }

    /** Cria uma mensagem explícita com o ID da sessão. */
    private fun intent(action: String, runId: Long) = Intent(appContext, RunTrackingService::class.java)
        .setAction(action)
        .putExtra(RunTrackingService.EXTRA_RUN_ID, runId)
}
