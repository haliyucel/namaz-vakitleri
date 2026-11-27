package com.namazvakti.app.model

import java.time.DayOfWeek

data class AppSettings(
    val location: LocationInfo = LocationInfo.DEFAULT_ISTANBUL,
    val calculationMethod: CalculationMethod = CalculationMethod.DIYANET,
    val warn15MinEnabled: Boolean = true,
    val warn5MinEnabled: Boolean = true,
    val kerahatAlertEnabled: Boolean = true,
    val fridayReminderEnabled: Boolean = true,
    val fridayReminderMinutesBefore: Int = 60,
    val alertEnabledPrayers: Set<PrayerType> = PrayerType.entries.toSet(),
    val adhanEnabled: Boolean = false,
    val adhanEnabledPrayers: Set<PrayerType> = PrayerType.ADHAN_PRAYERS.toSet(),
    val autoSilentEnabled: Boolean = true,
    val autoSilentDurationMinutes: Int = 20,
    val autoSilentDurationsByPrayer: Map<PrayerType, Int> = DEFAULT_SILENT_DURATIONS,
    val fridaySilentDurationMinutes: Int = DEFAULT_FRIDAY_SILENT_MINUTES,
    val autoSilentPrayers: Set<PrayerType> = PrayerType.ADHAN_PRAYERS.toSet(),
    val restoreAlwaysLoud: Boolean = true
) {
    /**
     * Returns the configured silent duration (minutes) for [prayer] on [dayOfWeek].
     * On Fridays, Öğle automatically uses [fridaySilentDurationMinutes] for Cuma prayer.
     */
    fun silentDurationFor(prayer: PrayerType, dayOfWeek: DayOfWeek = DayOfWeek.MONDAY): Int {
        if (prayer == PrayerType.OGLE && dayOfWeek == DayOfWeek.FRIDAY) {
            return fridaySilentDurationMinutes.coerceIn(MIN_SILENT_MINUTES, MAX_SILENT_MINUTES)
        }
        val raw = autoSilentDurationsByPrayer[prayer]
            ?: DEFAULT_SILENT_DURATIONS[prayer]
            ?: autoSilentDurationMinutes
        return raw.coerceIn(MIN_SILENT_MINUTES, MAX_SILENT_MINUTES)
    }

    fun withPrayerSilentDuration(prayer: PrayerType, minutes: Int): AppSettings {
        val clamped = minutes.coerceIn(MIN_SILENT_MINUTES, MAX_SILENT_MINUTES)
        val updated = autoSilentDurationsByPrayer.toMutableMap().apply {
            put(prayer, clamped)
        }
        return copy(autoSilentDurationsByPrayer = updated)
    }

    companion object {
        const val MIN_SILENT_MINUTES = 5
        const val MAX_SILENT_MINUTES = 180
        const val DEFAULT_FRIDAY_SILENT_MINUTES = 45

        val DEFAULT_SILENT_DURATIONS: Map<PrayerType, Int> = mapOf(
            PrayerType.IMSAK to 20,
            PrayerType.OGLE to 20,
            PrayerType.IKINDI to 20,
            PrayerType.AKSAM to 15,
            PrayerType.YATSI to 25
        )
    }
}
