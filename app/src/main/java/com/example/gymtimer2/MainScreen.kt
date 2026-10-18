package com.example.gymtimer2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.ThemeMode
import com.example.gymtimer2.ui.MainScreenDestination
import com.example.gymtimer2.ui.MainScreenViewModel
import com.example.gymtimer2.ui.MainScreenDestination.AddExercise
import com.example.gymtimer2.ui.MainScreenDestination.EditExercise
import com.example.gymtimer2.ui.MainScreenDestination.EditSongChorus
import com.example.gymtimer2.ui.MainScreenDestination.Exercises
import com.example.gymtimer2.ui.MainScreenDestination.LocalSongs
import com.example.gymtimer2.ui.MainScreenDestination.MusicHome
import com.example.gymtimer2.ui.MainScreenDestination.SavedSongs
import com.example.gymtimer2.ui.MainScreenDestination.Settings
import com.example.gymtimer2.ui.screens.add_exercise.AddExerciseScreen
import com.example.gymtimer2.ui.screens.edit_exercise.EditExerciseScreen
import com.example.gymtimer2.ui.screens.exercise_list.ExerciseListScreen
import com.example.gymtimer2.ui.screens.edit_chorus.EditSongChorusScreen
import com.example.gymtimer2.ui.screens.saved_songs.SavedSongsScreen
import com.example.gymtimer2.ui.screens.local_songs.LocalSongsScreen
import com.example.gymtimer2.ui.screens.settings.SettingsScreen
import com.example.gymtimer2.ui.screens.music_home.MusicHomeScreen

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val destination: MainScreenDestination
)

@Composable
fun MainScreen(
    onOpenOverlayClick: (Int) -> Unit,
    settings: AppSettings,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (AccentColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val uiState by mainScreenViewModel.uiState.collectAsStateWithLifecycle()
    val saveableStateHolder = rememberSaveableStateHolder()

    val navItemList = listOf(
        NavItem(stringResource(R.string.nav_exercises), Icons.Default.Home, Exercises),
        NavItem(stringResource(R.string.nav_new), Icons.Default.Add, AddExercise),
        NavItem(stringResource(R.string.nav_music), Icons.Default.MusicNote, MusicHome),
        NavItem(stringResource(R.string.nav_settings), Icons.Default.Settings, Settings)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                navItemList.forEach { navItem ->
                    NavigationBarItem(
                        label = { Text(navItem.label) },
                        selected = navItem.destination == uiState.destination ||
                            (navItem.destination == MusicHome && uiState.destination.isMusicDestination()),
                        onClick = { mainScreenViewModel.navigateTo(navItem.destination) },
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
    ) { innerPadding ->
        when (val destination = uiState.destination) {
            Exercises -> saveableStateHolder.SaveableStateProvider("exercise_list") {
                ExerciseListScreen(
                    modifier = Modifier.padding(innerPadding),
                    selectedDayId = uiState.selectedDayId,
                    onSelectDay = mainScreenViewModel::selectDay,
                    onOpenOverlayClick = onOpenOverlayClick,
                    onEditExercise = { exercise ->
                        mainScreenViewModel.navigateTo(EditExercise(exercise))
                    },
                )
            }
            AddExercise -> AddExerciseScreen(
                modifier = Modifier.padding(innerPadding),
                initialDayId = uiState.selectedDayId,
                goBack = { mainScreenViewModel.navigateTo(Exercises) }
            )
            MusicHome -> MusicHomeScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenLibrary = { mainScreenViewModel.navigateTo(SavedSongs) }
            )
            SavedSongs -> SavedSongsScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenSelection = { mainScreenViewModel.navigateTo(LocalSongs) },
                onEditSong = { song -> mainScreenViewModel.navigateTo(EditSongChorus(song)) },
                goBack = { mainScreenViewModel.navigateTo(MusicHome) }
            )
            Settings -> SettingsScreen(
                settings = settings,
                onThemeSelected = onThemeSelected,
                onAccentSelected = onAccentSelected,
                modifier = Modifier.padding(innerPadding)
            )
            LocalSongs -> LocalSongsScreen(
                modifier = Modifier.padding(innerPadding),
                goBack = { mainScreenViewModel.navigateTo(SavedSongs) }
            )
            is EditExercise -> EditExerciseScreen(
                modifier = Modifier.padding(innerPadding),
                scheduledExercise = destination.exercise,
                goBack = { mainScreenViewModel.navigateTo(Exercises) }
            )
            is EditSongChorus -> EditSongChorusScreen(
                modifier = Modifier.padding(innerPadding),
                songToEdit = destination.song,
                goBack = { mainScreenViewModel.navigateTo(SavedSongs) }
            )
        }
    }
}

private fun MainScreenDestination.isMusicDestination(): Boolean = when (this) {
    MusicHome, SavedSongs, LocalSongs -> true
    is EditSongChorus -> true
    else -> false
}
