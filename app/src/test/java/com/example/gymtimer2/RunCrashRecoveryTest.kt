package com.example.gymtimer2

import android.app.job.JobScheduler
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.tracking.RunCrashRecovery
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunCrashRecoveryTest {
    @Test
    fun resumesOnlySessionsThatWereNotManuallyPaused() {
        assertTrue(RunStatus.STARTING.canRecoverAfterProcessDeath)
        assertTrue(RunStatus.RUNNING.canRecoverAfterProcessDeath)
        assertTrue(RunStatus.AUTO_PAUSED.canRecoverAfterProcessDeath)
        assertTrue(RunStatus.INTERRUPTED.canRecoverAfterProcessDeath)
        assertFalse(RunStatus.PAUSED.canRecoverAfterProcessDeath)
        assertFalse(RunStatus.COMPLETED.canRecoverAfterProcessDeath)
    }

    @Test
    fun fallsBackWhenExpeditedJobSchedulingFails() {
        assertTrue(RunCrashRecovery.shouldFallbackToRegularJob(JobScheduler.RESULT_FAILURE, true))
        assertFalse(RunCrashRecovery.shouldFallbackToRegularJob(JobScheduler.RESULT_SUCCESS, true))
        assertFalse(RunCrashRecovery.shouldFallbackToRegularJob(JobScheduler.RESULT_FAILURE, false))
    }
}
