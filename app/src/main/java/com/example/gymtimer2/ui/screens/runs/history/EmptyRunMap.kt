package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R

/** Informa que a sessão foi salva sem uma posição GPS válida. */
@Composable
internal fun EmptyRunMap() {
    Card(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.run_no_route_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
