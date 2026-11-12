package fr.clementvassant.standbyclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import fr.clementvassant.standbyclock.BatteryLabel
import fr.clementvassant.standbyclock.R
import fr.clementvassant.standbyclock.StatusLine

/** Les icônes sont dessinées sur une grille de 24 × 24, un peu plus grandes que le texte qu'elles précèdent. */
private const val ICON_GRID = 24f
private const val ICON_TO_TEXT = 1.1f
private const val ICON_STROKE = 1.8f

/** Espace entre une icône et son texte, en fraction de la taille de l'icône. */
private const val ICON_GAP = 0.2f

/**
 * Batterie et prochain réveil, sur la ligne de la date (maquette A) : texte et icônes au trait, sans fond,
 * dans la couleur secondaire ; la batterie faible passe en accent.
 */
@Composable
internal fun StatusRow(status: StatusLine, colors: ClockColors, fontSize: TextUnit, modifier: Modifier = Modifier) {
    if (status.isEmpty) return
    val iconSize = fontSize.asDp() * ICON_TO_TEXT
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(iconSize * 0.4f)) {
        status.battery?.let { BatteryItem(it, colors, fontSize, iconSize) }
        status.alarm?.let { alarm ->
            val alarmDescription = stringResource(R.string.status_next_alarm, alarm)
            if (status.battery != null) Text("·", color = colors.secondary, fontSize = fontSize)
            Row(
                Modifier.testTag(TAG_ALARM).semantics(mergeDescendants = true) { contentDescription = alarmDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(iconSize * ICON_GAP)
            ) {
                AlarmIcon(colors.secondary, Modifier.size(iconSize))
                Text(alarm, color = colors.secondary, fontSize = fontSize)
            }
        }
    }
}

@Composable
private fun BatteryItem(battery: BatteryLabel, colors: ClockColors, fontSize: TextUnit, iconSize: Dp) {
    val color = if (battery.low) colors.accent else colors.secondary
    val state = stringResource(if (battery.charging) R.string.status_charging else R.string.status_low_battery)
    Row(
        Modifier.testTag(TAG_BATTERY).semantics(mergeDescendants = true) { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(iconSize * ICON_GAP)
    ) {
        BatteryIcon(color, battery.charging, battery.low, Modifier.size(iconSize))
        Text(stringResource(R.string.status_battery_percent, battery.percent), color = color, fontSize = fontSize)
    }
}

/** Pile au trait ; un éclair en charge, une seule barre quand elle est faible. */
@Composable
private fun BatteryIcon(color: Color, charging: Boolean, low: Boolean, modifier: Modifier = Modifier) = Canvas(modifier) {
    val unit = size.minDimension / ICON_GRID
    val stroke = Stroke(width = ICON_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(color, Offset(2 * unit, 7 * unit), Size(17 * unit, 10 * unit), CornerRadius(2 * unit), style = stroke)
    drawLine(color, Offset(22 * unit, 11 * unit), Offset(22 * unit, 13 * unit), stroke.width, StrokeCap.Round)
    if (charging) {
        val bolt = Path().apply {
            moveTo(11.5f * unit, 9 * unit)
            lineTo(9 * unit, 12 * unit)
            lineTo(12 * unit, 12 * unit)
            lineTo(9.5f * unit, 15 * unit)
        }
        drawPath(bolt, color, style = stroke)
    } else if (low) {
        drawLine(color, Offset(5 * unit, 10 * unit), Offset(5 * unit, 14 * unit), stroke.width, StrokeCap.Round)
    }
}

/** Réveil au trait : cadran, aiguilles et deux cloches. */
@Composable
private fun AlarmIcon(color: Color, modifier: Modifier = Modifier) = Canvas(modifier) {
    val unit = size.minDimension / ICON_GRID
    val width = ICON_STROKE * unit
    drawCircle(color, radius = 7 * unit, center = Offset(12 * unit, 13 * unit), style = Stroke(width))
    drawLine(color, Offset(12 * unit, 10 * unit), Offset(12 * unit, 13 * unit), width, StrokeCap.Round)
    drawLine(color, Offset(12 * unit, 13 * unit), Offset(14 * unit, 14.5f * unit), width, StrokeCap.Round)
    drawLine(color, Offset(5 * unit, 4 * unit), Offset(3 * unit, 6 * unit), width, StrokeCap.Round)
    drawLine(color, Offset(19 * unit, 4 * unit), Offset(21 * unit, 6 * unit), width, StrokeCap.Round)
}

/**
 * La date suivie de la batterie et du réveil, sur une ligne ; sur un écran étroit, batterie et réveil
 * passent à la ligne au lieu d'écraser la date.
 */
@Composable
internal fun DateAndStatus(textSize: TextUnit, content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(textSize.asDp(), Alignment.CenterHorizontally),
        itemVerticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/** Convertit une taille de texte en dp, pour espacer les éléments d'une ligne en proportion du texte. */
@Composable
internal fun TextUnit.asDp(): Dp = with(LocalDensity.current) { toDp() }
