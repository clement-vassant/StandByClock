package fr.clementvassant.standbyclock

import android.content.Context
import androidx.core.content.edit
import java.time.LocalTime

/** Les styles d'horloge disponibles, dans l'ordre du balayage horizontal. */
enum class ClockStyle { DIGITAL, ANALOG, MINIMAL }

/** Jour ou nuit : AUTO suit la plage horaire de nuit réglée dans l'app. */
enum class ThemeMode { AUTO, DAY, NIGHT }

/**
 * Orientation de l'app et de l'écran de veille : AUTO suit l'écran ; LANDSCAPE garde l'app en paysage et,
 * dans l'écran de veille (qui ne peut pas imposer le paysage), tourne l'horloge si l'écran est en portrait.
 */
enum class OrientationMode { AUTO, LANDSCAPE }

/** Fichier des préférences de l'app. */
const val PREFS_FILE = "standby_clock"

/** Préférences partagées entre l'activité, l'écran de veille et les réglages. */
class ClockPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var style: ClockStyle
        get() = ClockStyle.entries.getOrElse(prefs.getInt("style", 0)) { ClockStyle.DIGITAL }
        set(value) = prefs.edit { putInt("style", value.ordinal) }

    var showCalendar: Boolean
        get() = prefs.getBoolean("calendar", true)
        set(value) = prefs.edit { putBoolean("calendar", value) }

    var themeMode: ThemeMode
        get() = ThemeMode.entries.getOrElse(prefs.getInt("theme", 0)) { ThemeMode.AUTO }
        set(value) = prefs.edit { putInt("theme", value.ordinal) }

    /** Début de la nuit, en minutes depuis minuit (22:00 par défaut). */
    var nightStart: Int
        get() = prefs.getInt("night_start", 22 * 60)
        set(value) = prefs.edit { putInt("night_start", value) }

    /** Fin de la nuit, en minutes depuis minuit (07:00 par défaut). */
    var nightEnd: Int
        get() = prefs.getInt("night_end", 7 * 60)
        set(value) = prefs.edit { putInt("night_end", value) }

    /** Carte « musique en cours » à la place du calendrier (si l'accès aux notifications est accordé). */
    var showMedia: Boolean
        get() = prefs.getBoolean("media", true)
        set(value) = prefs.edit { putBoolean("media", value) }

    /** Événements du jour sous le calendrier (si la lecture de l'agenda est autorisée). */
    var showEvents: Boolean
        get() = prefs.getBoolean("events", true)
        set(value) = prefs.edit { putBoolean("events", value) }

    /**
     * Agendas dont on affiche les événements, cochés un par un dans les réglages. Aucun au départ, et un agenda
     * ajouté plus tard reste masqué : les agendas bavards (jours fériés, anniversaires) ne s'invitent pas.
     */
    var shownCalendars: Set<Long>
        get() = prefs.getStringSet("shown_calendars", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        set(value) = prefs.edit { putStringSet("shown_calendars", value.map { it.toString() }.toSet()) }

    /** La lecture de l'agenda a déjà été demandée : distingue un premier lancement d'un refus définitif. */
    var calendarAccessAsked: Boolean
        get() = prefs.getBoolean("calendar_access_asked", false)
        set(value) = prefs.edit { putBoolean("calendar_access_asked", value) }

    var orientation: OrientationMode
        get() = OrientationMode.entries.getOrElse(prefs.getInt("orientation", 0)) { OrientationMode.AUTO }
        set(value) = prefs.edit { putInt("orientation", value.ordinal) }

    /** Délai avant extinction automatique, en minutes ; 0 = jamais (2 h par défaut). */
    var autoOffMinutes: Int
        get() = prefs.getInt("auto_off", 120)
        set(value) = prefs.edit { putInt("auto_off", value) }

    fun isNight(mode: ThemeMode, time: LocalTime): Boolean = isNightAt(mode, time, nightStart, nightEnd)
}

/** Nuit selon le mode ; en AUTO, la plage [start, end[ (minutes depuis minuit) peut passer minuit. */
fun isNightAt(mode: ThemeMode, time: LocalTime, start: Int, end: Int): Boolean = when (mode) {
    ThemeMode.DAY -> false

    ThemeMode.NIGHT -> true

    ThemeMode.AUTO -> {
        val t = time.hour * MINUTES_PER_HOUR + time.minute
        if (start <= end) t in start until end else t >= start || t < end
    }
}

/** Nom d'extra des raccourcis : "day" ou "night" force le mode pour ce lancement. */
const val EXTRA_THEME = "theme"

fun themeFromExtra(value: String?): ThemeMode? = when (value) {
    "day" -> ThemeMode.DAY
    "night" -> ThemeMode.NIGHT
    else -> null
}
