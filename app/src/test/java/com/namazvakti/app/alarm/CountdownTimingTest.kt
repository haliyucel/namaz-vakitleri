package com.namazvakti.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownTimingTest {

    @Test
    fun computesFifteenMinutePrayerCountdown() {
        val nowWall = 1_700_000_000_000L
        val targetWall = nowWall + 15L * 60_000L
        val nowElapsed = 500_000L

        val timing = CountdownTiming.compute(nowWall, targetWall, nowElapsed)
        assertNotNull(timing)
        assertEquals(900_000L, timing!!.remainingMillis)
        assertEquals(500_000L + 900_000L, timing.chronometerBaseElapsed)
        assertEquals(900_000L, timing.timeoutAfterMillis)
    }

    @Test
    fun computesFortyFiveMinuteKerahatCountdown() {
        val nowWall = 1_700_000_000_000L
        val targetWall = nowWall + 45L * 60_000L
        val nowElapsed = 123_456L

        val timing = CountdownTiming.compute(nowWall, targetWall, nowElapsed)
        assertNotNull(timing)
        assertEquals(2_700_000L, timing!!.remainingMillis)
        assertEquals(123_456L + 2_700_000L, timing.chronometerBaseElapsed)
        assertEquals(2_700_000L, timing.timeoutAfterMillis)
    }

    @Test
    fun rejectsExpiredOrTooShortTargetsToPreventNegativeTimers() {
        val nowWall = 1_700_000_000_000L
        val nowElapsed = 10_000L

        assertNull(CountdownTiming.compute(nowWall, nowWall - 5_000L, nowElapsed))
        assertNull(CountdownTiming.compute(nowWall, nowWall, nowElapsed))
        assertNull(CountdownTiming.compute(nowWall, nowWall + 1_000L, nowElapsed))
    }
}
