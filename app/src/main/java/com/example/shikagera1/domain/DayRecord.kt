package com.example.shikagera1.domain

import java.time.LocalDate

data class DayRecord(
    val date: LocalDate,
    val arrivalMinutes: Int? = null,
    val departureMinutes: Int? = null,
    val note: String = "",
    val excludedMinutes: Int = 0,
    /** «Зачёт»: день засчитан как полный рабочий независимо от времени. */
    val isCredited: Boolean = false,
) {
    val isClosed: Boolean
        get() = arrivalMinutes != null && departureMinutes != null

    /** День больше не требует действий: закрыт по времени или зачтён. */
    val isFinished: Boolean
        get() = isCredited || isClosed
}
