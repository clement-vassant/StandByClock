package fr.clementvassant.standbyclock

import android.view.Window
import android.view.WindowManager

/** Luminosité de la fenêtre la nuit : le minimum, pour ne pas éblouir. */
const val NIGHT_BRIGHTNESS = 0.01f

/** Plafond de luminosité le jour dans l'app : un écran OLED s'use moins vite à faible luminosité. */
const val DAY_MAX_BRIGHTNESS = 0.35f

/** Aucune luminosité imposée : le système décide (niveau atténué d'un écran de veille, par exemple). */
const val SYSTEM_BRIGHTNESS = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

/** Où l'horloge s'affiche : l'app plein écran ou l'écran de veille. */
enum class ClockSurface { APP, SCREEN_SAVER }

/**
 * Luminosité de la fenêtre de l'horloge. La nuit, le minimum partout. Le jour, l'app plafonne à 35 % (usure OLED) ;
 * l'écran de veille laisse faire le système, qui l'atténue déjà (`isScreenBright = false`).
 */
fun windowBrightness(night: Boolean, surface: ClockSurface): Float = when {
    night -> NIGHT_BRIGHTNESS
    surface == ClockSurface.APP -> DAY_MAX_BRIGHTNESS
    else -> SYSTEM_BRIGHTNESS
}

fun Window.applyClockBrightness(night: Boolean, surface: ClockSurface) {
    attributes = attributes.apply { screenBrightness = windowBrightness(night, surface) }
}
