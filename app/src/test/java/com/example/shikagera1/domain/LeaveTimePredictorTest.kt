package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LeaveTimePredictorTest {
    @Test
    fun predictedDeparture_withNegativeBalance() {
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = -30,
            date = LocalDate.of(2026, 7, 9),
        )
        assertEquals(18 * 60 + 15, result)
    }

    @Test
    fun predictedDeparture_withPositiveBalance() {
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = 45,
            date = LocalDate.of(2026, 7, 9),
        )
        assertEquals(17 * 60, result)
    }
}