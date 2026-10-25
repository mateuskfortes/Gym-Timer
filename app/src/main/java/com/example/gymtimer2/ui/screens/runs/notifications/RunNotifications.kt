package com.example.gymtimer2.ui.screens.runs.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Identifica o mesmo canal no serviço e no atalho para as configurações do Android. */
internal const val RUN_NOTIFICATION_CHANNEL_ID = "run_tracking"

/** Reconsulta a permissão e o canal quando a pessoa retorna das configurações. */
@Composable
internal fun rememberRunNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) { mutableStateOf(areRunNotificationsEnabled(context)) }
    LifecycleResumeEffect(context) {
        enabled = areRunNotificationsEnabled(context)
        onPauseOrDispose { }
    }
    return enabled
}

/** Considera tanto o bloqueio geral do aplicativo quanto o bloqueio específico do canal. */
internal fun areRunNotificationsEnabled(context: Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    val channel = context.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(RUN_NOTIFICATION_CHANNEL_ID)
    return channel?.importance != NotificationManager.IMPORTANCE_NONE
}

/** Abre o canal da corrida para permitir o aviso sem encerrar a sessão atual. */
internal fun openRunNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, RUN_NOTIFICATION_CHANNEL_ID)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }
    context.startActivity(intent)
}
