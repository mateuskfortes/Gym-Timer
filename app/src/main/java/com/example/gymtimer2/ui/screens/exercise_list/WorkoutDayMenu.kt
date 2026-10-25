package com.example.gymtimer2.ui.screens.exercise_list

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.components.IconActionButton

/** Agrupa as ações do dia e fecha o menu antes de abrir o diálogo correspondente. */
@Composable
internal fun WorkoutDayMenu(
    canRename: Boolean,
    canDelete: Boolean,
    onCreate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconActionButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.workout_day_menu))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.create_day)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { expanded = false; onCreate() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename_day)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                enabled = canRename,
                onClick = { expanded = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_day)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                enabled = canDelete,
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}
