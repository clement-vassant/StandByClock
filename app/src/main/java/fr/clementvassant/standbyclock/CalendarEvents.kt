package fr.clementvassant.standbyclock

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Un agenda de l'appareil (Google, Samsung, Exchange...), tel que proposé dans les réglages. */
data class CalendarInfo(val id: Long, val name: String, val account: String)

/**
 * Une occurrence d'événement, telle que lue dans l'agenda d'Android.
 * [begin] et [end] sont en ms depuis l'epoch ; pour un événement « toute la journée », Android les stocke
 * de minuit à minuit en UTC, quel que soit le fuseau de l'appareil.
 */
data class CalendarEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val calendarId: Long,
    val declined: Boolean = false
)

/** Nombre de lignes au plus dans la liste du jour : elle doit rester lisible sur un petit écran. */
const val MAX_AGENDA_LINES = 4

/** Vrai si l'événement occupe une partie du jour [date] dans le fuseau [zone]. */
fun CalendarEvent.occursOn(date: LocalDate, zone: ZoneId): Boolean {
    if (allDay) {
        // Jours UTC de [begin, end[ : un événement d'un jour va de minuit à minuit le lendemain.
        val first = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
        val endExclusive = maxOf(Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate(), first.plusDays(1))
        return !date.isBefore(first) && date.isBefore(endExclusive)
    }
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    // Un événement sans durée (rappel à 14:00) compte pour le jour où il commence.
    return begin < dayEnd && (end > dayStart || begin >= dayStart)
}

/**
 * Les événements à afficher à l'instant [now] : ceux du jour, pas encore terminés, des agendas cochés,
 * sans les invitations refusées. Toute la journée d'abord, puis par heure de début.
 */
fun todayAgenda(events: List<CalendarEvent>, now: ZonedDateTime, shownCalendars: Set<Long>): List<CalendarEvent> {
    val nowMillis = now.toInstant().toEpochMilli()
    return events
        .filter { it.calendarId in shownCalendars && !it.declined }
        .filter { it.occursOn(now.toLocalDate(), now.zone) }
        .filter { it.allDay || it.end > nowMillis || it.begin >= nowMillis }
        .sortedWith(compareBy({ !it.allDay }, { it.begin }, { it.title }))
}

/**
 * Ce qui tient dans la liste : tout si ça rentre dans [maxLines] lignes, sinon les premiers
 * événements et le nombre de ceux qui restent (dernière ligne « +N »).
 */
data class AgendaLines(val shown: List<CalendarEvent>, val more: Int) {
    val count get() = shown.size + if (more > 0) 1 else 0
}

fun agendaLines(agenda: List<CalendarEvent>, maxLines: Int = MAX_AGENDA_LINES): AgendaLines = if (agenda.size <= maxLines) {
    AgendaLines(agenda, 0)
} else {
    AgendaLines(agenda.take(maxLines - 1), agenda.size - (maxLines - 1))
}

/**
 * Heure affichée devant le titre : toute la journée, l'heure de fin d'un événement en cours (« → 15:00 »,
 * seule info utile pendant l'événement ; « en cours » s'il finit un autre jour), sinon l'heure de début.
 * Le libellé lui-même vient des ressources, dans la langue de l'appareil.
 */
sealed interface AgendaTime {
    data object AllDay : AgendaTime

    data class StartsAt(val time: String) : AgendaTime

    data class EndsAt(val time: String) : AgendaTime

    data object Ongoing : AgendaTime
}

fun agendaTime(event: CalendarEvent, now: ZonedDateTime): AgendaTime {
    val nowMillis = now.toInstant().toEpochMilli()
    val end = Instant.ofEpochMilli(event.end).atZone(now.zone)
    return when {
        event.allDay -> AgendaTime.AllDay
        event.begin > nowMillis -> AgendaTime.StartsAt(Instant.ofEpochMilli(event.begin).atZone(now.zone).hhmm())
        end.toLocalDate() == now.toLocalDate() -> AgendaTime.EndsAt(end.hhmm())
        else -> AgendaTime.Ongoing
    }
}

/** Événement en cours (ou de toute la journée) : son heure est en accent. */
fun isHappeningNow(event: CalendarEvent, now: ZonedDateTime): Boolean = event.allDay || event.begin <= now.toInstant().toEpochMilli()

private fun ZonedDateTime.hhmm() = "%02d:%02d".format(hour, minute)

/** Libellé d'un agenda dans les réglages : son nom, et le compte s'il est différent. */
fun CalendarInfo.label(): String = if (account.isBlank() || account == name) name else "$name ($account)"
