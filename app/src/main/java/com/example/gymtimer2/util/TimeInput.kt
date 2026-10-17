package com.example.gymtimer2.util

fun secondsToMillisOrNull(input: String): Long? = input.trim().toLongOrNull()
    ?.takeIf { it >= 0 && it <= Long.MAX_VALUE / 1000L }
    ?.times(1000L)
