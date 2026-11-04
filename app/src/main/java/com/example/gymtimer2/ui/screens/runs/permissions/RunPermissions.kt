package com.example.gymtimer2.ui.screens.runs.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat

internal enum class RunStartIssue {
    GPS_DISABLED,
    APPROXIMATE_LOCATION,
    LOCATION_DENIED
}

internal fun runLocationPermissionIssue(
    preciseLocationGranted: Boolean,
    approximateLocationGranted: Boolean
): RunStartIssue? = when {
    preciseLocationGranted -> null
    approximateLocationGranted -> RunStartIssue.APPROXIMATE_LOCATION
    else -> RunStartIssue.LOCATION_DENIED
}

/** Lista as permissões ausentes; reconhecimento de atividade só entra se houver sensor de passos. */
internal fun missingRunPermissions(
    context: Context,
    requestOptionalPermissions: Boolean = true
): Array<String> {
    val optionalPermissions = if (requestOptionalPermissions) {
        missingOptionalRunPermissions(context)
    } else {
        emptyList()
    }
    return runPermissionsToRequest(
        preciseLocationGranted = hasPreciseLocation(context),
        optionalPermissions = optionalPermissions,
        requestOptionalPermissions = requestOptionalPermissions
    )
}

internal fun runPermissionsToRequest(
    preciseLocationGranted: Boolean,
    optionalPermissions: List<String>,
    requestOptionalPermissions: Boolean
): Array<String> = buildList {
    if (!preciseLocationGranted) {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (requestOptionalPermissions) addAll(optionalPermissions)
}.toTypedArray()

private fun missingOptionalRunPermissions(context: Context): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !hasNotificationPermission(context)
    ) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        hasStepDetector(context) && !hasActivityRecognitionPermission(context)
    ) {
        add(Manifest.permission.ACTIVITY_RECOGNITION)
    }
}

/** Confirma que o Android concedeu localização precisa para o percurso. */
internal fun hasPreciseLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

internal fun hasApproximateLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Confirma a permissão de notificação nas versões que a solicitam em tempo de execução. */
internal fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun hasStepDetector(context: Context): Boolean =
    context.getSystemService(SensorManager::class.java).getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

private fun hasActivityRecognitionPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED

/** Detecta se o usuário negou o aviso na gaveta, sem bloquear a corrida. */
internal fun notificationPermissionWasDenied(grants: Map<String, Boolean>): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        grants[Manifest.permission.POST_NOTIFICATIONS] == false

/** Confirma que o provedor GPS está ativo antes de criar uma sessão. */
internal fun isGpsEnabled(context: Context): Boolean =
    context.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)
