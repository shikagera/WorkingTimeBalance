package com.example.shikagera1.domain

object TimeParser {
    fun sanitizeDigits(input: String): String = input.filter { it.isDigit() }.take(4)

    fun isMorningHourPrefix(digits: String): Boolean {
        return digits.isNotEmpty() && digits.first() in '7'..'9'
    }

    fun formatInputDisplay(digits: String): String {
        val clean = sanitizeDigits(digits)
        return when (clean.length) {
            0 -> ""
            1 -> clean
            2 -> if (isMorningHourPrefix(clean)) "0${clean[0]}:${clean[1]}" else clean
            3 -> if (isMorningHourPrefix(clean)) {
                "0${clean[0]}:${clean.drop(1)}"
            } else {
                "${clean.take(2)}:${clean.drop(2)}"
            }
            else -> "${clean.take(2)}:${clean.drop(2).take(2)}"
        }
    }

    fun isValidTimeDigits(digits: String): Boolean {
        val clean = sanitizeDigits(digits)
        return parseCompact(clean) != null && (clean.length == 4 || (clean.length == 3 && isMorningHourPrefix(clean)))
    }

    fun parseCompact(input: String): Int? {
        val digits = sanitizeDigits(input)
        if (digits.isEmpty()) return null

        return when (digits.length) {
            3 -> if (isMorningHourPrefix(digits)) {
                parseHoursMinutes("0${digits[0]}", digits.substring(1))
            } else {
                null
            }
            4 -> parseHoursMinutes(digits.substring(0, 2), digits.substring(2, 4))
            else -> null
        }
    }

    private fun parseHoursMinutes(hoursPart: String, minutesPart: String): Int? {
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
    fun formatDurationSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }
}