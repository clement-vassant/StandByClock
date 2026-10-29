package fr.clementvassant.standbyclock

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fr.clementvassant.standbyclock.ui.DayColors
import fr.clementvassant.standbyclock.ui.TAG_CALENDAR_SWITCH
import fr.clementvassant.standbyclock.ui.TAG_EVENTS_SWITCH
import fr.clementvassant.standbyclock.ui.TAG_SETTINGS
import fr.clementvassant.standbyclock.ui.calendarChoiceTag

/**
 * Réglages : style, calendrier, événements du jour, jour/nuit, plage horaire de nuit, orientation et extinction automatique.
 * Ouvert aussi par le ⚙ de l'écran de veille.
 */
class SettingsActivity : ComponentActivity() {
    // Au retour de la demande, l'écran revérifie l'accès (reprise de l'activité) : rien à faire ici.
    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /**
     * Déjà demandé et Android ne propose plus d'explication : la demande a été refusée pour de bon et ne
     * s'afficherait plus, on ouvre donc la fiche de l'app. On décide avant de demander : fermer la fenêtre
     * sans répondre n'est pas un refus, et ne doit pas envoyer dans les réglages.
     */
    private fun requestCalendarAccess(prefs: ClockPrefs) {
        if (prefs.calendarAccessAsked && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            return
        }
        prefs.calendarAccessAsked = true
        calendarPermission.launch(Manifest.permission.READ_CALENDAR)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = ClockPrefs(this)
        setContent {
            SettingsTheme {
                SettingsScreen(
                    prefs = prefs,
                    pickTime = ::pickTime,
                    onLaunch = { startActivity(Intent(this, MainActivity::class.java)) },
                    onOpenScreenSaver = ::openScreenSaverSettings,
                    hasMediaAccess = { MediaListenerService.isGranted(this) },
                    onOpenNotificationAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    calendarSource = CalendarSource(
                        hasAccess = { DeviceCalendar.hasAccess(this) },
                        requestAccess = { requestCalendarAccess(prefs) },
                        calendars = { DeviceCalendar.calendars(this) }
                    )
                )
            }
        }
    }

    // Même écriture que le reste du téléphone : cadran 12 h avec AM/PM, ou 24 h.
    private fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m ->
            onPicked(h * MINUTES_PER_HOUR + m)
        }, initial / MINUTES_PER_HOUR, initial % MINUTES_PER_HOUR, DateFormat.is24HourFormat(this)).show()
    }

    private fun openScreenSaverSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
    }
}

/** Thème sombre des réglages, partagé avec leurs captures de référence (SettingsScreenshotTest). */
@Composable
internal fun SettingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = DayColors.accent, background = Color.Black, surface = Color.Black)) {
        Surface(Modifier.fillMaxSize(), content = content)
    }
}

