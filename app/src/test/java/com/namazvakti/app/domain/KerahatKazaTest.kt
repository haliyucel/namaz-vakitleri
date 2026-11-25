package com.namazvakti.app.domain

import com.namazvakti.app.data.KazaPrayerType
import com.namazvakti.app.data.KazaStateReducer
import com.namazvakti.app.data.KazaTrackerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun tracksPrayerAndFastingFulfillmentAndDailyReset() {
        val day1 = "2026-11-25"
        val day2 = "2026-11-26"
        var state = KazaTrackerState()

        // Add 10 days prayer debt (60 prayers) & 15 days fasting debt
        state = KazaStateReducer.addPrayerDaysToAll(state, days = 10, replaceExisting = true, todayIso = day1, nowMillis = 1000L)
        state = KazaStateReducer.addFastingDebt(state, days = 15, replaceExisting = true, todayIso = day1, nowMillis = 2000L)
        assertEquals(60, state.totalRemainingPrayers)
        assertEquals(15, state.fastingDays)

        // Fulfill Sabah prayer (-1), 1 full day (-6), and 1 fasting day (-1)
        state = KazaStateReducer.fulfillPrayer(state, KazaPrayerType.SABAH, count = 1, todayIso = day1, nowMillis = 3000L)
        state = KazaStateReducer.fulfillOneFullDay(state, todayIso = day1, nowMillis = 4000L)
        state = KazaStateReducer.fulfillFastingDay(state, days = 1, todayIso = day1, nowMillis = 5000L)

        assertEquals(8, state.counts[KazaPrayerType.SABAH])
        assertEquals(9, state.counts[KazaPrayerType.OGLE])
        assertEquals(53, state.totalRemainingPrayers)
        assertEquals(7, state.completedPrayers)
        assertEquals(7, state.todayCompletedPrayers)
        assertEquals(14, state.fastingDays)
        assertEquals(1, state.completedFastingDays)
        assertEquals(1, state.todayCompletedFasts)
        assertEquals(5, state.history.size)
        assertTrue(state.history.first().isFulfillment)

        // Next day resets daily counters while keeping totals intact
        state = KazaStateReducer.fulfillFastingDay(state, days = 1, todayIso = day2, nowMillis = 6000L)
        assertEquals(13, state.fastingDays)
        assertEquals(2, state.completedFastingDays)
        assertEquals(0, state.todayCompletedPrayers)
        assertEquals(1, state.todayCompletedFasts)
    }
}
