package fr.clementvassant.standbyclock.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutLogicTest {

    @Test
    fun `month grid starts on monday`() {
        // Octobre 2026 : le 1er est un jeudi, 31 jours, 5 semaines.
        assertEquals(MonthGrid(offset = 3, days = 31, weeks = 5), monthGrid(LocalDate.of(2026, 10, 20)))
        // Juin 2026 : le 1er est un lundi.
        assertEquals(MonthGrid(offset = 0, days = 30, weeks = 5), monthGrid(LocalDate.of(2026, 6, 15)))
        // Mars 2026 : le 1er est un dimanche, 31 jours, 6 semaines.
        assertEquals(MonthGrid(offset = 6, days = 31, weeks = 6), monthGrid(LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `month grid can start on sunday`() {
        // Octobre 2026 aux États-Unis : le jeudi 1er est en 5e colonne, toujours 5 semaines.
        assertEquals(MonthGrid(offset = 4, days = 31, weeks = 5), monthGrid(LocalDate.of(2026, 10, 20), DayOfWeek.SUNDAY))
        // Mars 2026 : le dimanche 1er ouvre la grille, qui tient alors en 5 semaines au lieu de 6.
        assertEquals(MonthGrid(offset = 0, days = 31, weeks = 5), monthGrid(LocalDate.of(2026, 3, 1), DayOfWeek.SUNDAY))
    }

    @Test
    fun `february in leap and common years`() {
        // Février 2027 : commence un lundi, 28 jours, tient en 4 semaines.
        assertEquals(MonthGrid(offset = 0, days = 28, weeks = 4), monthGrid(LocalDate.of(2027, 2, 10)))
        // Février 2028 (bissextile) : commence un mardi, 29 jours, 5 semaines.
        assertEquals(MonthGrid(offset = 1, days = 29, weeks = 5), monthGrid(LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun `only a screen taller than wide is portrait`() {
        assertTrue(isPortrait(width = 1080, height = 2520))
        assertFalse(isPortrait(width = 2520, height = 1080))
        // Écran carré : on garde la disposition côte à côte.
        assertFalse(isPortrait(width = 1000, height = 1000))
    }

    @Test
    fun `music goes under the clock while the calendar is shown, in the side pane otherwise`() {
        assertEquals(MediaPlacement.UNDER_CLOCK, mediaPlacementFor(hasMedia = true, showCalendar = true))
        // Calendrier masqué par l'appui long : la grande carte prend sa place.
        assertEquals(MediaPlacement.SIDE_CARD, mediaPlacementFor(hasMedia = true, showCalendar = false))
        assertEquals(MediaPlacement.NONE, mediaPlacementFor(hasMedia = false, showCalendar = true))
        assertEquals(MediaPlacement.NONE, mediaPlacementFor(hasMedia = false, showCalendar = false))
    }

    @Test
    fun `clock side alternates every hour`() {
        assertTrue(clockOnLeft(0))
        assertFalse(clockOnLeft(1))
        assertTrue(clockOnLeft(14))
        assertFalse(clockOnLeft(23))
    }

    @Test
    fun `anti burn-in orbit stays within 20 dp and always moves`() {
        val start = 29_000_000L
        var previous = orbitOffset(start - 1)
        for (m in start until start + 2 * 24 * 60) {
            val p = orbitOffset(m)
            assertTrue("minute $m: $p", hypot(p.x, p.y) <= 20.001f)
            assertNotEquals("minute $m", previous, p)
            previous = p
        }
    }

    @Test
    fun `ticker waits for next minute at night and next second by day`() {
        val minuteStart = 1_700_000_040_000L // multiple de 60 000
        val now = minuteStart + 12_345L
        assertEquals(47_655L, tickDelayMillis(everyMinute = true, nowMillis = now))
        assertEquals(655L, tickDelayMillis(everyMinute = false, nowMillis = now))
        assertEquals(60_000L, tickDelayMillis(everyMinute = true, nowMillis = minuteStart))
    }

    @Test
    fun `week runs from monday to sunday around the day`() {
        // Mardi 20 octobre 2026 : semaine du lundi 19 au dimanche 25, semaine ISO 43.
        val week = weekOf(LocalDate.of(2026, 10, 20))
        assertEquals(LocalDate.of(2026, 10, 19), week.first())
        assertEquals(LocalDate.of(2026, 10, 25), week.last())
        assertEquals(7, week.size)
        assertEquals(43, weekNumber(LocalDate.of(2026, 10, 20)))
        // Dimanche : il clôt sa semaine, il n'en ouvre pas une.
        assertEquals(LocalDate.of(2026, 10, 19), weekOf(LocalDate.of(2026, 10, 25)).first())
    }

    @Test
    fun `week can straddle two months and two years`() {
        // Jeudi 31 décembre 2026 : semaine du lundi 28 décembre au dimanche 3 janvier, semaine ISO 53.
        val week = weekOf(LocalDate.of(2026, 12, 31))
        assertEquals(LocalDate.of(2026, 12, 28), week.first())
        assertEquals(LocalDate.of(2027, 1, 3), week.last())
        assertEquals(53, weekNumber(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `week can run from sunday to saturday`() {
        // Mardi 20 octobre 2026 aux États-Unis : semaine du dimanche 18 au samedi 24, semaine 43 là-bas aussi.
        val week = weekOf(LocalDate.of(2026, 10, 20), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 10, 18), week.first())
        assertEquals(LocalDate.of(2026, 10, 24), week.last())
        assertEquals(43, weekNumber(LocalDate.of(2026, 10, 20), WeekFields.of(Locale.US)))
        // Dimanche : il ouvre sa semaine.
        assertEquals(LocalDate.of(2026, 10, 25), weekOf(LocalDate.of(2026, 10, 25), DayOfWeek.SUNDAY).first())
    }

    @Test
    fun `weekend is saturday and sunday`() {
        assertEquals(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), DayOfWeek.entries.filter { it.isWeekend() })
    }
}
