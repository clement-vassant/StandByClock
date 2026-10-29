package fr.clementvassant.standbyclock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.SECONDS_PER_MINUTE
import kotlin.math.roundToInt

/**
 * Une page du pager : l'horloge et son panneau, côte à côte en paysage, l'un au-dessus de l'autre en portrait.
 * Anti-marquage : le tout suit une orbite lente (nouvelle position chaque minute) et l'horloge échange
 * sa place avec le panneau chaque heure.
 */
@Composable
internal fun ClockAndSidePane(style: ClockStyle, state: ScreenState) {
    val epochMinute = state.now.toEpochSecond() / SECONDS_PER_MINUTE
    val shift = remember(epochMinute) { orbitOffset(epochMinute) }
    val clockFirst = clockOnLeft(state.now.hour)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = isPortrait(constraints.maxWidth, constraints.maxHeight)
        val orbit = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = if (portrait) 40.dp else 24.dp)
            .offset { IntOffset((shift.x * density).roundToInt(), (shift.y * density).roundToInt()) }
        if (portrait) {
            Column(orbit, horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.hasSidePane && !clockFirst) SidePane(state, Modifier.weight(1f).fillMaxWidth())
                ClockPane(style, state, portrait = true, modifier = Modifier.weight(CLOCK_PANE_WEIGHT).fillMaxWidth())
                if (state.hasSidePane && clockFirst) SidePane(state, Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            Row(orbit, verticalAlignment = Alignment.CenterVertically) {
                if (state.hasSidePane && !clockFirst) SidePane(state, Modifier.weight(1f).fillMaxHeight())
                ClockPane(style, state, portrait = false, modifier = Modifier.weight(CLOCK_PANE_WEIGHT).fillMaxHeight())
                if (state.hasSidePane && clockFirst) SidePane(state, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/**
 * L'horloge, avec la musique en cours en barre compacte dessous quand le calendrier est affiché.
 * La barre suit l'horloge (orbite, changement de côté) et réduit un peu ses chiffres ou son cadran.
 */
@Composable
private fun ClockPane(style: ClockStyle, state: ScreenState, portrait: Boolean, modifier: Modifier = Modifier) {
    val media = state.media?.takeIf { state.mediaPlacement == MediaPlacement.UNDER_CLOCK }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth().testTag(clockTag(style)), contentAlignment = Alignment.Center) {
            ClockFace(style, state, portrait, barUnderClock = media != null)
        }
        if (media != null) {
            Box(Modifier.weight(MEDIA_BAR_WEIGHT).fillMaxWidth(MEDIA_BAR_WIDTH_FRACTION), contentAlignment = Alignment.Center) {
                MediaBar(media, state.colors, state.night, Modifier.testTag(TAG_MEDIA_BAR))
            }
        }
    }
}

@Composable
private fun ClockFace(style: ClockStyle, state: ScreenState, portrait: Boolean, barUnderClock: Boolean) {
    val (now, colors, night) = Triple(state.now, state.colors, state.night)
    when (style) {
        ClockStyle.DIGITAL -> DigitalClock(now, colors, showSeconds = !night, hourFormat = state.hourFormat, status = state.status)

        ClockStyle.ANALOG -> AnalogClockWithDate(
            now,
            colors,
            showDate = !state.hasSidePane,
            showSeconds = !night,
            // La semaine et sa liste n'ont pas la hauteur du mois : le cadran remplit alors son panneau.
            matchCalendar = !portrait && !state.showsAgenda,
            // Barre musique sous l'horloge : le calendrier, lui, garde toute la hauteur du panneau.
            calendarHeight = if (barUnderClock) 1f + MEDIA_BAR_WEIGHT else 1f,
            status = state.status
        )

        ClockStyle.MINIMAL -> MinimalClock(now, colors, state.hourFormat, status = state.status)
    }
}

/**
 * Panneau à côté de l'horloge (ou dessous en portrait) : le calendrier du mois, ou la semaine et les
 * événements du jour qui restent ; s'il est masqué (appui long), la grande carte musique.
 */
@Composable
private fun SidePane(state: ScreenState, modifier: Modifier = Modifier) {
    val media = state.media?.takeIf { state.mediaPlacement == MediaPlacement.SIDE_CARD }
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            media != null -> MediaCard(media, state.colors, state.night, Modifier.testTag(TAG_MEDIA_CARD))
            state.showsAgenda -> WeekAgenda(state.now, state.agenda, state.colors, state.hourFormat)
            else -> MonthCalendar(state.now, state.colors)
        }
    }
}

/** Où afficher la musique en cours. */
enum class MediaPlacement { NONE, UNDER_CLOCK, SIDE_CARD }

/**
 * Calendrier affiché : barre compacte sous l'horloge, le panneau reste au calendrier.
 * Calendrier masqué : grande carte dans le panneau, l'horloge garde toute sa taille.
 */
fun mediaPlacementFor(hasMedia: Boolean, showCalendar: Boolean): MediaPlacement = when {
    !hasMedia -> MediaPlacement.NONE
    showCalendar -> MediaPlacement.UNDER_CLOCK
    else -> MediaPlacement.SIDE_CARD
}
