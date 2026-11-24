package com.namazvakti.app.domain

import com.namazvakti.app.model.PrayerDay
import com.namazvakti.app.model.PrayerType
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class KerahatType(
    val title: String,
    val subtitle: String,
    val fiqhNote: String
) {
    SABAH(
        title = "Sabah Kerahati",
        subtitle = "Güneş doğuşundan sonraki 45 dk (İşrak öncesi)",
        fiqhNote = "İşrak vaktine kadar namaz kılınmaz."
    ),
    OGLE(
        title = "Öğle Kerahati",
        subtitle = "Öğle vaktinden önceki 45 dk (İstiva)",
        fiqhNote = "Güneş tepe noktasındayken namaz kılınmaz."
    ),
    AKSAM(
        title = "Akşam Kerahati",
        subtitle = "Akşam vaktinden önceki 45 dk (İsfirar)",
        fiqhNote = "Sadece o günün ikindi namazının farzı kılınabilir."
    )
}

data class KerahatWindow(
    val type: KerahatType,
    val start: LocalTime,
    val end: LocalTime,
    val startZdt: ZonedDateTime,
    val endZdt: ZonedDateTime
) {
    fun remainingDuration(now: ZonedDateTime): Duration =
        Duration.between(now, endZdt).coerceAtLeast(Duration.ZERO)
}

/**
 * Calculates the 3 daily Kerahat (disliked prayer) intervals following
 * Diyanet İşleri Başkanlığı / Hanefi standard (45 minutes each).
 */
object KerahatCalculator {
    const val KERAHAT_MINUTES = 45L

    fun calculateWindows(prayerDay: PrayerDay): List<KerahatWindow> {
        val zone = runCatching { ZoneId.of(prayerDay.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val gunes = prayerDay.times[PrayerType.GUNES]
        val ogle = prayerDay.times[PrayerType.OGLE]
        val aksam = prayerDay.times[PrayerType.AKSAM]

        val list = mutableListOf<KerahatWindow>()
        if (gunes != null) {
            val startZdt = ZonedDateTime.of(prayerDay.date, gunes, zone)
            val endZdt = startZdt.plusMinutes(KERAHAT_MINUTES)
            list.add(
                KerahatWindow(
                    type = KerahatType.SABAH,
                    start = startZdt.toLocalTime(),
                    end = endZdt.toLocalTime(),
                    startZdt = startZdt,
                    endZdt = endZdt
                )
            )
        }
        if (ogle != null) {
            val endZdt = ZonedDateTime.of(prayerDay.date, ogle, zone)
            val startZdt = endZdt.minusMinutes(KERAHAT_MINUTES)
            list.add(
                KerahatWindow(
                    type = KerahatType.OGLE,
                    start = startZdt.toLocalTime(),
                    end = endZdt.toLocalTime(),
                    startZdt = startZdt,
                    endZdt = endZdt
                )
            )
        }
        if (aksam != null) {
            val endZdt = ZonedDateTime.of(prayerDay.date, aksam, zone)
            val startZdt = endZdt.minusMinutes(KERAHAT_MINUTES)
            list.add(
                KerahatWindow(
                    type = KerahatType.AKSAM,
                    start = startZdt.toLocalTime(),
                    end = endZdt.toLocalTime(),
                    startZdt = startZdt,
                    endZdt = endZdt
                )
            )
        }
        return list
    }

    fun findActiveWindow(prayerDay: PrayerDay, now: ZonedDateTime): KerahatWindow? =
        calculateWindows(prayerDay).firstOrNull { w ->
            !now.isBefore(w.startZdt) && now.isBefore(w.endZdt)
        }

    fun findNextWindow(today: PrayerDay, tomorrow: PrayerDay, now: ZonedDateTime): KerahatWindow? {
        val todayNext = calculateWindows(today).firstOrNull { w -> w.startZdt.isAfter(now) }
        if (todayNext != null) return todayNext
        return calculateWindows(tomorrow).firstOrNull()
    }
}
