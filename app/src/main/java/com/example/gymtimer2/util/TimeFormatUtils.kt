package com.example.gymtimer2.util

import java.util.Locale

fun formatMillisToMinSec(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(),"%d min %02d s", minutes, seconds)
}