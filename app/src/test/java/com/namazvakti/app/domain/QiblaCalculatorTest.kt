package com.namazvakti.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {

    @Test
    fun qiblaBearingAndDistanceForMajorCities() {
        // Istanbul -> ~151.6 deg SE, ~2395 km
        val istBearing = QiblaCalculator.bearingDegrees(41.0082, 28.9784)
        val istDist = QiblaCalculator.distanceKm(41.0082, 28.9784)
        assertEquals(151.6, istBearing, 1.0)
        assertEquals(2395.0, istDist, 30.0)
        assertEquals("GD", QiblaCalculator.cardinalDirectionTr(istBearing))

        // New York -> ~58.5 deg NE
        val nyBearing = QiblaCalculator.bearingDegrees(40.7128, -74.0060)
        assertEquals(58.5, nyBearing, 1.0)

        // Tokyo -> ~293.0 deg W-NW
        val tokyoBearing = QiblaCalculator.bearingDegrees(35.6762, 139.6503)
        assertEquals(293.0, tokyoBearing, 1.0)
    }

    @Test
    fun signedAngleDifferenceHandlesZeroWraparound() {
        assertEquals(4.0, QiblaCalculator.signedAngleDifference(358.0, 2.0), 0.001)
        assertEquals(-4.0, QiblaCalculator.signedAngleDifference(2.0, 358.0), 0.001)
        assertTrue(kotlin.math.abs(QiblaCalculator.signedAngleDifference(151.0, 151.0)) < 0.001)
    }
}
