package fr.clementvassant.standbyclock

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.clementvassant.standbyclock.ui.TAG_CALENDAR_SWITCH
import fr.clementvassant.standbyclock.ui.TAG_EVENTS_SWITCH
import fr.clementvassant.standbyclock.ui.calendarChoiceTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    private val work = CalendarInfo(id = 3, name = "Travail", account = "clement@example.com")
    private val family = CalendarInfo(id = 7, name = "Famille", account = "clement@example.com")
    private var calendarAccess = true
    private var accessRequests = 0

    @Before
    fun resetPrefs() {
        context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context)
    }

    private fun showSettings() = rule.setContent {
        SettingsScreen(
            prefs = prefs,
            pickTime = { _, _ -> },
            onLaunch = {},
            onOpenScreenSaver = {},
            hasMediaAccess = { true },
            onOpenNotificationAccess = {},
            calendarSource = CalendarSource(
                hasAccess = { calendarAccess },
                requestAccess = { accessRequests++ },
                calendars = { listOf(family, work) }
            )
        )
    }

    @Test
    fun calendarsAreShownOnlyOnceTicked() {
        showSettings()
        rule.onNodeWithText("Travail (clement@example.com)").performScrollTo().assertIsDisplayed()
        // Aucun agenda coché au départ.
        rule.onNodeWithTag(calendarChoiceTag(work.id)).performScrollTo().assertIsOff().performClick()
        assertEquals(setOf(work.id), prefs.shownCalendars)
        rule.onNodeWithTag(calendarChoiceTag(work.id)).performClick()
        assertEquals(emptySet<Long>(), prefs.shownCalendars)
    }

    // Toucher le nom de l'agenda suffit : toute la ligne coche la case.
    @Test
    fun touchingTheCalendarNameTicksIt() {
        showSettings()
        rule.onNodeWithText("Famille (clement@example.com)").performScrollTo().performClick()
        assertEquals(setOf(family.id), prefs.shownCalendars)
        rule.onNodeWithTag(calendarChoiceTag(family.id)).assertIsOn()
    }

    @Test
    fun calendarAccessIsAskedBeforeListingCalendars() {
        calendarAccess = false
        showSettings()
        rule.onNodeWithTag(calendarChoiceTag(work.id)).assertDoesNotExist()
        rule.onNodeWithText(context.getString(R.string.settings_calendar_access_button)).performScrollTo().performClick()
        assertEquals(1, accessRequests)
    }

    @Test
    fun eventsSwitchTurnsEventsOff() {
        showSettings()
        rule.onNodeWithTag(TAG_EVENTS_SWITCH).performScrollTo().assertIsOn().performClick()
        assertFalse(prefs.showEvents)
        rule.onNodeWithTag(calendarChoiceTag(work.id)).assertDoesNotExist()
    }

    @Test
    fun calendarSwitchSaysWhatItDoes() {
        showSettings()
        // Le libellé dit « afficher », pas « à droite » : le calendrier change de côté chaque heure.
        rule.onNodeWithText(context.getString(R.string.settings_show_calendar)).assertExists()
        rule.onNodeWithText("à droite", substring = true).assertDoesNotExist()
        rule.onNodeWithTag(TAG_CALENDAR_SWITCH).assertIsOn()
    }

    @Test
    fun orientationCanBeForcedToLandscape() {
        showSettings()
        assertEquals(OrientationMode.AUTO, prefs.orientation)
        rule.onNodeWithText(context.getString(R.string.orientation_landscape)).performScrollTo().performClick()
        assertEquals(OrientationMode.LANDSCAPE, prefs.orientation)
        rule.onNodeWithText(context.getString(R.string.settings_orientation_landscape_help)).assertExists()
    }

    @Test
    fun orientationSettingCoversTheAppToo() {
        showSettings()
        rule.onNodeWithText(context.getString(R.string.settings_orientation)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.settings_orientation_auto_help)).assertExists()
    }

    @Test
    fun calendarSwitchHidesCalendar() {
        showSettings()
        rule.onNodeWithTag(TAG_CALENDAR_SWITCH).performClick()
        assertFalse(prefs.showCalendar)
    }
}
