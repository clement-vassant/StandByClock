package fr.clementvassant.standbyclock.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import fr.clementvassant.standbyclock.DAYS_PER_WEEK
import fr.clementvassant.standbyclock.R
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/** Nom du jour seul (« jeudi ») et du mois seul (« octobre ») : même motif dans toutes les langues. */
private const val WEEKDAY_PATTERN = "EEEE"
private const val MONTH_PATTERN = "MMMM"

/**
 * Date complète sous l'horloge : jour de la semaine, quantième et mois. L'ordre et la ponctuation viennent de la
 * région (« jeudi 29 octobre », « Thursday, October 29 » aux États-Unis, « Thursday 29 October » au Royaume-Uni).
 */
private const val FULL_DATE_SKELETON = "EEEEdMMMM"

/**
 * Langue des textes de l'app (strings.xml), région de l'appareil. La langue n'est pas celle de l'appareil : sinon
 * un téléphone dans une langue non traduite mélangerait les textes de l'app et des noms de mois d'une autre langue.
 * La région, elle, décide de l'ordre de la date.
 */
fun dateLocale(languageTag: String, device: Locale): Locale = Locale.Builder().setLanguage(languageTag).setRegion(device.country).build()

fun dateFormatter(pattern: String, locale: Locale): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern, locale)

/** Motif de la date complète pour cette langue et cette région (voir [FULL_DATE_SKELETON]). */
fun fullDatePattern(locale: Locale): String = DateFormat.getBestDateTimePattern(locale, FULL_DATE_SKELETON)

/** Remet dans l'ordre de la semaine locale une liste rangée du lundi au dimanche. */
fun <T> fromFirstDay(mondayFirst: List<T>, firstDay: DayOfWeek): List<T> =
    List(DAYS_PER_WEEK) { mondayFirst[(firstDay.ordinal + it) % DAYS_PER_WEEK] }

@Composable
private fun rememberDateFormatter(pattern: (Locale) -> String): DateTimeFormatter {
    val language = stringResource(R.string.date_language)
    val device = LocalConfiguration.current.locales[0]
    return remember(language, device) {
        val locale = dateLocale(language, device)
        dateFormatter(pattern(locale), locale)
    }
}

/** Date complète sous l'horloge (« jeudi 29 octobre »). */
@Composable
internal fun fullDateFormat(): DateTimeFormatter = rememberDateFormatter(::fullDatePattern)

@Composable
internal fun weekdayFormat(): DateTimeFormatter = rememberDateFormatter { WEEKDAY_PATTERN }

@Composable
internal fun monthFormat(): DateTimeFormatter = rememberDateFormatter { MONTH_PATTERN }

/**
 * Règles de semaine de la région de l'appareil : premier jour (lundi en France, dimanche aux États-Unis) et
 * numérotation des semaines.
 */
@Composable
internal fun weekFields(): WeekFields {
    val device = LocalConfiguration.current.locales[0]
    return remember(device) { WeekFields.of(device) }
}

/** Initiales des jours dans l'ordre de la semaine locale (« L M M J V S D », « S M T W T F S »). */
@Composable
internal fun weekInitials(firstDay: DayOfWeek): List<String> = fromFirstDay(stringArrayResource(R.array.week_initials).toList(), firstDay)
