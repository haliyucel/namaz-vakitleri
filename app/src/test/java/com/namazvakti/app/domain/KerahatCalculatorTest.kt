package com.namazvakti.app.domain

import com.namazvakti.app.model.PrayerDay
import com.namazvakti.app.model.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class KerahatCalculatorTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val sampleDay = PrayerDay(
        date = LocalDate.of(2026, 11, 24),
        timezoneId = zone.id,
        hijriDateText = "14 Cemaziyelahir 1448",
        times = mapOf(
            PrayerType.IMSAK to LocalTime.of(6, 25),
            PrayerType.GUNES to LocalTime.of(7, 55),
            PrayerType.OGLE to LocalTime.of(12, 55),
            PrayerType.IKINDI to LocalTime.of(15, 25),
            PrayerType.AKSAM to LocalTime.of(17, 46),
            PrayerType.YATSI to LocalTime.of(19, 10)
        ),
        sourceLabel = "Test"
    )

    @Test
    fun calculatesThree45MinuteKerahatWindows() {
        val windows = KerahatCalculator.calculateWindows(sampleDay)
        assertEquals(3, windows.size)

        assertEquals(KerahatType.SABAH, windows[0].type)
        assertEquals(LocalTime.of(7, 55), windows[0].start)
        assertEquals(LocalTime.of(8, 40), windows[0].end)

        assertEquals(KerahatType.OGLE, windows[1].type)
        assertEquals(LocalTime.of(12, 10), windows[1].start)
        assertEquals(LocalTime.of(12, 55), windows[1].end)

        assertEquals(KerahatType.AKSAM, windows[2].type)
        assertEquals(LocalTime.of(17, 1), windows[2].start)
        assertEquals(LocalTime.of(17, 46), windows[2].end)
    }

    @Test
    fun findsActiveWindowRespectingInclusiveStartAndExclusiveEnd() {
        val atStart = ZonedDateTime.of(sampleDay.date, LocalTime.of(12, 10), zone)
        assertEquals(KerahatType.OGLE, KerahatCalculator.findActiveWindow(sampleDay, atStart)?.type)

        val inside = ZonedDateTime.of(sampleDay.date, LocalTime.of(17, 30), zone)
        assertNotNull(KerahatCalculator.findActiveWindow(sampleDay, inside))
        assertEquals(KerahatType.AKSAM, KerahatCalculator.findActiveWindow(sampleDay, inside)?.type)

        val atEnd = ZonedDateTime.of(sampleDay.date, LocalTime.of(12, 55), zone)
        assertNull(KerahatCalculator.findActiveWindow(sampleDay, atEnd))

        val outside = ZonedDateTime.of(sampleDay.date, LocalTime.of(10, 0), zone)
        assertNull(KerahatCalculator.findActiveWindow(sampleDay, outside))
    }
}
