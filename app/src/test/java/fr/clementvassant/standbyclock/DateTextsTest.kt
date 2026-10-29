package fr.clementvassant.standbyclock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.clementvassant.standbyclock.ui.dateFormatter
import java.time.LocalDate
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

    @Test
    @Config(qualifiers = "fr-rFR")
    fun `full date reads in french`() {
        val format = dateFormatter(resources.getString(R.string.date_full_pattern), resources.getString(R.string.date_language))
        assertEquals("jeudi 29 octobre", thursday.format(format))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `month names follow the app texts, not the device language`() {
        // Pas encore de traduction anglaise : textes en français, donc mois en français aussi, sans mélange.
        val format = dateFormatter("MMMM", resources.getString(R.string.date_language))
        assertEquals("octobre", thursday.format(format))
    }

    @Test
    fun `week initials run from monday to sunday`() {
        assertEquals(listOf("L", "M", "M", "J", "V", "S", "D"), resources.getStringArray(R.array.week_initials).toList())
    }
}
