package com.example.gymtimer2.ui.components

import com.example.gymtimer2.R

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.ui.MainScreenDestination
import com.example.gymtimer2.ui.MainScreenDestination.AddExercise
import com.example.gymtimer2.ui.MainScreenDestination.EditSongChorus
import com.example.gymtimer2.ui.MainScreenDestination.Exercises
import com.example.gymtimer2.ui.MainScreenDestination.LocalSongs
import com.example.gymtimer2.ui.MainScreenDestination.MusicHome
import com.example.gymtimer2.ui.MainScreenDestination.SavedSongs
import com.example.gymtimer2.ui.MainScreenDestination.Settings
import com.example.gymtimer2.ui.MainScreenDestination.Runs

/** Apresenta os destinos principais e permite ocultar a navegação na corrida em tela cheia. */
@Composable
internal fun MainNavigationBar(destination: MainScreenDestination, onNavigate: (MainScreenDestination) -> Unit) {
    val navItemList = listOf(
        NavItem(stringResource(R.string.nav_exercises), Icons.Default.Home, Exercises),
        NavItem(stringResource(R.string.nav_new), Icons.Default.Add, AddExercise),
        NavItem(stringResource(R.string.nav_music), Icons.Default.MusicNote, MusicHome),
        NavItem(stringResource(R.string.nav_runs), Icons.AutoMirrored.Filled.DirectionsRun, Runs),
        NavItem(stringResource(R.string.nav_settings), Icons.Default.Settings, Settings)
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        navItemList.forEach { navItem ->
            NavigationBarItem(
                label = { Text(navItem.label) },
                selected = navItem.destination == destination ||
                    (navItem.destination == MusicHome && destination.isMusicDestination()),
                onClick = { onNavigate(navItem.destination) },
                icon = {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun MainScreenDestination.isMusicDestination(): Boolean = when (this) {
    MusicHome, SavedSongs, LocalSongs -> true
    is EditSongChorus -> true
    else -> false
}
