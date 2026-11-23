package com.namazvakti.app.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes true-north Qibla bearing (degrees clockwise from North, 0..360)
 * and great-circle distance (km) to the Kaaba in Makkah.
 */
object QiblaCalculator {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0088

    fun bearingDegrees(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LON - longitude)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = Math.toDegrees(atan2(y, x))
        return ((theta % 360.0) + 360.0) % 360.0
    }

    fun distanceKm(latitude: Double, longitude: Double): Double {
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val a = sin(dLat / 2.0).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_KM * c
    }

    /** Signed shortest angular difference (-180..+180) from current heading to Qibla. */
    fun signedAngleDifference(currentHeadingDeg: Double, qiblaBearingDeg: Double): Double {
        var diff = (qiblaBearingDeg - currentHeadingDeg) % 360.0
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    fun cardinalDirectionTr(bearingDeg: Double): String {
        val dirs = arrayOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
        val idx = (((bearingDeg % 360.0) + 360.0) % 360.0 / 45.0 + 0.5).toInt() % 8
        return dirs[idx]
    }
}
