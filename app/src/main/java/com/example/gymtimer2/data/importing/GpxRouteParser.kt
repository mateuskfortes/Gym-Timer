package com.example.gymtimer2.data.importing

import com.example.gymtimer2.domain.model.ImportedRoute
import com.example.gymtimer2.domain.model.PlannedRoutePoint
import com.example.gymtimer2.domain.model.distanceMetersBetweenCoordinates
import org.xml.sax.Attributes
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/** Lê rotas GPX em fluxo e rejeita documentos com entidades XML externas. */
class GpxRouteParser {
    fun parse(input: InputStream, fallbackName: String): ImportedRoute {
        val handler = GpxRouteHandler()
        secureXmlReader(handler).parse(InputSource(input))
        return handler.toImportedRoute(fallbackName)
    }

    private fun secureXmlReader(handler: GpxRouteHandler): XMLReader {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }
        return factory.newSAXParser().xmlReader.apply {
            setFeature(DISALLOW_DOCTYPE, true)
            setFeature(EXTERNAL_GENERAL_ENTITIES, false)
            setFeature(EXTERNAL_PARAMETER_ENTITIES, false)
            entityResolver = object : EntityResolver {
                override fun resolveEntity(publicId: String?, systemId: String?) = InputSource(StringReader(""))
            }
            contentHandler = handler
        }
    }

    private companion object {
        const val DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl"
        const val EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities"
        const val EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities"
    }
}

private class GpxRouteHandler : DefaultHandler() {
    private val points = mutableListOf<PlannedRoutePoint>()
    private val capturedName = StringBuilder()
    private val capturedElevation = StringBuilder()
    private var pendingPoint: PlannedRoutePoint? = null
    private var routeName: String? = null
    private var currentSegment = 0
    private var nextSegment = 0
    private var routeDepth = 0
    private var hasGpxRoot = false
    private var capturingName = false
    private var capturingElevation = false
    private var elevationTextTooLong = false

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
        when ((localName ?: qName).orEmpty().substringAfter(':')) {
            "gpx" -> hasGpxRoot = true
            "trk" -> routeDepth++
            "rte" -> {
                routeDepth++
                currentSegment = nextSegment++
            }
            "trkseg" -> currentSegment = nextSegment++
            "name" -> startNameCapture()
            "ele" -> startElevationCapture()
            "trkpt", "rtept" -> startPoint(attributes)
        }
    }

    override fun characters(chars: CharArray, start: Int, length: Int) {
        if (capturingName && capturedName.length < MAX_ROUTE_NAME_LENGTH) {
            val remainingLength = MAX_ROUTE_NAME_LENGTH - capturedName.length
            capturedName.append(chars, start, minOf(length, remainingLength))
        }
        if (capturingElevation) captureElevation(chars, start, length)
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when ((localName ?: qName).orEmpty().substringAfter(':')) {
            "name" -> finishNameCapture()
            "ele" -> finishElevationCapture()
            "trkpt", "rtept" -> finishPoint()
            "trk", "rte" -> routeDepth--
        }
    }

    fun toImportedRoute(fallbackName: String): ImportedRoute {
        require(hasGpxRoot) { "not_a_gpx_file" }
        val distanceMeters = routeDistanceMeters()
        require(points.size >= MIN_ROUTE_POINTS && distanceMeters > 0.0) { "route_has_no_track" }
        return ImportedRoute(routeName ?: fallbackName, points, distanceMeters)
    }

    private fun startNameCapture() {
        if (routeDepth > 0 && routeName == null) {
            capturingName = true
            capturedName.setLength(0)
        }
    }

    private fun finishNameCapture() {
        if (!capturingName) return
        routeName = capturedName.toString().trim().takeIf(String::isNotEmpty)
        capturingName = false
    }

    private fun startPoint(attributes: Attributes) {
        if (points.size >= MAX_ROUTE_POINTS) throw SAXException("too_many_points")
        val latitude = attributes.getValue("lat")?.toDoubleOrNull()
        val longitude = attributes.getValue("lon")?.toDoubleOrNull()
        require(latitude != null && latitude.isFinite() && latitude in -90.0..90.0) { "invalid_latitude" }
        require(longitude != null && longitude.isFinite() && longitude in -180.0..180.0) { "invalid_longitude" }
        pendingPoint = PlannedRoutePoint(latitude, longitude, currentSegment)
    }

    private fun startElevationCapture() {
        if (pendingPoint == null) return
        capturingElevation = true
        elevationTextTooLong = false
        capturedElevation.setLength(0)
    }

    private fun captureElevation(chars: CharArray, start: Int, length: Int) {
        val remainingLength = MAX_ELEVATION_TEXT_LENGTH - capturedElevation.length
        if (length > remainingLength) elevationTextTooLong = true
        if (remainingLength > 0) capturedElevation.append(chars, start, minOf(length, remainingLength))
    }

    private fun finishElevationCapture() {
        if (!capturingElevation) return
        val elevation = capturedElevation.toString().trim().toDoubleOrNull()
            ?.takeIf(Double::isFinite)
            .takeUnless { elevationTextTooLong }
        pendingPoint = pendingPoint?.copy(altitudeMeters = elevation)
        capturingElevation = false
    }

    private fun finishPoint() {
        pendingPoint?.let(points::add)
        pendingPoint = null
        capturingElevation = false
    }

    private fun routeDistanceMeters(): Double {
        var previous: PlannedRoutePoint? = null
        var distanceMeters = 0.0
        points.forEach { current ->
            val previousPoint = previous
            if (previousPoint != null && previousPoint.segmentIndex == current.segmentIndex) {
                distanceMeters += distanceMetersBetweenCoordinates(
                    previousPoint.latitude,
                    previousPoint.longitude,
                    current.latitude,
                    current.longitude
                )
            }
            previous = current
        }
        return distanceMeters
    }

    private companion object {
        const val MIN_ROUTE_POINTS = 2
        const val MAX_ROUTE_POINTS = 100_000
        const val MAX_ROUTE_NAME_LENGTH = 200
        const val MAX_ELEVATION_TEXT_LENGTH = 32
    }
}
