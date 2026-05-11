package com.example.gymtimer2.controllers

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

fun canDrawOverlays(activity: Activity): Boolean {
    return Settings.canDrawOverlays(activity)
}

fun requestOverlayPermission(activity: Activity) {
    if (!Settings.canDrawOverlays(activity)) {

        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(true)
        builder.setTitle("Permisão de sobreposição necessária")
        builder.setMessage("Ative 'Aparecer sobre outros aplicativos' nas configurações")
        builder.setPositiveButton(
            "Abrir configurações",
            DialogInterface.OnClickListener { dialog, which ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${activity.packageName}".toUri()
                )
                activity.startActivity(intent)
            }
        )

        val dialog = builder.create()
        dialog.show()
    }
}