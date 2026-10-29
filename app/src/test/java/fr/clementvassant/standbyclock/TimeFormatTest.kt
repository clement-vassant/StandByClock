package fr.clementvassant.standbyclock

import fr.clementvassant.standbyclock.HourFormat.H12
import fr.clementvassant.standbyclock.HourFormat.H24
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `big clock in 24 h keeps two digits`() {
        assertEquals("00:00", clockTime(0, 0, H24))
        assertEquals("09:05", clockTime(9, 5, H24))
        assertEquals("14:15", clockTime(14, 15, H24))
        assertEquals("23:59", clockTime(23, 59, H24))
    }

    @Test
    fun `big clock in 12 h keeps two digits and has no AM PM`() {
        assertEquals("12:00", clockTime(0, 0, H12))
        assertEquals("09:05", clockTime(9, 5, H12))
        assertEquals("12:30", clockTime(12, 30, H12))
        assertEquals("02:15", clockTime(14, 15, H12))
        assertEquals("11:59", clockTime(23, 59, H12))
    }

    @Test
    fun `hour digits alone follow the same writing`() {
        assertEquals("08", clockHour(8, H24))
        assertEquals("20", clockHour(20, H24))
        assertEquals("08", clockHour(8, H12))
        assertEquals("08", clockHour(20, H12))
        assertEquals("12", clockHour(0, H12))
    }

    @Test
    fun `schedule times in 12 h say morning or afternoon`() {
        assertEquals("12:00 AM", scheduleTime(0, 0, H12))
        assertEquals("11:59 AM", scheduleTime(11, 59, H12))
        assertEquals("12:00 PM", scheduleTime(12, 0, H12))
        assertEquals("05:30 PM", scheduleTime(17, 30, H12))
    }

    @Test
    fun `schedule times in 24 h are unchanged`() {
        assertEquals("09:05", scheduleTime(9, 5, H24))
        assertEquals("17:30", scheduleTime(17, 30, H24))
    }

    @Test
    fun `night range bounds read in minutes since midnight`() {
        assertEquals("22:00", scheduleTime(22 * 60, H24))
        assertEquals("07:00", scheduleTime(7 * 60, H24))
        assertEquals("10:00 PM", scheduleTime(22 * 60, H12))
        assertEquals("07:00 AM", scheduleTime(7 * 60, H12))
    }
}
