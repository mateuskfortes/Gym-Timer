package com.example.gymtimer2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gymtimer2.domain.model.ExerciseModel
import com.example.gymtimer2.domain.model.SongModel
import com.example.gymtimer2.ui.screens.add_exercise.AddExerciseScreen
import com.example.gymtimer2.ui.screens.edit_exercise.EditExerciseScreen
import com.example.gymtimer2.ui.screens.exercise_list.ExerciseListScreen
import com.example.gymtimer2.ui.screens.edit_chorus.EditSongChorusScreen
import com.example.gymtimer2.ui.screens.saved_songs.SavedSongsScreen
import com.example.gymtimer2.ui.screens.local_songs.LocalSongsScreen

data class NavItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScreen(
    onOpenOverlayClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0
) {
    val navItemList = listOf(
        NavItem("Exercícios", Icons.Default.Home),
        NavItem("Novo", Icons.Default.Add),
        NavItem("Músicas", Icons.Default.PlayArrow)
    )

    var selectedIndex by remember { mutableIntStateOf(initialIndex) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseModel?>(null) }
    var songToEdit by remember { mutableStateOf<SongModel?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed { i, navItem ->
                    NavigationBarItem(
                        label = { Text(navItem.label) },
                        selected = i == selectedIndex,
                        onClick = {
                            selectedIndex = i
                        },
                        icon = {
                            Icon(
                                imageVector = navItem.icon,
                                contentDescription = navItem.label
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedIndex == 0 || selectedIndex == 1 || selectedIndex == 2) {
            exerciseToEdit = null
            songToEdit = null
        }
        when (selectedIndex) {
            0 -> ExerciseListScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenOverlayClick = onOpenOverlayClick,
                onEditExercise = { exercise ->
                    exerciseToEdit = exercise
                    selectedIndex = 10
                },
            )
            1 -> AddExerciseScreen(
                modifier = Modifier.padding(innerPadding),
                goBack = {
                    selectedIndex = 0
                }
            )
            2 -> SavedSongsScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenSelection = { selectedIndex = 11 },
                onEditSong = { song ->
                    songToEdit = song
                    selectedIndex = 12
                },
                goBack = {
                    selectedIndex = 0
                }
            )
            11 -> LocalSongsScreen(
                modifier = Modifier.padding(innerPadding),
                goBack = { selectedIndex = 2 }
            )
            10 -> EditExerciseScreen(
                modifier = Modifier.padding(innerPadding),
                exerciseToEdit = exerciseToEdit!!,
                goBack = {
                    exerciseToEdit = null
                    selectedIndex = 0
                }
            )
            12 -> EditSongChorusScreen(
                modifier = Modifier.padding(innerPadding),
                songToEdit = songToEdit!!,
                goBack = {
                    songToEdit = null
                    selectedIndex = 2
                }
            )
        }
    }
}