package com.example.gymtimer2

import com.example.gymtimer2.data.repository.isSupportedPoiSearchArea
import com.example.gymtimer2.data.repository.parseOpenStreetMapPois
import com.example.gymtimer2.domain.model.RunMapBounds
import com.example.gymtimer2.domain.model.RunMapPoi
import com.example.gymtimer2.domain.model.RunMapPoiType
import com.example.gymtimer2.ui.screens.runs.isMeaningfullyDifferentFrom
import com.example.gymtimer2.ui.screens.runs.map.toRunMapPoiOrNull
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenStreetMapPoiRepositoryTest {
    @Test
    fun parsesNamedNodesAndWayCentersAndSkipsUnsupportedElements() {
        val points = parseOpenStreetMapPois(
            """
                {"elements":[
                  {"type":"node","id":1,"lat":-23.5,"lon":-46.6,"tags":{"amenity":"drinking_water","name:pt":"Bebedouro","addr:street":"Rua Central","addr:housenumber":"12","addr:city":"São Paulo","opening_hours":"24/7","contact:phone":"+5511999999999","website":"https://example.org"}},
                  {"type":"way","id":2,"center":{"lat":-23.51,"lon":-46.61},"tags":{"amenity":"cafe","name":"Café Central"}},
                  {"type":"node","id":3,"lat":-23.5,"lon":-46.6,"tags":{"amenity":"school"}},
                  {"type":"node","id":4,"tags":{"amenity":"toilets"}}
                ]}
            """.trimIndent()
        )

        assertEquals(2, points.size)
        assertEquals("Bebedouro", points[0].name)
        assertEquals(RunMapPoiType.WATER, points[0].type)
        assertEquals("Rua Central, 12 · São Paulo", points[0].address)
        assertEquals("24/7", points[0].openingHours)
        assertEquals("+5511999999999", points[0].phone)
        assertEquals("https://example.org", points[0].website)
        assertEquals(-23.51, points[1].latitude, 0.0)
        assertEquals("Café Central", points[1].name)
        assertEquals(RunMapPoiType.FOOD, points[1].type)
    }

    @Test
    fun limitsQueriesToOrderedNearbyBounds() {
        assertTrue(bounds().isSupportedPoiSearchArea())
        assertFalse(bounds(north = 1.0).isSupportedPoiSearchArea())
        assertFalse(bounds(east = -46.7).isSupportedPoiSearchArea())
        assertFalse(bounds(south = Double.NaN).isSupportedPoiSearchArea())
    }

    @Test
    fun limitsPoiDetailsFromOverpass() {
        val phone = "1".repeat(200)
        val poi = parseOpenStreetMapPois(
            """{"elements":[{"type":"node","lat":-23.5,"lon":-46.6,"tags":{"amenity":"cafe","phone":"$phone"}}]}"""
        ).single()

        assertEquals(180, poi.phone?.length)
    }

    @Test
    fun ignoresSmallCameraDriftButDetectsAChangedArea() {
        val initial = bounds()

        assertFalse(initial.isMeaningfullyDifferentFrom(initial))
        assertFalse(bounds(south = -23.551, west = -46.649, north = -23.501, east = -46.599)
            .isMeaningfullyDifferentFrom(initial))
        assertTrue(bounds(south = -23.53).isMeaningfullyDifferentFrom(initial))
        assertTrue(bounds(south = -23.54, north = -23.51).isMeaningfullyDifferentFrom(initial))
    }

    @Test
    fun preservesPoiDetailsWhenConvertingMapFeatureSelection() {
        val expected = RunMapPoi(
            latitude = -23.5,
            longitude = -46.6,
            name = "Café Central",
            type = RunMapPoiType.FOOD,
            address = "Rua Central, 12",
            openingHours = "08:00-18:00",
            phone = "+5511999999999",
            website = "https://example.org"
        )
        val feature = Feature.fromGeometry(Point.fromLngLat(expected.longitude, expected.latitude)).apply {
            addStringProperty("type", expected.type.name)
            addStringProperty("name", expected.name)
            addStringProperty("address", expected.address)
            addStringProperty("opening_hours", expected.openingHours)
            addStringProperty("phone", expected.phone)
            addStringProperty("website", expected.website)
        }

        assertEquals(expected, feature.toRunMapPoiOrNull())
    }

    private fun bounds(
        south: Double = -23.55,
        west: Double = -46.65,
        north: Double = -23.50,
        east: Double = -46.60
    ) = RunMapBounds(south, west, north, east)
}
