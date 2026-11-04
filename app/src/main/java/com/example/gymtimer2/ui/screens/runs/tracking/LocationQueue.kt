package com.example.gymtimer2.ui.screens.runs.tracking

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Processa leituras em ordem e permite aguardar a gravação antes de mudar de trecho. */
internal class LocationQueue<T>(
    scope: CoroutineScope,
    private val onFailure: (Exception) -> Unit,
    private val process: suspend (T) -> Unit
) {
    /** Representa uma leitura GPS ou uma barreira de sincronização na mesma fila. */
    private sealed interface Item<out T> {
        /** Posição que será persistida na ordem em que chegou. */
        data class Reading<T>(val value: T) : Item<T>
        /** Marca o ponto até o qual o serviço deve aguardar. */
        data class Barrier(val done: CompletableDeferred<Unit>) : Item<Nothing>
    }

    private val channel = Channel<Item<T>>(Channel.UNLIMITED)
    private val finished = CompletableDeferred<Unit>()
    private val worker = scope.launch(Dispatchers.IO) {
        for (item in channel) when (item) {
            is Item.Reading -> processReading(item.value)
            is Item.Barrier -> item.done.complete(Unit)
        }
    }

    init {
        worker.invokeOnCompletion { error ->
            if (error == null) finished.complete(Unit) else finished.completeExceptionally(error)
        }
    }

    // Isola uma falha por amostra para manter a fila processando as próximas posições.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun processReading(value: T) {
        try {
            process(value)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onFailure(error)
        }
    }

    /** Enfileira uma posição sem bloquear o callback do provedor GPS. */
    fun offer(value: T) { channel.trySend(Item.Reading(value)) }

    /** Aguarda todas as posições enviadas antes da barreira. */
    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        val handle = worker.invokeOnCompletion { error ->
            if (!done.isCompleted) done.completeExceptionally(error ?: IllegalStateException("Location queue stopped"))
        }
        try {
            channel.send(Item.Barrier(done))
            done.await()
        } finally {
            handle.dispose()
        }
    }

    /** Fecha a fila depois de processar as posições pendentes. */
    suspend fun close() {
        channel.close()
        finished.await()
    }

    /** Descarta posições pendentes ao apagar uma corrida. */
    suspend fun cancel() {
        channel.close()
        worker.cancelAndJoin()
    }
}
