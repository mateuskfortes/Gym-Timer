package com.example.gymtimer2.data.exporting

import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object RunGpxDocument {
    const val MIME_TYPE = "application/gpx+xml"

    fun hasExportablePoints(points: List<RunPointModel>): Boolean = points.any(RunPointModel::isExportable)

    fun fileName(title: String, fallbackName: String): String =
        RunExportFileName.create(title, fallbackName, GPX_EXTENSION)

    fun serialize(
        run: RunModel,
        activityTypeLabel: String,
        points: List<RunPointModel>,
        privacyZones: List<RunPrivacyZone> = emptyList()
    ): String {
        val segments = RunExportPrivacy.visiblePoints(run, points, privacyZones).asSequence()
            .filter(RunPointModel::isExportable)
            .groupBy(RunPointModel::segmentIndex)
            .values

        val activityName = run.title?.takeIf(String::isNotBlank) ?: activityTypeLabel
        val timeFormatter = SimpleDateFormat(GPX_TIME_FORMAT, Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="Gym Timer" xmlns="$GPX_NAMESPACE">""")
            append("  <metadata><name>")
            appendEscaped(activityName)
            appendLine("</name></metadata>")
            appendLine("  <trk>")
            append("    <name>")
            appendEscaped(activityName)
            appendLine("</name>")
            append("    <type>")
            appendEscaped(activityTypeLabel)
            appendLine("</type>")
            segments.forEach { segment ->
                appendLine("    <trkseg>")
                segment.forEach { point -> appendPoint(point, timeFormatter) }
                appendLine("    </trkseg>")
            }
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    private fun StringBuilder.appendPoint(point: RunPointModel, timeFormatter: SimpleDateFormat) {
        append("      <trkpt lat=\"").append(point.latitude).append("\" lon=\"")
            .append(point.longitude).appendLine("\">")
        point.altitudeMeters?.takeIf(Double::isFinite)?.let { altitude ->
            append("        <ele>").append(altitude).appendLine("</ele>")
        }
        append("        <time>")
            .append(timeFormatter.format(Date(point.recordedAtEpochMs)))
            .appendLine("</time>")
        appendLine("      </trkpt>")
    }

    private fun StringBuilder.appendEscaped(value: String) {
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            when (codePoint) {
                AMPERSAND -> append("&amp;")
                LESS_THAN -> append("&lt;")
                GREATER_THAN -> append("&gt;")
                QUOTE -> append("&quot;")
                APOSTROPHE -> append("&apos;")
                else -> if (isValidXmlCodePoint(codePoint)) appendCodePoint(codePoint)
            }
            index += Character.charCount(codePoint)
        }
    }

    private fun isValidXmlCodePoint(codePoint: Int): Boolean =
        codePoint == TAB || codePoint == LINE_FEED || codePoint == CARRIAGE_RETURN ||
            codePoint in XML_BASIC_RANGE || codePoint in XML_PRIVATE_USE_RANGE ||
            codePoint in XML_SUPPLEMENTARY_RANGE

    private const val GPX_NAMESPACE = "http://www.topografix.com/GPX/1/1"
    private const val GPX_EXTENSION = ".gpx"
    private const val GPX_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private const val TAB = 0x9
    private const val LINE_FEED = 0xA
    private const val CARRIAGE_RETURN = 0xD
    private const val AMPERSAND = '&'.code
    private const val LESS_THAN = '<'.code
    private const val GREATER_THAN = '>'.code
    private const val QUOTE = '"'.code
    private const val APOSTROPHE = '\''.code
    private val XML_BASIC_RANGE = 0x20..0xD7FF
    private val XML_PRIVATE_USE_RANGE = 0xE000..0xFFFD
    private val XML_SUPPLEMENTARY_RANGE = 0x10000..0x10FFFF
}

internal fun RunPointModel.isExportable(): Boolean =
    !isFiltered && latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0

internal object RunExportFileName {
    fun create(title: String, fallbackName: String, extension: String): String = title
        .replace(INVALID_FILE_NAME_CHARACTERS, "-")
        .trim()
        .trimEnd('.')
        .take(MAX_FILE_NAME_LENGTH)
        .ifBlank { fallbackName }
        .plus(extension)

    private const val MAX_FILE_NAME_LENGTH = 80
    private val INVALID_FILE_NAME_CHARACTERS = Regex("""[\\/:*?"<>|]+""")
}
