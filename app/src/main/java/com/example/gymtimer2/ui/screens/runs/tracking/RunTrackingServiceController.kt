@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Envia comandos do ViewModel ao serviço de localização. */
class RunTrackingServiceController(context: Context) {
    private val appContext = context.applicationContext

    /** Inicia uma nova sessão enquanto a tela está visível. */
    fun start(runId: Long) = startForeground(RunTrackingService.ACTION_START, runId)

    /** Inicia a gravação de um trecho depois de uma pausa. */
    fun resume(runId: Long) = startForeground(RunTrackingService.ACTION_RESUME, runId)

    /** Para atualizações GPS e conserva os dados já salvos. */
    fun pause(runId: Long) = send(RunTrackingService.ACTION_PAUSE, runId)

    /** Finaliza uma sessão que ainda tem serviço ativo. */
    fun finish(runId: Long, title: String? = null) {
        appContext.startService(intent(RunTrackingService.ACTION_FINISH, runId).putExtra(RunTrackingService.EXTRA_TITLE, title))
    }

    /** Pede ao serviço para parar as gravações antes de excluir a sessão aberta. */
    fun discard(runId: Long) = send(RunTrackingService.ACTION_DISCARD, runId)

    /** Inicia o serviço do tipo localização antes de enviar o comando. */
    private fun startForeground(action: String, runId: Long) {
        ContextCompat.startForegroundService(appContext, intent(action, runId))
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
