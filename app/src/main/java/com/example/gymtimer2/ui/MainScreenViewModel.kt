package com.example.gymtimer2.ui

import androidx.lifecycle.ViewModel
import com.example.gymtimer2.domain.model.ScheduledExerciseModel
import com.example.gymtimer2.domain.model.SongModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class MainScreenDestination {
    data object Exercises : MainScreenDestination()
    data object AddExercise : MainScreenDestination()
    data object MusicHome : MainScreenDestination()
    data object SavedSongs : MainScreenDestination()
    data object Settings : MainScreenDestination()
    data object LocalSongs : MainScreenDestination()
    data class EditExercise(val exercise: ScheduledExerciseModel) : MainScreenDestination()
    data class EditSongChorus(val song: SongModel) : MainScreenDestination()
}

data class MainScreenUiState(
    val destination: MainScreenDestination = MainScreenDestination.Exercises,
    val selectedDayId: Long? = null
)

class MainScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun navigateTo(destination: MainScreenDestination) {
        _uiState.update { it.copy(destination = destination) }
    }

    fun selectDay(dayId: Long?) {
        _uiState.update { it.copy(selectedDayId = dayId) }
    }
}
