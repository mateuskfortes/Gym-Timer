package com.example.gymtimer2.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.gymtimer2.data.exporting.RunFitDocument
import com.example.gymtimer2.data.exporting.RunGpxDocument
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RunExportRepository(
    private val contentResolver: ContentResolver,
    private val cacheDirectory: File
) {
    suspend fun exportGpx(
        uri: Uri,
        run: RunModel,
        activityTypeLabel: String,
        points: List<RunPointModel>,
        privacyZones: List<RunPrivacyZone> = emptyList()
    ) = withContext(Dispatchers.IO) {
        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(RunGpxDocument.serialize(run, activityTypeLabel, points, privacyZones))
        } ?: throw IOException("gpx_destination_unavailable")
    }

    suspend fun exportFit(
        uri: Uri,
        run: RunModel,
        points: List<RunPointModel>,
        laps: List<RunLap>,
        privacyZones: List<RunPrivacyZone> = emptyList()
    ) =
        withContext(Dispatchers.IO) {
            val temporaryFile = File.createTempFile("run-export-", RunFitDocument.FILE_EXTENSION, cacheDirectory)
            try {
                try {
                    RunFitDocument.write(run, points, temporaryFile, laps, privacyZones)
                } catch (exception: com.garmin.fit.FitRuntimeException) {
                    throw IOException("fit_encoding_failed", exception)
                }
                contentResolver.openOutputStream(uri)?.use { output ->
                    temporaryFile.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IOException("fit_destination_unavailable")
            } finally {
                temporaryFile.delete()
            }
        }
}
