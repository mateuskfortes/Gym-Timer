package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R

@Composable
internal fun RunDetailsActions(
    canExport: Boolean,
    canCreateLocalSegment: Boolean,
    canCrop: Boolean,
    onShareSummary: () -> Unit,
    onEditDetails: () -> Unit,
    onExportGpx: () -> Unit,
    onExportFit: () -> Unit,
    onCreateLocalSegment: () -> Unit,
    onCrop: () -> Unit
) {
    TextButton(onClick = onShareSummary) {
        Text(stringResource(R.string.run_share_summary_action))
    }
    TextButton(onClick = onEditDetails) {
        Text(stringResource(R.string.run_edit_details_action))
    }
    if (canExport) {
        TextButton(onClick = onExportGpx) {
            Text(stringResource(R.string.run_export_gpx_action))
        }
        TextButton(onClick = onExportFit) {
            Text(stringResource(R.string.run_export_fit_action))
        }
    }
    if (canCreateLocalSegment) {
        TextButton(onClick = onCreateLocalSegment) {
            Text(stringResource(R.string.run_local_segment_create_action))
        }
    }
    if (canCrop) {
        TextButton(onClick = onCrop) {
            Text(stringResource(R.string.run_crop_action))
        }
    }
}
