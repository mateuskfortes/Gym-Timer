package com.example.gymtimer2.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.AccentColor

/** Color and shape tokens shared by the Compose screens. */
@Immutable
data class AppStyle(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val error: Color,
    val divider: Color,
    val shadow: Color,
    val cornerRadius: Dp = 16.dp,
    val surfaceElevation: Dp = 2.dp
) {
    fun withAccent(color: AccentColor, darkTheme: Boolean): AppStyle =
        copy(primary = if (darkTheme) color.tone().dark else color.tone().light)

    companion object {
        /** Light, white-first palette used by default throughout the app. */
        val Light = AppStyle(
            background = Color(0xFFF2F2F7),
            surface = Color.White,
            surfaceVariant = Color(0xFFF2F2F7),
            onSurface = Color(0xFF1C1C1E),
            onSurfaceVariant = Color(0xFF6D6D72),
            primary = Color(0xFF67419B),
            secondary = Color(0xFF8E8E93),
            error = Color(0xFFFF3B30),
            divider = Color(0xFFE5E5EA),
            shadow = Color(0xFF000000)
        )

        val Dark = AppStyle(
            background = Color(0xFF000000),
            surface = Color(0xFF1C1C1E),
            surfaceVariant = Color(0xFF2C2C2E),
            onSurface = Color(0xFFF5F5F7),
            onSurfaceVariant = Color(0xFFAEAEB2),
            primary = Color(0xFFB594E3),
            secondary = Color(0xFF8E8E93),
            error = Color(0xFFFF453A),
            divider = Color(0xFF38383A),
            shadow = Color.Black
        )
    }
}

val LocalAppStyle = staticCompositionLocalOf { AppStyle.Light }
