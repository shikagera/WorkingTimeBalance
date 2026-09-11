package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BalanceCalculatorTest {
    @Test
    fun dailyBalance_workday() {
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 7),
            arrivalMinutes = 9 * 60,
            departureMinutes = 17 * 60 + 45,
        )
        assertEquals(0, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun dailyBalance_openDayDoesNotCount() {
        val onlyArrival = DayRecord(
            date = LocalDate.of(2026, 7, 20),
            arrivalMinutes = 9 * 60,
            departureMinutes = null,
        )
        assertEquals(0, BalanceCalculator.dailyBalance(onlyArrival))
        assertEquals(
            0,
            BalanceCalculator.totalBalance(
                records = listOf(onlyArrival),
                accumulatedBalanceMinutes = 0,
                today = LocalDate.of(2026, 7, 20),
            ),
        )
    }

    @Test
    fun totalBalance_ignoresOpenTodayButCountsClosedDays() {
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 17), 9 * 60, 18 * 60), // +15 closed Fri
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, null), // open Mon — ignore
        )
        assertEquals(
            15,
            BalanceCalculator.totalBalance(records, accumulatedBalanceMinutes = 0, today),
        )
    }

    @Test
    fun dailyBalance_weekend() {
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 11),
            arrivalMinutes = 10 * 60,
            departureMinutes = 12 * 60,
        )
        assertEquals(120, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun currentWeekBalance_includesPeriodDaysAcrossCalendarWeeks() {
        // Period starts on the 16th; Mon 20th should still count Thu–Fri of previous week.
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            // Before period start — ignored
            DayRecord(LocalDate.of(2026, 7, 15), 9 * 60, 18 * 60),
            // Previous calendar week, inside period: +15 each
            DayRecord(LocalDate.of(2026, 7, 16), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 17), 9 * 60, 18 * 60),
            // Current week Monday: 0
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, 17 * 60 + 45),
        )
        assertEquals(30, BalanceCalculator.currentWeekBalance(records, today))
    }

    @Test
    fun currentWeekBalance_resetsOnSixteenth() {
        val today = LocalDate.of(2026, 7, 16)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 14), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 15), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 16), 9 * 60, 17 * 60 + 45),
        )
        assertEquals(0, BalanceCalculator.currentWeekBalance(records, today))
    }

    @Test
    fun totalBalance_carriesPreviousWeekWithinPeriod() {
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 17), 9 * 60, 18 * 60), // +15 Fri
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, 18 * 60), // +15 Mon
        )
        assertEquals(
            30,
            BalanceCalculator.totalBalance(records, accumulatedBalanceMinutes = 0, today),
        )
    }

    @Test
    fun workedMinutes_subtractsExcluded() {
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 9),
            arrivalMinutes = 9 * 60,
            departureMinutes = 18 * 60,
            excludedMinutes = 45,
        )
        assertEquals(495, BalanceCalculator.workedMinutes(record))
        assertEquals(-30, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun totalBalance_includesAccumulated() {
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, 18 * 60),
        )
        assertEquals(60, BalanceCalculator.totalBalance(records, accumulatedBalanceMinutes = 45, today))
    }

    @Test
    fun manualReset_zerosBalanceForCurrentWeek() {
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 17), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, 18 * 60),
        )
        assertEquals(
            0,
            BalanceCalculator.totalBalance(
                records = records,
                accumulatedBalanceMinutes = 0,
                today = today,
                manualResetDate = today,
            ),
        )
    }
}
