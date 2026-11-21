package com.mindpause.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EscalationPolicyTest {

    @Test fun intervalIsClampedTo3to10() {
        assertEquals(3 * 60_000L, EscalationPolicy.firstDelayMs(1))
        assertEquals(7 * 60_000L, EscalationPolicy.firstDelayMs(7))
        assertEquals(10 * 60_000L, EscalationPolicy.firstDelayMs(25))
    }

    @Test fun tierRisesThenRepeatsAtFour() {
        assertEquals(1, EscalationPolicy.tierFor(1))
        assertEquals(2, EscalationPolicy.tierFor(2))
        assertEquals(3, EscalationPolicy.tierFor(3))
        assertEquals(4, EscalationPolicy.tierFor(4))
        assertEquals(4, EscalationPolicy.tierFor(9))
    }

    @Test fun elapsedMinutesRoundsToNearest() {
        val start = 1_000_000L
        assertEquals(3, EscalationPolicy.elapsedMinutes(start, start + 3 * 60_000L))
        assertEquals(8, EscalationPolicy.elapsedMinutes(start, start + 8 * 60_000L + 10_000L))
        assertEquals(1, EscalationPolicy.elapsedMinutes(start, start))
    }

    @Test fun messagesUseRealMinutes() {
        assertEquals("You've been here 3 minutes.", EscalationPolicy.message(1, 3).time)
        assertEquals("Still here after 13 minutes.", EscalationPolicy.message(3, 13).time)
        assertEquals("What would feel better than this right now?", EscalationPolicy.message(7, 23).question)
    }

    @Test fun dailyLineGrammar() {
        assertEquals("No check-ins yet today.", DailyLine.text(0, 0))
        assertEquals("Today you had 1 check-in. You chose to stop 1 time.", DailyLine.text(1, 1))
        assertEquals("Today you had 6 check-ins. You chose to stop 4 times.", DailyLine.text(6, 4))
    }
}
