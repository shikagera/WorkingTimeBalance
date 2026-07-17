package com.example.shikagera1.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object WorkWeekCalculator {
    fun currentWeekMonday(today: LocalDate = LocalDate.now()): LocalDate {
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun currentWeekFriday(today: LocalDate = LocalDate.now()): LocalDate {
        return currentWeekMonday(today).plusDays(4)
    }

    fun previousWeekMonday(today: LocalDate = LocalDate.now()): LocalDate {
        return currentWeekMonday(today).minusWeeks(1)
    }

    /**
     * Keep previous calendar week for history UI, and the whole current period
     * so carry-over balance is not lost when early period days would otherwise expire.
     */
    fun retentionStartDate(today: LocalDate = LocalDate.now()): LocalDate {
        val previousMonday = previousWeekMonday(today)
        val periodStart = PeriodCalculator.currentPeriodStart(today)
        return minOf(previousMonday, periodStart)
    }

    fun isWorkday(date: LocalDate): Boolean {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> false
            else -> true
        }
    }

    fun visibleWorkDays(today: LocalDate = LocalDate.now()): List<LocalDate> {
        val monday = currentWeekMonday(today)
        val friday = currentWeekFriday(today)
        val lastVisible = when (today.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> friday
            else -> if (today.isAfter(friday)) friday else today
        }
        return generateSequence(monday) { it.plusDays(1) }
            .takeWhile { !it.isAfter(lastVisible) }
            .filter(::isWorkday)
            .toList()
    }

    fun previousWeekWorkDays(today: LocalDate = LocalDate.now()): List<LocalDate> {
        val monday = previousWeekMonday(today)
        return (0..4L).map { monday.plusDays(it) }
    }

    fun isInCurrentWorkWeek(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        val monday = currentWeekMonday(today)
        val friday = currentWeekFriday(today)
        return !date.isBefore(monday) && !date.isAfter(friday) && isWorkday(date)
    }

    fun isInPreviousWorkWeek(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        val monday = previousWeekMonday(today)
        val friday = monday.plusDays(4)
        return !date.isBefore(monday) && !date.isAfter(friday) && isWorkday(date)
    }
}