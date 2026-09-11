package com.example.shikagera1.domain

import java.time.LocalDate

object PeriodCalculator {
    fun isInCurrentPeriod(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        val periodStart = currentPeriodStart(today)
        val periodEnd = currentPeriodEnd(today)
        return !date.isBefore(periodStart) && !date.isAfter(periodEnd)
    }

    fun currentPeriodStart(today: LocalDate = LocalDate.now()): LocalDate {
        return if (today.dayOfMonth >= WorkConstants.PERIOD_START_DAY) {
            today.withDayOfMonth(WorkConstants.PERIOD_START_DAY)
        } else {
            today.minusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_DAY)
        }
    }

    fun currentPeriodEnd(today: LocalDate = LocalDate.now()): LocalDate {
        return currentPeriodStart(today).plusMonths(1).minusDays(1)
    }

    fun nextResetDate(today: LocalDate = LocalDate.now()): LocalDate {
        val thisMonthReset = today.withDayOfMonth(WorkConstants.PERIOD_START_DAY)
        return if (today.isBefore(thisMonthReset)) {
            thisMonthReset
        } else {
            today.plusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_DAY)
        }
    }

    fun shouldShowResetWarning(today: LocalDate = LocalDate.now()): Boolean {
        val resetDate = nextResetDate(today)
        val warningDate = resetDate.minusDays(WorkConstants.RESET_WARNING_DAYS_BEFORE.toLong())
        return today == warningDate
    }

    fun resetWarningMessage(today: LocalDate = LocalDate.now()): String {
        val resetDate = nextResetDate(today)
        return "Через 2 дня баланс периода будет сброшен (${resetDate.dayOfMonth}-го числа)"
    }

    fun isPeriodResetDay(today: LocalDate = LocalDate.now()): Boolean {
        return today.dayOfMonth == WorkConstants.PERIOD_START_DAY
    }
}
