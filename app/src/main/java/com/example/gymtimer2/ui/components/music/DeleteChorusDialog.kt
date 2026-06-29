package com.example.gymtimer2.ui.components.music

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.example.gymtimer2.domain.model.ChorusModel

fun deleteChorusDialog(context: Context, chorus: ChorusModel, onDelete: () -> Unit) {
    val builder = AlertDialog.Builder(context)
    builder.setCancelable(true)
    builder.setTitle("Excluir refrão")
    builder.setMessage("Deseja excluir o refrão ${chorus.name}?")

    builder.setPositiveButton("Excluir") { dialog, _ ->
        onDelete()
        dialog.dismiss()
    }

    builder.setNegativeButton("Cancelar") { dialog, _ ->
        dialog.dismiss()
    }

    val dialog = builder.create()

    // Set exclude button red
    dialog.setOnShowListener {
        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setTextColor(
            context.getColor(android.R.color.holo_red_dark)
        )
    }

    dialog.show()
}