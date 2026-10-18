package com.example.gymtimer2.ui.components

import androidx.compose.runtime.Composable

@Composable
fun WeightUnitChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AppFilterChip(
        text = text,
        selected = selected,
        onClick = onClick
    )
}
