package com.example.gymtimer2.ui.screens.music_home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.components.ScreenTitle
import com.example.gymtimer2.ui.components.music.MusicDestinationCard

private data class MusicHomeCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MusicHomeScreen(
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        MusicHomeCategory(
            id = "library",
            title = stringResource(R.string.music_library_title),
            description = stringResource(R.string.music_library_description),
            icon = Icons.Outlined.LibraryMusic,
            onClick = onOpenLibrary
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "music_home_title") {
            ScreenTitle(text = stringResource(R.string.nav_music))
        }

        items(
            items = categories,
            key = { it.id }
        ) { category ->
            MusicDestinationCard(
                title = category.title,
                description = category.description,
                icon = category.icon,
                onClick = category.onClick
            )
        }
    }
}
