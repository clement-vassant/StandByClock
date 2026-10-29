package fr.clementvassant.standbyclock.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.MainActivity
import fr.clementvassant.standbyclock.ThemeMode
import fr.clementvassant.standbyclock.ui.TAG_ALARM
import fr.clementvassant.standbyclock.ui.TAG_BATTERY
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Le prochain réveil sur la ligne de la date, tel que l'app Horloge le programme : les alarmes sont posées par
 * l'intention publique `SET_ALARM` (sans écran), comme le ferait l'utilisateur, et l'horloge suit sans être relancée.
 * Les données de l'app Horloge sont effacées avant et après : aucune alarme d'un autre test ne vient s'intercaler.
 */
@RunWith(AndroidJUnit4::class)
class AlarmJourneyTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var prefs: ClockPrefs

    @Before
    fun freshStart() {
        prefs = E2e.freshStart()
        clearAlarms()
        shell("dumpsys battery set ac 1")
        shell("dumpsys battery set level 76")
    }

    @After
    fun restore() {
        clearAlarms()
        shell("dumpsys battery reset")
    }

    @Test
    fun alarmWithin24HoursShowsAndFollowsTheNextOne() = uiAutomator {
        val later = setAlarmIn(hours = 2)
        E2e.open(MainActivity::class.java)
        clockScreen()
        byTag(TAG_ALARM)
        byText(later)
        capture("reveil-dans-24h")

        // Une alarme plus proche, horloge affichée : l'heure change sans relancer l'app.
        val sooner = setAlarmIn(hours = 1)
        byText(sooner)
        capture("reveil-plus-proche")
    }

    @Test
    fun alarmBeyond24HoursIsHidden() = uiAutomator {
        // Une alarme répétée un seul jour, dans trois jours : la prochaine sonnerie est au-delà de 24 heures.
        setWeeklyAlarm(LocalDateTime.now().plusDays(DAYS_AHEAD).plusHours(1))
        E2e.open(MainActivity::class.java)
        clockScreen()
        byTag(TAG_BATTERY)
        assertTrue("Réveil affiché alors qu'il sonne dans plus de 24 h", device.wait(Until.gone(By.res(TAG_ALARM)), E2e.SCREEN_TIMEOUT_MS))
        capture("reveil-au-dela-24h")
    }

    @Test
    fun atNightOnlyTheAlarmAndALowBatteryStay() = uiAutomator {
        prefs.themeMode = ThemeMode.NIGHT
        val alarm = setAlarmIn(hours = 2)
        E2e.open(MainActivity::class.java)
        clockScreen()
        byText(alarm)
        assertTrue("Batterie en charge affichée la nuit", device.wait(Until.gone(By.res(TAG_BATTERY)), E2e.SCREEN_TIMEOUT_MS))
        capture("reveil-nuit")

        // Débranchée et faible, la batterie revient même la nuit.
        shell("dumpsys battery unplug")
        shell("dumpsys battery set level 12")
        byTag(TAG_BATTERY)
    }

    /** Pose une alarme dans [hours] heures et rend l'heure attendue à l'écran (« 07:00 »). */
    private fun setAlarmIn(hours: Long): String {
        val time = LocalDateTime.now().plusHours(hours)
        shell("am start -a android.intent.action.SET_ALARM $SKIP_UI ${hourAndMinutes(time)}")
        return time.format(ALARM_FORMAT)
    }

    private fun setWeeklyAlarm(time: LocalDateTime) {
        val days = "--eial $EXTRA_DAYS ${calendarDay(time.dayOfWeek)}"
        shell("am start -a android.intent.action.SET_ALARM $SKIP_UI ${hourAndMinutes(time)} $days")
    }

    private fun hourAndMinutes(time: LocalDateTime) = "--ei $EXTRA_HOUR ${time.hour} --ei $EXTRA_MINUTES ${time.minute}"

    /** Les jours de `java.util.Calendar` : dimanche = 1, samedi = 7. */
    private fun calendarDay(day: DayOfWeek) = day.value % DayOfWeek.SUNDAY.value + 1

    /** Efface les alarmes de l'app Horloge (émulateur seulement : ses alarmes ne servent qu'aux tests). */
    private fun clearAlarms() {
        val clockApp = shell("cmd package resolve-activity --brief -a android.intent.action.SET_ALARM").lines()
            .lastOrNull { "/" in it }?.substringBefore("/")?.trim()
        if (clockApp != null) shell("pm clear $clockApp")
    }

    private fun shell(command: String): String = device.executeShellCommand(command)

    private companion object {
        const val SKIP_UI = "--ez android.intent.extra.alarm.SKIP_UI true"
        const val EXTRA_HOUR = "android.intent.extra.alarm.HOUR"
        const val EXTRA_MINUTES = "android.intent.extra.alarm.MINUTES"
        const val EXTRA_DAYS = "android.intent.extra.alarm.DAYS"
        const val DAYS_AHEAD = 3L
        val ALARM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
