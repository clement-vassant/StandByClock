package fr.clementvassant.standbyclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import fr.clementvassant.standbyclock.R
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Nom du jour seul (« jeudi ») et du mois seul (« octobre ») : même motif dans toutes les langues. */
private const val WEEKDAY_PATTERN = "EEEE"
private const val MONTH_PATTERN = "MMMM"

/**
 * Format de date dans la langue des textes de l'app (strings.xml), pas dans celle de l'appareil : sinon un
 * téléphone dans une langue non traduite mélangerait les textes de l'app et des noms de mois d'une autre langue.
 */
fun dateFormatter(pattern: String, languageTag: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(languageTag))

@Composable
private fun rememberDateFormatter(pattern: String): DateTimeFormatter {
    val language = stringResource(R.string.date_language)
    return remember(pattern, language) { dateFormatter(pattern, language) }
}

/** Date complète sous l'horloge (« jeudi 29 octobre »). */
@Composable
internal fun fullDateFormat(): DateTimeFormatter = rememberDateFormatter(stringResource(R.string.date_full_pattern))

@Composable
internal fun weekdayFormat(): DateTimeFormatter = rememberDateFormatter(WEEKDAY_PATTERN)

@Composable
internal fun monthFormat(): DateTimeFormatter = rememberDateFormatter(MONTH_PATTERN)

/** Initiales des jours, du lundi au dimanche (« L M M J V S D »). */
@Composable
internal fun weekInitials(): List<String> = stringArrayResource(R.array.week_initials).toList()
