package fr.clementvassant.standbyclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import fr.clementvassant.standbyclock.DAYS_PER_WEEK
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Hauteur du calendrier en cellules : en-tête du mois, jours de la semaine et 6 semaines au plus. */
private const val CALENDAR_ROWS = 8.5f

private fun ZonedDateTime.hhmm() = "%02d:%02d".format(hour, minute)

/** Convertit une taille en dp en sp, pour que les chiffres suivent la place disponible. */
@Composable
internal fun Dp.asSp(): TextUnit = with(LocalDensity.current) { toSp() }

/** Style 1 : grands chiffres façon StandBy, heures en clair, minutes en accent. */
@Composable
fun DigitalClock(now: ZonedDateTime, colors: ClockColors, showSeconds: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val digits = min(maxHeight * 0.55f, maxWidth * 0.27f).asSp()
        val small = min(maxHeight * 0.08f, maxWidth * 0.045f).asSp()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%02d".format(now.hour), color = colors.primary, fontSize = digits, fontWeight = FontWeight.Bold)
                Text(
                    ":",
                    color = if (!showSeconds || now.second % 2 == 0) colors.secondary else colors.secondary.copy(alpha = 0.3f),
                    fontSize = digits,
                    fontWeight = FontWeight.Bold
                )
                Text("%02d".format(now.minute), color = colors.accent, fontSize = digits, fontWeight = FontWeight.Bold)
            }
            Text(now.format(fullDateFormat()).replaceFirstChar { it.uppercase() }, color = colors.secondary, fontSize = small)
        }
    }
}

/**
 * Style 2 : cadran analogique, avec la date en grand à droite si le calendrier est masqué.
 *
 * @param matchCalendar calendrier à côté : le cadran prend sa hauteur. Faux en portrait, où le calendrier
 * est dessous : le cadran remplit alors son panneau.
 * @param calendarHeight hauteur du calendrier, en fraction de celle de l'horloge.
 */
@Composable
fun AnalogClockWithDate(
    now: ZonedDateTime,
    colors: ClockColors,
    showDate: Boolean,
    showSeconds: Boolean,
    modifier: Modifier = Modifier,
    matchCalendar: Boolean = true,
    calendarHeight: Float = 1f
) {
    Row(
        modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (showDate) {
            AnalogDial(now, colors, showSeconds, Modifier.fillMaxHeight(0.8f).aspectRatio(1f))
        } else {
            // Calendrier à côté : le cadran ne dépasse pas sa hauteur (mêmes calculs que MonthCalendar,
            // dont la colonne fait 1/1.4 de celle de l'horloge) : jours de la semaine + semaines du mois.
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cell = min(maxWidth / CLOCK_PANE_WEIGHT / DAYS_PER_WEEK, maxHeight * calendarHeight / CALENDAR_ROWS)
                val rows = 0.8f + monthGrid(now.toLocalDate(), weekFields().firstDayOfWeek).weeks
                val paneSize = min(maxWidth, maxHeight) * 0.9f
                AnalogDial(now, colors, showSeconds, Modifier.size(if (matchCalendar) min(cell * rows, paneSize) else paneSize))
            }
        }
        if (showDate) {
            Column {
                Text(
                    now.format(weekdayFormat()).uppercase(),
                    color = colors.accent,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(now.dayOfMonth.toString(), color = colors.primary, fontSize = 140.sp, fontWeight = FontWeight.Bold)
                Text(now.format(monthFormat()), color = colors.secondary, fontSize = 30.sp)
            }
        }
    }
}

@Composable
fun AnalogDial(now: ZonedDateTime, colors: ClockColors, showSeconds: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.testTag(TAG_DIAL)) {
        val r = min(size.width, size.height) / 2f
        val c = center
        // Graduations
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val inner = if (major) r * 0.86f else r * 0.93f
            drawRadial(c, i / 60f, inner, r * 0.98f, if (major) colors.primary else colors.secondary, if (major) 6f else 2f)
        }
        val sec = now.second / 60f
        val minute = (now.minute + sec) / 60f
        val hour = ((now.hour % 12) + minute) / 12f
        drawRadial(c, hour, 0f, r * 0.5f, colors.primary, 14f)
        drawRadial(c, minute, 0f, r * 0.78f, colors.primary, 9f)
        if (showSeconds) drawRadial(c, sec, -r * 0.12f, r * 0.85f, colors.accent, 3f)
        drawCircle(colors.accent, radius = 10f, center = c)
    }
}

