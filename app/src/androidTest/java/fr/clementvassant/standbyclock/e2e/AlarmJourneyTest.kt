package fr.clementvassant.standbyclock.e2e

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.MainActivity
import fr.clementvassant.standbyclock.ThemeMode
import fr.clementvassant.standbyclock.ui.SystemHourFormatRule
import fr.clementvassant.standbyclock.ui.TAG_ALARM
import fr.clementvassant.standbyclock.ui.TAG_BATTERY
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Le prochain réveil sur la ligne de la date. Les alarmes sont programmées par `AlarmManager.setAlarmClock`, comme
 * le fait une app Horloge (l'image d'émulateur de la CI n'en a pas) : l'app les lit par le même chemin système et
 * suit sans être relancée. Elles sont annulées après chaque test.
 */
@RunWith(AndroidJUnit4::class)
class AlarmJourneyTest {
    // Heures attendues en 24 h, quelle que soit la langue de l'émulateur (celui de la CI est en 12 h).
    @get:Rule
    val hourFormat = SystemHourFormatRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val scheduled = mutableListOf<PendingIntent>()
    private lateinit var prefs: ClockPrefs

    @Before
    fun freshStart() {
        prefs = E2e.freshStart()
        shell("dumpsys battery set ac 1")
        shell("dumpsys battery set level 76")
    }

    @After
    fun restore() {
        scheduled.forEach(alarms::cancel)
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
        setAlarmAt(ZonedDateTime.now().plusDays(DAYS_AHEAD))
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

    /** Programme une alarme dans [hours] heures et rend l'heure attendue à l'écran (« 07:00 »). */
    private fun setAlarmIn(hours: Long): String {
        val time = ZonedDateTime.now().plusHours(hours)
        setAlarmAt(time)
        return time.format(ALARM_FORMAT)
    }

    private fun setAlarmAt(time: ZonedDateTime) {
        val code = scheduled.size
        val show = PendingIntent.getActivity(context, code, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        // Rien n'écoute cette sonnerie : elle tombe bien après la fin du test, qui l'annule.
        val ring = Intent(RING_ACTION).setPackage(context.packageName)
        val operation = PendingIntent.getBroadcast(context, code, ring, PendingIntent.FLAG_IMMUTABLE)
        alarms.setAlarmClock(AlarmManager.AlarmClockInfo(time.toInstant().toEpochMilli(), show), operation)
        scheduled += operation
    }

    private fun shell(command: String): String = device.executeShellCommand(command)

    private companion object {
        const val RING_ACTION = "fr.clementvassant.standbyclock.e2e.TEST_ALARM"
        const val DAYS_AHEAD = 3L
        val ALARM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
