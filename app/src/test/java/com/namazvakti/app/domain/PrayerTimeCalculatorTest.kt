package com.namazvakti.app.domain

import com.namazvakti.app.model.CalculationMethod
import com.namazvakti.app.model.PrayerType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

class PrayerTimeCalculatorTest {

    @Test
    fun istanbulDiyanetMatchesOfficialTimingsWithin2Minutes() {
        val day = PrayerTimeCalculator.calculate(
            date = LocalDate.of(2026, 11, 23),
            latitude = 41.0082,
            longitude = 28.9784,
            timezoneId = "Europe/Istanbul",
            method = CalculationMethod.DIYANET
        )

        // Expected from Aladhan Method 13 (Diyanet) for 2026-11-23 Istanbul
        assertWithinMinutes("İmsak", LocalTime.of(6, 24), day.times[PrayerType.IMSAK]!!, 2)
        assertWithinMinutes("Güneş", LocalTime.of(7, 54), day.times[PrayerType.GUNES]!!, 2)
        assertWithinMinutes("Öğle", LocalTime.of(12, 55), day.times[PrayerType.OGLE]!!, 2)
        assertWithinMinutes("İkindi", LocalTime.of(15, 25), day.times[PrayerType.IKINDI]!!, 2)
        assertWithinMinutes("Akşam", LocalTime.of(17, 47), day.times[PrayerType.AKSAM]!!, 2)
        assertWithinMinutes("Yatsı", LocalTime.of(19, 11), day.times[PrayerType.YATSI]!!, 2)
    }

    @Test
    fun worldwideAndHighLatitudeSummerAreStrictlyOrdered() {
        val cases = listOf(
            Triple(LocalDate.of(2026, 11, 23), 35.6762 to 139.6503, "Asia/Tokyo" to CalculationMethod.MWL),
            Triple(LocalDate.of(2026, 11, 23), 40.7128 to -74.0060, "America/New_York" to CalculationMethod.ISNA),
            Triple(LocalDate.of(2026, 11, 23), 21.4225 to 39.8262, "Asia/Riyadh" to CalculationMethod.UMM_AL_QURA),
            Triple(LocalDate.of(2026, 6, 21), 59.3293 to 18.0686, "Europe/Stockholm" to CalculationMethod.DIYANET)
        )

        for ((date, coords, tzMethod) in cases) {
            val day = PrayerTimeCalculator.calculate(
                date = date,
                latitude = coords.first,
                longitude = coords.second,
                timezoneId = tzMethod.first,
                method = tzMethod.second
            )
            val ordered = PrayerType.entries.map { day.times[it]!! }
            for (i in 0 until ordered.lastIndex) {
                assertTrue(
                    "Expected ${ordered[i]} < ${ordered[i + 1]} for ${tzMethod.first}",
                    ordered[i].isBefore(ordered[i + 1])
                )
            }
        }
    }

    private fun assertWithinMinutes(name: String, expected: LocalTime, actual: LocalTime, toleranceMin: Long) {
        val diff = abs(Duration.between(expected, actual).toMinutes())
        assertTrue(
            "$name expected ~$expected but got $actual (diff=${diff}m > ${toleranceMin}m)",
            diff <= toleranceMin
        )
    }
}
