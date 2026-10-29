package fr.clementvassant.standbyclock.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** L'horloge suit le réglage 12 h / 24 h du téléphone (Réglages › Date et heure). */
@RunWith(AndroidJUnit4::class)
class HourFormatScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val hourFormat = SystemHourFormatRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    // Mardi 20 octobre 2026, 14:15.
    private val clock = Clock.fixed(
        ZonedDateTime.of(2026, 10, 20, 14, 15, 0, 0, ZoneId.of("Europe/Paris")).toInstant(),
        ZoneId.of("Europe/Paris")
    )

    @Before
    fun minimalStyle() {
        context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context).apply { style = ClockStyle.MINIMAL }
    }

    @Test
    fun twelveHourPhoneShowsTwelveHourClockWithoutAmPm() {
        hourFormat.set(SystemHourFormatRule.TWELVE_HOURS)
        show()
        rule.onNodeWithText("02:15").assertIsDisplayed()
    }

    @Test
    fun twentyFourHourPhoneShowsTwentyFourHourClock() {
        hourFormat.set(SystemHourFormatRule.TWENTY_FOUR_HOURS)
        show()
        rule.onNodeWithText("14:15").assertIsDisplayed()
    }

    private fun show() = rule.setContent {
        StandByScreen(
            prefs = prefs,
            forcedTheme = null,
            onNightModeChange = {},
            onOpenSettings = null,
            clock = clock,
            nowPlayingSource = { null }
        )
    }
}
