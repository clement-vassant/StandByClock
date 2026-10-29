package fr.clementvassant.standbyclock.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.clementvassant.standbyclock.CalendarEvent
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.ThemeMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Événements du jour : la semaine et la liste remplacent le mois quand il reste quelque chose à montrer. */
@RunWith(AndroidJUnit4::class)
class TodayEventsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    private val paris = ZoneId.of("Europe/Paris")
    private val work = 3L
    private val family = 7L

    // Mardi 20 octobre 2026, 14:15, en plein jour.
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 20, 14, 15, 0, 0, paris).toInstant(), paris)

    private fun at(hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, 20, hour, minute, 0, 0, paris).toInstant().toEpochMilli()

    private val sprintReview = CalendarEvent("Revue de sprint", at(14), at(15), allDay = false, calendarId = work)
    private val dentist = CalendarEvent("Dentiste", at(17, 30), at(18), allDay = false, calendarId = family)
    private val standup = CalendarEvent("Point d'équipe", at(9, 30), at(9, 45), allDay = false, calendarId = work)

    @Before
    fun resetPrefs() {
        context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context)
        prefs.shownCalendars = setOf(work, family)
    }

    // Paysage qui tient en entier dans l'émulateur de la CI (320 dp de large) : assertIsDisplayed échoue
    // sur un texte rogné par le bord de l'écran, comme « semaine 43 » collé à droite.
    private fun show(events: List<CalendarEvent>, forcedTheme: ThemeMode? = null) = rule.setContent {
        Box(Modifier.size(width = 320.dp, height = 192.dp)) {
            StandByScreen(
                prefs = prefs,
                forcedTheme = forcedTheme,
                onNightModeChange = {},
                onOpenSettings = null,
                clock = clock,
                nowPlayingSource = { null },
                eventsSource = { enabled, _: LocalDate -> if (enabled) events else emptyList() }
            )
        }
    }

    @Test
    fun remainingEventsReplaceTheMonthWithTheWeek() {
        show(listOf(standup, sprintReview, dentist))
        rule.onNodeWithTag(TAG_AGENDA).assertIsDisplayed()
        rule.onNodeWithTag(TAG_CALENDAR).assertDoesNotExist()
        rule.onNodeWithText(
            context.getString(R.string.agenda_week_number, weekNumber(LocalDate.of(2026, 10, 20), WeekFields.of(Locale.getDefault())))
        ).assertIsDisplayed()
        // En cours : l'heure de fin ; à venir : l'heure de début ; terminé : plus affiché.
        rule.onNodeWithText(context.getString(R.string.agenda_ends_at, "15:00")).assertIsDisplayed()
        rule.onNodeWithText("17:30").assertIsDisplayed()
        rule.onNodeWithText("Point d'équipe").assertDoesNotExist()
    }

    @Test
    fun monthStaysWhenEveryEventIsOver() {
        show(listOf(standup))
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(TAG_AGENDA).assertDoesNotExist()
    }

    @Test
    fun uncheckedCalendarsAreNotShown() {
        prefs.shownCalendars = setOf(work)
        show(listOf(dentist))
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithText("Dentiste").assertDoesNotExist()
    }

    @Test
    fun nightKeepsTheMonth() {
        show(listOf(sprintReview, dentist), forcedTheme = ThemeMode.NIGHT)
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(TAG_AGENDA).assertDoesNotExist()
    }

    @Test
    fun eventsSettingOffKeepsTheMonth() {
        prefs.showEvents = false
        show(listOf(sprintReview))
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
    }

    @Test
    fun longPressHidesEventsWithTheCalendar() {
        show(listOf(sprintReview))
        rule.onNodeWithTag(TAG_AGENDA).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { longClick(center) }
        rule.onNodeWithTag(TAG_AGENDA).assertDoesNotExist()
    }

    @Test
    fun tooManyEventsEndWithACount() {
        val evening = (15..20).map { CalendarEvent("Rendez-vous $it", at(it), at(it, 30), allDay = false, calendarId = work) }
        show(evening)
        rule.onNodeWithText(context.resources.getQuantityString(R.plurals.agenda_more_events, 3, 3)).assertIsDisplayed()
        rule.onNodeWithText("Rendez-vous 18").assertDoesNotExist()
    }

    // Semaine et liste à côté : le cadran ne se cale plus sur la hauteur du mois, il remplit son panneau.
    @Test
    fun analogDialFillsItsPaneNextToTheWeek() {
        prefs.style = ClockStyle.ANALOG
        show(listOf(sprintReview))
        rule.onNodeWithTag(TAG_AGENDA).assertIsDisplayed()
        val dial = rule.onNodeWithTag(TAG_DIAL).fetchSemanticsNode().size.height
        val pane = rule.onNodeWithTag(clockTag(ClockStyle.ANALOG)).fetchSemanticsNode().size.height
        assertTrue("cadran $dial trop petit pour son panneau $pane", dial * 10 >= pane * 8)
    }
}
