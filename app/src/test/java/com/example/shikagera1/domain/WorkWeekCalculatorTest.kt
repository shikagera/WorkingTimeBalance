package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun retentionStartDate_isOneMonthBack() {
        assertEquals(LocalDate.of(2026, 6, 17), WorkWeekCalculator.retentionStartDate(LocalDate.of(2026, 7, 17)))
        // 31 марта минус месяц — конец февраля, без выхода за границы месяца.
        assertEquals(LocalDate.of(2026, 2, 28), WorkWeekCalculator.retentionStartDate(LocalDate.of(2026, 3, 31)))
    }

    @Test
    fun retentionStartDate_alwaysCoversCurrentPeriodAndPreviousWeek() {
        // Худший случай: 15-е число — период начался месяц назад, 16-го.
        val today = LocalDate.of(2026, 8, 15)
        val retention = WorkWeekCalculator.retentionStartDate(today)
        assertTrue(!retention.isAfter(PeriodCalculator.currentPeriodStart(today)))
        assertTrue(!retention.isAfter(WorkWeekCalculator.previousWeekMonday(today)))
    }
}
