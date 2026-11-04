package com.example.gymtimer2.ui.screens.runs.history

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

internal fun createRunImageShareIntent(context: Context, uris: List<Uri>, label: String): Intent {
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.single())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    intent.type = "image/png"
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newUri(context.contentResolver, label, uris.first()).apply {
        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
    }
    return intent
}
