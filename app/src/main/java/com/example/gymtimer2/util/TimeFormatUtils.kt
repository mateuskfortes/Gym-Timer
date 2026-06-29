package com.example.gymtimer2.util

import java.util.Locale

fun formatMillisToMinSec(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    if (minutes == 0L) {
        return String.format(Locale.getDefault(),"%d s", seconds)
    }
    else if (seconds == 0L) {
        return String.format(Locale.getDefault(),"%d min", minutes)
    }
    return String.format(Locale.getDefault(),"%d min %02d s", minutes, seconds)
}