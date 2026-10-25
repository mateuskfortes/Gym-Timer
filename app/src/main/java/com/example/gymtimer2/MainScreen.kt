package com.example.gymtimer2

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.ThemeMode
import com.example.gymtimer2.ui.MainScreenDestination.AddExercise
import com.example.gymtimer2.ui.MainScreenDestination.EditExercise
import com.example.gymtimer2.ui.MainScreenDestination.EditSongChorus
import com.example.gymtimer2.ui.MainScreenDestination.Exercises
import com.example.gymtimer2.ui.MainScreenDestination.LocalSongs
import com.example.gymtimer2.ui.MainScreenDestination.MusicHome
import com.example.gymtimer2.ui.MainScreenDestination.Runs
import com.example.gymtimer2.ui.MainScreenDestination.SavedSongs
import com.example.gymtimer2.ui.MainScreenDestination.Settings
import com.example.gymtimer2.ui.MainScreenViewModel
import com.example.gymtimer2.ui.components.MainNavigationBar
import com.example.gymtimer2.ui.screens.add_exercise.AddExerciseScreen
import com.example.gymtimer2.ui.screens.edit_chorus.EditSongChorusScreen
import com.example.gymtimer2.ui.screens.edit_exercise.EditExerciseScreen
import com.example.gymtimer2.ui.screens.exercise_list.ExerciseListScreen
import com.example.gymtimer2.ui.screens.local_songs.LocalSongsScreen
import com.example.gymtimer2.ui.screens.music_home.MusicHomeScreen
import com.example.gymtimer2.ui.screens.runs.RunScreen
import com.example.gymtimer2.ui.screens.runs.RunUiState
import com.example.gymtimer2.ui.screens.saved_songs.SavedSongsScreen
import com.example.gymtimer2.ui.screens.settings.SettingsScreen

/** Apresenta a tela selecionada e encaminha ações para os ViewModels. */
@Composable
fun MainScreen(
    onOpenOverlayClick: (Int) -> Unit,
    settings: AppSettings,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (AccentColor) -> Unit,
    runState: RunUiState,
    onStartRun: () -> Unit,
    onPauseRun: (Long) -> Unit,
    onResumeRun: (Long) -> Unit,
    onFinishRun: (Long, String?) -> Unit,
    onDiscardRun: (Long) -> Unit,
    onSelectRun: (Long) -> Unit,
    onDeleteRun: (Long) -> Unit,
    onBackFromRunDetails: () -> Unit,
    onOpenActiveRun: () -> Unit,
    onCloseActiveRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val uiState by mainScreenViewModel.uiState.collectAsStateWithLifecycle()
    val saveableStateHolder = rememberSaveableStateHolder()

    val fullScreenRun = uiState.destination == Runs && runState.isActiveRunOpen && runState.activeRun != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (fullScreenRun) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (!fullScreenRun) {
                MainNavigationBar(uiState.destination, mainScreenViewModel::navigateTo)
            }
        }
    ) { innerPadding ->
        // As telas com Scaffold próprio não devem aplicar novamente os insets já reservados.
        val contentModifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        when (val destination = uiState.destination) {
            Exercises -> saveableStateHolder.SaveableStateProvider("exercise_list") {
                ExerciseListScreen(
                    modifier = contentModifier,
                    selectedDayId = uiState.selectedDayId,
                    onSelectDay = mainScreenViewModel::selectDay,
                    onOpenOverlayClick = onOpenOverlayClick,
                    onEditExercise = { exercise ->
                        mainScreenViewModel.navigateTo(EditExercise(exercise))
                    },
                )
            }
            AddExercise -> AddExerciseScreen(
                modifier = contentModifier,
                initialDayId = uiState.selectedDayId,
                goBack = { mainScreenViewModel.navigateTo(Exercises) }
            )
            MusicHome -> MusicHomeScreen(
                modifier = contentModifier,
                onOpenLibrary = { mainScreenViewModel.navigateTo(SavedSongs) }
            )
            SavedSongs -> SavedSongsScreen(
                modifier = contentModifier,
                onOpenSelection = { mainScreenViewModel.navigateTo(LocalSongs) },
                onEditSong = { song -> mainScreenViewModel.navigateTo(EditSongChorus(song)) },
                goBack = { mainScreenViewModel.navigateTo(MusicHome) }
            )
            Settings -> SettingsScreen(
                settings = settings,
                onThemeSelected = onThemeSelected,
                onAccentSelected = onAccentSelected,
                modifier = contentModifier
            )
            Runs -> RunScreen(
                state = runState,
                onStartRun = onStartRun,
                onPauseRun = onPauseRun,
                onResumeRun = onResumeRun,
                onFinishRun = onFinishRun,
                onDiscardRun = onDiscardRun,
                onSelectRun = onSelectRun,
                onDeleteRun = onDeleteRun,
                onBackFromDetails = onBackFromRunDetails,
                onOpenActiveRun = onOpenActiveRun,
                onCloseActiveRun = onCloseActiveRun,
                modifier = if (fullScreenRun) Modifier.fillMaxSize() else contentModifier
            )
            LocalSongs -> LocalSongsScreen(
                modifier = contentModifier,
                goBack = { mainScreenViewModel.navigateTo(SavedSongs) }
            )
            is EditExercise -> EditExerciseScreen(
                modifier = contentModifier,
                scheduledExercise = destination.exercise,
                goBack = { mainScreenViewModel.navigateTo(Exercises) }
            )
            is EditSongChorus -> EditSongChorusScreen(
                modifier = contentModifier,
                songToEdit = destination.song,
                goBack = { mainScreenViewModel.navigateTo(SavedSongs) }
            )
        }
    }
}
