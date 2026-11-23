package com.namazvakti.app.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class PrayerDay(
    val date: LocalDate,
    val timezoneId: String,
    val hijriDateText: String,
    val times: Map<PrayerType, LocalTime>,
    val sourceLabel: String
) {
    fun zonedDateTime(type: PrayerType): ZonedDateTime {
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val time = times[type] ?: LocalTime.MIDNIGHT
        return ZonedDateTime.of(date, time, zone)
    }
}
