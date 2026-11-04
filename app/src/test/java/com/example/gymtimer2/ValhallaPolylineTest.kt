package com.example.gymtimer2

import com.example.gymtimer2.data.repository.decodePolyline6
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ValhallaPolylineTest {
    @Test
    fun decodePolyline6ReadsCoordinatesAtSixDecimalPlaces() {
        val points = decodePolyline6("??AA@?")

        assertEquals(3, points.size)
        assertEquals(0.0, points[0].latitude, 0.0)
        assertEquals(0.0, points[0].longitude, 0.0)
        assertEquals(0.000001, points[1].latitude, 0.0)
        assertEquals(0.000001, points[1].longitude, 0.0)
        assertEquals(0.0, points[2].latitude, 0.0)
        assertEquals(0.000001, points[2].longitude, 0.0)
    }

    @Test
    fun decodePolyline6RejectsIncompleteCoordinatePairs() {
        assertThrows(IllegalArgumentException::class.java) {
            decodePolyline6("??A")
        }
    }
}
