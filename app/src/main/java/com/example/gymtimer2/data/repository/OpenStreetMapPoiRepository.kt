package com.example.gymtimer2.data.repository

import com.example.gymtimer2.BuildConfig
import com.example.gymtimer2.domain.model.RunMapBounds
import com.example.gymtimer2.domain.model.RunMapPoi
import com.example.gymtimer2.domain.model.RunMapPoiType
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class OpenStreetMapPoiRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(POI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
) {
    private var cachedBounds: RunMapBounds? = null
    private var cachedPois = emptyList<RunMapPoi>()
    private var cachedAtMs = 0L

    suspend fun findNearby(bounds: RunMapBounds): List<RunMapPoi> = withContext(Dispatchers.IO) {
        require(bounds.isSupportedPoiSearchArea())
        if (bounds == cachedBounds && System.currentTimeMillis() - cachedAtMs < CACHE_TTL_MS) {
            return@withContext cachedPois
        }

        val response = client.newCall(createRequest(bounds)).execute()
        val points = response.use {
            if (!it.isSuccessful) throw IOException("Overpass returned HTTP ${it.code}")
            parseOpenStreetMapPois(readResponseBody(it))
        }
        cachedBounds = bounds
        cachedPois = points
        cachedAtMs = System.currentTimeMillis()
        points
    }

    private fun createRequest(bounds: RunMapBounds): Request {
        val box = listOf(
            bounds.southLatitude,
            bounds.westLongitude,
            bounds.northLatitude,
            bounds.eastLongitude
        ).joinToString(",") { String.format(Locale.US, "%.5f", it) }
        val query = """
            [out:json][timeout:10];
            (
              nwr["$AMENITY_TAG"~"^($SEARCHABLE_AMENITIES)$"]($box);
              nwr["$SHELTER_TAG"="$OSM_YES"]($box);
              nwr["$LEISURE_TAG"="$PARK_VALUE"]($box);
              nwr["$NATURAL_TAG"="$SPRING_VALUE"]($box);
            );
            out center $MAX_POI_COUNT;
        """.trimIndent()
        return Request.Builder()
            .url(OVERPASS_ENDPOINT)
            .header("User-Agent", "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/json")
            .post(FormBody.Builder().add("data", query).build())
            .build()
    }

    private companion object {
        // shortcut: shared public service; use a managed instance before broad app distribution.
        const val OVERPASS_ENDPOINT = "https://overpass-api.de/api/interpreter"
        const val POI_TIMEOUT_SECONDS = 12L
        const val CACHE_TTL_MS = 5 * 60 * 1_000L
    }
}

internal fun RunMapBounds.isSupportedPoiSearchArea(): Boolean =
    hasValidPoiCoordinates() && hasOrderedPoiBounds() && hasSmallPoiSearchSpan()

private fun RunMapBounds.hasValidPoiCoordinates(): Boolean =
    listOf(southLatitude, northLatitude, westLongitude, eastLongitude).all(Double::isFinite) &&
        southLatitude in -90.0..90.0 && northLatitude in -90.0..90.0 &&
        westLongitude in -180.0..180.0 && eastLongitude in -180.0..180.0

private fun RunMapBounds.hasOrderedPoiBounds(): Boolean =
    southLatitude < northLatitude && westLongitude < eastLongitude

private fun RunMapBounds.hasSmallPoiSearchSpan(): Boolean =
    northLatitude - southLatitude <= MAX_POI_SEARCH_SPAN &&
        eastLongitude - westLongitude <= MAX_POI_SEARCH_SPAN

internal fun parseOpenStreetMapPois(json: String): List<RunMapPoi> {
    val root = JsonParser.parseString(json).asJsonObjectOrNull()
        ?: throw IOException("Overpass returned an invalid JSON object")
    val elements = root.get(ELEMENTS_PROPERTY)?.takeIf { it.isJsonArray }?.asJsonArray
        ?: throw IOException("Overpass response omitted its elements")
    return (0 until minOf(elements.size(), MAX_POI_COUNT))
        .mapNotNull { index -> elements[index].asJsonObjectOrNull()?.toRunMapPoi() }
}

private fun JsonObject.toRunMapPoi(): RunMapPoi? {
    val tags = get(TAGS_PROPERTY)?.asJsonObjectOrNull() ?: return null
    val type = tags.toPoiType() ?: return null
    val (latitude, longitude) = coordinates() ?: return null
    val name = tags.firstString(PORTUGUESE_NAME_TAG, NAME_TAG)
    return RunMapPoi(
        latitude = latitude,
        longitude = longitude,
        name = name,
        type = type,
        address = tags.poiAddress(),
        openingHours = tags.firstString("opening_hours"),
        phone = tags.firstString("contact:phone", "phone"),
        website = tags.firstString("contact:website", "website")
    )
}

