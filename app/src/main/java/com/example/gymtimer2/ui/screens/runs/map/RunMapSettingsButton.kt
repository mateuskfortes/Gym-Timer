package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R

/** Abre os ajustes sem fechar a sessão de gravação. */
@Composable
internal fun RunMapSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.run_preparation_open_settings)
            )
        }
    }
}
