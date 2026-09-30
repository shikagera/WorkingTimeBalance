package com.example.shikagera1.domain

/** Содержимое двух полей ввода времени: часы и минуты, только цифры. */
data class TimeFieldInput(
    val hours: String = "",
    val minutes: String = "",
) {
    val isEmpty: Boolean
        get() = hours.isEmpty() && minutes.isEmpty()

    /** Минуты от полуночи, если оба поля заполнены корректно. */
    fun toMinutesOfDay(): Int? = TimeParser.parseParts(hours, minutes)

    companion object {
        val EMPTY = TimeFieldInput()

        fun fromMinutesOfDay(minutes: Int): TimeFieldInput {
            val hours = (minutes / 60).coerceIn(0, 23)
            val mins = (minutes % 60).coerceIn(0, 59)
            return TimeFieldInput("%02d".format(hours), "%02d".format(mins))
        }
    }
}

object TimeParser {
    const val TIME_INPUT_HINT = "Введите часы и минуты, например 8 : 10"

    /**
     * Часы: до двух цифр, вторая цифра сверх 23 отбрасывается.
     * Набранная «2» сразу разворачивается в «20» — позже работа не засчитывается.
     * [previous] нужен, чтобы отличить набор от стирания: иначе из «20»
     * нельзя было бы удалить цифру, «2» снова становилась бы «20».
     */
    fun sanitizeHours(input: String, previous: String = ""): String {
        val digits = input.filter { it.isDigit() }.take(2)
        if (digits.length == 2 && digits.toInt() > 23) return digits.take(1)
        if (digits == "2" && previous.length <= digits.length) return "20"
        return digits
    }

    /**
     * Минуты: ровно две цифры. Первая цифра «6»…«9» не может быть десятками,
     * значит это единицы — подставляем ведущий ноль («7» → «07»).
     */
    fun sanitizeMinutes(input: String): String {
        val digits = input.filter { it.isDigit() }.take(2)
        if (digits.length == 1 && digits[0] >= '6') return "0$digits"
        return digits
    }

    /**
     * Часы набраны полностью — пора переходить к минутам.
     * Первая цифра «2»…«9» — это уже целый час (6 → 06:__);
     * вторую цифру ждём только после «0» и «1».
     */
    fun isHoursComplete(hours: String): Boolean {
        return hours.length == 2 || (hours.length == 1 && hours[0] >= '2')
    }

    fun isMinutesComplete(minutes: String): Boolean = minutes.length == 2

    fun parseParts(hoursPart: String, minutesPart: String): Int? {
        if (hoursPart.isEmpty() || minutesPart.length != 2) return null
        val hours = hoursPart.toIntOrNull() ?: return null
        val minutes = minutesPart.toIntOrNull() ?: return null
        return if (hours in 0..23 && minutes in 0..59) hours * 60 + minutes else null
    }

    fun formatMinutes(minutes: Int): String {
        val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val hours = normalized / 60
        val mins = normalized % 60
        return "%02d:%02d".format(hours, mins)
    }

    fun formatBalance(minutes: Int): String {
        val sign = if (minutes >= 0) "+" else "−"
        val absolute = kotlin.math.abs(minutes)
        val hours = absolute / 60
        val mins = absolute % 60
        return "$sign${hours}ч ${mins}м"
    }

    /** Formats a non-negative duration as H:MM:SS or HH:MM:SS. */
    fun formatDurationSeconds(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun formatDurationSeconds(totalSeconds: Int): String = formatDurationSeconds(totalSeconds.toLong())
}
