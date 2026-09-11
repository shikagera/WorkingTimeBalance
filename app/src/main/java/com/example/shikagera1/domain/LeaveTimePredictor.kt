package com.example.shikagera1.domain

import java.time.LocalDate

object LeaveTimePredictor {
    /**
     * Время, когда суммарный баланс периода станет нулевым.
     * Учитывает перерыв (если до него дойдёт) и уже исключённые сегодня минуты.
     */
    fun predictedDepartureMinutes(
        arrivalMinutes: Int,
        balanceBeforeToday: Int,
        excludedMinutes: Int = 0,
        date: LocalDate = LocalDate.now(),
    ): Int? {
        val norm = BalanceCalculator.dailyNormMinutes(date)
        if (norm == 0) return null

        val workNeeded = norm - balanceBeforeToday
        val presenceNeeded = WorkDayPhases.presenceForWorkMinutes(workNeeded)
        return arrivalMinutes + presenceNeeded + excludedMinutes.coerceAtLeast(0)
    }
}
