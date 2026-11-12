package fr.clementvassant.standbyclock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.clementvassant.standbyclock.ui.dateFormatter
import fr.clementvassant.standbyclock.ui.dateLocale
import fr.clementvassant.standbyclock.ui.fromFirstDay
import fr.clementvassant.standbyclock.ui.fullDatePattern
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Dates sous l'horloge et dans les calendriers, lues dans les ressources comme sur l'appareil. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DateTextsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val thursday = LocalDate.of(2026, 10, 29)

    private fun fullDate(device: Locale): String {
        val locale = dateLocale(resources.getString(R.string.date_language), device)
        return thursday.format(dateFormatter(fullDatePattern(locale), locale))
    }

    private fun month(device: Locale): String =
        thursday.format(dateFormatter("MMMM", dateLocale(resources.getString(R.string.date_language), device)))

    @Test
    @Config(qualifiers = "fr-rFR")
    fun `full date reads in french`() {
        assertEquals("jeudi 29 octobre", fullDate(Locale.FRANCE))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `full date reads in english, in the order of the region`() {
        assertEquals("Thursday, October 29", fullDate(Locale.US))
        assertEquals("Thursday 29 October", fullDate(Locale.UK))
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun `month names follow the app texts, not the device language`() {
        // Pas de traduction allemande : textes en français, donc mois en français aussi, sans mélange.
        assertEquals("octobre", month(Locale.GERMANY))
    }

    @Test
    @Config(qualifiers = "fr-rFR")
    fun `week initials run from monday to sunday in french`() {
        val initials = resources.getStringArray(R.array.week_initials).toList()
        assertEquals(listOf("L", "M", "M", "J", "V", "S", "D"), fromFirstDay(initials, DayOfWeek.MONDAY))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `week initials start on sunday in the united states`() {
        val initials = resources.getStringArray(R.array.week_initials).toList()
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), fromFirstDay(initials, DayOfWeek.SUNDAY))
    }
}
