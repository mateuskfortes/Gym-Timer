package com.example.gymtimer2.ui.components.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.ui.components.PrimaryButton

@Composable
fun SongPermissionContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Preciso de permissão para ler suas músicas")
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(onClick = onRequestPermission) {
            Text("Permitir")
        }
    }
}

