package com.namazvakti.app.domain

import com.namazvakti.app.model.CalculationMethod
import com.namazvakti.app.model.PrayerDay
import com.namazvakti.app.model.PrayerType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure-Kotlin astronomical prayer time engine with high-latitude safety and
 * Diyanet İşleri Başkanlığı (plus worldwide methods) support.
 */
object PrayerTimeCalculator {

    private val HIJRI_MONTHS_TR = arrayOf(
        "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
        "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban",
        "Ramazan", "Şevval", "Zilkade", "Zilhicce"
    )

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        method: CalculationMethod = CalculationMethod.DIYANET
    ): PrayerDay {
        val zone = runCatching { ZoneId.of(timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val tzOffsetHours = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jd0 = julianDay(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)

        // Initial approximations (in fraction of day)
        var tImsak = 5.0 / 24.0
        var tGunes = 6.0 / 24.0
        var tOgle = 12.0 / 24.0
        var tIkindi = 13.0 / 24.0
        var tAksam = 18.0 / 24.0
        var tYatsi = 18.0 / 24.0

        // 2-pass iterative refinement for solar declination & equation of time
        repeat(2) {
            val fajrH = sunAngleTime(jd0 + tImsak, latitude, -method.fajrAngle, isMorning = true)
            val sunriseH = sunAngleTime(jd0 + tGunes, latitude, -0.8333, isMorning = true)
            val dhuhrH = midDay(jd0 + tOgle)
            val asrH = asrTime(jd0 + tIkindi, latitude, method.asrShadowFactor)
            val sunsetH = sunAngleTime(jd0 + tAksam, latitude, -0.8333, isMorning = false)
            val ishaH = if (method.ishaAngle != null) {
                sunAngleTime(jd0 + tYatsi, latitude, -method.ishaAngle, isMorning = false)
            } else {
                sunsetH + (method.ishaMinutesAfterMaghrib ?: 90) / 60.0
            }

            // High-latitude angle-based adjustment if Fajr or Isha cannot be solved directly
            val nightHours = positiveMod(sunriseH - sunsetH, 24.0)
            val safeFajr = if (fajrH.isNaN()) {
                sunriseH - (method.fajrAngle / 60.0) * nightHours
            } else fajrH
            val safeIsha = if (ishaH.isNaN()) {
                val angle = method.ishaAngle ?: 17.0
                sunsetH + (angle / 60.0) * nightHours
            } else ishaH

            tImsak = safeFajr / 24.0
            tGunes = sunriseH / 24.0
            tOgle = dhuhrH / 24.0
            tIkindi = asrH / 24.0
            tAksam = sunsetH / 24.0
            tYatsi = safeIsha / 24.0
        }

        val shift = tzOffsetHours - longitude / 15.0
        val imsakHours = tImsak * 24.0 + shift + method.imsakOffsetMin / 60.0
        val gunesHours = tGunes * 24.0 + shift + method.gunesOffsetMin / 60.0
        val ogleHours = tOgle * 24.0 + shift + method.ogleOffsetMin / 60.0
        val ikindiHours = tIkindi * 24.0 + shift + method.ikindiOffsetMin / 60.0
        val aksamHours = tAksam * 24.0 + shift + method.aksamOffsetMin / 60.0
        val yatsiHours = tYatsi * 24.0 + shift + method.yatsiOffsetMin / 60.0

        val times = linkedMapOf(
            PrayerType.IMSAK to hoursToLocalTime(imsakHours),
            PrayerType.GUNES to hoursToLocalTime(gunesHours),
            PrayerType.OGLE to hoursToLocalTime(ogleHours),
            PrayerType.IKINDI to hoursToLocalTime(ikindiHours),
            PrayerType.AKSAM to hoursToLocalTime(aksamHours),
            PrayerType.YATSI to hoursToLocalTime(yatsiHours)
        )

        return PrayerDay(
            date = date,
            timezoneId = zone.id,
            hijriDateText = computeHijriDateTurkish(date),
            times = times,
            sourceLabel = "${method.displayName} (Çevrimdışı)"
        )
    }

    fun computeHijriDateTurkish(date: LocalDate): String {
        val jd = julianDay(date.year, date.monthValue, date.dayOfMonth).toLong() + 1L
        val l0 = jd - 1948440L + 10632L
        val n = (l0 - 1L) / 10631L
        val l1 = l0 - 10631L * n + 354L
        val j = ((10985L - l1) / 5316L) * ((50L * l1) / 17719L) +
            (l1 / 5670L) * ((43L * l1) / 15238L)
        val l2 = l1 - ((30L - j) / 15L) * ((17719L * j) / 50L) -
            (j / 16L) * ((15238L * j) / 43L) + 29L
        val month = ((24L * l2) / 709L).toInt().coerceIn(1, 12)
        val day = (l2 - (709L * month) / 24L).toInt().coerceIn(1, 30)
        val year = (30L * n + j - 30L).toInt()
        return "$day ${HIJRI_MONTHS_TR[month - 1]} $year"
    }

    fun turkishHijriMonthName(monthNumber: Int): String =
        HIJRI_MONTHS_TR[(monthNumber - 1).coerceIn(0, 11)]

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))
        return decl to eqt
    }

    private fun midDay(jd: Double): Double {
        val (_, eqt) = sunPosition(jd)
        return fixHour(12.0 - eqt)
    }

    private fun sunAngleTime(jd: Double, latitude: Double, altitudeDeg: Double, isMorning: Boolean): Double {
        val (decl, _) = sunPosition(jd)
        val noon = midDay(jd)
        val cosH = (sinDeg(altitudeDeg) - sinDeg(latitude) * sinDeg(decl)) /
            (cosDeg(latitude) * cosDeg(decl))
        if (cosH < -1.0 || cosH > 1.0) return Double.NaN
        val t = acosDeg(cosH) / 15.0
        return if (isMorning) noon - t else noon + t
    }

    private fun asrTime(jd: Double, latitude: Double, shadowFactor: Double): Double {
        val (decl, _) = sunPosition(jd)
        val altitudeDeg = atanDeg(1.0 / (shadowFactor + tanDeg(abs(latitude - decl))))
        return sunAngleTime(jd, latitude, altitudeDeg, isMorning = false)
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun hoursToLocalTime(hours: Double): LocalTime {
        if (hours.isNaN() || hours.isInfinite()) return LocalTime.MIDNIGHT
        val normalized = positiveMod(hours, 24.0)
        val totalMinutes = (normalized * 60.0).roundToInt() % (24 * 60)
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60)
    }

    private fun positiveMod(value: Double, mod: Double): Double {
        val r = value % mod
        return if (r < 0) r + mod else r
    }

    private fun fixAngle(a: Double): Double = positiveMod(a, 360.0)
    private fun fixHour(h: Double): Double = positiveMod(h, 24.0)

    private fun sinDeg(deg: Double): Double = sin(deg * PI / 180.0)
    private fun cosDeg(deg: Double): Double = cos(deg * PI / 180.0)
    private fun tanDeg(deg: Double): Double = tan(deg * PI / 180.0)
    private fun asinDeg(x: Double): Double = asin(x) * 180.0 / PI
    private fun acosDeg(x: Double): Double = acos(x) * 180.0 / PI
    private fun atanDeg(x: Double): Double = atan(x) * 180.0 / PI
    private fun atan2Deg(y: Double, x: Double): Double = atan2(y, x) * 180.0 / PI
}
