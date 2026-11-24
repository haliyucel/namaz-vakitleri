package com.namazvakti.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class KazaCalculatorTest {

    @Test
    fun calculatesDurationWithoutExemption() {
        val res = KazaCalculator.calculateFromDuration(years = 2, months = 1, days = 5, monthlyExemptionDays = 0)
        assertEquals(765, res.totalDays)
        assertEquals(0, res.exemptedDays)
        assertEquals(765, res.netPrayerDays)
        assertEquals(765 * 6, res.totalPrayersIncludingVitir)
    }

    @Test
    fun calculatesAgesWithMonthlyExemption() {
        val res = KazaCalculator.calculateFromAges(pubertyAge = 12, startPrayingAge = 22, monthlyExemptionDays = 7)
        // 10 years = 3650 days, 120 months * 7 = 840 exempted -> 2810 net days
        assertEquals(3650, res.totalDays)
        assertEquals(840, res.exemptedDays)
        assertEquals(2810, res.netPrayerDays)
        assertEquals(2810 * 6, res.totalPrayersIncludingVitir)
    }

    @Test
    fun formatsEstimatedCompletionTime() {
        assertEquals("1 yıl", KazaCalculator.estimatedCompletionText(365 * 6, dailySets = 1))
        assertEquals("15 gün", KazaCalculator.estimatedCompletionText(15 * 6, dailySets = 1))
        assertEquals("Kalan kaza borcu yok", KazaCalculator.estimatedCompletionText(0))
    }
}
