package com.example.gymtimer2.ui.screens.runs.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Lista só as permissões que ainda faltam para iniciar a gravação. */
internal fun missingRunPermissions(context: Context): Array<String> = buildList {
    if (!hasPreciseLocation(context)) {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

/** Confirma que o Android concedeu localização precisa para o percurso. */
internal fun hasPreciseLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Confirma a permissão de notificação nas versões que a solicitam em tempo de execução. */
internal fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** Detecta se o usuário negou o aviso na gaveta, sem bloquear a corrida. */
internal fun notificationPermissionWasDenied(grants: Map<String, Boolean>): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        grants[Manifest.permission.POST_NOTIFICATIONS] == false

/** Confirma que o provedor GPS está ativo antes de criar uma sessão. */
internal fun isGpsEnabled(context: Context): Boolean =
    context.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)
