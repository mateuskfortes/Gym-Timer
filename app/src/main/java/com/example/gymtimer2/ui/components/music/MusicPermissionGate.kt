package com.example.gymtimer2.ui.components.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.ui.components.AppSurface

@Composable
fun MusicPermissionGate(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (hasPermission) {
            content()
        } else {
            AppSurface(
                modifier = Modifier.padding(24.dp),
                shape = MaterialTheme.shapes.large,
                elevation = 8.dp,
                contentAlignment = Alignment.Center
            ) {
                SongPermissionContent(onRequestPermission = onRequestPermission)
            }
        }
    }
}

