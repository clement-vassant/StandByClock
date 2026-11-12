package fr.clementvassant.standbyclock.e2e

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.MainActivity
import fr.clementvassant.standbyclock.OrientationMode
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.SettingsActivity
import fr.clementvassant.standbyclock.ui.TAG_SCREEN
import fr.clementvassant.standbyclock.ui.TAG_SETTINGS
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Réglage Orientation de l'horloge plein écran : « Automatique » suit la rotation du téléphone (côte à côte en
 * paysage, l'un au-dessus de l'autre en portrait), « Toujours paysage » reste en paysage téléphone debout.
 * La rotation est imposée par UI Automator (verrou de rotation), comme un utilisateur qui tourne son téléphone.
 * En « Automatique », on tourne une fois l'horloge affichée : tourné sur l'écran d'accueil, qui reste en portrait
 * sur un téléphone, l'émulateur de la CI n'appliquait pas la rotation à l'horloge ouverte ensuite.
 */
@RunWith(AndroidJUnit4::class)
class OrientationJourneyTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun prepare() {
        E2e.freshStart()
    }

    @After
    fun restoreRotation() {
        device.setOrientationNatural()
        device.unfreezeRotation()
    }

    @Test
    fun automaticPutsClockAndCalendarSideBySideInLandscape() = uiAutomator {
        E2e.open(MainActivity::class.java)
        clockScreen()
        device.setOrientationLandscape()
        clockScreen(landscape = true)
        val (clock, calendar) = clockAndCalendar()
        assertTrue("Horloge à côté du calendrier : $clock / $calendar", clock.right <= calendar.left || calendar.right <= clock.left)
        capture("orientation-automatique-paysage")
    }

    @Test
    fun automaticStacksClockAndCalendarInPortrait() = uiAutomator {
        E2e.open(MainActivity::class.java)
        clockScreen()
        device.setOrientationPortrait()
        clockScreen(landscape = false)
        val (clock, calendar) = clockAndCalendar()
        assertTrue(
            "Horloge au-dessus ou au-dessous du calendrier : $clock / $calendar",
            clock.bottom <= calendar.top || calendar.bottom <= clock.top
        )
        capture("orientation-automatique-portrait")
    }

    @Test
    fun alwaysLandscapeChosenInSettingsKeepsTheClockInLandscapeWithThePhoneUpright() = uiAutomator {
        device.setOrientationPortrait()
        E2e.open(SettingsActivity::class.java)
        val alwaysLandscape = E2e.context.getString(R.string.orientation_landscape)
        val choice = byTag(TAG_SETTINGS).scrollUntil(Direction.DOWN, Until.findObject(By.text(alwaysLandscape)))
        assertNotNull("Choix « $alwaysLandscape » introuvable dans les réglages", choice)
        choice.click()
        assertEquals(OrientationMode.LANDSCAPE, ClockPrefs(E2e.context).orientation)

        E2e.open(MainActivity::class.java)
        clockScreen(landscape = true)
        val (clock, calendar) = clockAndCalendar()
        assertTrue("Horloge à côté du calendrier : $clock / $calendar", clock.right <= calendar.left || calendar.right <= clock.left)
        capture("orientation-toujours-paysage")
    }

    /**
     * L'écran de l'horloge une fois tourné comme attendu : la rotation et le changement d'orientation de l'activité
     * prennent un moment, on attend que ses dimensions suivent plutôt que de lire l'écran d'avant.
     */
    private fun UiAutomatorTestScope.clockScreen(landscape: Boolean): UiObject2 = onElement(E2e.SCREEN_TIMEOUT_MS) {
        val bounds = Rect().also(::getBoundsInScreen)
        viewIdResourceName == TAG_SCREEN && (bounds.width() > bounds.height()) == landscape
    }
}
