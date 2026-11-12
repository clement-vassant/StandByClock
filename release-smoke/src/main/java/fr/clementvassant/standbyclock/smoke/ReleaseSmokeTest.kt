package fr.clementvassant.standbyclock.smoke

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import fr.clementvassant.standbyclock.e2e.ScreenSaver
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L'APK release, passé par R8, démarre ses trois points d'entrée : l'horloge depuis le lanceur, les réglages et
 * l'écran de veille. Une classe retirée ou renommée à tort par R8 fait planter l'un d'eux dès l'ouverture, ce que
 * l'APK debug, qui ne passe pas par R8, ne peut pas montrer. Les parcours détaillés restent dans :app (APK debug).
 */
@RunWith(AndroidJUnit4::class)
class ReleaseSmokeTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val screenSaver = ScreenSaver(device, PACKAGE, CAPTURES_DIR, SCREEN_TIMEOUT_MS, TAG_SCREEN)

    /** Réglages d'un premier lancement, comme après l'installation de la release. */
    @Before
    fun freshStart() {
        device.pressHome()
        shell("pm clear $PACKAGE")
    }

    @After
    fun restore() {
        screenSaver.stop()
        device.pressHome()
    }

    @Test
    fun launcherOpensTheClock() {
        shell("monkey -p $PACKAGE -c android.intent.category.LAUNCHER 1")
        assertShown(TAG_SCREEN, "L'horloge ne s'est pas ouverte depuis le lanceur")
        capture("release-lancement")
    }

    @Test
    fun settingsOpen() {
        shell("am start -W -n $PACKAGE/.SettingsActivity")
        assertShown(TAG_SETTINGS, "Les réglages ne se sont pas ouverts")
        capture("release-reglages")
    }

    @Test
    fun screenSaverShowsTheClock() {
        screenSaver.start()
        assertShown(TAG_SCREEN, "L'écran de veille n'affiche pas l'horloge")
        capture("release-ecran-de-veille")
    }

    private fun assertShown(tag: String, message: String) {
        assertTrue(message, device.wait(Until.hasObject(By.res(tag)), SCREEN_TIMEOUT_MS))
    }

    private fun capture(name: String) {
        shell("mkdir -p $CAPTURES_DIR")
        shell("screencap -p $CAPTURES_DIR/$name.png")
    }

    private fun shell(command: String): String = device.executeShellCommand(command)

    private companion object {
        const val PACKAGE = "fr.clementvassant.standbyclock"

        // Copies des testTags de ui/TestTags.kt : ce module ne compile pas avec les classes de l'app. Un tag renommé
        // dans l'app sans être reporté ici fait échouer ces parcours, donc ne passe pas inaperçu.
        const val TAG_SCREEN = "standby"
        const val TAG_SETTINGS = "settings"

        /** Même dossier que les parcours de :app : scripts/connected-tests.sh le récupère après chaque passe. */
        const val CAPTURES_DIR = "/sdcard/Download/standbyclock-e2e"

        /** L'émulateur de la CI, sans accélération graphique, est lent. */
        const val SCREEN_TIMEOUT_MS = 15_000L
    }
}
