package com.example.gymtimer2.ui.screens.runs.history

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.example.gymtimer2.R
import com.example.gymtimer2.data.exporting.RunShareCardStorage
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.ui.screens.runs.components.RunShareCardColors
import com.example.gymtimer2.ui.screens.runs.components.RunSummaryShareCardContent
import com.example.gymtimer2.ui.screens.runs.components.RunSummaryShareMetric
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.map.readMapStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import kotlin.coroutines.resume

internal suspend fun shareRunMap(
    context: Context,
    run: RunModel,
    visiblePoints: List<RunPointModel>,
    distanceUnit: RunDistanceUnit,
    colors: RunShareCardColors
) {
    val content = runMapShareCardContent(context, run, distanceUnit)
    val snapshot = try {
        withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(MAP_SNAPSHOT_TIMEOUT_MS) { createRunMapSnapshot(context, visiblePoints) }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
    val card = snapshot?.let { mapBitmap ->
        withContext(Dispatchers.Default) {
            try {
                if (mapSnapshotHasTileCoverage(mapBitmap)) {
                    RunMapShareCardRenderer.render(content, mapBitmap, colors)
                } else {
                    null
                }
            } finally {
                mapBitmap.recycle()
            }
        }
    }
    val uri = card?.let { bitmap ->
        withContext(Dispatchers.IO) {
            try {
                RunShareCardStorage(context).save(bitmap)
            } finally {
                bitmap.recycle()
            }
        }
    }
    val intent = uri?.let {
        createRunImageShareIntent(context, listOf(it), context.getString(R.string.run_share_map_action))
    } ?: Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, formatRunMapShareText(content))
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.run_share_map_title)))
}

private fun formatRunMapShareText(content: RunMapShareCardContent): String =
    formatRunSummaryShareText(
        RunSummaryShareCardContent(
            appName = content.appName,
            activityTitle = content.activityTitle,
            activityDate = content.activityDate,
            metrics = listOf(
                RunSummaryShareMetric(content.distanceLabel, content.distance),
                RunSummaryShareMetric(content.durationLabel, content.duration),
                RunSummaryShareMetric(content.paceLabel, content.pace)
            ),
            footer = "${content.textFallback}\n${content.attribution}"
        )
    )

private suspend fun createRunMapSnapshot(context: Context, points: List<RunPointModel>): Bitmap? {
    val region = runMapShareRegion(points) ?: return null
    val options = MapSnapshotter.Options(MAP_SNAPSHOT_WIDTH_PX, MAP_SNAPSHOT_HEIGHT_PX)
        .withStyleBuilder(Style.Builder().fromJson(runShareStyle(context, points)))
        .withRegion(region.toMapLibreBounds())
        .withPadding(MAP_SNAPSHOT_PADDING_PX, MAP_SNAPSHOT_PADDING_PX, MAP_SNAPSHOT_PADDING_PX, MAP_SNAPSHOT_PADDING_PX)
        .withLogo(false)
        .withAttribution(false)
    val snapshotter = MapSnapshotter(context.applicationContext, options)
    // shortcut: transparent-area sampling catches missing tiles; opaque error placeholders need MapLibre tile events.
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { snapshotter.cancel() }
        snapshotter.start(
            { snapshot ->
                if (continuation.isActive) continuation.resume(snapshot.bitmap) else snapshot.bitmap.recycle()
            },
            { if (continuation.isActive) continuation.resume(null) }
        )
    }
}

private fun mapSnapshotHasTileCoverage(bitmap: Bitmap): Boolean {
    val pixels = IntArray(MAP_SNAPSHOT_SAMPLE_COUNT)
    for (row in 0 until MAP_SNAPSHOT_SAMPLE_AXIS) {
        val y = ((row + 0.5f) * bitmap.height / MAP_SNAPSHOT_SAMPLE_AXIS).toInt()
        for (column in 0 until MAP_SNAPSHOT_SAMPLE_AXIS) {
            val x = ((column + 0.5f) * bitmap.width / MAP_SNAPSHOT_SAMPLE_AXIS).toInt()
            pixels[row * MAP_SNAPSHOT_SAMPLE_AXIS + column] = bitmap.getPixel(x, y)
        }
    }
    return hasSufficientRunMapTileCoverage(pixels)
}

private fun runShareStyle(context: Context, points: List<RunPointModel>): String {
    val style = JSONObject(readMapStyle(context))
    style.getJSONObject("sources").put(
        SHARE_ROUTE_SOURCE,
        JSONObject().put("type", "geojson").put("data", routeFeatureCollection(points))
    )
    style.getJSONArray("layers").put(
        JSONObject()
            .put("id", SHARE_ROUTE_LAYER)
            .put("type", "line")
            .put("source", SHARE_ROUTE_SOURCE)
            .put("layout", JSONObject().put("line-cap", "round").put("line-join", "round"))
            .put("paint", JSONObject().put("line-color", SHARE_ROUTE_COLOR).put("line-width", 6))
    )
    return style.toString()
}

