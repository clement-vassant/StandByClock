package fr.clementvassant.standbyclock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Libellés des événements du jour, lus dans les ressources comme sur l'appareil. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fr-rFR")
class AgendaTextsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `remaining count agrees with its number`() {
        assertEquals("+1 autre", resources.getQuantityString(R.plurals.agenda_more_events, 1, 1))
        assertEquals("+3 autres", resources.getQuantityString(R.plurals.agenda_more_events, 3, 3))
    }

    @Test
    fun `event time labels`() {
        assertEquals("Journée", resources.getString(R.string.agenda_all_day))
        assertEquals("→ 15:00", resources.getString(R.string.agenda_ends_at, "15:00"))
        assertEquals("En cours", resources.getString(R.string.agenda_ongoing))
        assertEquals("semaine 43", resources.getString(R.string.agenda_week_number, 43))
    }
}
