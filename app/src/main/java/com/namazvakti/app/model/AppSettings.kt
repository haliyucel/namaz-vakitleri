package com.namazvakti.app.model

data class AppSettings(
    val location: LocationInfo = LocationInfo.DEFAULT_ISTANBUL,
    val calculationMethod: CalculationMethod = CalculationMethod.DIYANET,
    val warn15MinEnabled: Boolean = true,
    val warn5MinEnabled: Boolean = true,
    val alertEnabledPrayers: Set<PrayerType> = PrayerType.entries.toSet(),
    val adhanEnabled: Boolean = true,
    val adhanEnabledPrayers: Set<PrayerType> = PrayerType.ADHAN_PRAYERS.toSet(),
    val autoSilentEnabled: Boolean = true,
    val autoSilentDurationMinutes: Int = 20,
    val autoSilentPrayers: Set<PrayerType> = PrayerType.ADHAN_PRAYERS.toSet(),
    val restoreAlwaysLoud: Boolean = true
)
