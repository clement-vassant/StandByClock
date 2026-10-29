package fr.clementvassant.standbyclock.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import fr.clementvassant.standbyclock.MainActivity
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.ui.TAG_BATTERY
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La batterie sur la ligne de la date, telle que le système la donne : `dumpsys battery` simule le chargeur
 * et le niveau, et l'horloge suit sans être relancée.
 */
@RunWith(AndroidJUnit4::class)
class BatteryJourneyTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun freshStart() {
        E2e.freshStart()
    }

    @After
    fun realBattery() {
        device.executeShellCommand("dumpsys battery reset")
    }

    @Test
    fun batteryShowsWhileChargingOrWhenLow() = uiAutomator {
        battery("set ac 1", "set level 76")
        E2e.open(MainActivity::class.java)
        clockScreen()
        byTag(TAG_BATTERY)
        byText(percent(76))
        capture("batterie-en-charge")

        // Débranché et bien chargé : rien à signaler.
        battery("unplug", "set level 50")
        assertTrue("Batterie encore affichée hors charge à 50 %", device.wait(Until.gone(By.res(TAG_BATTERY)), E2e.SCREEN_TIMEOUT_MS))

        // Débranché et faible : la batterie revient.
        battery("set level 12")
        byText(percent(12))
        // Sous le seuil de batterie faible, l'interface système de l'émulateur plante et la capture de l'app sort
        // noire tant qu'elle n'est pas rouverte : on la rouvre pour une capture lisible.
        E2e.open(MainActivity::class.java)
        byText(percent(12))
        capture("batterie-faible")
    }

    /** Niveau tel que l'app l'écrit dans la langue de l'émulateur (« 76 % » ou « 76% »). */
    private fun percent(level: Int) = E2e.context.getString(R.string.status_battery_percent, level)

    private fun battery(vararg commands: String) = commands.forEach { device.executeShellCommand("dumpsys battery $it") }
}
