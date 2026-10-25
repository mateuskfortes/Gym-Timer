package com.example.gymtimer2.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gymtimer2.ui.MainScreenDestination

/** Item visível na barra de navegação inferior. */
internal data class NavItem(
    /** Texto acessível e rótulo do destino. */
    val label: String,
    /** Ícone que identifica visualmente o destino. */
    val icon: ImageVector,
    /** Tela que será aberta ao selecionar o item. */
    val destination: MainScreenDestination
)