@Composable
internal fun SettingsScreen(
    prefs: ClockPrefs,
    pickTime: (Int, (Int) -> Unit) -> Unit,
    onLaunch: () -> Unit,
    onOpenScreenSaver: () -> Unit,
    hasMediaAccess: () -> Boolean,
    onOpenNotificationAccess: () -> Unit,
    calendarSource: CalendarSource
) {
    var style by remember { mutableStateOf(prefs.style) }
    var calendar by remember { mutableStateOf(prefs.showCalendar) }
    var theme by remember { mutableStateOf(prefs.themeMode) }
    var nightStart by remember { mutableIntStateOf(prefs.nightStart) }
    var nightEnd by remember { mutableIntStateOf(prefs.nightEnd) }
    var autoOff by remember { mutableIntStateOf(prefs.autoOffMinutes) }
    var media by remember { mutableStateOf(prefs.showMedia) }
    var orientation by remember { mutableStateOf(prefs.orientation) }
    // Revérifiés au retour des réglages Android.
    var mediaAccess by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hourFormat by remember { mutableStateOf(systemHourFormat(context)) }
    val currentHasMediaAccess by rememberUpdatedState(hasMediaAccess)
    LifecycleResumeEffect(Unit) {
        mediaAccess = currentHasMediaAccess()
        hourFormat = systemHourFormat(context)
        onPauseOrDispose { }
    }

    Column(
        Modifier
            // Identifiants visibles des parcours e2e (UI Automator), comme sur l'écran de l'horloge.
            .semantics { testTagsAsResourceId = true }
            .testTag(TAG_SETTINGS)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        Section(stringResource(R.string.settings_style)) {
            Choices(
                options = listOf(
                    ClockStyle.DIGITAL to stringResource(R.string.style_digital),
                    ClockStyle.ANALOG to stringResource(R.string.style_analog),
                    ClockStyle.MINIMAL to stringResource(R.string.style_minimal)
                ),
                selected = style
            ) {
                style = it
                prefs.style = it
            }
        }

        // Le calendrier n'a pas de côté fixe : il échange sa place avec l'horloge chaque heure (anti-marquage OLED).
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_show_calendar))
                Text(
                    stringResource(R.string.settings_show_calendar_help),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = calendar, modifier = Modifier.testTag(TAG_CALENDAR_SWITCH), onCheckedChange = {
                calendar = it
                prefs.showCalendar = it
            })
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_show_media), Modifier.weight(1f))
            Switch(checked = media, onCheckedChange = {
                media = it
                prefs.showMedia = it
            })
        }
        if (media && !mediaAccess) {
            Text(
                stringResource(R.string.settings_media_access_help),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onOpenNotificationAccess) { Text(stringResource(R.string.settings_media_access_button)) }
        }

        EventsSettings(prefs, calendarSource)

        Section(stringResource(R.string.settings_day_night)) {
            Choices(
                options = listOf(
                    ThemeMode.AUTO to stringResource(R.string.theme_auto),
                    ThemeMode.DAY to stringResource(R.string.theme_day),
                    ThemeMode.NIGHT to stringResource(R.string.theme_night)
                ),
                selected = theme
            ) {
                theme = it
                prefs.themeMode = it
            }
            if (theme == ThemeMode.AUTO) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_night_from))
                    OutlinedButton(onClick = {
                        pickTime(nightStart) {
                            nightStart = it
                            prefs.nightStart = it
                        }
                    }) { Text(scheduleTime(nightStart, hourFormat)) }
                    Text(stringResource(R.string.settings_night_to))
                    OutlinedButton(onClick = {
                        pickTime(nightEnd) {
                            nightEnd = it
                            prefs.nightEnd = it
                        }
                    }) { Text(scheduleTime(nightEnd, hourFormat)) }
                }
            }
        }

        Section(stringResource(R.string.settings_orientation)) {
            Choices(
                options = listOf(
                    OrientationMode.AUTO to stringResource(R.string.orientation_auto),
                    OrientationMode.LANDSCAPE to stringResource(R.string.orientation_landscape)
                ),
                selected = orientation
            ) {
                orientation = it
                prefs.orientation = it
            }
            Text(
                stringResource(
                    if (orientation == OrientationMode.AUTO) {
                        R.string.settings_orientation_auto_help
                    } else {
                        R.string.settings_orientation_landscape_help
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Section(stringResource(R.string.settings_auto_off)) {
            Choices(
                options = AUTO_OFF_HOURS.map { it * MINUTES_PER_HOUR to stringResource(R.string.auto_off_hours, it) } +
                    (0 to stringResource(R.string.auto_off_never)),
                selected = autoOff
            ) {
                autoOff = it
                prefs.autoOffMinutes = it
            }
        }

        Button(onClick = onLaunch, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_launch)) }

        Section(stringResource(R.string.settings_auto_launch)) {
            Text(stringResource(R.string.help_screen_saver), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenScreenSaver) { Text(stringResource(R.string.settings_open_screen_saver)) }
            Text(stringResource(R.string.help_routines), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Durées proposées pour l'extinction automatique, en heures (« Jamais » s'ajoute à la fin). */
private val AUTO_OFF_HOURS = listOf(1, 2, 4, 8)

/** Accès à l'agenda de l'appareil, fourni par l'activité (remplacé dans les tests). */
internal class CalendarSource(val hasAccess: () -> Boolean, val requestAccess: () -> Unit, val calendars: () -> List<CalendarInfo>)

/** Événements du jour : interrupteur, autorisation de lire l'agenda, puis choix des agendas affichés. */
@Composable
private fun EventsSettings(prefs: ClockPrefs, source: CalendarSource) {
    var show by remember { mutableStateOf(prefs.showEvents) }
    var shown by remember { mutableStateOf(prefs.shownCalendars) }
    var access by remember { mutableStateOf(false) }
    var calendars by remember { mutableStateOf(emptyList<CalendarInfo>()) }
    // Revérifié au retour de la demande d'autorisation ou des réglages Android.
    val currentSource by rememberUpdatedState(source)
    LifecycleResumeEffect(Unit) {
        access = currentSource.hasAccess()
        calendars = if (access) currentSource.calendars() else emptyList()
        onPauseOrDispose { }
    }

    // Un seul bloc émis : interrupteur, puis autorisation ou choix des agendas.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_show_events))
                Text(
                    stringResource(R.string.settings_show_events_help),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = show, modifier = Modifier.testTag(TAG_EVENTS_SWITCH), onCheckedChange = {
                show = it
                prefs.showEvents = it
            })
        }
        when {
            !show -> Unit

            !access -> {
                Text(stringResource(R.string.settings_calendar_access_help), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = source.requestAccess) { Text(stringResource(R.string.settings_calendar_access_button)) }
            }

            else -> Section(stringResource(R.string.settings_shown_calendars)) {
                Text(
                    stringResource(R.string.settings_shown_calendars_help),
                    style = MaterialTheme.typography.bodySmall
                )
                if (calendars.isEmpty()) Text(stringResource(R.string.settings_no_calendars), style = MaterialTheme.typography.bodyMedium)
                for (calendar in calendars) {
                    // Toute la ligne se coche, libellé compris : la case seule est une cible trop petite.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = calendar.id in shown, role = Role.Checkbox) { checked ->
                                shown = if (checked) shown + calendar.id else shown - calendar.id
                                prefs.shownCalendars = shown
                            }
                            .testTag(calendarChoiceTag(calendar.id)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = calendar.id in shown, onCheckedChange = null)
                        Text(calendar.label())
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun <T> Choices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((value, label) in options) {
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}
