package com.nikhilbhatt.rang

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryTest {
    @Test fun zones() {
        assertEquals(Zone.GREEN, Battery.zone(100))
        assertEquals(Zone.GREEN, Battery.zone(60))
        assertEquals(Zone.YELLOW, Battery.zone(59))
        assertEquals(Zone.YELLOW, Battery.zone(35))
        assertEquals(Zone.ORANGE, Battery.zone(34))
        assertEquals(Zone.ORANGE, Battery.zone(15))
        assertEquals(Zone.RED, Battery.zone(14))
        assertEquals(Zone.RED, Battery.zone(0))
    }

    @Test fun lowAlerts() {
        assertEquals(emptySet<Int>(), Battery.newlyCrossed(15, false, emptySet()))
        assertEquals(setOf(15), Battery.newlyCrossed(14, false, emptySet()))
        assertEquals(emptySet<Int>(), Battery.newlyCrossed(12, false, setOf(15)))
        assertEquals(setOf(10), Battery.newlyCrossed(9, false, setOf(15)))
        // Slept from 20% to 4%: every threshold at once, so the caller alerts a single time.
        assertEquals(setOf(15, 10, 5), Battery.newlyCrossed(4, false, emptySet()))
        // Never while plugged in.
        assertEquals(emptySet<Int>(), Battery.newlyCrossed(4, true, emptySet()))
    }
}
