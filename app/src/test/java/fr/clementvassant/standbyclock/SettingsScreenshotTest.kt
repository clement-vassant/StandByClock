package fr.clementvassant.standbyclock

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import fr.clementvassant.standbyclock.ui.TAG_SETTINGS
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures de référence de l'écran Réglages, comme StandByScreenshotTest pour l'horloge.
 * Deux états : premier lancement (aucun accès accordé, les boutons d'autorisation s'affichent) et réglages faits
 * (accès accordés, un agenda coché, style Analogique, « Toujours paysage », extinction après 8 h).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Largeur d'un téléphone, et assez de hauteur pour tout capturer d'un coup sans faire défiler.
@Config(sdk = [35], qualifiers = "fr-rFR-w411dp-h1400dp-xhdpi")
class SettingsScreenshotTest(private val case: Case) {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    // Même tolérance que pour l'horloge : anticrénelage seulement.
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))

    @Before
    fun prefs() {
        if (case.english) RuntimeEnvironment.setQualifiers("+en-rUS")
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit()
        // Écriture des heures du téléphone, explicite : le défaut de Robolectric dépend de la langue.
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, if (case.hour12) "12" else "24")
        prefs = ClockPrefs(context)
        if (case.configured) {
            prefs.apply {
                style = ClockStyle.ANALOG
                shownCalendars = setOf(WORK.id)
                orientation = OrientationMode.LANDSCAPE
                autoOffMinutes = EIGHT_HOURS
            }
        }
    }

    @Test
    fun matchesReferenceImage() {
        rule.setContent {
            SettingsTheme {
                SettingsScreen(
                    prefs = prefs,
                    pickTime = { _, _ -> },
                    onLaunch = {},
                    onOpenScreenSaver = {},
                    hasMediaAccess = { case.configured },
                    onOpenNotificationAccess = {},
                    calendarSource = CalendarSource(
                        hasAccess = { case.configured },
                        requestAccess = {},
                        calendars = { listOf(WORK, FAMILY) }
                    )
                )
            }
        }
        rule.onNodeWithTag(TAG_SETTINGS).captureRoboImage("src/test/screenshots/${case.name}.png", roborazziOptions = options)
    }

    /** Un cas : premier lancement, ou accès accordés et réglages modifiés ; téléphone en 24 h ou en 12 h, en français ou en anglais. */
    class Case(val configured: Boolean, val hour12: Boolean = false, val english: Boolean = false) {
        val name: String
            get() = (if (configured) "reglages-personnalises" else "reglages-premier-lancement") +
                (if (hour12) "-12h" else "") + if (english) "-anglais" else ""

        override fun toString() = name
    }

    companion object {
        private val WORK = CalendarInfo(id = 3, name = "Travail", account = "moi@example.com")
        private val FAMILY = CalendarInfo(id = 7, name = "Famille", account = "moi@example.com")
        private const val EIGHT_HOURS = 480

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Case> = listOf(
            Case(configured = false),
            Case(configured = true),
            // Plage de nuit en 12 h : « 10:00 PM » à « 07:00 AM ».
            Case(configured = true, hour12 = true),
            Case(configured = false, english = true)
        )
    }
}
