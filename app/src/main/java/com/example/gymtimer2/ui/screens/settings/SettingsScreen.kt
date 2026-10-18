package com.example.gymtimer2.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.ThemeMode
import com.example.gymtimer2.ui.theme.tone

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (AccentColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeOption(
                title = stringResource(R.string.settings_light_theme),
                icon = Icons.Default.LightMode,
                selected = settings.themeMode == ThemeMode.LIGHT,
                onClick = { onThemeSelected(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeOption(
                title = stringResource(R.string.settings_dark_theme),
                icon = Icons.Default.DarkMode,
                selected = settings.themeMode == ThemeMode.DARK,
                onClick = { onThemeSelected(ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_accent_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(AccentColor.entries) { color ->
                AccentOption(
                    color = color,
                    darkTheme = settings.themeMode == ThemeMode.DARK,
                    selected = settings.accentColor == color,
                    onClick = { onAccentSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, if (selected) primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AccentOption(
    color: AccentColor,
    darkTheme: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = stringResource(color.labelResource())
    val swatch = if (darkTheme) color.tone().dark else color.tone().light
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .size(48.dp)
                .background(swatch, CircleShape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (darkTheme) Color(0xFF1C1C1E) else Color.White
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

private fun AccentColor.labelResource(): Int = when (this) {
    AccentColor.PURPLE -> R.string.accent_purple
    AccentColor.INDIGO -> R.string.accent_indigo
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.CYAN -> R.string.accent_cyan
    AccentColor.TEAL -> R.string.accent_teal
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.LIME -> R.string.accent_lime
    AccentColor.YELLOW -> R.string.accent_yellow
    AccentColor.ORANGE -> R.string.accent_orange
    AccentColor.RED -> R.string.accent_red
    AccentColor.PINK -> R.string.accent_pink
    AccentColor.BROWN -> R.string.accent_brown
}
