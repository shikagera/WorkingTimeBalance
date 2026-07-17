package com.example.shikagera1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeParserTest {
    @Test
    fun formatInputDisplay_fourDigits() {
        assertEquals("14:44", TimeParser.formatInputDisplay("1444"))
    }

    @Test
    fun formatInputDisplay_morningThreeDigits() {
        assertEquals("08:10", TimeParser.formatInputDisplay("810"))
        assertEquals("09:45", TimeParser.formatInputDisplay("945"))
    }

    @Test
    fun formatInputDisplay_morningTwoDigits() {
        assertEquals("08:1", TimeParser.formatInputDisplay("81"))
    }

    @Test
    fun formatInputDisplay_partialAfternoon() {
        assertEquals("14:4", TimeParser.formatInputDisplay("144"))
    }

    @Test
    fun parseCompact_fourDigits() {
        assertEquals(17 * 60 + 45, TimeParser.parseCompact("1745"))
        assertEquals(14 * 60 + 44, TimeParser.parseCompact("1444"))
    }

    @Test
    fun parseCompact_morningThreeDigits() {
        assertEquals(8 * 60 + 10, TimeParser.parseCompact("810"))
        assertEquals(9 * 60 + 11, TimeParser.parseCompact("911"))
        assertEquals(7 * 60 + 30, TimeParser.parseCompact("730"))
    }

    @Test
    fun parseCompact_threeDigitsNonMorning() {
        assertNull(TimeParser.parseCompact("630"))
    }

    @Test
    fun parseCompact_invalid() {
        assertNull(TimeParser.parseCompact("9999"))
        assertNull(TimeParser.parseCompact("2460"))
        assertNull(TimeParser.parseCompact("895"))
    }

    @Test
    fun isValidTimeDigits() {
        assertTrue(TimeParser.isValidTimeDigits("0900"))
        assertTrue(TimeParser.isValidTimeDigits("810"))
        assertFalse(TimeParser.isValidTimeDigits("9999"))
        assertFalse(TimeParser.isValidTimeDigits("630"))
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
    }
}