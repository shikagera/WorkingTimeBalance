package com.example.shikagera1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shikagera1.domain.DayRecord
import java.time.LocalDate

@Entity(tableName = "day_entries")
data class DayEntryEntity(
    @PrimaryKey val dateIso: String,
    val arrivalMinutes: Int? = null,
    val departureMinutes: Int? = null,
    val note: String = "",
    val excludedMinutes: Int = 0,
)

fun DayEntryEntity.toDomain(): DayRecord = DayRecord(
    date = LocalDate.parse(dateIso),
    arrivalMinutes = arrivalMinutes,
    departureMinutes = departureMinutes,
    note = note,
    excludedMinutes = excludedMinutes,
)

fun DayRecord.toEntity(): DayEntryEntity = DayEntryEntity(
    dateIso = date.toString(),
    arrivalMinutes = arrivalMinutes,
    departureMinutes = departureMinutes,
    note = note,
    excludedMinutes = excludedMinutes,
)