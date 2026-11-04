package com.example.gymtimer2.ui.screens.runs.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils

internal data class RunSummaryShareMetric(val label: String, val value: String)

internal data class RunSummaryShareCardContent(
    val appName: String,
    val activityTitle: String,
    val activityDate: String,
    val metrics: List<RunSummaryShareMetric>,
    val footer: String
)

internal object RunSummaryShareCardRenderer {
    private const val CARD_WIDTH_PX = 1_080
    private const val CARD_HEIGHT_PX = 1_000
    private const val HORIZONTAL_PADDING_PX = 72f
    private const val CELL_GAP_PX = 24f

    fun render(content: RunSummaryShareCardContent, colors: RunShareCardColors): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colors.background)
        drawHeader(canvas, content, colors)
        drawMetrics(canvas, content.metrics, colors)
        drawFooter(canvas, content.footer, colors)
        return bitmap
    }

    private fun drawHeader(canvas: Canvas, content: RunSummaryShareCardContent, colors: RunShareCardColors) {
        canvas.drawRect(0f, 0f, CARD_WIDTH_PX.toFloat(), 18f, Paint().apply { color = colors.accent })
        canvas.drawText(content.appName, HORIZONTAL_PADDING_PX, 98f, textPaint(colors.accent, 28f, true))
        val titlePaint = textPaint(colors.text, 52f, true)
        canvas.drawText(ellipsize(content.activityTitle, titlePaint, CARD_WIDTH_PX - HORIZONTAL_PADDING_PX * 2), HORIZONTAL_PADDING_PX, 184f, titlePaint)
        canvas.drawText(content.activityDate, HORIZONTAL_PADDING_PX, 232f, textPaint(colors.secondaryText, 27f))
    }

    private fun drawMetrics(canvas: Canvas, metrics: List<RunSummaryShareMetric>, colors: RunShareCardColors) {
        val gridTop = 292f
        val cellWidth = (CARD_WIDTH_PX - 2 * HORIZONTAL_PADDING_PX - CELL_GAP_PX) / 2f
        val cellHeight = 260f
        metrics.take(4).forEachIndexed { index, metric ->
            val column = index % 2
            val row = index / 2
            val left = HORIZONTAL_PADDING_PX + column * (cellWidth + CELL_GAP_PX)
            val top = gridTop + row * (cellHeight + CELL_GAP_PX)
            canvas.drawRoundRect(
                RectF(left, top, left + cellWidth, top + cellHeight),
                30f,
                30f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surface }
            )
            drawMetric(canvas, metric, left, top, cellWidth, colors)
        }
    }

    private fun drawMetric(
        canvas: Canvas,
        metric: RunSummaryShareMetric,
        left: Float,
        top: Float,
        width: Float,
        colors: RunShareCardColors
    ) {
        val padding = 32f
        val labelPaint = textPaint(colors.secondaryText, 28f)
        val valuePaint = textPaint(colors.text, 46f, true)
        canvas.drawText(ellipsize(metric.label, labelPaint, width - 2 * padding), left + padding, top + 92f, labelPaint)
        canvas.drawText(ellipsize(metric.value, valuePaint, width - 2 * padding), left + padding, top + 164f, valuePaint)
    }

    private fun drawFooter(canvas: Canvas, footer: String, colors: RunShareCardColors) {
        val paint = textPaint(colors.secondaryText, 22f)
        canvas.drawText(ellipsize(footer, paint, CARD_WIDTH_PX - 2 * HORIZONTAL_PADDING_PX), HORIZONTAL_PADDING_PX, 934f, paint)
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
