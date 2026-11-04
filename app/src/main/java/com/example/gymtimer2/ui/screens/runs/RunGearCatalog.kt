package com.example.gymtimer2.ui.screens.runs

import android.graphics.Bitmap
import android.net.Uri
import com.example.gymtimer2.domain.model.RunGear

/** Itens e ações do catálogo local usados pelos formulários de corrida. */
data class RunGearCatalog(
    val items: List<RunGear> = emptyList(),
    val create: (String, (RunGear) -> Unit) -> Unit = { _, _ -> },
    val setDefault: (Long) -> Unit = {},
    val setRetired: (Long, Boolean) -> Unit = { _, _ -> },
    val rename: (Long, String, (Boolean) -> Unit) -> Unit = { _, _, onResult -> onResult(false) },
    val setPhoto: (Long, Bitmap) -> Unit = { _, _ -> },
    val removePhoto: (Long) -> Unit = {},
    val createPhotoCapture: ((Uri?) -> Unit) -> Unit = { onCreated -> onCreated(null) },
    val deletePhotoCapture: (Uri) -> Unit = {}
)
