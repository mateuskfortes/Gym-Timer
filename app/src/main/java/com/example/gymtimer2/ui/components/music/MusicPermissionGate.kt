package com.example.gymtimer2.ui.components.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.ui.components.AppSurface

@Composable
fun MusicPermissionGate(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    content: @Composable () -> Unit
) {
    Box {
        content()

        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { }
                    ),
                contentAlignment = Alignment.Center
            ) {
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
}

