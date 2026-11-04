package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.runs.tracking.LocationQueue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocationQueueTest {
    @Test
    fun flushWaitsForPendingReadingsAndKeepsTheQueueUsable() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val processed = mutableListOf<Int>()
        val queue = LocationQueue<Int>(this, onFailure = {}) { value ->
            if (value == 1) {
                started.complete(Unit)
                release.await()
            }
            processed.add(value)
        }
        queue.offer(1)
        queue.offer(2)
        started.await()
        val flush = async { queue.flush() }
        yield()
        assertFalse(flush.isCompleted)
        release.complete(Unit)
        flush.await()
        assertEquals(listOf(1, 2), processed)
        queue.offer(3)
        queue.close()
        assertEquals(listOf(1, 2, 3), processed)
    }

    @Test
    fun continuesProcessingAfterALocationFailure() = runBlocking {
        val processed = mutableListOf<Int>()
        val failures = mutableListOf<Exception>()
        val queue = LocationQueue<Int>(this, onFailure = failures::add) { value ->
            if (value == 1) throw IllegalStateException("simulated location failure")
            processed.add(value)
        }

        queue.offer(1)
        queue.offer(2)
        queue.flush()
        queue.close()

        assertEquals(listOf(2), processed)
        assertEquals(1, failures.size)
    }
}
