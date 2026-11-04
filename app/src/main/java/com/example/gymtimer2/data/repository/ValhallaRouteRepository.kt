package com.example.gymtimer2.data.repository

import com.example.gymtimer2.BuildConfig
import com.example.gymtimer2.domain.model.PlannedRoutePoint
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunStreetInstruction
import com.example.gymtimer2.domain.model.RunStreetRoute
import com.example.gymtimer2.domain.model.runRouteLengthMeters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class ValhallaRouteRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(ROUTE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
) {
    suspend fun routeToStart(current: RoutePoint, start: RoutePoint): RunStreetRoute = withContext(Dispatchers.IO) {
        require(current.hasValidCoordinates() && start.hasValidCoordinates())
        val locations = JSONArray()
            .put(JSONObject().put("lat", current.latitude).put("lon", current.longitude))
            .put(JSONObject().put("lat", start.latitude).put("lon", start.longitude))
        val body = JSONObject()
            .put("locations", locations)
            .put("costing", PEDESTRIAN_COSTING)
            .put("units", KILOMETER_UNITS)
            .put("language", PORTUGUESE_LANGUAGE)
            .put("directions_type", INSTRUCTIONS_DIRECTIONS)
            .toString()
        client.newCall(postRequest(ROUTE_ENDPOINT, body)).execute().use {
            parseRouteResponse(it, expectedSegmentCount = 1)
        }
    }

    suspend fun instructionsForRoute(points: List<RoutePoint>): RunStreetRoute = withContext(Dispatchers.IO) {
        val segments = sampleRouteSegments(points)
        val shape = JSONArray()
        segments.forEachIndexed { segmentIndex, segment ->
            segment.forEachIndexed { pointIndex, point ->
                val location = JSONObject().put("lat", point.latitude).put("lon", point.longitude)
                if (pointIndex == segment.lastIndex || segmentIndex > 0 && pointIndex == 0) {
                    location.put("type", BREAK_LOCATION)
                }
                shape.put(location)
            }
        }
        val body = JSONObject()
            .put("shape", shape)
            .put("shape_match", MAP_SNAP_MATCHING)
            .put("costing", PEDESTRIAN_COSTING)
            .put(
                "directions_options",
                JSONObject()
                    .put("units", KILOMETER_UNITS)
                    .put("language", PORTUGUESE_LANGUAGE)
                    .put("directions_type", INSTRUCTIONS_DIRECTIONS)
            )
            .toString()
        client.newCall(postRequest(TRACE_ROUTE_ENDPOINT, body)).execute().use {
            parseRouteResponse(it, expectedSegmentCount = segments.size)
        }
    }

    private fun postRequest(url: String, body: String): Request = Request.Builder()
        .url(url)
        .header(CLIENT_ID_HEADER, BuildConfig.APPLICATION_ID)
        .post(body.toRequestBody(JSON_MEDIA_TYPE))
        .build()

    private fun parseRouteResponse(response: Response, expectedSegmentCount: Int): RunStreetRoute {
        if (!response.isSuccessful) throw IOException("Valhalla returned HTTP ${response.code}")
        val legs = JSONObject(readResponseBody(response, MAX_RESPONSE_BYTES))
            .getJSONObject("trip")
            .getJSONArray("legs")
        validateRouteLegCount(legs.length(), expectedSegmentCount)
        val parsedLegs = List(legs.length()) { index ->
            val leg = legs.getJSONObject(index)
            ValhallaRouteLeg(
                points = decodePolyline6(leg.getString("shape")),
                maneuvers = parseManeuvers(leg.getJSONArray("maneuvers"))
            )
        }
        return buildRunStreetRoute(parsedLegs, expectedSegmentCount)
    }

    private fun parseManeuvers(maneuvers: JSONArray): List<ValhallaManeuver> {
        validateManeuverCount(maneuvers.length())
        return List(maneuvers.length()) { index ->
            val maneuver = maneuvers.getJSONObject(index)
            ValhallaManeuver(
                instruction = maneuver.optString("instruction"),
                lengthKilometers = maneuver.getDouble("length")
            )
        }
    }

    private fun sampleRouteSegments(points: List<RoutePoint>): List<List<RoutePoint>> {
        val acceptedPoints = points.filterNot(RoutePoint::isFiltered)
        require(acceptedPoints.all { it.hasValidCoordinates() })
        val segments = acceptedPoints.groupBy(RoutePoint::segmentIndex)
            .values
            .filter { it.size >= MIN_POINTS_PER_SEGMENT }
        if (segments.isEmpty()) throw IOException("Valhalla requires at least one continuous route segment")
        if (segments.size * MIN_POINTS_PER_SEGMENT > MAX_TRACE_POINTS) {
            throw IOException("Valhalla route contains too many separate segments")
        }

        val maxPointsPerSegment = MAX_TRACE_POINTS / segments.size
        return segments.map { segment ->
            if (segment.size <= maxPointsPerSegment) segment
            else {
                val stride = ceil(segment.lastIndex.toDouble() / (maxPointsPerSegment - 1)).toInt()
                segment.filterIndexed { index, _ -> index % stride == 0 || index == segment.lastIndex }
            }
        }
    }

    private fun RoutePoint.hasValidCoordinates(): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

    private companion object {
        const val ROUTE_ENDPOINT = "https://valhalla1.openstreetmap.de/route"
        const val TRACE_ROUTE_ENDPOINT = "https://valhalla1.openstreetmap.de/trace_route"
        const val CLIENT_ID_HEADER = "X-Client-Id"
        const val PEDESTRIAN_COSTING = "pedestrian"
        const val KILOMETER_UNITS = "kilometers"
        const val PORTUGUESE_LANGUAGE = "pt-BR"
        const val INSTRUCTIONS_DIRECTIONS = "instructions"
        const val MAP_SNAP_MATCHING = "map_snap"
        const val BREAK_LOCATION = "break"
        const val ROUTE_TIMEOUT_SECONDS = 15L
        const val MAX_RESPONSE_BYTES = 1_048_576L
        // shortcut: caps each request at 1,000 points; raise after validating public service capacity.
        const val MAX_TRACE_POINTS = 1_000
        const val MIN_POINTS_PER_SEGMENT = 2
        const val LEGS_PER_SEGMENT = 2
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

internal data class ValhallaManeuver(val instruction: String, val lengthKilometers: Double)

internal data class ValhallaRouteLeg(
    val points: List<PlannedRoutePoint>,
    val maneuvers: List<ValhallaManeuver>
)

private fun readResponseBody(response: Response, maxBytes: Long): String {
    val source = response.body?.source() ?: throw IOException("Valhalla returned an empty response")
    source.request(maxBytes + 1L)
    if (source.buffer.size > maxBytes) throw IOException("Valhalla response exceeded the allowed size")
    return source.readUtf8()
}

private fun validateRouteLegCount(actualLegCount: Int, segmentCount: Int) {
    if (segmentCount < 1 || actualLegCount != segmentCount * 2 - 1) {
        throw IOException("Valhalla returned incomplete route segments")
    }
}

private fun validateManeuverCount(count: Int) {
    if (count > MAX_MANEUVER_COUNT) throw IOException("Valhalla returned too many route instructions")
}

internal fun buildRunStreetRoute(legs: List<ValhallaRouteLeg>, segmentCount: Int): RunStreetRoute {
    validateRouteLegCount(legs.size, segmentCount)

    val points = mutableListOf<PlannedRoutePoint>()
    val instructions = mutableListOf<RunStreetInstruction>()
    var distanceAlongRouteMeters = 0.0
    repeat(segmentCount) { segmentIndex ->
        val leg = legs[segmentIndex * 2]
        val segmentPoints = leg.points.map { it.copy(segmentIndex = segmentIndex) }
        val segmentDistanceMeters = runRouteLengthMeters(segmentPoints)
        if (!segmentDistanceMeters.isFinite() || segmentDistanceMeters <= 0.0) {
            throw IOException("Valhalla returned a zero-length route segment")
        }
        instructions += buildRunStreetInstructions(leg.maneuvers).map { instruction ->
            instruction.copy(distanceAlongRouteMeters = distanceAlongRouteMeters + instruction.distanceAlongRouteMeters)
        }
        points += segmentPoints
        distanceAlongRouteMeters += segmentDistanceMeters
        if (!distanceAlongRouteMeters.isFinite()) throw IOException("Valhalla returned an invalid route length")
    }
    return RunStreetRoute(points, distanceAlongRouteMeters, instructions)
}

internal fun buildRunStreetInstructions(maneuvers: List<ValhallaManeuver>): List<RunStreetInstruction> {
    validateManeuverCount(maneuvers.size)
    var distanceAlongRouteMeters = 0.0
    return buildList {
        maneuvers.forEachIndexed { index, maneuver ->
            val maneuverLengthMeters = maneuver.lengthKilometers * METERS_PER_KILOMETER
            if (!maneuverLengthMeters.isFinite() || maneuverLengthMeters < 0.0) {
                throw IOException("Valhalla returned an invalid maneuver length")
            }
            val instruction = maneuver.instruction.trim().take(MAX_INSTRUCTION_LENGTH)
            if (index > 0 && instruction.isNotEmpty()) {
                add(RunStreetInstruction(instruction, distanceAlongRouteMeters))
            }
            distanceAlongRouteMeters += maneuverLengthMeters
            if (!distanceAlongRouteMeters.isFinite()) throw IOException("Valhalla returned an invalid route length")
        }
    }
}

private const val MAX_MANEUVER_COUNT = 2_000
private const val MAX_INSTRUCTION_LENGTH = 300
private const val METERS_PER_KILOMETER = 1_000.0
