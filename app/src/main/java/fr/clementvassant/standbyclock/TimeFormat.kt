package fr.clementvassant.standbyclock

import android.content.Context
import android.text.format.DateFormat

/** Heures par demi-journée, en écriture 12 h (midi et minuit s'écrivent 12). */
private const val HOURS_PER_HALF_DAY = 12

/** Écriture des heures choisie dans les réglages d'Android : l'app la suit au lieu d'imposer le 24 h. */
enum class HourFormat { H12, H24 }

/** Format du téléphone (Réglages › Date et heure). Relu à chaque reprise : il peut changer pendant que l'app tourne. */
fun systemHourFormat(context: Context): HourFormat = if (DateFormat.is24HourFormat(context)) HourFormat.H24 else HourFormat.H12

/**
 * Chiffres des heures, toujours deux : « 09 » ou « 14 » en 24 h, « 09 » ou « 02 » en 12 h (voulu :
 * la largeur de l'horloge ne bouge jamais), sans AM/PM.
 */
fun clockHour(hour: Int, format: HourFormat): String = when (format) {
    HourFormat.H24 -> "%02d".format(hour)
    HourFormat.H12 -> "%02d".format((hour % HOURS_PER_HALF_DAY).let { if (it == 0) HOURS_PER_HALF_DAY else it })
}

/** Heure de la grande horloge : « 14:05 » ou « 02:05 ». Pas d'AM/PM : jour et nuit se voient déjà aux couleurs. */
fun clockTime(hour: Int, minute: Int, format: HourFormat): String = "${clockHour(hour, format)}:%02d".format(minute)

/**
 * Heure d'un horaire (agenda, plage de nuit) : « 17:30 » ou « 05:30 PM ». Ici AM/PM est utile : un horaire isolé
 * ne dit pas s'il est du matin ou du soir.
 */
fun scheduleTime(hour: Int, minute: Int, format: HourFormat): String = when (format) {
    HourFormat.H24 -> clockTime(hour, minute, format)
    HourFormat.H12 -> "${clockTime(hour, minute, format)} ${if (hour < HOURS_PER_HALF_DAY) "AM" else "PM"}"
}

/** Heure d'un horaire donné en minutes depuis minuit (plage de nuit des réglages). */
fun scheduleTime(minutesOfDay: Int, format: HourFormat): String =
    scheduleTime(minutesOfDay / MINUTES_PER_HOUR, minutesOfDay % MINUTES_PER_HOUR, format)
