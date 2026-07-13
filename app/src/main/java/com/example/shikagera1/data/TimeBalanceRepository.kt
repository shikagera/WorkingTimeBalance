package com.example.shikagera1.data

import com.example.shikagera1.domain.BalanceCalculator
import com.example.shikagera1.domain.DayRecord
import com.example.shikagera1.domain.WorkWeekCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TimeBalanceRepository(
    private val dao: DayEntryDao,
    private val preferences: UserPreferences,
) {
    fun observeRetainedRecords(today: LocalDate): Flow<List<DayRecord>> {
        val rangeStart = WorkWeekCalculator.retentionStartDate(today)
        val rangeEnd = maxOf(
            WorkWeekCalculator.currentWeekFriday(today),
            WorkWeekCalculator.previousWeekMonday(today).plusDays(4),
            today,
        )

        return dao.observeRange(rangeStart.toString(), rangeEnd.toString()).map { entries ->
            entries.map { it.toDomain() }
        }
    }

    suspend fun getDay(date: LocalDate): DayRecord? {
        return dao.getByDate(date.toString())?.toDomain()
    }

    suspend fun saveDay(record: DayRecord) {
        dao.upsert(record.toEntity())
    }

    suspend fun updateArrival(date: LocalDate, arrivalMinutes: Int) {
        val existing = getDay(date) ?: DayRecord(date = date)
        saveDay(existing.copy(arrivalMinutes = arrivalMinutes))
    }

    suspend fun updateDeparture(date: LocalDate, departureMinutes: Int) {
        val existing = getDay(date) ?: DayRecord(date = date)
        saveDay(existing.copy(departureMinutes = departureMinutes))
    }

    suspend fun updateExcludedMinutes(date: LocalDate, excludedMinutes: Int) {
        val existing = getDay(date) ?: DayRecord(date = date)
        saveDay(existing.copy(excludedMinutes = excludedMinutes.coerceAtLeast(0)))
    }

    suspend fun purgeExpiredRecords(today: LocalDate = LocalDate.now()) {
        val cutoff = WorkWeekCalculator.retentionStartDate(today)
        val expired = dao.getBefore(cutoff.toString())
        if (expired.isEmpty()) return

        val expiredBalance = expired
            .map { it.toDomain() }
            .sumOf(BalanceCalculator::dailyBalance)
        preferences.addToAccumulatedBalance(expiredBalance)
        dao.deleteBefore(cutoff.toString())
    }
}