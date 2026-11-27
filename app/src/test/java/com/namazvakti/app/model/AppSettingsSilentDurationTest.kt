package com.namazvakti.app.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class AppSettingsSilentDurationTest {

    @Test
    fun usesRecommendedPerPrayerDefaultsAndFridayOverride() {
        val settings = AppSettings()

        assertEquals(20, settings.silentDurationFor(PrayerType.IMSAK, DayOfWeek.MONDAY))
        assertEquals(20, settings.silentDurationFor(PrayerType.OGLE, DayOfWeek.WEDNESDAY))
        assertEquals(45, settings.silentDurationFor(PrayerType.OGLE, DayOfWeek.FRIDAY))
        assertEquals(20, settings.silentDurationFor(PrayerType.IKINDI, DayOfWeek.FRIDAY))
        assertEquals(15, settings.silentDurationFor(PrayerType.AKSAM, DayOfWeek.FRIDAY))
        assertEquals(25, settings.silentDurationFor(PrayerType.YATSI, DayOfWeek.SUNDAY))
    }

    @Test
    fun updatesSinglePrayerIndependentlyAndClampsBounds() {
        val updated = AppSettings()
            .withPrayerSilentDuration(PrayerType.AKSAM, 12)
            .withPrayerSilentDuration(PrayerType.YATSI, 90)
            .withPrayerSilentDuration(PrayerType.IMSAK, 2)     // clamped to 5
            .withPrayerSilentDuration(PrayerType.IKINDI, 999)  // clamped to 180
            .copy(fridaySilentDurationMinutes = 60)

        assertEquals(5, updated.silentDurationFor(PrayerType.IMSAK, DayOfWeek.TUESDAY))
        assertEquals(20, updated.silentDurationFor(PrayerType.OGLE, DayOfWeek.TUESDAY))
        assertEquals(60, updated.silentDurationFor(PrayerType.OGLE, DayOfWeek.FRIDAY))
        assertEquals(180, updated.silentDurationFor(PrayerType.IKINDI, DayOfWeek.TUESDAY))
        assertEquals(12, updated.silentDurationFor(PrayerType.AKSAM, DayOfWeek.TUESDAY))
        assertEquals(90, updated.silentDurationFor(PrayerType.YATSI, DayOfWeek.TUESDAY))
    }
}
