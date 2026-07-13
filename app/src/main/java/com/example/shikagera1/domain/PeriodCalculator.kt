package com.example.shikagera1.domain

import java.time.LocalDate

object PeriodCalculator {
    fun isInCurrentPeriod(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        val periodStart = currentPeriodStart(today)
        val periodEnd = currentPeriodEnd(today)
        return !date.isBefore(periodStart) && !date.isAfter(periodEnd)
    }

    fun currentPeriodStart(today: LocalDate = LocalDate.now()): LocalDate {
        return if (today.dayOfMonth >= WorkConstants.PERIOD_START_FIRST &&
            today.dayOfMonth < WorkConstants.PERIOD_START_SECOND
        ) {
            today.withDayOfMonth(WorkConstants.PERIOD_START_FIRST)
        } else if (today.dayOfMonth >= WorkConstants.PERIOD_START_SECOND) {
            today.withDayOfMonth(WorkConstants.PERIOD_START_SECOND)
        } else {
            today.minusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_SECOND)
        }
    }

    fun currentPeriodEnd(today: LocalDate = LocalDate.now()): LocalDate {
        return if (today.dayOfMonth >= WorkConstants.PERIOD_START_FIRST &&
            today.dayOfMonth < WorkConstants.PERIOD_START_SECOND
        ) {
            today.withDayOfMonth(WorkConstants.PERIOD_START_SECOND - 1)
        } else if (today.dayOfMonth >= WorkConstants.PERIOD_START_SECOND) {
            today.plusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_FIRST - 1)
        } else {
            today.withDayOfMonth(WorkConstants.PERIOD_START_FIRST - 1)
        }
    }

    fun nextResetDate(today: LocalDate = LocalDate.now()): LocalDate {
        val firstReset = today.withDayOfMonth(WorkConstants.PERIOD_START_FIRST)
        val secondReset = today.withDayOfMonth(WorkConstants.PERIOD_START_SECOND)

        return when {
            today.isBefore(firstReset) -> firstReset
            today.isBefore(secondReset) -> secondReset
            else -> today.plusMonths(1).withDayOfMonth(WorkConstants.PERIOD_START_FIRST)
        }
    }

    fun shouldShowResetWarning(today: LocalDate = LocalDate.now()): Boolean {
        val resetDate = nextResetDate(today)
        val warningDate = resetDate.minusDays(WorkConstants.RESET_WARNING_DAYS_BEFORE.toLong())
        return today == warningDate
    }

    fun resetWarningMessage(today: LocalDate = LocalDate.now()): String {
        val resetDate = nextResetDate(today)
        return "Через 2 дня баланс недели будет сброшен (${resetDate.dayOfMonth}-го числа)"
    }

    fun isWeeklyResetDay(today: LocalDate = LocalDate.now()): Boolean {
        return today.dayOfMonth == WorkConstants.PERIOD_START_FIRST ||
            today.dayOfMonth == WorkConstants.PERIOD_START_SECOND
    }
}