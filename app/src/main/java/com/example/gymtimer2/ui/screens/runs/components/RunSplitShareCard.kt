package com.example.gymtimer2.ui.screens.runs.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.example.gymtimer2.domain.model.RunSplit

internal const val RUN_SPLITS_PER_SHARE_PAGE = 8
private const val MIN_BAR_WIDTH_RATIO = 0.35f

internal data class RunShareCardColors(
    val background: Int,
    val surface: Int,
    val text: Int,
    val secondaryText: Int,
    val accent: Int,
    val divider: Int
)

internal data class RunSplitShareCardRow(
    val label: String,
    val pace: String,
    val barWidthRatio: Float
)

internal data class RunSplitShareCardContent(
    val appName: String,
    val activityTitle: String,
    val activityDate: String,
    val distanceLabel: String,
    val distance: String,
    val durationLabel: String,
    val duration: String,
    val paceLabel: String,
    val pace: String,
    val splitsTitle: String,
    val pageLabel: String,
    val estimateNote: String,
    val rows: List<RunSplitShareCardRow>
)

internal fun runSplitSharePages(splits: List<RunSplit>): List<List<RunSplit>> =
    splits.chunked(RUN_SPLITS_PER_SHARE_PAGE)

// shortcut: bars compare pace only within each page; exact Strava ratios require its server template.
internal fun runSplitPaceBarWidthRatios(splits: List<RunSplit>): List<Float> {
    if (splits.isEmpty()) return emptyList()
    val fastestDuration = splits.minOf(RunSplit::durationMs)
    val slowestDuration = splits.maxOf(RunSplit::durationMs)
    if (fastestDuration == slowestDuration) return List(splits.size) { 1f }

    val durationRange = (slowestDuration - fastestDuration).toDouble()
    return splits.map { split ->
        (
            1.0 - (split.durationMs - fastestDuration) / durationRange * (1.0 - MIN_BAR_WIDTH_RATIO)
        ).toFloat().coerceIn(MIN_BAR_WIDTH_RATIO, 1f)
    }
}

internal object RunSplitShareCardRenderer {
    private const val CARD_WIDTH_PX = 1_080
    private const val CARD_BASE_HEIGHT_PX = 800
    private const val ROW_HEIGHT_PX = 82
    private const val HORIZONTAL_PADDING_PX = 72f
    fun render(content: RunSplitShareCardContent, colors: RunShareCardColors): Bitmap {
        val height = CARD_BASE_HEIGHT_PX + content.rows.size * ROW_HEIGHT_PX
        val bitmap = Bitmap.createBitmap(CARD_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colors.background)
        drawAccentHeader(canvas, colors.accent)
        drawHeading(canvas, content, colors)
        drawMetrics(canvas, content, colors)
        drawSplits(canvas, content, colors)
        drawFooter(canvas, content.estimateNote, height, colors)
        return bitmap
    }

    private fun drawAccentHeader(canvas: Canvas, accent: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRect(0f, 0f, CARD_WIDTH_PX.toFloat(), 18f, paint)
    }

    private fun drawHeading(canvas: Canvas, content: RunSplitShareCardContent, colors: RunShareCardColors) {
        val appPaint = textPaint(colors.accent, 28f, bold = true)
        val titlePaint = textPaint(colors.text, 50f, bold = true)
        val detailPaint = textPaint(colors.secondaryText, 26f)
        canvas.drawText(content.appName, HORIZONTAL_PADDING_PX, 92f, appPaint)
        canvas.drawText(
            ellipsize(content.activityTitle, titlePaint, CARD_WIDTH_PX - 2 * HORIZONTAL_PADDING_PX),
            HORIZONTAL_PADDING_PX,
            164f,
            titlePaint
        )
        canvas.drawText(content.activityDate, HORIZONTAL_PADDING_PX, 208f, detailPaint)
    }

    private fun drawMetrics(canvas: Canvas, content: RunSplitShareCardContent, colors: RunShareCardColors) {
        val top = 250f
        val bottom = 440f
        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surface }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.divider; strokeWidth = 2f }
        canvas.drawRoundRect(
            RectF(HORIZONTAL_PADDING_PX, top, CARD_WIDTH_PX - HORIZONTAL_PADDING_PX, bottom),
            28f,
            28f,
            surfacePaint
        )
        val columnWidth = (CARD_WIDTH_PX - 2 * HORIZONTAL_PADDING_PX) / 3f
        for (index in 1..2) {
            val x = HORIZONTAL_PADDING_PX + columnWidth * index
            canvas.drawLine(x, top + 32f, x, bottom - 32f, dividerPaint)
        }
        drawMetric(canvas, 0, content.distanceLabel, content.distance, columnWidth, colors)
        drawMetric(canvas, 1, content.durationLabel, content.duration, columnWidth, colors)
        drawMetric(canvas, 2, content.paceLabel, content.pace, columnWidth, colors)
    }

    private fun drawMetric(
        canvas: Canvas,
        index: Int,
        label: String,
        value: String,
        columnWidth: Float,
        colors: RunShareCardColors
    ) {
        val left = HORIZONTAL_PADDING_PX + columnWidth * index + 24f
        val availableWidth = columnWidth - 48f
        val labelPaint = textPaint(colors.secondaryText, 23f)
        val valuePaint = textPaint(colors.text, 33f, bold = true)
        canvas.drawText(ellipsize(label, labelPaint, availableWidth), left, 318f, labelPaint)
        canvas.drawText(ellipsize(value, valuePaint, availableWidth), left, 374f, valuePaint)
    }

    private fun drawSplits(canvas: Canvas, content: RunSplitShareCardContent, colors: RunShareCardColors) {
        val titlePaint = textPaint(colors.text, 36f, bold = true)
        val pagePaint = textPaint(colors.secondaryText, 24f)
        val yStart = 526f
        canvas.drawText(content.splitsTitle, HORIZONTAL_PADDING_PX, yStart, titlePaint)
        pagePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(content.pageLabel, CARD_WIDTH_PX - HORIZONTAL_PADDING_PX, yStart, pagePaint)

        content.rows.forEachIndexed { index, row ->
            drawSplitRow(canvas, row, 592f + index * ROW_HEIGHT_PX, colors)
        }
    }

    private fun drawSplitRow(canvas: Canvas, row: RunSplitShareCardRow, top: Float, colors: RunShareCardColors) {
        val labelPaint = textPaint(colors.text, 28f)
        val pacePaint = textPaint(colors.text, 28f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(row.label, HORIZONTAL_PADDING_PX, top + 30f, labelPaint)
        canvas.drawText(row.pace, CARD_WIDTH_PX - HORIZONTAL_PADDING_PX, top + 30f, pacePaint)

        val barLeft = HORIZONTAL_PADDING_PX
        val barRight = CARD_WIDTH_PX - HORIZONTAL_PADDING_PX
        val barTop = top + 47f
        val barHeight = 12f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.divider }
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent }
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barTop + barHeight), 6f, 6f, trackPaint)
        canvas.drawRoundRect(
            RectF(barLeft, barTop, barLeft + (barRight - barLeft) * row.barWidthRatio.coerceIn(0f, 1f), barTop + barHeight),
            6f,
            6f,
            barPaint
        )
    }

    private fun drawFooter(canvas: Canvas, note: String, height: Int, colors: RunShareCardColors) {
        val paint = textPaint(colors.secondaryText, 22f)
        val visibleNote = ellipsize(note, paint, CARD_WIDTH_PX - 2 * HORIZONTAL_PADDING_PX)
        canvas.drawText(visibleNote, HORIZONTAL_PADDING_PX, height - 58f, paint)
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
