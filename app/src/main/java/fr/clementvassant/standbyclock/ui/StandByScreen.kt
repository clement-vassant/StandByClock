package fr.clementvassant.standbyclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import fr.clementvassant.standbyclock.AgendaLines
import fr.clementvassant.standbyclock.CalendarEvent
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.DeviceStatus
import fr.clementvassant.standbyclock.MILLIS_PER_MINUTE
import fr.clementvassant.standbyclock.MILLIS_PER_SECOND
import fr.clementvassant.standbyclock.NowPlaying
import fr.clementvassant.standbyclock.SECONDS_PER_MINUTE
import fr.clementvassant.standbyclock.StatusLine
import fr.clementvassant.standbyclock.ThemeMode
import fr.clementvassant.standbyclock.agendaLines
import fr.clementvassant.standbyclock.rememberCalendarEvents
import fr.clementvassant.standbyclock.rememberDeviceStatus
import fr.clementvassant.standbyclock.rememberNowPlaying
import fr.clementvassant.standbyclock.statusLine
import fr.clementvassant.standbyclock.todayAgenda
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Largeur relative de l'horloge ; le panneau d'à côté (calendrier, musique) a une largeur de 1. */
const val CLOCK_PANE_WEIGHT = 1.4f

/** Hauteur relative de la barre musique sous l'horloge (l'horloge a une hauteur de 1). */
const val MEDIA_BAR_WEIGHT = 0.28f

/** Largeur de la barre musique, en fraction de celle de l'horloge : elle reste groupée sous l'heure. */
const val MEDIA_BAR_WIDTH_FRACTION = 0.9f

/** Orbite anti-marquage : positions par tour, tours pour aller du centre au bord, rayons min et max (dp). */
private const val ORBIT_POSITIONS_PER_TURN = 12
private const val ORBIT_TURNS = 4
private const val ORBIT_MIN_RADIUS_DP = 4f
private const val ORBIT_MAX_RADIUS_DP = 20f

/**
 * L'écran de l'horloge, partagé par l'app plein écran et l'écran de veille : horloge (3 styles au balayage),
 * calendrier du mois et musique en cours, jour ou nuit, avec les protections anti-marquage.
 *
 * @param onOpenSettings double toucher ; null dans l'écran de veille, qui a son propre bouton de réglages.
 * @param clock heure utilisée (injectable pour les tests).
 * @param nowPlayingSource musique en cours (injectable pour les tests).
 * @param eventsSource événements de l'agenda autour d'un jour (injectable pour les tests).
 * @param deviceStatusSource batterie et prochain réveil (injectable pour les tests).
 */
@Composable
fun StandByScreen(
    prefs: ClockPrefs,
    forcedTheme: ThemeMode?,
    onNightModeChange: (Boolean) -> Unit,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemDefaultZone(),
    nowPlayingSource: @Composable (enabled: Boolean) -> NowPlaying? = { rememberNowPlaying(it).value },
    eventsSource: @Composable (enabled: Boolean, day: LocalDate) -> List<CalendarEvent> = { enabled, day ->
        rememberCalendarEvents(enabled, day).value
    },
    deviceStatusSource: @Composable () -> DeviceStatus = { rememberDeviceStatus().value }
) {
    // La nuit, rien n'affiche les secondes : on ne se réveille qu'une fois par minute (économie d'énergie).
    var minuteTicks by remember { mutableStateOf(false) }
    val now by rememberTicker(everyMinute = minuteTicks, clock = clock)
    // Les réglages peuvent changer pendant que l'horloge est en arrière-plan : on les relit à chaque reprise.
    val resumes = rememberResumeCount()
    val themeMode = remember(forcedTheme, resumes) { forcedTheme ?: prefs.themeMode }
    var showCalendar by remember(resumes) { mutableStateOf(prefs.showCalendar) }
    val showMedia = remember(resumes) { prefs.showMedia }
    // Relancé à chaque reprise : l'accès aux notifications a pu être accordé entre-temps.
    val nowPlaying = key(resumes) { nowPlayingSource(showMedia) }

    // Un toucher inverse jour/nuit jusqu'à la prochaine bascule programmée.
    val scheduledNight = prefs.isNight(themeMode, now.toLocalTime())
    var manualNight by remember(scheduledNight, themeMode) { mutableStateOf<Boolean?>(null) }
    val night = manualNight ?: scheduledNight
    val currentNight by rememberUpdatedState(night)
    val currentOnNightModeChange by rememberUpdatedState(onNightModeChange)
    LaunchedEffect(night) {
        minuteTicks = night
        currentOnNightModeChange(night)
    }
    LaunchedEffect(showCalendar) { prefs.showCalendar = showCalendar }

    // Événements du jour : jamais la nuit (le mois seul, moins de pixels allumés), donc pas de lecture de l'agenda.
    val showEvents = remember(resumes) { prefs.showEvents }
    val shownCalendars = remember(resumes) { prefs.shownCalendars }
    val events = eventsSource(showEvents && !night && shownCalendars.isNotEmpty(), now.toLocalDate())
    // Recalculé une fois par minute : un événement terminé disparaît, un autre passe en cours.
    val epochMinute = now.toEpochSecond() / SECONDS_PER_MINUTE
    val agenda = remember(events, shownCalendars, night, epochMinute) {
        if (night) NO_AGENDA else agendaLines(todayAgenda(events, now, shownCalendars))
    }

    val pager = rememberStylePager(prefs, resumes)

    val device = deviceStatusSource()
    val status = remember(device, night, epochMinute) { statusLine(device.battery, device.nextAlarmMillis, now, night) }

    // Musique en lecture (ou en pause depuis peu) : barre sous l'horloge, ou tout le panneau si le calendrier est masqué.
    val media = nowPlaying?.takeIf { it.isRecent(clock.millis()) }
    val state = ScreenState(now, if (night) NightColors else DayColors, night, showCalendar, media, agenda, status)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(TAG_SCREEN)
            // Les testTag deviennent des identifiants visibles d'UI Automator : les parcours e2e retrouvent l'écran
            // dans l'écran de veille, hors de l'app.
            .semantics {
                testTagsAsResourceId = true
                stateDescription = if (night) STATE_NIGHT else STATE_DAY
            }
            // Toucher : jour/nuit. Appui long : calendrier. Double toucher : réglages (dans l'app seulement).
            .pointerInput(onOpenSettings) {
                detectTapGestures(
                    onTap = { manualNight = !currentNight },
                    onLongPress = { showCalendar = !showCalendar },
                    onDoubleTap = if (onOpenSettings != null) { _: Offset -> onOpenSettings() } else null
                )
            }
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            ClockAndSidePane(ClockStyle.entries[page], state)
        }
    }
}

