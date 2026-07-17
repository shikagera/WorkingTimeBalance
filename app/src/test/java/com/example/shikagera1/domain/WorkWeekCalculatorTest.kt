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
        val today = LocalDate.of(2026, 7, 9)
        // Previous week Mon 29.06; period start 08.07 → keep from previous week.
        assertEquals(LocalDate.of(2026, 6, 29), WorkWeekCalculator.retentionStartDate(today))
    }

    @Test
    fun retentionStartDate_extendsToPeriodStart_whenEarlierThanPreviousWeek() {
        // Wed 22.07: previous Mon = 13.07, period start = 08.07 → keep from 08.07 for balance.
        val today = LocalDate.of(2026, 7, 22)
        assertEquals(LocalDate.of(2026, 7, 8), WorkWeekCalculator.retentionStartDate(today))
    }
}