package fr.clementvassant.standbyclock.e2e

import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.EXTRA_THEME
import fr.clementvassant.standbyclock.MainActivity
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.SettingsActivity
import fr.clementvassant.standbyclock.ThemeMode
import fr.clementvassant.standbyclock.ui.STATE_DAY
import fr.clementvassant.standbyclock.ui.STATE_NIGHT
import fr.clementvassant.standbyclock.ui.TAG_DIAL
import fr.clementvassant.standbyclock.ui.TAG_SETTINGS
import fr.clementvassant.standbyclock.ui.clockTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Lancer l'horloge comme le ferait l'utilisateur : depuis le lanceur, les réglages ou un raccourci de l'icône. */
@RunWith(AndroidJUnit4::class)
class LaunchJourneyTest {
    @Before
    fun freshStart() {
        E2e.freshStart()
    }

    @Test
    fun launcherOpensTheClockAndASwipeChangesTheStyleForGood() = uiAutomator {
        E2e.openFromLauncher()
        byTag(clockTag(ClockStyle.DIGITAL))
        capture("lancement-digital")

        // Balayage vers la gauche : style suivant, enregistré et retrouvé au retour dans l'app.
        clockScreen().swipe(Direction.LEFT, SWIPE_FRACTION)
        byTag(TAG_DIAL)
        pressHome()
        E2e.openFromLauncher()
        byTag(TAG_DIAL)
        assertEquals(ClockStyle.ANALOG, ClockPrefs(E2e.context).style)
        capture("lancement-analogique-garde")
    }

    @Test
    fun styleChosenInSettingsIsTheOneLaunched() = uiAutomator {
        E2e.open(SettingsActivity::class.java)
        byTag(TAG_SETTINGS)
        byText("Minimal").click()
        // Le bouton est sous la ligne de flottaison sur un petit écran : on fait défiler jusqu'à lui.
        val label = E2e.context.getString(R.string.settings_launch)
        val launch = byTag(TAG_SETTINGS).scrollUntil(Direction.DOWN, Until.findObject(By.text(label)))
        assertNotNull("Bouton « $label » introuvable", launch)
        launch.click()
        byTag(clockTag(ClockStyle.MINIMAL))
        capture("reglages-minimal")
    }

    // Les raccourcis « Horloge jour » et « Horloge nuit » de l'appui long sur l'icône (aussi lancés par Tasker...).
    // On envoie l'intent déclaré dans res/xml/shortcuts.xml : le menu de l'appui long dépend du lanceur.
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun nightAndDayShortcutsForceTheirMode() = uiAutomator {
        E2e.open(shortcutIntent(THEME_NIGHT))
        clockScreenIn(STATE_NIGHT)
        capture("raccourci-horloge-nuit")

        // Un toucher inverse jour et nuit jusqu'à la prochaine bascule.
        clockScreen().click()
        clockScreenIn(STATE_DAY)

        // Réglages en Nuit : seul le raccourci « Horloge jour » peut ramener le jour.
        ClockPrefs(E2e.context).themeMode = ThemeMode.NIGHT
        E2e.open(shortcutIntent(THEME_DAY))
        clockScreenIn(STATE_DAY)
    }

    private fun shortcutIntent(theme: String) = Intent(Intent.ACTION_VIEW)
        .setClass(E2e.context, MainActivity::class.java)
        .putExtra(EXTRA_THEME, theme)

    private companion object {
        const val SWIPE_FRACTION = 0.8f

        /** Valeurs de l'extra `theme` dans res/xml/shortcuts.xml. */
        const val THEME_DAY = "day"
        const val THEME_NIGHT = "night"
    }
}
