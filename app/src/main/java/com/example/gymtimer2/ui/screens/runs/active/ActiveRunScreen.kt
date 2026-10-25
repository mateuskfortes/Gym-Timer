package com.example.gymtimer2.ui.screens.runs.active

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.map.RunMap
import kotlinx.coroutines.launch

/** Mostra a corrida em tela cheia e mantém os controles num painel inferior expansível. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveRunScreen(
    run: RunModel,
    points: List<RunPointModel>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: (String?) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val scope = rememberCoroutineScope()
    var confirmFinish by rememberSaveable(run.id) { mutableStateOf(false) }
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    val expanded = sheetState.currentValue == SheetValue.Expanded
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val peekHeight = 96.dp + navigationInset
    val sheetHeight = with(LocalDensity.current) { sheetHeightPx.toDp() }
    val mapBottomPadding = if (expanded) maxOf(sheetHeight, peekHeight) else peekHeight
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.7f
    BackHandler(onBack = onBack)

    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetDragHandle = null,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            RunBottomSheet(
                run = run,
                expanded = expanded,
                onToggle = {
                    scope.launch {
                        if (expanded) sheetState.partialExpand() else sheetState.expand()
                    }
                },
                onPause = onPause,
                onResume = onResume,
                onFinish = { confirmFinish = true },
                modifier = Modifier.heightIn(max = maxSheetHeight).onSizeChanged { sheetHeightPx = it.height }
            )
        }
    ) { _ ->
        // O mapa ocupa o fundo inteiro; a câmera reserva o espaço coberto pelo painel.
        Box(Modifier.fillMaxSize()) {
            RunMap(
                points = points,
                followLatestPoint = run.status == RunStatus.RUNNING,
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                bottomPadding = mapBottomPadding,
                showRecenterControl = true
            )
            RunBackButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(16.dp))
        }
    }
    if (confirmFinish) {
        RunFinishDialog(
            onConfirm = { title ->
                confirmFinish = false
                onFinish(title)
            },
            onDiscard = {
                confirmFinish = false
                onDiscard()
            },
            onDismiss = { confirmFinish = false }
        )
    }
}
