package com.example.shikagera1.domain

import java.time.DayOfWeek
import java.time.LocalDate

object BalanceCalculator {
    fun dailyNormMinutes(date: LocalDate): Int {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 0
            else -> WorkConstants.DAILY_NORM_MINUTES
        }
    }

    fun workedMinutes(record: DayRecord): Int {
        val arrival = record.arrivalMinutes ?: return 0
        val departure = record.departureMinutes ?: return 0
        if (departure < arrival) return 0
        val gross = departure - arrival
        return (gross - record.excludedMinutes).coerceAtLeast(0)
    }

    fun dailyBalance(record: DayRecord): Int {
        return workedMinutes(record) - dailyNormMinutes(record.date)
    }

    fun lastWeeklyResetDate(today: LocalDate = LocalDate.now()): LocalDate {
        val candidates = listOf(
            today.withDayOfMonth(WorkConstants.PERIOD_START_FIRST),
            today.withDayOfMonth(WorkConstants.PERIOD_START_SECOND),
            today.minusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_FIRST),
            today.minusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_SECOND),
        ).filter { !it.isAfter(today) }

        return candidates.maxOrNull()
            ?: today.minusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_SECOND)
    }

    fun isAfterManualReset(recordDate: LocalDate, manualResetDate: LocalDate?): Boolean {
        return manualResetDate == null || recordDate.isAfter(manualResetDate)
    }

    fun currentWeekBalance(
        records: List<DayRecord>,
        today: LocalDate = LocalDate.now(),
        manualResetDate: LocalDate? = null,
    ): Int {
        val autoResetDate = lastWeeklyResetDate(today)
        return records
            .filter { record ->
                WorkWeekCalculator.isInCurrentWorkWeek(record.date, today) &&
                    !record.date.isAfter(today) &&
                    !record.date.isBefore(autoResetDate) &&
                    isAfterManualReset(record.date, manualResetDate)
            }
            .sumOf(::dailyBalance)
    }

    fun balanceBeforeDate(
        records: List<DayRecord>,
        date: LocalDate,
        accumulatedBalanceMinutes: Int,
        today: LocalDate = LocalDate.now(),
        manualResetDate: LocalDate? = null,
    ): Int {
        val autoResetDate = lastWeeklyResetDate(today)
        val weekBefore = records
            .filter { record ->
                WorkWeekCalculator.isInCurrentWorkWeek(record.date, today) &&
                    record.date.isBefore(date) &&
                    !record.date.isBefore(autoResetDate) &&
                    isAfterManualReset(record.date, manualResetDate)
            }
            .sumOf(::dailyBalance)
        return accumulatedBalanceMinutes + weekBefore
    }

    fun totalBalance(
        records: List<DayRecord>,
        accumulatedBalanceMinutes: Int,
        today: LocalDate = LocalDate.now(),
        manualResetDate: LocalDate? = null,
    ): Int {
        return accumulatedBalanceMinutes + currentWeekBalance(records, today, manualResetDate)
    }
}