package com.example.gymtimer2.ui.screens.song_list.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun SongPermissionContent(
    onRequestPermission: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Preciso de permissão para ler suas músicas")
        Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
        Button(onClick = onRequestPermission) {
            Text("Permitir")
        }
    }
}