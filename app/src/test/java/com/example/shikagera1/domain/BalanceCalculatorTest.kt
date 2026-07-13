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
    fun dailyBalance_weekend() {
        val record = DayRecord(
            date = LocalDate.of(2026, 7, 11),
            arrivalMinutes = 10 * 60,
            departureMinutes = 12 * 60,
        )
        assertEquals(120, BalanceCalculator.dailyBalance(record))
    }

    @Test
    fun currentWeekBalance_countsOnlyCurrentMonToFri() {
        val today = LocalDate.of(2026, 7, 9)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 6), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 7), 9 * 60, 17 * 60 + 45),
            DayRecord(LocalDate.of(2026, 7, 8), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 9), 9 * 60, 17 * 60 + 45),
        )
        assertEquals(15, BalanceCalculator.currentWeekBalance(records, today))
    }

    @Test
    fun currentWeekBalance_resetsOnEighth() {
        val today = LocalDate.of(2026, 7, 8)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 6), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 7), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 8), 9 * 60, 17 * 60 + 45),
        )
        assertEquals(0, BalanceCalculator.currentWeekBalance(records, today))
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
        val today = LocalDate.of(2026, 7, 9)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 9), 9 * 60, 18 * 60),
        )
        assertEquals(60, BalanceCalculator.totalBalance(records, accumulatedBalanceMinutes = 45, today))
    }

    @Test
    fun manualReset_zerosBalanceForCurrentWeek() {
        val today = LocalDate.of(2026, 7, 9)
        val records = listOf(
            DayRecord(LocalDate.of(2026, 7, 7), 9 * 60, 18 * 60),
            DayRecord(LocalDate.of(2026, 7, 9), 9 * 60, 18 * 60),
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