private fun JsonObject.poiAddress(): String? {
    val street = firstString("addr:street", "addr:place")
    val number = string("addr:housenumber").trim().takeIf(String::isNotEmpty)
    val streetAddress = listOfNotNull(street, number).joinToString(", ")
    return listOfNotNull(
        streetAddress.takeIf(String::isNotBlank),
        string("addr:suburb").trim().takeIf(String::isNotEmpty),
        string("addr:city").trim().takeIf(String::isNotEmpty)
    ).joinToString(" · ").takeIf(String::isNotBlank)?.take(MAX_POI_DETAIL_LENGTH)
}

private fun JsonObject.firstString(vararg properties: String): String? =
    properties.asSequence().map(::string).firstNotNullOfOrNull { it.trim().takeIf(String::isNotEmpty) }
        ?.take(MAX_POI_DETAIL_LENGTH)

private fun JsonObject.coordinates(): Pair<Double, Double>? {
    val center = get(CENTER_PROPERTY)?.asJsonObjectOrNull()
    val latitude = double(LATITUDE_PROPERTY) ?: center?.double(LATITUDE_PROPERTY) ?: return null
    val longitude = double(LONGITUDE_PROPERTY) ?: center?.double(LONGITUDE_PROPERTY) ?: return null
    if (!latitude.isValidLatitude() || !longitude.isValidLongitude()) return null
    return latitude to longitude
}

private fun JsonObject.double(property: String): Double? {
    val value = get(property) ?: return null
    return value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble
}

private fun JsonObject.string(property: String): String {
    val value = get(property) ?: return ""
    return value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString.orEmpty()
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? = takeIf { it.isJsonObject }?.asJsonObject

private fun Double.isValidLatitude(): Boolean = isFinite() && this in -90.0..90.0

private fun Double.isValidLongitude(): Boolean = isFinite() && this in -180.0..180.0

private fun JsonObject.toPoiType(): RunMapPoiType? {
    val amenity = string(AMENITY_TAG)
    return when {
        amenity in WATER_AMENITIES || string(NATURAL_TAG) == SPRING_VALUE -> RunMapPoiType.WATER
        amenity == TOILETS_VALUE -> RunMapPoiType.TOILETS
        string(SHELTER_TAG) == OSM_YES -> RunMapPoiType.SHELTER
        amenity == BENCH_VALUE -> RunMapPoiType.BENCH
        amenity in FOOD_AMENITIES -> RunMapPoiType.FOOD
        string(LEISURE_TAG) == PARK_VALUE -> RunMapPoiType.PARK
        else -> null
    }
}

private fun readResponseBody(response: okhttp3.Response): String {
    val source = response.body?.source() ?: throw IOException("Overpass returned an empty response")
    source.request(MAX_RESPONSE_BYTES + 1L)
    if (source.buffer.size > MAX_RESPONSE_BYTES) throw IOException("Overpass response exceeded the allowed size")
    return source.readUtf8()
}

private const val MAX_POI_COUNT = 100
private const val MAX_RESPONSE_BYTES = 512_000L
private const val MAX_POI_DETAIL_LENGTH = 180
private const val MAX_POI_SEARCH_SPAN = 0.08
private const val AMENITY_TAG = "amenity"
private const val SHELTER_TAG = "shelter"
private const val LEISURE_TAG = "leisure"
private const val NATURAL_TAG = "natural"
private const val TAGS_PROPERTY = "tags"
private const val ELEMENTS_PROPERTY = "elements"
private const val CENTER_PROPERTY = "center"
private const val LATITUDE_PROPERTY = "lat"
private const val LONGITUDE_PROPERTY = "lon"
private const val NAME_TAG = "name"
private const val PORTUGUESE_NAME_TAG = "name:pt"
private const val OSM_YES = "yes"
private const val PARK_VALUE = "park"
private const val SPRING_VALUE = "spring"
private const val TOILETS_VALUE = "toilets"
private const val BENCH_VALUE = "bench"
private const val SEARCHABLE_AMENITIES = "drinking_water|fountain|water_point|toilets|bench|cafe|fast_food|restaurant"
private val WATER_AMENITIES = setOf("drinking_water", "fountain", "water_point")
private val FOOD_AMENITIES = setOf("cafe", "fast_food", "restaurant")
