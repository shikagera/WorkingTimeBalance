package com.example.shikagera1.domain

import java.time.LocalDate

data class DayRecord(
    val date: LocalDate,
    val arrivalMinutes: Int? = null,
    val departureMinutes: Int? = null,
    val note: String = "",
    val excludedMinutes: Int = 0,
) {
    val isClosed: Boolean
        get() = arrivalMinutes != null && departureMinutes != null
}