@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.history.RunDetailsScreen
import com.example.gymtimer2.ui.screens.runs.map.RunMap
import com.example.gymtimer2.ui.theme.GymTimerTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/** Verifica a câmera e o botão sobre um MapView real, incluindo posições recebidas após o carregamento. */
@RunWith(AndroidJUnit4::class)
class RunMapIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Receber a primeira posição depois do estilo deve tirar a câmera da visão global. */
    @Test
    fun firstLocationAfterStyleLoadsInitializesCamera() {
        val points = mutableStateOf(emptyList<RunPointModel>())
        composeRule.setContent {
            GymTimerTheme {
                RunMap(
                    points = points.value,
                    followLatestPoint = true,
                    showRecenterControl = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        val map = awaitMapStyle()
        composeRule.runOnIdle { points.value = listOf(samplePoint()) }
        awaitCameraAt(map, LatLng(-23.55, -46.63))
        composeRule.runOnIdle { assertEquals(17.0, map.cameraPosition.zoom, 0.01) }
    }

    /** Zoom baixo não deve ocultar o botão só porque o deslocamento ocupa poucos pixels. */
    @Test
    fun wideZoomDoesNotHideRecenterForDistantPosition() {
        composeRule.setContent {
            GymTimerTheme {
                RunMap(
                    points = listOf(samplePoint()),
                    followLatestPoint = true,
                    showRecenterControl = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        val map = awaitMapStyle()
        awaitCameraAt(map, LatLng(-23.55, -46.63))
        composeRule.runOnIdle {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-23.45, -46.63), 3.0))
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.run_map_recenter)).isDisplayed()
        }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.run_map_recenter)).performClick()
        awaitCameraAt(map, LatLng(-23.55, -46.63))
        composeRule.runOnIdle { assertEquals(17.0, map.cameraPosition.zoom, 0.01) }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.run_map_recenter))
            .assertDoesNotExist()
    }

    /** Centralizar restaura a escala da corrida; os próximos pontos respeitam o zoom manual. */
    @Test
    fun recenterRestoresFollowZoomAndGpsKeepsManualZoom() {
        val points = mutableStateOf(listOf(samplePoint()))
        composeRule.setContent {
            GymTimerTheme {
                RunMap(
                    points = points.value,
                    followLatestPoint = true,
                    showRecenterControl = true,
                    bottomPadding = 280.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        val map = awaitMapStyle()
        awaitCameraAt(map, LatLng(-23.55, -46.63))
        composeRule.runOnIdle {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-23.55, -46.63), 8.0))
        }
        onView(isAssignableFrom(MapView::class.java)).perform(swipeLeft())
        // A inércia do gesto nativo termina fora do relógio de sincronização do Compose.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            var displaced = false
            composeRule.runOnIdle {
                val target = checkNotNull(map.cameraPosition.target)
                displaced = target.distanceTo(LatLng(-23.55, -46.63)) > 1_000
            }
            displaced
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.run_map_recenter)).isDisplayed()
        }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.run_map_recenter)).assertIsDisplayed()
        composeRule.runOnIdle { points.value = listOf(samplePoint(latitude = -23.551)) }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.run_map_recenter)).performClick()
        awaitCameraAt(map, LatLng(-23.551, -46.63))
        composeRule.runOnIdle {
            assertEquals(17.0, map.cameraPosition.zoom, 0.01)
            val view = checkNotNull(composeRule.activity.window.decorView.findMapView())
            val locationOnScreen = map.projection.toScreenLocation(LatLng(-23.551, -46.63))
            val bottomPaddingPx = 280 * view.resources.displayMetrics.density
            assertEquals((view.height - bottomPaddingPx) / 2.0, locationOnScreen.y.toDouble(), 3.0)
            map.moveCamera(CameraUpdateFactory.zoomTo(14.0))
            points.value = listOf(samplePoint(latitude = -23.552))
        }
        awaitCameraAt(map, LatLng(-23.552, -46.63))
        composeRule.runOnIdle { assertEquals(14.0, map.cameraPosition.zoom, 0.01) }
    }

    /** O histórico deve enquadrar e renderizar a rota dentro do cartão rolável. */
    @Test
    fun historyRendersSavedRoute() {
        val points = listOf(samplePoint(), samplePoint(latitude = -23.56))
        showHistory(points)
        val map = awaitMapStyle()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            var visible = false
            composeRule.runOnIdle {
                val view = checkNotNull(composeRule.activity.window.decorView.findMapView())
                val features = map.queryRenderedFeatures(
                    RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()), "run-route-layer"
                )
                visible = features.isNotEmpty()
            }
            visible
        }
        composeRule.waitUntil(timeoutMillis = 10_000) { isRouteVisibleOnScreen() }
    }

    /** Uma rota de poucos metros não deve aproximar além da escala de acompanhamento. */
    @Test
    fun shortHistoryRouteKeepsStreetContext() {
        showHistory(listOf(samplePoint(), samplePoint(latitude = -23.55003)))
        val map = awaitMapStyle()
        awaitCameraAt(map, LatLng(-23.550015, -46.63))
        composeRule.runOnIdle { assertEquals(17.0, map.cameraPosition.zoom, 0.01) }
        composeRule.waitUntil(timeoutMillis = 10_000) { isRouteVisibleOnScreen() }
    }

    /** Pontos repetidos devem mostrar a posição sem tentar enquadrar limites vazios. */
    @Test
    fun repeatedHistoryCoordinatesShowLocation() {
        showHistory(listOf(samplePoint(), samplePoint().copy(id = 2L)))
        val map = awaitMapStyle()
        awaitCameraAt(map, LatLng(-23.55, -46.63))
        composeRule.runOnIdle { assertEquals(15.5, map.cameraPosition.zoom, 0.01) }
        composeRule.waitUntil(timeoutMillis = 10_000) { isRouteVisibleOnScreen() }
    }

    /** Reproduz o mapa com recorte e rolagem usados na tela de detalhes. */
    private fun showHistory(points: List<RunPointModel>) {
        composeRule.setContent {
            GymTimerTheme {
                RunDetailsScreen(
                    run = RunModel(
                        id = 1L,
                        startedAtEpochMs = 1_700_000_000_000L,
                        finishedAtEpochMs = 1_700_000_060_000L,
                        durationMs = 60_000L,
                        distanceMeters = 1_000.0,
                        status = RunStatus.COMPLETED,
                        currentSegment = 0
                    ),
                    points = points,
                    onDelete = {},
                    onBack = {}
                )
            }
        }
    }

    /** Confere a imagem composta na tela, pois consultar o SDK não detecta uma superfície oculta. */
    private fun isRouteVisibleOnScreen(): Boolean {
        val bitmap = InstrumentationRegistry.getInstrumentation()
            .uiAutomation.takeScreenshot() ?: return false
        val bounds = Rect()
        composeRule.runOnIdle {
            checkNotNull(composeRule.activity.window.decorView.findMapView()).getGlobalVisibleRect(bounds)
        }
        var routePixels = 0
        for (y in bounds.top until bounds.bottom) {
            for (x in bounds.left until bounds.right) {
                val color = bitmap.getPixel(x, y)
                if (Color.red(color) == 239 && Color.green(color) == 108 &&
                    Color.blue(color) == 0) routePixels++
            }
        }
        bitmap.recycle()
        return routePixels > 20
    }

    /** Aguarda o SDK e o estilo sem depender da disponibilidade dos blocos de mapa na rede. */
    private fun awaitMapStyle(): MapLibreMap {
        val map = AtomicReference<MapLibreMap>()
        val loaded = AtomicBoolean(false)
        composeRule.runOnIdle {
            val view = composeRule.activity.window.decorView.findMapView()
            checkNotNull(view).getMapAsync { ready ->
                map.set(ready)
                ready.getStyle { loaded.set(true) }
            }
        }
        composeRule.waitUntil(timeoutMillis = 20_000) { loaded.get() }
        return map.get()
    }

    /** Lê a posição na thread principal até a atualização de câmera ser aplicada pelo SDK. */
    private fun awaitCameraAt(map: MapLibreMap, expected: LatLng) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            var centered = false
            composeRule.runOnIdle {
                val target = map.cameraPosition.target
                centered = target != null && abs(target.latitude - expected.latitude) < 0.0001 &&
                    abs(target.longitude - expected.longitude) < 0.0001
            }
            centered
        }
    }

    /** Localiza o MapView nativo dentro da hierarquia criada pelo AndroidView. */
    private fun View.findMapView(): MapView? = when (this) {
        is MapView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findMapView() }
        else -> null
    }

    /** Usa coordenadas de teste fixas e não acessa dados reais de corrida. */
    private fun samplePoint(latitude: Double = -23.55) = RunPointModel(
        id = 1L,
        runId = 1L,
        latitude = latitude,
        longitude = -46.63,
        recordedAtEpochMs = 1_700_000_000_000L,
        accuracyMeters = 4f,
        segmentIndex = 0
    )
}
