package fr.clementvassant.standbyclock.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import androidx.test.uiautomator.watcher.PermissionDialog
import fr.clementvassant.standbyclock.DeviceCalendar
import fr.clementvassant.standbyclock.MainActivity
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.SettingsActivity
import fr.clementvassant.standbyclock.ui.TAG_AGENDA
import fr.clementvassant.standbyclock.ui.TAG_SETTINGS
import fr.clementvassant.standbyclock.ui.calendarChoiceTag
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Événements du jour : de l'autorisation dans les réglages jusqu'à l'événement affiché à côté de l'horloge. */
@RunWith(AndroidJUnit4::class)
class CalendarJourneyTest {
    private lateinit var calendar: TestCalendar

    @Before
    fun createCalendar() {
        E2e.freshStart()
        calendar = TestCalendar.create(CALENDAR_NAME)
        calendar.addAllDayEventToday(EVENT_TITLE)
    }

    @After
    fun deleteCalendar() {
        if (::calendar.isInitialized) calendar.delete()
    }

    @Test
    fun grantedAndTickedCalendarShowsTodayEventNextToTheClock() = uiAutomator {
        // La vraie boîte de dialogue d'Android (celle du permissioncontroller de Google, présent sur les images
        // google_apis de la CI et du Mac). Sur un appareil déjà autorisé, il n'y a ni bouton ni boîte.
        watchFor(PermissionDialog) { clickAllow() }
        E2e.open(SettingsActivity::class.java)
        val settings = byTag(TAG_SETTINGS)
        if (!DeviceCalendar.hasAccess(E2e.context)) {
            // Les réglages dépassent l'écran : Compose n'expose que ce qui est affiché, on fait défiler jusqu'au bouton.
            val label = E2e.context.getString(R.string.settings_calendar_access_button)
            val allow = settings.scrollUntil(Direction.DOWN, Until.findObject(By.text(label)))
            assertNotNull("Bouton « $label » introuvable", allow)
            allow.click()
        }

        // Au retour de la demande, la liste des agendas de l'appareil remplace le bouton. L'écran se recompose
        // à la reprise : un nœud lu juste avant peut disparaître (StaleObjectException), on le relit alors.
        val choice = retryWhileStale {
            byTag(TAG_SETTINGS).scrollUntil(Direction.DOWN, Until.findObject(By.res(calendarChoiceTag(calendar.id))))
        }
        assertNotNull("Agenda « $CALENDAR_NAME » absent des réglages", choice)
        retryWhileStale { choice.click() }
        capture("agenda-coche")

        E2e.open(MainActivity::class.java)
        byTag(TAG_AGENDA)
        byText(EVENT_TITLE)
        capture("agenda-evenement-du-jour")
    }

    private companion object {
        const val CALENDAR_NAME = "Agenda e2e"
        const val EVENT_TITLE = "Revue e2e"
    }
}
