package fr.clementvassant.standbyclock.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import fr.clementvassant.standbyclock.BatteryStatus
import fr.clementvassant.standbyclock.CalendarEvent
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.DeviceStatus
import fr.clementvassant.standbyclock.PREFS_FILE
import fr.clementvassant.standbyclock.ThemeMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures de référence de l'écran de l'horloge, comparées pixel à pixel à celles du dépôt (src/test/screenshots).
 * Toujours la même heure, les mêmes réglages et les mêmes événements : une différence vient forcément du code.
 *
 * `./gradlew verifyRoborazziDebug` compare, `./gradlew recordRoborazziDebug` remplace les images de référence.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Écran assez grand pour les deux orientations ; chaque capture se limite à la boîte de l'horloge.
@Config(sdk = [35], qualifiers = "fr-rFR-w1000dp-h1000dp-xhdpi")
class StandByScreenshotTest(private val case: Case) {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    // Petites différences d'anticrénelage tolérées entre machines Linux ; un élément déplacé dépasse largement 1 %.
    // Créé dans le test, pas dans le compagnon : Roborazzi a besoin de l'environnement Robolectric.
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))

    @Before
    fun prefs() {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context).apply {
            style = case.style
            shownCalendars = setOf(WORK)
        }
    }

    @Test
    fun matchesReferenceImage() {
        rule.setContent {
            Box(Modifier.size(width = case.width, height = case.height)) {
                StandByScreen(
                    prefs = prefs,
                    forcedTheme = if (case.night) ThemeMode.NIGHT else ThemeMode.DAY,
                    onNightModeChange = {},
                    onOpenSettings = null,
                    clock = CLOCK,
                    nowPlayingSource = { null },
                    eventsSource = { enabled, _: LocalDate -> if (enabled) case.events else emptyList() },
                    deviceStatusSource = { case.status }
                )
            }
        }
        rule.onNodeWithTag(TAG_SCREEN).captureRoboImage("src/test/screenshots/${case.name}.png", roborazziOptions = options)
    }

    /** Un cas : style, jour ou nuit, taille d'écran (dp), événements du jour, batterie et réveil. */
    class Case(
        val style: ClockStyle,
        val night: Boolean,
        val portrait: Boolean,
        val events: List<CalendarEvent> = emptyList(),
        val status: DeviceStatus = DeviceStatus(),
        private val suffix: String = ""
    ) {
        val width: Dp get() = if (portrait) SHORT_SIDE else LONG_SIDE
        val height: Dp get() = if (portrait) LONG_SIDE else SHORT_SIDE
        val name: String
            get() = listOf(
                style.name.lowercase(),
                if (night) "nuit" else "jour",
                if (portrait) "portrait" else "paysage"
            ).joinToString("-") + suffix

        override fun toString() = name
    }

    companion object {
        // Format d'un écran externe de pliable posé en paysage.
        private val LONG_SIDE = 840.dp
        private val SHORT_SIDE = 400.dp

        private const val WORK = 3L
        private val PARIS = ZoneId.of("Europe/Paris")

        // Mardi 20 octobre 2026, 14:15 : heure paire, l'horloge est à gauche (en haut en portrait).
        private val CLOCK = Clock.fixed(ZonedDateTime.of(2026, 10, 20, 14, 15, 0, 0, PARIS).toInstant(), PARIS)

        private fun at(hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, 20, hour, minute, 0, 0, PARIS).toInstant().toEpochMilli()

        // En charge à 76 %, réveil le lendemain à 7 h : la nuit, seul le réveil reste.
        private val CHARGING_WITH_ALARM = DeviceStatus(
            BatteryStatus(percent = 76, charging = true),
            nextAlarmMillis = ZonedDateTime.of(2026, 10, 21, 7, 0, 0, 0, PARIS).toInstant().toEpochMilli()
        )

        private val TODAY_EVENTS = listOf(
            CalendarEvent("Revue de sprint", at(14), at(15), allDay = false, calendarId = WORK),
            CalendarEvent("Dentiste", at(17, 30), at(18), allDay = false, calendarId = WORK)
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Case> = ClockStyle.entries.flatMap { style ->
            listOf(false, true).flatMap { night -> listOf(false, true).map { portrait -> Case(style, night, portrait) } }
        } + Case(ClockStyle.DIGITAL, night = false, portrait = false, events = TODAY_EVENTS, suffix = "-evenements") +
            ClockStyle.entries.flatMap { style ->
                listOf(false, true).map { night ->
                    Case(style, night, portrait = false, status = CHARGING_WITH_ALARM, suffix = "-batterie-reveil")
                }
            } + Case(ClockStyle.DIGITAL, night = false, portrait = true, status = CHARGING_WITH_ALARM, suffix = "-batterie-reveil")
    }
}
