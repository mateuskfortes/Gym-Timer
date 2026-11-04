package com.example.gymtimer2

import com.example.gymtimer2.data.importing.GpxRouteParser
import com.example.gymtimer2.domain.model.calculateRunElevationProfile
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.SAXException

class GpxRouteParserTest {
    @Test
    fun parsesTrackNameAndKeepsSegmentsSeparate() {
        val route = GpxRouteParser().parse(
            gpx("""
                <trk><name>Parque</name>
                  <trkseg><trkpt lat="0" lon="0"/><trkpt lat="0" lon="0.001"/></trkseg>
                  <trkseg><trkpt lat="1" lon="1"/><trkpt lat="1" lon="1.001"/></trkseg>
                </trk>
            """.trimIndent()),
            "rota.gpx"
        )

        assertEquals("Parque", route.name)
        assertEquals(listOf(0, 0, 1, 1), route.points.map { it.segmentIndex })
        assertTrue(route.distanceMeters in 220.0..225.0)
    }

    @Test
    fun parsesElevationForRouteProfileAndIgnoresInvalidValues() {
        val route = GpxRouteParser().parse(
            gpx("""
                <trk><trkseg>
                  <trkpt lat="0" lon="0"><ele>10.5</ele></trkpt>
                  <trkpt lat="0" lon="0.001"><ele>20</ele></trkpt>
                  <trkpt lat="0" lon="0.002"><ele>NaN</ele></trkpt>
                  <trkpt lat="0" lon="0.003"><ele>12345678901234567890123456789012345678901234567890</ele></trkpt>
                </trkseg></trk>
            """.trimIndent()),
            "route.gpx"
        )

        assertEquals(listOf(10.5, 20.0, null, null), route.points.map { it.altitudeMeters })
        val profile = calculateRunElevationProfile(route.points)
        assertEquals(2, profile.size)
        assertEquals(20.0, profile.last().elevationMeters, 0.0)
    }

    @Test
    fun rejectsDocumentsThatAreNotGpxOrContainInvalidCoordinates() {
        val parser = GpxRouteParser()
        assertThrows(IllegalArgumentException::class.java) { parser.parse(gpx("<route/>"), "route.gpx") }
        assertThrows(IllegalArgumentException::class.java) {
            parser.parse(gpx("""<trk><trkseg><trkpt lat="91" lon="0"/></trkseg></trk>"""), "route.gpx")
        }
    }

    @Test
    fun rejectsDocumentsWithExternalEntityDeclarations() {
        val xml = """
            <!DOCTYPE gpx [<!ENTITY outside SYSTEM "file:///etc/passwd">]>
            <gpx><trk><name>&outside;</name><trkseg>
              <trkpt lat="0" lon="0"/><trkpt lat="0" lon="0.001"/>
            </trkseg></trk></gpx>
        """.trimIndent()

        assertThrows(SAXException::class.java) {
            GpxRouteParser().parse(ByteArrayInputStream(xml.toByteArray()), "route.gpx")
        }
    }

    private fun gpx(body: String) = ByteArrayInputStream(
        """<gpx version="1.1" creator="test">$body</gpx>""".toByteArray()
    )
}
