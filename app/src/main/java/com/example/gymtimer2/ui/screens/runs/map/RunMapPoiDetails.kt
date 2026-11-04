package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMapPoi

@Composable
internal fun RunMapPoiDetails(poi: RunMapPoi) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            poi.name ?: stringResource(poi.type.labelResource()),
            style = MaterialTheme.typography.bodyMedium
        )
        if (poi.name != null) {
            Text(stringResource(poi.type.labelResource()), style = MaterialTheme.typography.labelSmall)
        }
        poi.address?.let { Text(stringResource(R.string.run_map_poi_address, it)) }
        poi.openingHours?.let { Text(stringResource(R.string.run_map_poi_opening_hours, it)) }
        poi.phone?.let { Text(stringResource(R.string.run_map_poi_phone, it)) }
        poi.website?.let { Text(stringResource(R.string.run_map_poi_website, it)) }
    }
}
