package com.example.gymtimer2.data.repository

import android.content.ContentResolver
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.OpenableColumns
import com.example.gymtimer2.data.dao.PlannedRouteDao
import com.example.gymtimer2.data.entity.PlannedRouteEntity
import com.example.gymtimer2.data.entity.PlannedRoutePointEntity
import com.example.gymtimer2.data.entity.toModel
import com.example.gymtimer2.data.importing.GpxRouteParser
import com.example.gymtimer2.domain.model.ImportedRoute
import com.example.gymtimer2.domain.model.LocalSegmentStart
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.PlannedRoutePoint
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.MIN_LOCAL_SEGMENT_DISTANCE_METERS
import com.example.gymtimer2.domain.model.runRouteLengthMeters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.ParserConfigurationException
import org.xml.sax.SAXException

/** Importa percursos GPX e guarda as coordenadas no banco local. */
class PlannedRouteRepository(
    private val plannedRouteDao: PlannedRouteDao,
    private val contentResolver: ContentResolver,
    private val defaultRouteName: String,
    private val parser: GpxRouteParser = GpxRouteParser()
) {
    val routes: Flow<List<PlannedRouteModel>> = plannedRouteDao.observeRoutes().map { routes ->
        routes.map { PlannedRouteModel(it.id, it.name, it.distanceMeters, it.isFavorite) }
    }
    val localSegmentStarts: Flow<List<LocalSegmentStart>> = plannedRouteDao.observeLocalSegmentStarts().map { rows ->
        rows.map { LocalSegmentStart(it.routeId, it.name, it.distanceMeters, it.latitude, it.longitude) }
    }

    suspend fun setFavorite(routeId: Long, isFavorite: Boolean) =
        plannedRouteDao.setFavorite(routeId, isFavorite)

    suspend fun saveLocalSegment(name: String, points: List<RunPointModel>): Long =
        withContext(Dispatchers.IO) {
            val routeName = name.trim().take(MAX_LOCAL_ROUTE_NAME_LENGTH)
            require(routeName.isNotEmpty())
            require(points.size in MIN_LOCAL_ROUTE_POINTS..MAX_LOCAL_ROUTE_POINTS)
            require(points.none(RunPointModel::isFiltered))

            val routePoints = points.map { point ->
                require(
                    point.latitude.isFinite() && point.latitude in -90.0..90.0 &&
                        point.longitude.isFinite() && point.longitude in -180.0..180.0
                )
                PlannedRoutePoint(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    segmentIndex = 0,
                    altitudeMeters = point.altitudeMeters?.takeIf(Double::isFinite)
                )
            }
            val distanceMeters = runRouteLengthMeters(routePoints)
            require(distanceMeters >= MIN_LOCAL_SEGMENT_DISTANCE_METERS)

            val route = PlannedRouteEntity(
                name = routeName,
                distanceMeters = distanceMeters,
                importedAtEpochMs = System.currentTimeMillis(),
                isFavorite = true,
                isLocalSegment = true
            )
            plannedRouteDao.insertImportedRoute(route, routePoints.mapIndexed { index, point ->
                point.toEntity(index)
            })
        }

    suspend fun importGpx(uri: Uri): Long = withContext(Dispatchers.IO) {
        val fallbackName = try {
            displayName(uri)?.substringBeforeLast('.')?.takeIf(String::isNotBlank) ?: defaultRouteName
        } catch (failure: SecurityException) {
            throw PlannedRouteImportException(failure)
        }
        val input = try {
            contentResolver.openInputStream(uri) ?: throw IOException("route_file_unavailable")
        } catch (failure: IOException) {
            throw PlannedRouteImportException(failure)
        } catch (failure: SecurityException) {
            throw PlannedRouteImportException(failure)
        }
        val imported = input.use { parseGpx(it, fallbackName) }
        saveImportedRoute(imported, fallbackName)
    }

    fun observePoints(routeId: Long): Flow<List<PlannedRoutePoint>> =
        plannedRouteDao.observePoints(routeId).map { points -> points.map { it.toModel() } }

    private fun parseGpx(input: InputStream, fallbackName: String) = try {
        parser.parse(input, fallbackName)
    } catch (failure: IOException) {
        throw PlannedRouteImportException(failure)
    } catch (failure: SAXException) {
        throw PlannedRouteImportException(failure)
    } catch (failure: ParserConfigurationException) {
        throw PlannedRouteImportException(failure)
    } catch (failure: IllegalArgumentException) {
        throw PlannedRouteImportException(failure)
    }

    private suspend fun saveImportedRoute(imported: ImportedRoute, fallbackName: String): Long {
        val route = PlannedRouteEntity(
            name = imported.name.ifBlank { fallbackName },
            distanceMeters = imported.distanceMeters,
            importedAtEpochMs = System.currentTimeMillis()
        )
        return try {
            plannedRouteDao.insertImportedRoute(route, imported.points.mapIndexed { index, point ->
                point.toEntity(index)
            })
        } catch (failure: SQLiteException) {
            throw PlannedRouteImportException(failure)
        }
    }

    private fun displayName(uri: Uri): String? = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0)?.substringBeforeLast('.') else null
    }

    private fun PlannedRoutePoint.toEntity(index: Int) = PlannedRoutePointEntity(
        routeId = 0,
        pointIndex = index,
        latitude = latitude,
        longitude = longitude,
        segmentIndex = segmentIndex,
        altitudeMeters = altitudeMeters
    )
}

class PlannedRouteImportException(cause: Throwable) : Exception(cause)

private const val MIN_LOCAL_ROUTE_POINTS = 2
private const val MAX_LOCAL_ROUTE_POINTS = 100_000
private const val MAX_LOCAL_ROUTE_NAME_LENGTH = 200
