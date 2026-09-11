package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LeaveTimePredictorTest {
    private val thursday = LocalDate.of(2026, 7, 9)

    @Test
    fun predictedDeparture_zeroBalance_fullDayPlusBreak() {
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = 0,
            date = thursday,
        )
        assertEquals(17 * 60 + 45, result)
    }

    @Test
    fun predictedDeparture_withNegativeBalance() {
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = -30,
            date = thursday,
        )
        assertEquals(18 * 60 + 15, result)
    }

    @Test
    fun predictedDeparture_withPositiveBalance() {
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = 45,
            date = thursday,
        )
        assertEquals(17 * 60, result)
    }

    @Test
    fun predictedDeparture_bigPositiveBalance_noBreakNeeded() {
        // Нужно отработать 3 часа — до перерыва не дойдёт, ухожу в 12:00.
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = 300,
            date = thursday,
        )
        assertEquals(12 * 60, result)
    }

    @Test
    fun predictedDeparture_balanceCoversWholeDay() {
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = 600,
            date = thursday,
        )
        assertEquals(9 * 60, result)
    }

    @Test
    fun predictedDeparture_shiftsByExcludedMinutes() {
        val result = LeaveTimePredictor.predictedDepartureMinutes(
            arrivalMinutes = 9 * 60,
            balanceBeforeToday = 0,
            excludedMinutes = 30,
            date = thursday,
        )
        assertEquals(18 * 60 + 15, result)
    }

    @Test
    fun predictedDeparture_weekendIsNull() {
        assertNull(
            LeaveTimePredictor.predictedDepartureMinutes(
                arrivalMinutes = 9 * 60,
                balanceBeforeToday = 0,
                date = LocalDate.of(2026, 7, 11),
            ),
        )
    }
}
