package com.namazvakti.app.domain

import kotlin.math.roundToInt

data class KazaCalculationResult(
    val totalDays: Int,
    val exemptedDays: Int,
    val netPrayerDays: Int,
    val totalPrayersIncludingVitir: Int
)

/**
 * Pure-Kotlin Kaza prayer calculator supporting both duration-based (years/months/days)
 * and age-based (bulug/puberty age -> start age) calculations, plus optional monthly
 * menstrual exemption days per Hanefi/Diyanet fiqh.
 */
object KazaCalculator {
    const val PRAYERS_PER_DAY = 6 // Sabah, Öğle, İkindi, Akşam, Yatsı, Vitir

    fun calculateFromDuration(
        years: Int,
        months: Int,
        days: Int,
        monthlyExemptionDays: Int = 0
    ): KazaCalculationResult {
        val safeYears = years.coerceAtLeast(0)
        val safeMonths = months.coerceAtLeast(0)
        val safeDays = days.coerceAtLeast(0)
        val safeExemptPerMonth = monthlyExemptionDays.coerceIn(0, 10)

        val totalDays = (safeYears * 365) + (safeMonths * 30) + safeDays
        val totalMonthsApprox = (safeYears * 12.0) + safeMonths + (safeDays / 30.0)
        val exemptedDays = (totalMonthsApprox * safeExemptPerMonth).roundToInt().coerceIn(0, totalDays)
        val netDays = (totalDays - exemptedDays).coerceAtLeast(0)

        return KazaCalculationResult(
            totalDays = totalDays,
            exemptedDays = exemptedDays,
            netPrayerDays = netDays,
            totalPrayersIncludingVitir = netDays * PRAYERS_PER_DAY
        )
    }

    fun calculateFromAges(
        pubertyAge: Int,
        startPrayingAge: Int,
        monthlyExemptionDays: Int = 0
    ): KazaCalculationResult {
        val missedYears = (startPrayingAge - pubertyAge).coerceAtLeast(0)
        return calculateFromDuration(
            years = missedYears,
            months = 0,
            days = 0,
            monthlyExemptionDays = monthlyExemptionDays
        )
    }

    /** Estimates days needed to complete all remaining kazas if praying [dailySets] full sets/day. */
    fun estimatedCompletionText(remainingTotalPrayers: Int, dailySets: Int = 1): String {
        if (remainingTotalPrayers <= 0) return "Kalan kaza borcu yok"
        val prayersPerDay = (dailySets.coerceAtLeast(1) * PRAYERS_PER_DAY)
        val daysNeeded = (remainingTotalPrayers + prayersPerDay - 1) / prayersPerDay
        val years = daysNeeded / 365
        val months = (daysNeeded % 365) / 30
        val days = (daysNeeded % 365) % 30
        return buildList {
            if (years > 0) add("$years yıl")
            if (months > 0) add("$months ay")
            if (days > 0 || (years == 0 && months == 0)) add("$days gün")
        }.joinToString(" ")
    }
}
