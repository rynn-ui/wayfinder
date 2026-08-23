package com.roadguardian.app.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun haversineDistance_samePointReturnsZero() {
        val distance = GeoUtils.haversineDistance(26.4499, 80.3319, 26.4499, 80.3319)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun haversineDistance_calculatesKnownDistancesAccurately() {
        val lat1 = 26.8467
        val lon1 = 80.9462
        val lat2 = 26.8476
        val lon2 = 80.9462
        val distance = GeoUtils.haversineDistance(lat1, lon1, lat2, lon2)
        assertEquals(100.0, distance, 5.0)

        val kanpurLat = 26.4499
        val kanpurLon = 80.3319
        val lucknowLat = 26.8467
        val lucknowLon = 80.9462
        val cityDistance = GeoUtils.haversineDistance(kanpurLat, kanpurLon, lucknowLat, lucknowLon)
        assertTrue(cityDistance in 70000.0..85000.0)
    }

    @Test
    fun isValidCoordinate_validatesCorrectly() {
        assertTrue(GeoUtils.isValidCoordinate(26.4499, 80.3319))
        assertTrue(GeoUtils.isValidCoordinate(-33.8688, 151.2093))
        assertTrue(GeoUtils.isValidCoordinate(90.0, 180.0))
        assertTrue(GeoUtils.isValidCoordinate(-90.0, -180.0))

        assertFalse(GeoUtils.isValidCoordinate(0.0, 0.0))
        assertFalse(GeoUtils.isValidCoordinate(91.0, 80.0))
        assertFalse(GeoUtils.isValidCoordinate(-91.0, 80.0))
        assertFalse(GeoUtils.isValidCoordinate(26.0, 181.0))
        assertFalse(GeoUtils.isValidCoordinate(26.0, -181.0))
        assertFalse(GeoUtils.isValidCoordinate(Double.NaN, 80.0))
        assertFalse(GeoUtils.isValidCoordinate(26.0, Double.NaN))
    }

    @Test
    fun isInBounds_checksBoundingBoxProperly() {
        val minLat = 26.0
        val maxLat = 27.0
        val minLon = 80.0
        val maxLon = 81.0

        assertTrue(GeoUtils.isInBounds(26.5, 80.5, minLat, maxLat, minLon, maxLon))
        assertTrue(GeoUtils.isInBounds(26.0, 80.0, minLat, maxLat, minLon, maxLon))
        assertTrue(GeoUtils.isInBounds(27.0, 81.0, minLat, maxLat, minLon, maxLon))

        assertFalse(GeoUtils.isInBounds(25.9, 80.5, minLat, maxLat, minLon, maxLon))
        assertFalse(GeoUtils.isInBounds(27.1, 80.5, minLat, maxLat, minLon, maxLon))
        assertFalse(GeoUtils.isInBounds(26.5, 79.9, minLat, maxLat, minLon, maxLon))
        assertFalse(GeoUtils.isInBounds(26.5, 81.1, minLat, maxLat, minLon, maxLon))
    }
}
