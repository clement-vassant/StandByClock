package fr.clementvassant.standbyclock.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.OrientationMode
import fr.clementvassant.standbyclock.ui.TAG_SCREEN
import fr.clementvassant.standbyclock.ui.clockTag
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L'écran de veille, tel qu'Android le lance pendant la charge : l'horloge et le calendrier s'affichent en entier,
 * sans se chevaucher. Sur un téléphone, Android garde l'écran de veille en portrait : avec « Toujours paysage »,
 * c'est l'app qui couche l'horloge, ce que montre la ligne de date, plus haute que large une fois tournée.
 */
@RunWith(AndroidJUnit4::class)
class ScreenSaverJourneyTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var prefs: ClockPrefs
    private lateinit var screenSaver: ScreenSaver

    @Before
    fun prepare() {
        prefs = E2e.freshStart()
        screenSaver = ScreenSaver(device, E2e.PACKAGE, E2e.CAPTURES_DIR, E2e.SCREEN_TIMEOUT_MS, TAG_SCREEN)
    }

    @After
    fun restore() {
        screenSaver.stop()
    }

    @Test
    fun automaticShowsClockAndCalendarUpright() = uiAutomator {
        screenSaver.start()
        clockScreen()
        clockAndCalendar()
        val date = dateLine().visibleBounds
        assertTrue("Date de l'horloge couchée alors que l'orientation est automatique : $date", date.width() > date.height())
        capture("ecran-de-veille")
    }

    @Test
    fun alwaysLandscapeTurnsTheClockInAPortraitScreenSaver() = uiAutomator {
        prefs.orientation = OrientationMode.LANDSCAPE
        screenSaver.start()
        clockScreen()
        clockAndCalendar()
        val screen = clockScreen().visibleBounds
        val date = dateLine().visibleBounds
        // Écran de veille en paysage (tablette, pliable déplié) : rien à tourner, la date reste couchée normalement.
        val portraitScreenSaver = screen.height() > screen.width()
        assertTrue(
            "Date de l'horloge non tournée en « Toujours paysage » : $date (écran $screen)",
            if (portraitScreenSaver) date.height() > date.width() else date.width() > date.height()
        )
        capture("ecran-de-veille-toujours-paysage")
    }

    @Test
    fun backLeavesTheScreenSaver() = uiAutomator {
        screenSaver.start()
        clockScreen()
        pressBack()
        assertTrue("L'écran de veille est resté affiché", device.wait(Until.gone(By.res(TAG_SCREEN)), E2e.SCREEN_TIMEOUT_MS))
    }

    /** La ligne de date sous les chiffres de l'horloge numérique (« Jeudi 22 octobre »). */
    private fun UiAutomatorTestScope.dateLine() = byTag(clockTag(ClockStyle.DIGITAL)).let { clock ->
        val today = LocalDate.now().format(DATE_FORMAT).replaceFirstChar { it.uppercase() }
        checkNotNull(clock.wait(Until.findObject(By.text(today)), E2e.SCREEN_TIMEOUT_MS)) { "Date « $today » absente de l'horloge" }
    }

    private companion object {
        /** Le format de la date de l'horloge numérique (`Clocks.kt`). */
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    }
}
