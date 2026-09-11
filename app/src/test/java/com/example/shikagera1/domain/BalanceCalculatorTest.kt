package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BalanceCalculatorTest {
    @Test
    fun dailyBalance_fullDayWithBreakIsZero() {
        // 9:00 → 17:45 = 8ч45 присутствия = 8ч работы + 45 мин перерыва.
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 7),
            arrivalMinutes = 9 * 60,
            departureMinutes = 17 * 60 + 45,
        )
        assertEquals(480, BalanceCalculator.workedMinutes(record))
        assertEquals(0, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun dailyBalance_shortDayHasNoBreakDeducted() {
        // Ушёл через 3 часа — до перерыва не дошло, отработано ровно 3 часа.
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 7),
            arrivalMinutes = 9 * 60,
            departureMinutes = 12 * 60,
        )
        assertEquals(180, BalanceCalculator.workedMinutes(record))
        assertEquals(-300, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun dailyBalance_leavingDuringBreakCountsFirstHalfOnly() {
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 7),
            arrivalMinutes = 9 * 60,
            departureMinutes = 13 * 60 + 20,
        )
        assertEquals(240, BalanceCalculator.workedMinutes(record))
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
            BalanceCalculator.periodBalance(listOf(onlyArrival), today = LocalDate.of(2026, 7, 20)),
        )
    }

    @Test
    fun dailyBalance_creditedDayIsZeroRegardlessOfTimes() {
        val noTimes = DayRecord(date = LocalDate.of(2026, 7, 7), isCredited = true)
        val shortDay = DayRecord(
            date = LocalDate.of(2026, 7, 7),
            arrivalMinutes = 9 * 60,
            departureMinutes = 11 * 60,
            isCredited = true,
        )
        assertEquals(0, BalanceCalculator.dailyBalance(noTimes))
        assertEquals(0, BalanceCalculator.dailyBalance(shortDay))
    }

    @Test
    fun periodBalance_ignoresOpenTodayButCountsClosedDays() {
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 17), 9 * 60, 18 * 60), // +15 closed Fri
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, null), // open Mon — ignore
        )
        assertEquals(15, BalanceCalculator.periodBalance(records, today))
    }

    @Test
    fun dailyBalance_weekendIsPureOvertime() {
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 11),
            arrivalMinutes = 10 * 60,
            departureMinutes = 12 * 60,
        )
        assertEquals(120, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun periodBalance_includesPeriodDaysAcrossCalendarWeeks() {
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
        assertEquals(30, BalanceCalculator.periodBalance(records, today))
    }

    @Test
    fun periodBalance_resetsOnSixteenth() {
        val today = LocalDate.of(2026, 7, 16)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 14), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 15), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 16), 9 * 60, 17 * 60 + 45),
        )
        assertEquals(0, BalanceCalculator.periodBalance(records, today))
    }

    @Test
    fun balanceBeforeDate_excludesGivenDay() {
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 17), 9 * 60, 18 * 60), // +15
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, 18 * 60), // +15, but it's "today"
        )
        assertEquals(15, BalanceCalculator.balanceBeforeDate(records, today, today))
        assertEquals(30, BalanceCalculator.periodBalance(records, today))
    }

    @Test
    fun workedMinutes_subtractsExcluded() {
        // 9:00 → 18:00 = 540, минус 30 исключённых = 510 присутствия, минус перерыв = 465.
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 9),
            arrivalMinutes = 9 * 60,
            departureMinutes = 18 * 60,
            excludedMinutes = 30,
        )
        assertEquals(465, BalanceCalculator.workedMinutes(record))
        assertEquals(-15, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun manualReset_zerosBalanceThroughResetDay() {
        val today = LocalDate.of(2026, 7, 20)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 17), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 20), 9 * 60, 18 * 60),
        )
        assertEquals(
            0,
            BalanceCalculator.periodBalance(records, today = today, manualResetDate = today),
        )
        assertEquals(
            15,
            BalanceCalculator.periodBalance(records, today = today, manualResetDate = LocalDate.of(2026, 7, 17)),
        )
    }
}
