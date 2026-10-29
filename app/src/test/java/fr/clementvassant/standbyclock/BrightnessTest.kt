package fr.clementvassant.standbyclock

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessTest {
    @Test
    fun `the app is at the minimum at night`() {
        assertEquals(NIGHT_BRIGHTNESS, windowBrightness(night = true, ClockSurface.APP))
    }

    @Test
    fun `the app is capped during the day`() {
        assertEquals(DAY_MAX_BRIGHTNESS, windowBrightness(night = false, ClockSurface.APP))
    }

    @Test
    fun `the screen saver is at the minimum at night too`() {
        assertEquals(NIGHT_BRIGHTNESS, windowBrightness(night = true, ClockSurface.SCREEN_SAVER))
    }

    @Test
    fun `the screen saver leaves the day brightness to the system`() {
        assertEquals(SYSTEM_BRIGHTNESS, windowBrightness(night = false, ClockSurface.SCREEN_SAVER))
    }

    @Test
    fun `the system brightness means no override`() {
        assertEquals(-1f, SYSTEM_BRIGHTNESS)
    }
}
