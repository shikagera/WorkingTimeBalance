package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodCalculatorTest {
    @Test
    fun currentPeriod_firstHalf() {
        val today = LocalDate.of(2026, 7, 15)
        assertEquals(LocalDate.of(2026, 7, 8), PeriodCalculator.currentPeriodStart(today))
        assertEquals(LocalDate.of(2026, 7, 22), PeriodCalculator.currentPeriodEnd(today))
    }

    @Test
    fun shouldShowResetWarning_onSixth() {
        assertTrue(PeriodCalculator.shouldShowResetWarning(LocalDate.of(2026, 7, 6)))
        assertFalse(PeriodCalculator.shouldShowResetWarning(LocalDate.of(2026, 7, 7)))
    }

    @Test
    fun nextResetDate_fromJulyFifth() {
        assertEquals(LocalDate.of(2026, 7, 8), PeriodCalculator.nextResetDate(LocalDate.of(2026, 7, 5)))
    }
}