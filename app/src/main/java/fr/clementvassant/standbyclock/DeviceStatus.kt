package fr.clementvassant.standbyclock

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/** Sous ce niveau (en %), la batterie s'affiche même téléphone débranché. */
const val LOW_BATTERY_PERCENT = 20

/** Un réveil ne s'affiche que s'il sonne dans ce délai (en heures) : au-delà, c'est du texte allumé pour rien. */
const val NEXT_ALARM_HORIZON_HOURS = 24L

private const val FULL_PERCENT = 100

/** Niveau de la batterie (en %) et branchement sur un chargeur. */
data class BatteryStatus(val percent: Int, val charging: Boolean)

/** Ce que le système dit de la batterie et du prochain réveil (heure murale en ms), lu sans autorisation. */
data class DeviceStatus(val battery: BatteryStatus? = null, val nextAlarmMillis: Long? = null)

/** La batterie telle qu'affichée : « 76 % », avec l'éclair en charge, en accent si faible. */
data class BatteryLabel(val text: String, val charging: Boolean, val low: Boolean)

/** Batterie et réveil à afficher à côté de la date ; null pour ce qui reste caché. */
data class StatusLine(val battery: BatteryLabel? = null, val alarm: String? = null) {
    val isEmpty get() = battery == null && alarm == null
}

/**
 * Batterie : en charge (le jour seulement : la nuit on ne veut que l'essentiel), ou faible hors charge.
 * Réveil : seulement s'il sonne dans les [NEXT_ALARM_HORIZON_HOURS] heures, jour et nuit.
 */
fun statusLine(
    battery: BatteryStatus?,
    nextAlarmMillis: Long?,
    now: ZonedDateTime,
    night: Boolean,
    hourFormat: HourFormat = HourFormat.H24
): StatusLine {
    val low = battery != null && !battery.charging && battery.percent < LOW_BATTERY_PERCENT
    val shownBattery = battery?.takeIf { low || (it.charging && !night) }
    return StatusLine(
        battery = shownBattery?.let { BatteryLabel("${it.percent} %", charging = it.charging, low = low) },
        alarm = nextAlarmMillis?.let { nextAlarmLabel(it, now, hourFormat) }
    )
}

/**
 * Heure du réveil dans le fuseau de l'horloge, s'il sonne dans les 24 heures ; sinon null. Écrite comme un horaire
 * de l'agenda : « 07:00 », ou « 07:00 AM » en 12 h.
 */
fun nextAlarmLabel(alarmMillis: Long, now: ZonedDateTime, hourFormat: HourFormat = HourFormat.H24): String? {
    val alarm = Instant.ofEpochMilli(alarmMillis).atZone(now.zone)
    val delay = Duration.between(now, alarm)
    if (delay.isNegative || delay > Duration.ofHours(NEXT_ALARM_HORIZON_HOURS)) return null
    return scheduleTime(alarm.hour, alarm.minute, hourFormat)
}

/** Niveau en % à partir des extras de l'état de la batterie ; null s'ils manquent. */
fun batteryPercent(level: Int, scale: Int): Int? =
    if (level < 0 || scale <= 0) null else (level * FULL_PERCENT / scale).coerceIn(0, FULL_PERCENT)

/**
 * Suit la batterie et le prochain réveil. Les deux viennent de diffusions du système qui ne demandent
 * aucune autorisation : l'état de la batterie (collant, donc connu dès l'abonnement) et le changement
 * de prochain réveil programmé dans l'app Horloge.
 */
@Composable
fun rememberDeviceStatus(): State<DeviceStatus> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(DeviceStatus()) }
    DisposableEffect(context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        fun nextAlarm() = alarms?.nextAlarmClock?.triggerTime
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                state.value = when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> state.value.copy(battery = intent.batteryStatus())
                    else -> state.value.copy(nextAlarmMillis = nextAlarm())
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        // Diffusions du système seulement : aucune autre app n'a à nous écrire.
        val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        state.value = DeviceStatus(sticky?.batteryStatus(), nextAlarm())
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

private fun Intent.batteryStatus(): BatteryStatus? {
    val percent = batteryPercent(getIntExtra(BatteryManager.EXTRA_LEVEL, -1), getIntExtra(BatteryManager.EXTRA_SCALE, -1))
    return percent?.let { BatteryStatus(it, charging = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0) }
}
