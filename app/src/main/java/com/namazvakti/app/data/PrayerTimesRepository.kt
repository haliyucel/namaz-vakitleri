package com.namazvakti.app.data

import android.content.Context
import com.namazvakti.app.domain.PrayerTimeCalculator
import com.namazvakti.app.model.CalculationMethod
import com.namazvakti.app.model.LocationInfo
import com.namazvakti.app.model.PrayerDay
import com.namazvakti.app.model.PrayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Hybrid prayer times repository:
 * - Immediately provides exact local/cached or offline astronomical prayer times (zero delay).
 * - Syncs and caches official monthly timetables from Aladhan API (including Diyanet Method 13)
 *   for exact worldwide accuracy when internet is available.
 */
class PrayerTimesRepository(context: Context) {

    private val cacheDir = File(context.applicationContext.filesDir, "prayer_cache").apply { mkdirs() }

    /** Synchronous instant lookup: uses cached monthly official data if present, else offline calculator. */
    fun getPrayerDayImmediate(
        date: LocalDate,
        location: LocationInfo,
        method: CalculationMethod
    ): PrayerDay {
        val cached = readCachedMonth(date.year, date.monthValue, location, method)
        if (cached != null) {
            parseDayFromMonthJson(cached, date, location.timezoneId, method)?.let { return it }
        }
        return PrayerTimeCalculator.calculate(
            date = date,
            latitude = location.latitude,
            longitude = location.longitude,
            timezoneId = location.timezoneId,
            method = method
        )
    }

    /** Refreshes official monthly calendar from Aladhan API if not yet cached for this month/location/method. */
    suspend fun getOrFetchPrayerDay(
        date: LocalDate,
        location: LocationInfo,
        method: CalculationMethod,
        forceRefresh: Boolean = false
    ): PrayerDay = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = readCachedMonth(date.year, date.monthValue, location, method)
            if (cached != null) {
                parseDayFromMonthJson(cached, date, location.timezoneId, method)?.let { return@withContext it }
            }
        }

        val fetchedJson = fetchMonthFromAladhan(date.year, date.monthValue, location, method)
        if (fetchedJson != null) {
            writeCachedMonth(date.year, date.monthValue, location, method, fetchedJson)
            parseDayFromMonthJson(fetchedJson, date, location.timezoneId, method)?.let { return@withContext it }
        }

        getPrayerDayImmediate(date, location, method)
    }

    private fun cacheFile(year: Int, month: Int, loc: LocationInfo, method: CalculationMethod): File {
        val latKey = String.format(Locale.US, "%.2f", loc.latitude)
        val lonKey = String.format(Locale.US, "%.2f", loc.longitude)
        return File(cacheDir, "m${method.aladhanId}_${year}_${month}_${latKey}_${lonKey}.json")
    }

    private fun readCachedMonth(year: Int, month: Int, loc: LocationInfo, method: CalculationMethod): String? {
        val file = cacheFile(year, month, loc, method)
        if (!file.exists()) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    private fun writeCachedMonth(year: Int, month: Int, loc: LocationInfo, method: CalculationMethod, content: String) {
        runCatching {
            // Prune old cache files if more than 20 accumulate
            val existing = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            if (existing.size > 20) {
                existing.take(existing.size - 20).forEach { it.delete() }
            }
            cacheFile(year, month, loc, method).writeText(content, Charsets.UTF_8)
        }
    }

    private fun fetchMonthFromAladhan(
        year: Int,
        month: Int,
        loc: LocationInfo,
        method: CalculationMethod
    ): String? {
        val urlStr = String.format(
            Locale.US,
            "https://api.aladhan.com/v1/calendar/%d/%d?latitude=%.5f&longitude=%.5f&method=%d",
            year, month, loc.latitude, loc.longitude, method.aladhanId
        )
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "NamazVakitleriAndroid/1.0")
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun parseDayFromMonthJson(
        rawJson: String,
        targetDate: LocalDate,
        fallbackTz: String,
        method: CalculationMethod
    ): PrayerDay? = runCatching {
        val root = JSONObject(rawJson)
        if (root.optInt("code") != 200) return@runCatching null
        val dataArr = root.optJSONArray("data") ?: return@runCatching null
        val index = targetDate.dayOfMonth - 1
        if (index !in 0 until dataArr.length()) return@runCatching null

        val dayObj = dataArr.getJSONObject(index)
        val timings = dayObj.getJSONObject("timings")
        val meta = dayObj.optJSONObject("meta")
        val apiTz = meta?.optString("timezone")?.takeIf { it.isNotBlank() } ?: fallbackTz

        // Note: In Diyanet (Method 13), Fajr IS official İmsak time.
        val times = linkedMapOf(
            PrayerType.IMSAK to parseHhMm(timings.getString("Fajr")),
            PrayerType.GUNES to parseHhMm(timings.getString("Sunrise")),
            PrayerType.OGLE to parseHhMm(timings.getString("Dhuhr")),
            PrayerType.IKINDI to parseHhMm(timings.getString("Asr")),
            PrayerType.AKSAM to parseHhMm(timings.getString("Maghrib")),
            PrayerType.YATSI to parseHhMm(timings.getString("Isha"))
        )

        val hijriObj = dayObj.optJSONObject("date")?.optJSONObject("hijri")
        val hijriText = if (hijriObj != null) {
            val hDay = hijriObj.optString("day", "").trimStart('0')
            val hMonthNum = hijriObj.optJSONObject("month")?.optInt("number", 1) ?: 1
            val hYear = hijriObj.optString("year", "")
            "$hDay ${PrayerTimeCalculator.turkishHijriMonthName(hMonthNum)} $hYear"
        } else {
            PrayerTimeCalculator.computeHijriDateTurkish(targetDate)
        }

        PrayerDay(
            date = targetDate,
            timezoneId = apiTz,
            hijriDateText = hijriText,
            times = times,
            sourceLabel = method.displayName
        )
    }.getOrNull()

    private fun parseHhMm(raw: String): LocalTime {
        // Strips suffixes like "06:24 (+03)" -> "06:24"
        val clean = raw.trim().substringBefore(" ")
        val parts = clean.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return LocalTime.of(h, m)
    }
}
