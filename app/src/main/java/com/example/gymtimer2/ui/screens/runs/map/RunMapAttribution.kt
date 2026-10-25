package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R

/** Mantém a atribuição OSM visível acima do painel inferior. */
@Composable
internal fun RunMapAttribution(bottomPadding: Dp, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.run_map_attribution),
        modifier = modifier.padding(bottom = bottomPadding).padding(8.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall
    )
}
