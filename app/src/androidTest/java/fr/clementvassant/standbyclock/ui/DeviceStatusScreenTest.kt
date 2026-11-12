package fr.clementvassant.standbyclock.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.clementvassant.standbyclock.BatteryStatus
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.DeviceStatus
import fr.clementvassant.standbyclock.PREFS_FILE
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.ThemeMode
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Batterie et prochain réveil sur la ligne de la date, selon la charge, le niveau et le mode nuit. */
@RunWith(AndroidJUnit4::class)
class DeviceStatusScreenTest {
    @get:Rule
    val rule = createComposeRule()

    // L'heure du réveil suit le réglage 12 h / 24 h du téléphone : 24 h sauf mention contraire.
    @get:Rule
    val hourFormat = SystemHourFormatRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    private val paris = ZoneId.of("Europe/Paris")

    // Mardi 20 octobre 2026, 14:15 ; réveil le lendemain à 7 h.
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 20, 14, 15, 0, 0, paris).toInstant(), paris)
    private val tomorrowSeven = ZonedDateTime.of(2026, 10, 21, 7, 0, 0, 0, paris).toInstant().toEpochMilli()

    @Before
    fun resetPrefs() {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context)
    }

    /** Niveau tel que l'app l'écrit dans la langue de l'appareil (« 76 % » ou « 76% »). */
    private fun percent(level: Int) = context.getString(R.string.status_battery_percent, level)

    private fun show(status: DeviceStatus, theme: ThemeMode = ThemeMode.DAY, style: ClockStyle = ClockStyle.DIGITAL) {
        prefs.style = style
        rule.setContent {
            StandByScreen(
                prefs = prefs,
                forcedTheme = theme,
                onNightModeChange = {},
                onOpenSettings = null,
                clock = clock,
                nowPlayingSource = { null },
                deviceStatusSource = { status }
            )
        }
    }

    @Test
    fun chargingShowsBatteryAndAlarmNextToTheDate() {
        show(DeviceStatus(BatteryStatus(percent = 76, charging = true), tomorrowSeven))
        rule.onNodeWithTag(TAG_BATTERY).assertIsDisplayed().assertTextEquals(percent(76))
        rule.onNodeWithTag(TAG_ALARM).assertIsDisplayed().assertTextEquals("07:00")
    }

    @Test
    fun phoneIn12HoursShowsTheAlarmWithAmPm() {
        hourFormat.set(SystemHourFormatRule.TWELVE_HOURS)
        show(DeviceStatus(nextAlarmMillis = tomorrowSeven))
        rule.onNodeWithTag(TAG_ALARM).assertIsDisplayed().assertTextEquals("07:00 AM")
    }

    @Test
    fun atNightOnlyTheAlarmStays() {
        show(DeviceStatus(BatteryStatus(percent = 76, charging = true), tomorrowSeven), theme = ThemeMode.NIGHT)
        rule.onNodeWithTag(TAG_BATTERY).assertDoesNotExist()
        rule.onNodeWithTag(TAG_ALARM).assertIsDisplayed()
    }

    @Test
    fun unpluggedBatteryShowsOnlyWhenLow() {
        show(DeviceStatus(BatteryStatus(percent = 12, charging = false)))
        rule.onNodeWithTag(TAG_BATTERY).assertIsDisplayed().assertTextEquals(percent(12))
        rule.onNodeWithTag(TAG_ALARM).assertDoesNotExist()
    }

    @Test
    fun unpluggedAndWellChargedShowsNothing() {
        show(DeviceStatus(BatteryStatus(percent = 50, charging = false)))
        rule.onNodeWithTag(TAG_BATTERY).assertDoesNotExist()
        rule.onNodeWithTag(TAG_ALARM).assertDoesNotExist()
    }

    @Test
    fun analogClockShowsTheStatusUnderTheDial() {
        show(DeviceStatus(BatteryStatus(percent = 76, charging = true), tomorrowSeven), style = ClockStyle.ANALOG)
        rule.onNodeWithTag(TAG_DIAL).assertIsDisplayed()
        rule.onNodeWithTag(TAG_BATTERY).assertIsDisplayed()
        rule.onNodeWithTag(TAG_ALARM).assertIsDisplayed()
    }

    @Test
    fun analogClockWithoutCalendarShowsTheStatusUnderTheBigDate() {
        prefs.showCalendar = false
        show(DeviceStatus(BatteryStatus(percent = 76, charging = true), tomorrowSeven), style = ClockStyle.ANALOG)
        rule.onNodeWithTag(TAG_CALENDAR).assertDoesNotExist()
        rule.onNodeWithTag(TAG_BATTERY).assertIsDisplayed()
        rule.onNodeWithTag(TAG_ALARM).assertIsDisplayed()
    }
}
