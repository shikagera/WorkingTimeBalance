package com.example.shikagera1.data

import com.example.shikagera1.domain.DayRecord
import com.example.shikagera1.domain.WorkWeekCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TimeBalanceRepository(
    private val dao: DayEntryDao,
) {
    fun observeRetainedRecords(today: LocalDate): Flow<List<DayRecord>> {
        val rangeStart = WorkWeekCalculator.retentionStartDate(today)
        val rangeEnd = maxOf(WorkWeekCalculator.currentWeekFriday(today), today)

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

    suspend fun setCredited(date: LocalDate, credited: Boolean) {
        val existing = getDay(date) ?: DayRecord(date = date)
        saveDay(existing.copy(isCredited = credited))
    }

    /**
     * Удаляет записи старше срока хранения. Они всегда лежат до начала
     * текущего периода, поэтому на баланс не влияют.
     */
    suspend fun purgeExpiredRecords(today: LocalDate = LocalDate.now()) {
        val cutoff = WorkWeekCalculator.retentionStartDate(today)
        dao.deleteBefore(cutoff.toString())
    }
}
