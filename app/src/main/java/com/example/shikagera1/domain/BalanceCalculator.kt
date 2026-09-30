package com.example.shikagera1.domain

import java.time.DayOfWeek
import java.time.LocalDate

object BalanceCalculator {
    fun dailyNormMinutes(date: LocalDate): Int {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 0
            else -> WorkConstants.WORK_NORM_MINUTES
        }
    }

    /**
     * Присутствие: от прихода до ухода минус исключённые минуты.
     * Уход позже [WorkConstants.MAX_COUNTED_DEPARTURE_MINUTES] считается как уход в 20:00.
     */
    fun presenceMinutes(record: DayRecord): Int {
        val arrival = record.arrivalMinutes ?: return 0
        val departure = record.departureMinutes ?: return 0
        val counted = countedDepartureMinutes(departure)
        if (counted < arrival) return 0
        return (counted - arrival - record.excludedMinutes).coerceAtLeast(0)
    }

    /** Время ухода, обрезанное по потолку засчитываемого дня. */
    fun countedDepartureMinutes(departureMinutes: Int): Int {
        return departureMinutes.coerceAtMost(WorkConstants.MAX_COUNTED_DEPARTURE_MINUTES)
    }

    /** Чистая работа: присутствие без обеденного перерыва (см. [WorkDayPhases]). */
    fun workedMinutes(record: DayRecord): Int {
        return WorkDayPhases.workedMinutes(presenceMinutes(record))
    }

    /**
     * Day balance only after the day is closed (both arrival and departure).
     * An open day (only "пришёл") does not subtract the daily norm from the total.
     * A credited day («зачёт») always counts as exactly the norm.
     */
    fun dailyBalance(record: DayRecord): Int {
        if (record.isCredited) return 0
        if (!record.isClosed) return 0
        return workedMinutes(record) - dailyNormMinutes(record.date)
    }

    fun isAfterManualReset(recordDate: LocalDate, manualResetDate: LocalDate?): Boolean {
        return manualResetDate == null || recordDate.isAfter(manualResetDate)
    }

    /**
     * Balance of all days from the period start (16th) through [today].
     */
    fun periodBalance(
        records: List<DayRecord>,
        today: LocalDate = LocalDate.now(),
        manualResetDate: LocalDate? = null,
    ): Int {
        return balanceBeforeDate(records, today.plusDays(1), today, manualResetDate)
    }

    /** Balance of period days strictly before [date]. */
    fun balanceBeforeDate(
        records: List<DayRecord>,
        date: LocalDate,
        today: LocalDate = LocalDate.now(),
        manualResetDate: LocalDate? = null,
    ): Int {
        val periodStart = PeriodCalculator.currentPeriodStart(today)
        return records
            .filter { record ->
                record.date.isBefore(date) &&
                    !record.date.isBefore(periodStart) &&
                    isAfterManualReset(record.date, manualResetDate)
            }
            .sumOf(::dailyBalance)
    }
}
