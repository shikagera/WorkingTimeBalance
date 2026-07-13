package com.example.shikagera1.domain

import java.time.LocalDate

object LeaveTimePredictor {
    fun predictedDepartureMinutes(
        arrivalMinutes: Int,
        balanceBeforeToday: Int,
        date: LocalDate = LocalDate.now(),
    ): Int? {
        val norm = BalanceCalculator.dailyNormMinutes(date)
        if (norm == 0) return null

        val minutesNeeded = norm - balanceBeforeToday
        return arrivalMinutes + minutesNeeded
    }
}