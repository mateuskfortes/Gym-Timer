@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import com.example.gymtimer2.domain.model.GpsPoint
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingService
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingServiceController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Verifica a notificação publicada pelo serviço real em um dispositivo com GPS habilitado. */
class RunTrackingNotificationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule
    val permissions = GrantPermissionRule.grant(*buildList {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray())

    /** A notificação deve acompanhar início, pausa e retomada e desaparecer ao concluir. */
    @Test
    fun notificationFollowsTrackingLifecycle() {
        val context = composeRule.activity
        val repository = (context.application as GymApplication).container.runRepository
        val manager = context.getSystemService(NotificationManager::class.java)
        val controller = RunTrackingServiceController(context)
        val id = runBlocking { repository.startRun(System.currentTimeMillis()) }
        try {
            composeRule.runOnIdle { controller.start(id) }
            awaitNotification(manager, R.string.run_notification_running)
            assertTrue(notification(manager).flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertNotNull(notification(manager).contentIntent)
            composeRule.waitUntil(10_000) { runBlocking { repository.getRun(id)?.status == RunStatus.RUNNING } }
            composeRule.runOnIdle { controller.pause(id) }
            awaitNotification(manager, R.string.run_notification_paused)
            composeRule.waitUntil(10_000) { runBlocking { repository.getRun(id)?.status == RunStatus.PAUSED } }
            composeRule.runOnIdle { controller.resume(id) }
            awaitNotification(manager, R.string.run_notification_running)
            composeRule.waitUntil(10_000) { runBlocking { repository.getRun(id)?.status == RunStatus.RUNNING } }
            composeRule.runOnIdle { controller.finish(id, "Corrida no parque") }
            composeRule.waitUntil(10_000) { manager.activeNotifications.none {
                it.notification.extras.getString(Notification.EXTRA_TITLE) == context.getString(R.string.run_notification_title)
            } }
            composeRule.waitUntil(10_000) { runBlocking { repository.getRun(id)?.status == RunStatus.COMPLETED } }
            assertEquals("Corrida no parque", runBlocking { repository.getRun(id)?.title })
        } finally {
            context.stopService(Intent(context, RunTrackingService::class.java))
            runBlocking {
                repository.finishRun(id, 0L, System.currentTimeMillis())
                repository.deleteCompletedRun(id)
            }
        }
    }

    /** O descarte pelo serviço remove dados e notificação sem salvar uma corrida concluída. */
    @Test
    fun discardStopsTrackingAndRemovesRoute() {
        val context = composeRule.activity
        val repository = (context.application as GymApplication).container.runRepository
        val manager = context.getSystemService(NotificationManager::class.java)
        val controller = RunTrackingServiceController(context)
        val id = runBlocking { repository.startRun(System.currentTimeMillis()) }
        try {
            composeRule.runOnIdle { controller.start(id) }
            awaitNotification(manager, R.string.run_notification_running)
            composeRule.waitUntil(10_000) { runBlocking { repository.getRun(id)?.status == RunStatus.RUNNING } }
            runBlocking { repository.appendLocation(id, GpsPoint(-23.55, -46.63, 1_700_000_000_000L, 4f), 0.0) }
            composeRule.runOnIdle { controller.discard(id) }
            composeRule.waitUntil(10_000) { runBlocking { repository.getRun(id) == null } }
            composeRule.waitUntil(10_000) { !RunTrackingService.isTracking(id) }
            composeRule.waitUntil(10_000) { manager.activeNotifications.none {
                it.notification.extras.getString(Notification.EXTRA_TITLE) == context.getString(R.string.run_notification_title)
            } }
            assertNull(runBlocking { repository.getRun(id) })
            assertEquals(0, runBlocking { repository.observePoints(id).first().size })
        } finally {
            context.stopService(Intent(context, RunTrackingService::class.java))
            runBlocking {
                repository.finishRun(id, 0L, System.currentTimeMillis())
                repository.deleteCompletedRun(id)
            }
        }
    }

    /** Aguarda a publicação efetiva pelo Android, sem usar tempos fixos de espera. */
    private fun awaitNotification(manager: NotificationManager, textResource: Int) {
        composeRule.waitUntil(10_000) {
            manager.activeNotifications.any {
                it.notification.extras.getString(Notification.EXTRA_TEXT) == composeRule.activity.getString(textResource)
            }
        }
    }

    /** Localiza somente a notificação do serviço de corrida. */
    private fun notification(manager: NotificationManager): Notification = manager.activeNotifications.first {
        it.notification.extras.getString(Notification.EXTRA_TITLE) == composeRule.activity.getString(R.string.run_notification_title)
    }.notification
}
