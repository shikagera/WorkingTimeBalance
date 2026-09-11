package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeParserTest {
    @Test
    fun sanitizeHours_keepsUpToTwoDigits() {
        assertEquals("", TimeParser.sanitizeHours(""))
        assertEquals("0", TimeParser.sanitizeHours("0"))
        assertEquals("09", TimeParser.sanitizeHours("09"))
        assertEquals("17", TimeParser.sanitizeHours("17"))
        assertEquals("23", TimeParser.sanitizeHours("23"))
        assertEquals("17", TimeParser.sanitizeHours("17x5"))
    }

    @Test
    fun sanitizeHours_dropsSecondDigitAbove23() {
        assertEquals("2", TimeParser.sanitizeHours("24"))
        assertEquals("8", TimeParser.sanitizeHours("81"))
    }

    @Test
    fun sanitizeMinutes_prefixesZeroForSixToNine() {
        assertEquals("07", TimeParser.sanitizeMinutes("7"))
        assertEquals("5", TimeParser.sanitizeMinutes("5"))
        assertEquals("45", TimeParser.sanitizeMinutes("45"))
        assertEquals("45", TimeParser.sanitizeMinutes("4567"))
    }

    @Test
    fun isHoursComplete_singleDigitTwoToNine() {
        assertTrue(TimeParser.isHoursComplete("8"))
        assertTrue(TimeParser.isHoursComplete("2"))
        assertTrue(TimeParser.isHoursComplete("6"))
        assertTrue(TimeParser.isHoursComplete("09"))
        assertTrue(TimeParser.isHoursComplete("17"))
        assertFalse(TimeParser.isHoursComplete("1"))
        assertFalse(TimeParser.isHoursComplete("0"))
        assertFalse(TimeParser.isHoursComplete(""))
    }

    @Test
    fun parseParts_validAndInvalid() {
        assertEquals(8 * 60 + 10, TimeParser.parseParts("8", "10"))
        assertEquals(8 * 60 + 10, TimeParser.parseParts("08", "10"))
        assertEquals(17 * 60 + 45, TimeParser.parseParts("17", "45"))
        assertEquals(0, TimeParser.parseParts("0", "00"))
        assertNull(TimeParser.parseParts("", "10"))
        assertNull(TimeParser.parseParts("8", ""))
        assertNull(TimeParser.parseParts("8", "5"))
        assertNull(TimeParser.parseParts("24", "00"))
        assertNull(TimeParser.parseParts("12", "60"))
    }

    @Test
    fun timeFieldInput_roundTrip() {
        val input = TimeFieldInput.fromMinutesOfDay(9 * 60 + 5)
        assertEquals("09", input.hours)
        assertEquals("05", input.minutes)
        assertEquals(9 * 60 + 5, input.toMinutesOfDay())
        assertTrue(TimeFieldInput.EMPTY.isEmpty)
        assertNull(TimeFieldInput.EMPTY.toMinutesOfDay())
    }

    @Test
    fun formatMinutes() {
        assertEquals("08:10", TimeParser.formatMinutes(8 * 60 + 10))
        assertEquals("00:00", TimeParser.formatMinutes(24 * 60))
    }

    @Test
    fun formatBalance_positive() {
        assertEquals("+1ч 15м", TimeParser.formatBalance(75))
    }

    @Test
    fun formatBalance_negative() {
        assertEquals("−0ч 30м", TimeParser.formatBalance(-30))
    }

    @Test
    fun formatDurationSeconds() {
        assertEquals("0:00:00", TimeParser.formatDurationSeconds(0))
        assertEquals("0:01:05", TimeParser.formatDurationSeconds(65))
        assertEquals("2:30:15", TimeParser.formatDurationSeconds(2 * 3600 + 30 * 60 + 15))
        assertEquals("0:00:00", TimeParser.formatDurationSeconds(-10))
        assertEquals("10:00:00", TimeParser.formatDurationSeconds(36_000L))
    }
}