/** Trait radial ; [fraction] = 0 à midi, sens horaire. */
private fun DrawScope.drawRadial(c: Offset, fraction: Float, from: Float, to: Float, color: Color, width: Float) {
    val a = fraction * 2 * PI - PI / 2
    val dx = cos(a).toFloat()
    val dy = sin(a).toFloat()
    drawLine(color, Offset(c.x + dx * from, c.y + dy * from), Offset(c.x + dx * to, c.y + dy * to), width, StrokeCap.Round)
}

/** Style 3 : très épuré, chiffres fins, peu de pixels allumés (idéal la nuit). */
@Composable
fun MinimalClock(now: ZonedDateTime, colors: ClockColors, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val digits = min(maxHeight * 0.7f, maxWidth * 0.3f).asSp()
        val small = min(maxHeight * 0.06f, maxWidth * 0.03f).asSp()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now.hhmm(), color = colors.primary, fontSize = digits, fontWeight = FontWeight.Thin, fontFamily = FontFamily.SansSerif)
            Text(
                now.format(fullDateFormat()),
                color = colors.secondary,
                fontSize = small,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
        }
    }
}

/** Calendrier du mois affiché à côté de l'horloge ; semaine selon la région, aujourd'hui entouré. */
@Composable
fun MonthCalendar(now: ZonedDateTime, colors: ClockColors, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // CALENDAR_ROWS : en-tête, jours de la semaine, 6 semaines au plus
        val cell = min(maxWidth / DAYS_PER_WEEK, maxHeight / CALENDAR_ROWS)
        val text = (cell * 0.42f).asSp()
        val firstDay = weekFields().firstDayOfWeek
        val (offset, days, weeks) = monthGrid(now.toLocalDate(), firstDay)
        val today = now.dayOfMonth

        Column(Modifier.fillMaxWidth().testTag(TAG_CALENDAR)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    now.format(monthFormat()).uppercase(),
                    color = colors.accent,
                    fontSize = (cell * 0.5f).asSp(),
                    fontWeight = FontWeight.SemiBold
                )
                Text(now.year.toString(), color = colors.secondary, fontSize = text)
            }
            Row(Modifier.fillMaxWidth()) {
                for (label in weekInitials(firstDay)) {
                    Box(Modifier.weight(1f).height(cell * 0.8f), contentAlignment = Alignment.Center) {
                        Text(label, color = colors.secondary, fontSize = (cell * 0.34f).asSp(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            for (w in 0 until weeks) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val day = w * 7 + col - offset + 1
                        Box(Modifier.weight(1f).height(cell), contentAlignment = Alignment.Center) {
                            if (day in 1..days) {
                                if (day == today) {
                                    // Anneau plutôt que disque plein : moins de pixels allumés.
                                    Box(Modifier.size(cell * 0.8f).border(2.dp, colors.accent, CircleShape))
                                }
                                Text(
                                    day.toString(),
                                    color = when {
                                        day == today -> colors.accent
                                        firstDay.plus(col.toLong()).isWeekend() -> colors.secondary
                                        else -> colors.primary
                                    },
                                    fontSize = text,
                                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grille d'un mois, semaine commençant par [firstDay] (lundi par défaut) : cases vides avant le 1er, nombre de
 * jours et de semaines.
 */
data class MonthGrid(val offset: Int, val days: Int, val weeks: Int)

fun monthGrid(date: LocalDate, firstDay: DayOfWeek = DayOfWeek.MONDAY): MonthGrid {
    val first = date.withDayOfMonth(1)
    val offset = (first.dayOfWeek.value - firstDay.value).mod(DAYS_PER_WEEK)
    val days = first.lengthOfMonth()
    return MonthGrid(offset, days, (offset + days + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK)
}
