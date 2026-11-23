package com.namazvakti.app.data

import android.content.Context
import android.content.SharedPreferences
import com.namazvakti.app.model.AppSettings
import com.namazvakti.app.model.CalculationMethod
import com.namazvakti.app.model.LocationInfo
import com.namazvakti.app.model.PrayerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun current(): AppSettings = _settings.value

    fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        val next = transform(_settings.value)
        save(next)
        _settings.value = next
        return next
    }

    private fun load(): AppSettings {
        val defaultLoc = LocationInfo.DEFAULT_ISTANBUL
        val location = LocationInfo(
            country = prefs.getString(KEY_LOC_COUNTRY, defaultLoc.country) ?: defaultLoc.country,
            countryCode = prefs.getString(KEY_LOC_COUNTRY_CODE, defaultLoc.countryCode) ?: defaultLoc.countryCode,
            province = prefs.getString(KEY_LOC_PROVINCE, defaultLoc.province) ?: defaultLoc.province,
            district = prefs.getString(KEY_LOC_DISTRICT, defaultLoc.district) ?: defaultLoc.district,
            latitude = java.lang.Double.longBitsToDouble(
                prefs.getLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(defaultLoc.latitude))
            ),
            longitude = java.lang.Double.longBitsToDouble(
                prefs.getLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(defaultLoc.longitude))
            ),
            timezoneId = prefs.getString(KEY_LOC_TZ, defaultLoc.timezoneId) ?: defaultLoc.timezoneId,
            isGpsDetected = prefs.getBoolean(KEY_LOC_IS_GPS, false)
        )

        return AppSettings(
            location = location,
            calculationMethod = CalculationMethod.fromNameOrDefault(
                prefs.getString(KEY_CALC_METHOD, CalculationMethod.DIYANET.name)
            ),
            warn15MinEnabled = prefs.getBoolean(KEY_WARN_15, true),
            warn5MinEnabled = prefs.getBoolean(KEY_WARN_5, true),
            alertEnabledPrayers = loadPrayerSet(KEY_ALERT_PRAYERS, PrayerType.entries.toSet()),
            adhanEnabled = prefs.getBoolean(KEY_ADHAN_ENABLED, true),
            adhanEnabledPrayers = loadPrayerSet(KEY_ADHAN_PRAYERS, PrayerType.ADHAN_PRAYERS.toSet()),
            autoSilentEnabled = prefs.getBoolean(KEY_AUTO_SILENT_ENABLED, true),
            autoSilentDurationMinutes = prefs.getInt(KEY_AUTO_SILENT_MINUTES, 20).coerceIn(5, 180),
            autoSilentPrayers = loadPrayerSet(KEY_AUTO_SILENT_PRAYERS, PrayerType.ADHAN_PRAYERS.toSet()),
            restoreAlwaysLoud = prefs.getBoolean(KEY_RESTORE_ALWAYS_LOUD, true)
        )
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putString(KEY_LOC_COUNTRY, s.location.country)
            .putString(KEY_LOC_COUNTRY_CODE, s.location.countryCode)
            .putString(KEY_LOC_PROVINCE, s.location.province)
            .putString(KEY_LOC_DISTRICT, s.location.district)
            .putLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(s.location.latitude))
            .putLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(s.location.longitude))
            .putString(KEY_LOC_TZ, s.location.timezoneId)
            .putBoolean(KEY_LOC_IS_GPS, s.location.isGpsDetected)
            .putString(KEY_CALC_METHOD, s.calculationMethod.name)
            .putBoolean(KEY_WARN_15, s.warn15MinEnabled)
            .putBoolean(KEY_WARN_5, s.warn5MinEnabled)
            .putStringSet(KEY_ALERT_PRAYERS, s.alertEnabledPrayers.map { it.name }.toSet())
            .putBoolean(KEY_ADHAN_ENABLED, s.adhanEnabled)
            .putStringSet(KEY_ADHAN_PRAYERS, s.adhanEnabledPrayers.map { it.name }.toSet())
            .putBoolean(KEY_AUTO_SILENT_ENABLED, s.autoSilentEnabled)
            .putInt(KEY_AUTO_SILENT_MINUTES, s.autoSilentDurationMinutes)
            .putStringSet(KEY_AUTO_SILENT_PRAYERS, s.autoSilentPrayers.map { it.name }.toSet())
            .putBoolean(KEY_RESTORE_ALWAYS_LOUD, s.restoreAlwaysLoud)
            .apply()
    }

    private fun loadPrayerSet(key: String, default: Set<PrayerType>): Set<PrayerType> {
        val raw = prefs.getStringSet(key, null) ?: return default
        return raw.mapNotNull { PrayerType.fromNameOrNull(it) }.toSet()
    }

    companion object {
        private const val PREFS_NAME = "namaz_vakitleri_settings"
        private const val KEY_LOC_COUNTRY = "loc_country"
        private const val KEY_LOC_COUNTRY_CODE = "loc_country_code"
        private const val KEY_LOC_PROVINCE = "loc_province"
        private const val KEY_LOC_DISTRICT = "loc_district"
        private const val KEY_LOC_LAT = "loc_lat"
        private const val KEY_LOC_LON = "loc_lon"
        private const val KEY_LOC_TZ = "loc_tz"
        private const val KEY_LOC_IS_GPS = "loc_is_gps"
        private const val KEY_CALC_METHOD = "calc_method"
        private const val KEY_WARN_15 = "warn_15"
        private const val KEY_WARN_5 = "warn_5"
        private const val KEY_ALERT_PRAYERS = "alert_prayers"
        private const val KEY_ADHAN_ENABLED = "adhan_enabled"
        private const val KEY_ADHAN_PRAYERS = "adhan_prayers"
        private const val KEY_AUTO_SILENT_ENABLED = "auto_silent_enabled"
        private const val KEY_AUTO_SILENT_MINUTES = "auto_silent_minutes"
        private const val KEY_AUTO_SILENT_PRAYERS = "auto_silent_prayers"
        private const val KEY_RESTORE_ALWAYS_LOUD = "restore_always_loud"
    }
}
