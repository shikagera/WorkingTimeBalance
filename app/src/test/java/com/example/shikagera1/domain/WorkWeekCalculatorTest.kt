package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WorkWeekCalculatorTest {
    @Test
    fun visibleWorkDays_showsOnlyUpToToday() {
        val today = LocalDate.of(2026, 7, 8)
        val days = WorkWeekCalculator.visibleWorkDays(today)
        assertEquals(listOf(6, 7, 8), days.map { it.dayOfMonth })
    }

    @Test
    fun visibleWorkDays_showsFullWeekOnWeekend() {
        val today = LocalDate.of(2026, 7, 11)
        val days = WorkWeekCalculator.visibleWorkDays(today)
        assertEquals(listOf(6, 7, 8, 9, 10), days.map { it.dayOfMonth })
    }

    @Test
    fun retentionStartDate_isPreviousWeekMonday_whenEarlierThanPeriod() {
        // Fri 17.07: previous Mon = 06.07, period start = 16.07 → keep from previous week.
        val today = LocalDate.of(2026, 7, 17)
        assertEquals(LocalDate.of(2026, 7, 6), WorkWeekCalculator.retentionStartDate(today))
    }

    @Test
    fun retentionStartDate_extendsToPeriodStart_whenEarlierThanPreviousWeek() {
        // Wed 29.07: previous Mon = 20.07, period start = 16.07 → keep from 16.07 for balance.
        val today = LocalDate.of(2026, 7, 29)
        assertEquals(LocalDate.of(2026, 7, 16), WorkWeekCalculator.retentionStartDate(today))
    }
}