private fun routeFeatureCollection(points: List<RunPointModel>): JSONObject {
    val features = shareableRunSegments(points).map { segment ->
        val coordinates = segment.joinToString(",") { point -> "[${point.longitude},${point.latitude}]" }
        JSONObject()
            .put("type", "Feature")
            .put("properties", JSONObject())
            .put(
                "geometry",
                JSONObject()
                    .put("type", "LineString")
                    .put("coordinates", JSONArray("[$coordinates]"))
            )
    }
    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", JSONArray(features))
}

private fun RunMapShareRegion.toMapLibreBounds() =
    LatLngBounds.from(northLatitude, eastLongitude, southLatitude, westLongitude)

private fun runMapShareCardContent(
    context: Context,
    run: RunModel,
    distanceUnit: RunDistanceUnit
) = RunMapShareCardContent(
    appName = context.getString(R.string.app_name),
    activityTitle = run.title ?: context.getString(RunActivityFormatting.activityTypeResource(run.activityType)),
    activityDate = RunActivityFormatting.formatDate(run.startedAtEpochMs),
    distanceLabel = context.getString(R.string.run_distance_label),
    distance = RunFormatting.distanceLabel(context, run.distanceMeters, distanceUnit),
    durationLabel = context.getString(R.string.run_time_label),
    duration = RunMetrics.formatDuration(run.performanceDurationMs),
    paceLabel = context.getString(R.string.run_pace_label),
    pace = RunMetrics.formatPace(run.averagePaceSecondsPerKm, distanceUnit),
    attribution = context.getString(R.string.run_share_map_attribution),
    textFallback = context.getString(R.string.run_share_map_fallback)
)

private data class RunMapShareCardContent(
    val appName: String,
    val activityTitle: String,
    val activityDate: String,
    val distanceLabel: String,
    val distance: String,
    val durationLabel: String,
    val duration: String,
    val paceLabel: String,
    val pace: String,
    val attribution: String,
    val textFallback: String
)

private object RunMapShareCardRenderer {
    private const val CARD_WIDTH_PX = 1_080
    private const val CARD_HEIGHT_PX = 1_300
    private const val SIDE_PADDING_PX = 56f

    fun render(
        content: RunMapShareCardContent,
        map: Bitmap,
        colors: RunShareCardColors
    ): Bitmap {
        val card = Bitmap.createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        canvas.drawColor(colors.background)
        canvas.drawRect(0f, 0f, CARD_WIDTH_PX.toFloat(), 18f, Paint().apply { color = colors.accent })
        canvas.drawText(content.appName, SIDE_PADDING_PX, 92f, textPaint(colors.accent, 28f, true))
        val titlePaint = textPaint(colors.text, 48f, true)
        canvas.drawText(
            ellipsize(content.activityTitle, titlePaint, CARD_WIDTH_PX - SIDE_PADDING_PX * 2),
            SIDE_PADDING_PX,
            166f,
            titlePaint
        )
        canvas.drawText(content.activityDate, SIDE_PADDING_PX, 210f, textPaint(colors.secondaryText, 26f))
        canvas.drawBitmap(map, null, RectF(SIDE_PADDING_PX, 246f, CARD_WIDTH_PX - SIDE_PADDING_PX, 942f), Paint(Paint.FILTER_BITMAP_FLAG))
        drawMetrics(canvas, content, colors)
        canvas.drawText(content.attribution, SIDE_PADDING_PX, 1_238f, textPaint(colors.secondaryText, 22f))
        return card
    }

    private fun drawMetrics(canvas: Canvas, content: RunMapShareCardContent, colors: RunShareCardColors) {
        val values = listOf(
            content.distanceLabel to content.distance,
            content.durationLabel to content.duration,
            content.paceLabel to content.pace
        )
        val columnWidth = (CARD_WIDTH_PX - 2 * SIDE_PADDING_PX) / values.size
        values.forEachIndexed { index, (label, value) ->
            val left = SIDE_PADDING_PX + columnWidth * index + 20f
            val labelPaint = textPaint(colors.secondaryText, 22f)
            val valuePaint = textPaint(colors.text, 32f, true)
            canvas.drawText(ellipsize(value, valuePaint, columnWidth - 40f), left, 1_044f, valuePaint)
            canvas.drawText(ellipsize(label, labelPaint, columnWidth - 40f), left, 1_090f, labelPaint)
        }
    }

    private fun textPaint(color: Int, size: Float, bold: Boolean = false) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun ellipsize(text: String, paint: TextPaint, width: Float): String =
        TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()
}

private const val MAP_SNAPSHOT_WIDTH_PX = 1_080
private const val MAP_SNAPSHOT_HEIGHT_PX = 760
private const val MAP_SNAPSHOT_PADDING_PX = 64
private const val MAP_SNAPSHOT_TIMEOUT_MS = 15_000L
private const val MAP_SNAPSHOT_SAMPLE_AXIS = 16
private const val MAP_SNAPSHOT_SAMPLE_COUNT = MAP_SNAPSHOT_SAMPLE_AXIS * MAP_SNAPSHOT_SAMPLE_AXIS
private const val SHARE_ROUTE_SOURCE = "gym-timer-share-route"
private const val SHARE_ROUTE_LAYER = "gym-timer-share-route-line"
private const val SHARE_ROUTE_COLOR = "#ef6c00"
