package com.example.gymtimer2.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.gymtimer2.domain.model.AccentColor

data class AccentTone(val light: Color, val dark: Color)

fun AccentColor.tone(): AccentTone = AccentTone(Color(lightArgb), Color(darkArgb))
