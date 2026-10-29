package fr.clementvassant.standbyclock

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventsTest {
    private val paris = ZoneId.of("Europe/Paris")
    private val newYork = ZoneId.of("America/New_York")
    private val work = 1L
    private val family = 2L
    private val both = setOf(work, family)

    // Mercredi 21 octobre 2026, 14:15 à Paris.
    private val now = ZonedDateTime.of(2026, 10, 21, 14, 15, 0, 0, paris)
    private val today = now.toLocalDate()

    private fun millis(date: LocalDate, time: String, zone: ZoneId = paris) =
        LocalDateTime.of(date, LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()

    private fun timed(title: String, from: String, to: String, calendar: Long = work, day: LocalDate = today) =
        CalendarEvent(title, millis(day, from), millis(day, to), allDay = false, calendarId = calendar)

    // Android range les événements « toute la journée » de minuit à minuit en UTC.
    private fun allDay(title: String, first: LocalDate, days: Long = 1, calendar: Long = family) = CalendarEvent(
        title,
        first.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        first.plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        allDay = true,
        calendarId = calendar
    )

    @Test
    fun `all-day event belongs to its UTC day whatever the time zone`() {
        val birthday = allDay("Anniversaire de Léa", today)
        assertTrue(birthday.occursOn(today, paris))
        assertTrue(birthday.occursOn(today, newYork))
        assertFalse(birthday.occursOn(today.minusDays(1), paris))
        assertFalse(birthday.occursOn(today.plusDays(1), newYork))
    }

    @Test
    fun `multi-day all-day event covers each of its days`() {
        val holidays = allDay("Vacances", today.minusDays(2), days = 5)
        assertTrue(holidays.occursOn(today.minusDays(2), paris))
        assertTrue(holidays.occursOn(today, paris))
        assertTrue(holidays.occursOn(today.plusDays(2), paris))
        assertFalse(holidays.occursOn(today.plusDays(3), paris))
    }

    @Test
    fun `timed event crossing midnight shows on both days`() {
        val party = timed("Soirée", "23:00", "23:59").copy(end = millis(today.plusDays(1), "01:00"))
        assertTrue(party.occursOn(today, paris))
        assertTrue(party.occursOn(today.plusDays(1), paris))
        assertFalse(party.occursOn(today.plusDays(2), paris))
    }

    @Test
    fun `event ending at midnight does not spill on the next day`() {
        val evening = timed("Cinéma", "21:00", "23:00").copy(end = millis(today.plusDays(1), "00:00"))
        assertFalse(evening.occursOn(today.plusDays(1), paris))
    }

    @Test
    fun `zero-length reminder counts for its day`() {
        val reminder = timed("Rappel", "16:00", "16:00")
        assertTrue(reminder.occursOn(today, paris))
        assertEquals(listOf(reminder), todayAgenda(listOf(reminder), now, both))
    }

    @Test
    fun `agenda keeps today's unfinished events, all-day first then by time`() {
        val standup = timed("Point d'équipe", "09:30", "09:45")
        val meeting = timed("Revue de sprint", "14:00", "15:00")
        val dentist = timed("Dentiste", "17:30", "18:00", calendar = family)
        val lunch = timed("Déjeuner", "12:30", "13:30")
        val tomorrow = timed("Train pour Lyon", "08:00", "10:00", day = today.plusDays(1))
        val birthday = allDay("Anniversaire de Léa", today)
        val agenda = todayAgenda(listOf(dentist, standup, tomorrow, meeting, birthday, lunch), now, both)
        assertEquals(listOf(birthday, meeting, dentist), agenda)
    }

    @Test
    fun `only ticked calendars are shown, without declined invitations`() {
        val meeting = timed("Revue de sprint", "15:00", "16:00")
        val declined = timed("Comité", "16:00", "17:00").copy(declined = true)
        val dentist = timed("Dentiste", "17:30", "18:00", calendar = family)
        assertEquals(listOf(meeting), todayAgenda(listOf(meeting, declined, dentist), now, shownCalendars = setOf(work)))
    }

    @Test
    fun `no ticked calendar means no events`() {
        val meeting = timed("Revue de sprint", "15:00", "16:00")
        assertEquals(emptyList<CalendarEvent>(), todayAgenda(listOf(meeting), now, shownCalendars = emptySet()))
    }

    @Test
    fun `event time reads all day, end time when ongoing, or start time`() {
        assertEquals(AgendaTime.AllDay, agendaTime(allDay("Anniversaire de Léa", today), now, HourFormat.H24))
        assertEquals(AgendaTime.EndsAt("15:00"), agendaTime(timed("Revue de sprint", "14:00", "15:00"), now, HourFormat.H24))
        val nightShift = timed("Astreinte", "13:00", "23:00").copy(end = millis(today.plusDays(1), "08:00"))
        assertEquals(AgendaTime.Ongoing, agendaTime(nightShift, now, HourFormat.H24))
        assertEquals(AgendaTime.StartsAt("17:30"), agendaTime(timed("Dentiste", "17:30", "18:00"), now, HourFormat.H24))
        assertEquals(AgendaTime.StartsAt("09:05"), agendaTime(timed("Café", "09:05", "09:30"), now.withHour(8), HourFormat.H24))
    }

    @Test
    fun `event time in 12 h says AM or PM`() {
        assertEquals(AgendaTime.AllDay, agendaTime(allDay("Anniversaire de Léa", today), now, HourFormat.H12))
        assertEquals(AgendaTime.EndsAt("03:00 PM"), agendaTime(timed("Revue de sprint", "14:00", "15:00"), now, HourFormat.H12))
        assertEquals(AgendaTime.StartsAt("05:30 PM"), agendaTime(timed("Dentiste", "17:30", "18:00"), now, HourFormat.H12))
        assertEquals(AgendaTime.StartsAt("09:05 AM"), agendaTime(timed("Café", "09:05", "09:30"), now.withHour(8), HourFormat.H12))
    }

    @Test
    fun `too many events end with a count of the others`() {
        val events = (15..20).map { timed("Rendez-vous $it", "$it:00", "$it:30") }
        val lines = agendaLines(events)
        assertEquals(events.take(3), lines.shown)
        assertEquals(3, lines.more)
        assertEquals(MAX_AGENDA_LINES, lines.count)
    }

    @Test
    fun `events that fit are all shown`() {
        val events = (15..18).map { timed("Rendez-vous $it", "$it:00", "$it:30") }
        assertEquals(AgendaLines(events, 0), agendaLines(events))
        assertEquals(0, agendaLines(emptyList()).count)
    }

    @Test
    fun `calendar label names the account when it differs`() {
        assertEquals("Travail (clement@example.com)", CalendarInfo(1, "Travail", "clement@example.com").label())
        assertEquals("clement@example.com", CalendarInfo(2, "clement@example.com", "clement@example.com").label())
        assertEquals("Jours fériés", CalendarInfo(3, "Jours fériés", "").label())
    }

    @Test
    fun `ongoing and all-day events are highlighted, upcoming ones are not`() {
        assertTrue(isHappeningNow(timed("Revue de sprint", "14:00", "15:00"), now))
        assertTrue(isHappeningNow(allDay("Anniversaire de Léa", today), now))
        assertFalse(isHappeningNow(timed("Dentiste", "17:30", "18:00"), now))
    }
}
