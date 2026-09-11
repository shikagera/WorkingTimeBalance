package com.example.shikagera1.domain

object WorkConstants {
    /** Чистая норма работы в день, без перерыва (8 часов). */
    const val WORK_NORM_MINUTES = 480
    /** Первая половина дня — после неё начинается перерыв (4 часа). */
    const val FIRST_HALF_MINUTES = 240
    /** Обеденный перерыв между половинами дня, в норму не входит. */
    const val BREAK_MINUTES = 45
    /** Day of month when the balance period resets (once per month). */
    const val PERIOD_START_DAY = 16
    const val RESET_WARNING_DAYS_BEFORE = 2
    /** Сколько месяцев истории хранить в базе. */
    const val RETENTION_MONTHS = 1L
}
