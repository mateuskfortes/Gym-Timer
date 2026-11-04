package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R

/** Enquadra a rota planejada e a registrada sem ativar o acompanhamento GPS. */
@Composable
internal fun RunMapRouteOverviewButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.Route, contentDescription = stringResource(R.string.run_map_route_overview))
        }
    }
}
