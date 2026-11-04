package com.example.gymtimer2.ui.screens.runs.active

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

@Composable
internal fun RunScreenBrightnessEffect(
    keepScreenAwake: Boolean,
    dimTimeoutMs: Long,
    userInteractionVersion: Int,
    onWindowFocusRegained: () -> Unit
) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window }

    DisposableEffect(view, window, keepScreenAwake) {
        val observer = view.viewTreeObserver
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) onWindowFocusRegained()
        }
        observer.addOnWindowFocusChangeListener(focusListener)
        view.keepScreenOn = keepScreenAwake
        onDispose {
            if (observer.isAlive) observer.removeOnWindowFocusChangeListener(focusListener)
            view.keepScreenOn = false
            window?.setScreenBrightness(SYSTEM_BRIGHTNESS)
        }
    }

    LaunchedEffect(window, keepScreenAwake, dimTimeoutMs, userInteractionVersion) {
        if (window == null) return@LaunchedEffect
        window.setScreenBrightness(SYSTEM_BRIGHTNESS)
        if (!keepScreenAwake || dimTimeoutMs <= 0L) return@LaunchedEffect
        delay(dimTimeoutMs)
        window.setScreenBrightness(DIMMED_SCREEN_BRIGHTNESS)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Window.setScreenBrightness(brightness: Float) {
    attributes = attributes.apply { screenBrightness = brightness }
}

private const val DIMMED_SCREEN_BRIGHTNESS = 0.1f
private const val SYSTEM_BRIGHTNESS = -1f
