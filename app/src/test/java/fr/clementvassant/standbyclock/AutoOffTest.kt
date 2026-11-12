package fr.clementvassant.standbyclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val LAUNCHED_AT = 5_000L
private const val TWO_HOURS = 120

class AutoOffTest {
    @Test
    fun `zero minutes means never`() {
        assertNull(autoOffDelayMillis(LAUNCHED_AT, 0, LAUNCHED_AT))
    }

    @Test
    fun `a negative setting is treated as never`() {
        assertNull(autoOffDelayMillis(LAUNCHED_AT, -1, LAUNCHED_AT))
    }

    @Test
    fun `right after launch the whole delay remains`() {
        assertEquals(TWO_HOURS * MILLIS_PER_MINUTE, autoOffDelayMillis(LAUNCHED_AT, TWO_HOURS, LAUNCHED_AT))
    }

    @Test
    fun `time already spent on the clock is deducted after a resume`() {
        val now = LAUNCHED_AT + 30 * MILLIS_PER_MINUTE
        assertEquals(90 * MILLIS_PER_MINUTE, autoOffDelayMillis(LAUNCHED_AT, TWO_HOURS, now))
    }

    @Test
    fun `a delay that has run out turns off at once`() {
        val now = LAUNCHED_AT + 3 * MINUTES_PER_HOUR * MILLIS_PER_MINUTE
        assertEquals(0L, autoOffDelayMillis(LAUNCHED_AT, TWO_HOURS, now))
    }

    @Test
    fun `a shorter setting chosen meanwhile counts from the launch`() {
        val now = LAUNCHED_AT + 10 * MILLIS_PER_MINUTE
        assertEquals(5 * MILLIS_PER_MINUTE, autoOffDelayMillis(LAUNCHED_AT, 15, now))
    }
}
