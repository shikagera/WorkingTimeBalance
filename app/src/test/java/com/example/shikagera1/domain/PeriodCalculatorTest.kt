package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodCalculatorTest {
    @Test
    fun currentPeriod_afterSixteenth() {
        val today = LocalDate.of(2026, 7, 20)
        assertEquals(LocalDate.of(2026, 7, 16), PeriodCalculator.currentPeriodStart(today))
        assertEquals(LocalDate.of(2026, 8, 15), PeriodCalculator.currentPeriodEnd(today))
    }

    @Test
    fun currentPeriod_beforeSixteenth() {
        val today = LocalDate.of(2026, 7, 10)
        assertEquals(LocalDate.of(2026, 6, 16), PeriodCalculator.currentPeriodStart(today))
        assertEquals(LocalDate.of(2026, 7, 15), PeriodCalculator.currentPeriodEnd(today))
    }

    @Test
    fun shouldShowResetWarning_twoDaysBefore() {
        assertTrue(PeriodCalculator.shouldShowResetWarning(LocalDate.of(2026, 7, 14)))
        assertFalse(PeriodCalculator.shouldShowResetWarning(LocalDate.of(2026, 7, 15)))
    }

    @Test
    fun nextResetDate_fromJulyFifth() {
        assertEquals(LocalDate.of(2026, 7, 16), PeriodCalculator.nextResetDate(LocalDate.of(2026, 7, 5)))
    }

    @Test
    fun nextResetDate_onOrAfterSixteenth() {
        assertEquals(LocalDate.of(2026, 8, 16), PeriodCalculator.nextResetDate(LocalDate.of(2026, 7, 16)))
        assertEquals(LocalDate.of(2026, 8, 16), PeriodCalculator.nextResetDate(LocalDate.of(2026, 7, 20)))
    }
}
