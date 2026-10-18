package com.example.gymtimer2.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.AccentColor

@Composable
fun GymTimerTheme(
    darkTheme: Boolean = false,
    accentColor: AccentColor = AccentColor.PURPLE,
    style: AppStyle = (if (darkTheme) AppStyle.Dark else AppStyle.Light).withAccent(accentColor, darkTheme),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = style.primary,
            onPrimary = Color(0xFF1C1C1E),
            primaryContainer = style.primary.copy(alpha = 0.2f),
            onPrimaryContainer = style.onSurface,
            inversePrimary = style.primary,
            secondary = style.secondary,
            onSecondary = Color(0xFF1C1C1E),
            secondaryContainer = style.surfaceVariant,
            onSecondaryContainer = style.onSurface,
            tertiary = style.secondary,
            onTertiary = Color(0xFF1C1C1E),
            tertiaryContainer = style.surfaceVariant,
            onTertiaryContainer = style.onSurface,
            background = style.background,
            onBackground = style.onSurface,
            surface = style.surface,
            onSurface = style.onSurface,
            surfaceVariant = style.surfaceVariant,
            onSurfaceVariant = style.onSurfaceVariant,
            outline = style.divider,
            outlineVariant = style.divider,
            error = style.error,
            onError = Color.White,
            errorContainer = style.error.copy(alpha = 0.2f),
            onErrorContainer = style.error,
            surfaceTint = Color.Transparent
        )
    } else {
        lightColorScheme(
            primary = style.primary,
            onPrimary = Color.White,
            primaryContainer = style.primary.copy(alpha = 0.12f),
            onPrimaryContainer = style.primary,
            inversePrimary = style.primary,
            secondary = style.secondary,
            onSecondary = Color.White,
            secondaryContainer = style.surfaceVariant,
            onSecondaryContainer = style.onSurface,
            tertiary = style.secondary,
            onTertiary = Color.White,
            tertiaryContainer = style.surfaceVariant,
            onTertiaryContainer = style.onSurface,
            background = style.background,
            onBackground = style.onSurface,
            surface = style.surface,
            onSurface = style.onSurface,
            surfaceVariant = style.surfaceVariant,
            onSurfaceVariant = style.onSurfaceVariant,
            outline = style.divider,
            outlineVariant = style.divider,
            error = style.error,
            onError = Color.White,
            errorContainer = style.error.copy(alpha = 0.12f),
            onErrorContainer = style.error,
            surfaceTint = Color.Transparent
        )
    }

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(style.cornerRadius),
        large = RoundedCornerShape(style.cornerRadius + 4.dp),
        extraLarge = RoundedCornerShape(style.cornerRadius + 8.dp)
    )

    CompositionLocalProvider(LocalAppStyle provides style) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}
