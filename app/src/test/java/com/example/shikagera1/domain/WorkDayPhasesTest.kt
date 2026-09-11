package com.example.shikagera1.domain

import com.example.shikagera1.domain.WorkDayPhases.Phase
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkDayPhasesTest {
    @Test
    fun phaseMinutes_boundaries() {
        assertEquals(Phase.FIRST_HALF, WorkDayPhases.phaseMinutes(0))
        assertEquals(Phase.FIRST_HALF, WorkDayPhases.phaseMinutes(239))
        assertEquals(Phase.BREAK, WorkDayPhases.phaseMinutes(240))
        assertEquals(Phase.BREAK, WorkDayPhases.phaseMinutes(284))
        assertEquals(Phase.SECOND_HALF, WorkDayPhases.phaseMinutes(285))
        assertEquals(Phase.SECOND_HALF, WorkDayPhases.phaseMinutes(600))
    }

    @Test
    fun workedMinutes_deductsBreakOnlyAfterReachingIt() {
        assertEquals(0, WorkDayPhases.workedMinutes(0))
        assertEquals(180, WorkDayPhases.workedMinutes(180))
        assertEquals(240, WorkDayPhases.workedMinutes(240))
        assertEquals(240, WorkDayPhases.workedMinutes(260))
        assertEquals(240, WorkDayPhases.workedMinutes(285))
        assertEquals(480, WorkDayPhases.workedMinutes(525))
        assertEquals(540, WorkDayPhases.workedMinutes(585))
        assertEquals(0, WorkDayPhases.workedMinutes(-10))
    }

    @Test
    fun presenceForWorkMinutes_addsBreakWhenBeyondFirstHalf() {
        assertEquals(0, WorkDayPhases.presenceForWorkMinutes(0))
        assertEquals(180, WorkDayPhases.presenceForWorkMinutes(180))
        assertEquals(240, WorkDayPhases.presenceForWorkMinutes(240))
        assertEquals(286, WorkDayPhases.presenceForWorkMinutes(241))
        assertEquals(525, WorkDayPhases.presenceForWorkMinutes(480))
        assertEquals(0, WorkDayPhases.presenceForWorkMinutes(-30))
    }

    @Test
    fun workedAndPresenceAreInverseOutsideBreak() {
        for (work in listOf(0, 60, 240, 241, 300, 480, 600)) {
            val presence = WorkDayPhases.presenceForWorkMinutes(work)
            assertEquals("work=$work", work, WorkDayPhases.workedMinutes(presence))
        }
    }

    @Test
    fun seconds_variantsMatchMinutes() {
        val presenceSec = 260 * 60L
        assertEquals(Phase.BREAK, WorkDayPhases.phaseSeconds(presenceSec))
        assertEquals(240 * 60L, WorkDayPhases.workedSeconds(presenceSec))
        assertEquals(25 * 60L, WorkDayPhases.secondsLeftInPhase(presenceSec))

        assertEquals(240 * 60L, WorkDayPhases.secondsLeftInPhase(0))
        assertEquals(0L, WorkDayPhases.secondsLeftInPhase(300 * 60L))
    }
}