/** Ce que l'écran affiche à un instant donné. */
internal class ScreenState(
    val now: ZonedDateTime,
    val colors: ClockColors,
    val night: Boolean,
    val showCalendar: Boolean,
    val media: NowPlaying?,
    /** Événements du jour restants : la semaine et leur liste remplacent alors le mois. */
    val agenda: AgendaLines = NO_AGENDA,
    /** Batterie et prochain réveil, à côté de la date. */
    val status: StatusLine = StatusLine()
) {
    val hasSidePane get() = media != null || showCalendar
    val showsAgenda get() = showCalendar && agenda.count > 0
    val mediaPlacement get() = mediaPlacementFor(hasMedia = media != null, showCalendar = showCalendar)
}

/** Aucun événement à afficher : le panneau garde le mois. */
internal val NO_AGENDA = AgendaLines(emptyList(), 0)

/** Nombre de reprises de l'écran (onResume) : sert de clé pour relire les réglages. */
@Composable
private fun rememberResumeCount(): Int {
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    return resumes
}

/** Pager des styles d'horloge, synchronisé avec le style enregistré. */
@Composable
private fun rememberStylePager(prefs: ClockPrefs, resumes: Int): PagerState {
    val styles = ClockStyle.entries
    val pager = rememberPagerState(initialPage = prefs.style.ordinal) { styles.size }
    LaunchedEffect(resumes) {
        if (prefs.style.ordinal != pager.currentPage) pager.scrollToPage(prefs.style.ordinal)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { prefs.style = styles[it] }
    }
    return pager
}

/** Heures paires : horloge à gauche (en haut en portrait) ; heures impaires : à droite (en bas) (anti-marquage). */
fun clockOnLeft(hour: Int) = hour % 2 == 0

/**
 * Position anti-marquage (en dp) à la minute [epochMinute] : [ORBIT_POSITIONS_PER_TURN] positions par tour,
 * le rayon va de [ORBIT_MIN_RADIUS_DP] à [ORBIT_MAX_RADIUS_DP] puis revient en [ORBIT_TURNS] tours,
 * pour ne jamais rester deux minutes au même endroit.
 */
internal fun orbitOffset(epochMinute: Long): Offset {
    val steps = ORBIT_POSITIONS_PER_TURN * ORBIT_TURNS
    val phase = (epochMinute % (2L * steps)).toInt()
    val progress = if (phase < steps) phase else 2 * steps - phase
    val radius = ORBIT_MIN_RADIUS_DP + (ORBIT_MAX_RADIUS_DP - ORBIT_MIN_RADIUS_DP) * progress / steps
    val angle = 2 * PI * (epochMinute % ORBIT_POSITIONS_PER_TURN) / ORBIT_POSITIONS_PER_TURN
    return Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
}

/** Attente jusqu'au début de la prochaine seconde, ou de la prochaine minute si [everyMinute]. */
fun tickDelayMillis(everyMinute: Boolean, nowMillis: Long): Long {
    val period = if (everyMinute) MILLIS_PER_MINUTE else MILLIS_PER_SECOND
    return period - nowMillis % period
}

/** L'heure courante, rafraîchie au début de chaque seconde, ou de chaque minute si [everyMinute]. */
@Composable
fun rememberTicker(everyMinute: Boolean = false, clock: Clock = Clock.systemDefaultZone()) =
    produceState(initialValue = ZonedDateTime.now(clock), everyMinute, clock) {
        while (true) {
            value = ZonedDateTime.now(clock)
            delay(tickDelayMillis(everyMinute, clock.millis()))
        }
    }
