package fr.clementvassant.standbyclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import fr.clementvassant.standbyclock.AgendaLines
import fr.clementvassant.standbyclock.CalendarEvent
import fr.clementvassant.standbyclock.DAYS_PER_WEEK
import fr.clementvassant.standbyclock.HourFormat
import fr.clementvassant.standbyclock.MAX_AGENDA_LINES
import fr.clementvassant.standbyclock.agendaTime
import fr.clementvassant.standbyclock.isHappeningNow
import fr.clementvassant.standbyclock.moreEventsLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Hauteur du bloc en lignes de liste : en-tête, semaine (libellés + jours), séparateur, puis
 * [MAX_AGENDA_LINES] lignes d'événements.
 */
private const val AGENDA_ROWS = 1f + 1.8f + 0.6f + MAX_AGENDA_LINES

/** Largeur de la colonne des heures en 24 h (« 14:30 », « Journée », « → 15:00 »), en lignes de liste. */
private const val AGENDA_TIME_WIDTH_24H = 2.4f

/** En 12 h, la colonne fait place à AM/PM (« → 03:00 PM »). */
private const val AGENDA_TIME_WIDTH_12H = 3.4f

/** Largeur de la colonne des heures selon l'écriture des heures, en lignes de liste. */
fun agendaTimeWidth(hourFormat: HourFormat): Float = when (hourFormat) {
    HourFormat.H24 -> AGENDA_TIME_WIDTH_24H
    HourFormat.H12 -> AGENDA_TIME_WIDTH_12H
}

/** Les sept jours (lundi à dimanche) de la semaine de [date]. */
fun weekOf(date: LocalDate): List<LocalDate> {
    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0 until DAYS_PER_WEEK).map { monday.plusDays(it.toLong()) }
}

/** Numéro de semaine ISO (celui des agendas en France). */
fun isoWeekNumber(date: LocalDate): Int = date.get(WeekFields.ISO.weekOfWeekBasedYear())

/**
 * Les jours où il reste un événement : la semaine en cours sur une ligne, puis les événements du jour.
 * Texte seulement, aujourd'hui entouré d'un anneau, sans fond : peu de pixels allumés (OLED).
 */
@Composable
fun WeekAgenda(now: ZonedDateTime, lines: AgendaLines, colors: ClockColors, hourFormat: HourFormat, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val line = min(maxWidth / DAYS_PER_WEEK, maxHeight / AGENDA_ROWS)
        val text = (line * 0.42f).asSp()
        Column(Modifier.fillMaxWidth().testTag(TAG_AGENDA)) {
            WeekHeader(now, colors, line)
            WeekRow(now.toLocalDate(), colors, line)
            Box(Modifier.fillMaxWidth().height(line * 0.6f), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.secondary.copy(alpha = 0.4f)))
            }
            for (event in lines.shown) {
                AgendaLine(event, now, hourFormat, colors, line, text)
            }
            if (lines.more > 0) {
                Row(Modifier.fillMaxWidth().height(line), verticalAlignment = Alignment.CenterVertically) {
                    Text(moreEventsLabel(lines.more), color = colors.secondary, fontSize = text, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(now: ZonedDateTime, colors: ClockColors, line: Dp) {
    Row(
        Modifier.fillMaxWidth().height(line),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            now.format(DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH)).uppercase(),
            color = colors.accent,
            fontSize = (line * 0.5f).asSp(),
            fontWeight = FontWeight.SemiBold
        )
        Text("semaine ${isoWeekNumber(now.toLocalDate())}", color = colors.secondary, fontSize = (line * 0.38f).asSp())
    }
}

@Composable
private fun WeekRow(today: LocalDate, colors: ClockColors, line: Dp) {
    Row(Modifier.fillMaxWidth().height(line * 1.8f)) {
        for ((index, day) in weekOf(today).withIndex()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(line * 0.7f), contentAlignment = Alignment.Center) {
                    Text(WEEK_LABELS[index], color = colors.secondary, fontSize = (line * 0.34f).asSp(), fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.size(line), contentAlignment = Alignment.Center) {
                    val isToday = day == today
                    // Anneau plutôt que disque plein : moins de pixels allumés.
                    if (isToday) Box(Modifier.size(line * 0.9f).border(2.dp, colors.accent, CircleShape))
                    Text(
                        day.dayOfMonth.toString(),
                        color = when {
                            isToday -> colors.accent
                            day.dayOfWeek.value > DayOfWeek.FRIDAY.value -> colors.secondary
                            else -> colors.primary
                        },
                        fontSize = (line * 0.42f).asSp(),
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/** Une ligne : l'heure en accent si l'événement est en cours ou dure toute la journée, puis le titre. */
@Composable
private fun AgendaLine(event: CalendarEvent, now: ZonedDateTime, hourFormat: HourFormat, colors: ClockColors, line: Dp, text: TextUnit) {
    val current = isHappeningNow(event, now)
    Row(Modifier.fillMaxWidth().height(line), verticalAlignment = Alignment.CenterVertically) {
        Text(
            agendaTime(event, now, hourFormat),
            Modifier.width(line * agendaTimeWidth(hourFormat)),
            color = if (current) colors.accent else colors.secondary,
            fontSize = text,
            maxLines = 1
        )
        Text(
            event.title.ifBlank { "(Sans titre)" },
            color = colors.primary,
            fontSize = text,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val WEEK_LABELS = listOf("L", "M", "M", "J", "V", "S", "D")
