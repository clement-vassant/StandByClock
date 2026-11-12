package fr.clementvassant.standbyclock

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceStatusTest {
    private val paris = ZoneId.of("Europe/Paris")

    // Mercredi 28 octobre 2026, 20:53 à Paris.
    private val now = ZonedDateTime.of(2026, 10, 28, 20, 53, 0, 0, paris)

    private fun alarmIn(hours: Long, minutes: Long = 0) = now.plusHours(hours).plusMinutes(minutes).toInstant().toEpochMilli()

    private fun status(battery: BatteryStatus? = null, alarm: Long? = null, night: Boolean = false) = statusLine(battery, alarm, now, night)

    @Test
    fun `en charge le jour, la batterie s'affiche avec l'éclair`() {
        val line = status(BatteryStatus(percent = 76, charging = true))
        assertEquals(76, line.battery?.percent)
        assertEquals(true, line.battery?.charging)
        assertEquals(false, line.battery?.low)
    }

    @Test
    fun `hors charge, la batterie ne s'affiche que sous le seuil bas`() {
        assertNull(status(BatteryStatus(percent = LOW_BATTERY_PERCENT, charging = false)).battery)
        val low = status(BatteryStatus(percent = LOW_BATTERY_PERCENT - 1, charging = false)).battery
        assertEquals(19, low?.percent)
        assertEquals(true, low?.low)
        assertEquals(false, low?.charging)
    }

    @Test
    fun `la nuit, la charge se tait mais la batterie faible reste`() {
        assertNull(status(BatteryStatus(percent = 76, charging = true), night = true).battery)
        assertEquals(14, status(BatteryStatus(percent = 14, charging = false), night = true).battery?.percent)
    }

    @Test
    fun `batterie inconnue, rien ne s'affiche`() {
        assertNull(status(battery = null).battery)
    }

    @Test
    fun `un réveil dans les 24 heures s'affiche à son heure, jour et nuit`() {
        val alarm = alarmIn(hours = 10, minutes = 7)
        assertEquals("07:00", status(alarm = alarm).alarm)
        assertEquals("07:00", status(alarm = alarm, night = true).alarm)
    }

    @Test
    fun `téléphone en 12 heures, le réveil s'écrit comme un horaire de l'agenda`() {
        val morning = alarmIn(hours = 10, minutes = 7)
        val evening = alarmIn(hours = 20, minutes = 7)
        assertEquals("07:00 AM", statusLine(null, morning, now, night = false, HourFormat.H12).alarm)
        assertEquals("05:00 PM", statusLine(null, evening, now, night = true, HourFormat.H12).alarm)
    }

    @Test
    fun `un réveil au-delà de 24 heures ou passé ne s'affiche pas`() {
        assertEquals("20:53", status(alarm = alarmIn(hours = NEXT_ALARM_HORIZON_HOURS)).alarm)
        assertNull(status(alarm = alarmIn(hours = NEXT_ALARM_HORIZON_HOURS, minutes = 1)).alarm)
        assertNull(status(alarm = alarmIn(hours = 0, minutes = -1)).alarm)
        assertNull(status(alarm = null).alarm)
    }

    @Test
    fun `l'heure du réveil suit le fuseau de l'horloge`() {
        val utcAlarm = ZonedDateTime.of(2026, 10, 29, 6, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        assertEquals("07:00", status(alarm = utcAlarm).alarm)
    }

    @Test
    fun `rien à montrer, la ligne est vide`() {
        assertEquals(true, status().isEmpty)
        assertEquals(false, status(alarm = alarmIn(hours = 1)).isEmpty)
    }

    @Test
    fun `le niveau se calcule sur l'échelle donnée par le système`() {
        assertEquals(76, batteryPercent(level = 76, scale = 100))
        assertEquals(50, batteryPercent(level = 128, scale = 256))
        assertNull(batteryPercent(level = -1, scale = 100))
        assertNull(batteryPercent(level = 50, scale = 0))
    }
}
