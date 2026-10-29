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
import fr.clementvassant.standbyclock.NIGHT_BRIGHTNESS
import fr.clementvassant.standbyclock.OrientationMode
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.ThemeMode
import fr.clementvassant.standbyclock.ui.STATE_DAY
import fr.clementvassant.standbyclock.ui.STATE_NIGHT
import fr.clementvassant.standbyclock.ui.TAG_SCREEN
import fr.clementvassant.standbyclock.ui.clockTag
import fr.clementvassant.standbyclock.ui.dateFormatter
import fr.clementvassant.standbyclock.ui.dateLocale
import fr.clementvassant.standbyclock.ui.fullDatePattern
import java.time.LocalDate
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L'écran de veille, tel qu'Android le lance pendant la charge : l'horloge et le calendrier s'affichent en entier,
 * sans se chevaucher. Sur un téléphone, Android garde l'écran de veille en portrait : avec « Toujours paysage »,
 * c'est l'app qui couche l'horloge, ce que montre la ligne de date, plus haute que large une fois tournée.
 * La nuit, l'écran de veille impose la luminosité minimale ; le jour, il laisse le niveau atténué du système.
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

    @Test
    fun nightAsksForTheLowestBrightness() = uiAutomator {
        prefs.themeMode = ThemeMode.NIGHT
        screenSaver.start()
        clockScreenIn(STATE_NIGHT)
        assertEquals(NIGHT_BRIGHTNESS, waitForBrightnessOverride { it == NIGHT_BRIGHTNESS })
        capture("ecran-de-veille-nuit")
    }

    @Test
    fun dayLeavesTheBrightnessToTheSystem() = uiAutomator {
        screenSaver.start()
        clockScreenIn(STATE_DAY)
        // Rien d'imposé : Android affiche NaN (aucune valeur de fenêtre transmise au gestionnaire d'énergie).
        assertTrue("Luminosité imposée le jour", waitForBrightnessOverride { it.isNaN() }.isNaN())
    }

    /**
     * Luminosité que la fenêtre au premier plan impose au système, lue dans `dumpsys power` : c'est ce qu'Android
     * applique vraiment, pas seulement ce que l'app a demandé. Relue jusqu'à la valeur attendue ou l'expiration du délai,
     * le temps que Compose signale le jour ou la nuit et que le gestionnaire de fenêtres transmette la valeur.
     */
    private fun waitForBrightnessOverride(expected: (Float) -> Boolean): Float {
        val deadline = System.currentTimeMillis() + E2e.SCREEN_TIMEOUT_MS
        var override = brightnessOverride()
        while (!expected(override) && System.currentTimeMillis() < deadline) {
            Thread.sleep(BRIGHTNESS_POLL_MS)
            override = brightnessOverride()
        }
        return override
    }

    private fun brightnessOverride(): Float = device.executeShellCommand("dumpsys power").lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith(BRIGHTNESS_OVERRIDE_KEY) }
        ?.substringAfter('=')?.toFloatOrNull()
        ?: error("$BRIGHTNESS_OVERRIDE_KEY absent de dumpsys power")

    /** La ligne de date sous les chiffres de l'horloge numérique (« Jeudi 22 octobre »), formatée comme dans `Clocks.kt`. */
    private fun UiAutomatorTestScope.dateLine() = byTag(clockTag(ClockStyle.DIGITAL)).let { clock ->
        val locale = dateLocale(E2e.context.getString(R.string.date_language), Locale.getDefault())
        val today = LocalDate.now().format(dateFormatter(fullDatePattern(locale), locale)).replaceFirstChar { it.uppercase() }
        checkNotNull(clock.wait(Until.findObject(By.text(today)), E2e.SCREEN_TIMEOUT_MS)) { "Date « $today » absente de l'horloge" }
    }
}

private const val BRIGHTNESS_OVERRIDE_KEY = "mScreenBrightnessOverrideFromWindowManager="
private const val BRIGHTNESS_POLL_MS = 500L
