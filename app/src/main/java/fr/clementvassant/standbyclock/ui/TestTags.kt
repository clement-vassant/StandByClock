package fr.clementvassant.standbyclock.ui

/** Repères pour les tests d'interface. */
const val TAG_SCREEN = "standby"
const val TAG_SETTINGS = "settings"
const val TAG_CALENDAR = "calendar"
const val TAG_DIAL = "dial"
const val TAG_MEDIA_BAR = "media-bar"
const val TAG_MEDIA_CARD = "media-card"
const val TAG_CALENDAR_SWITCH = "calendar-switch"
const val TAG_AGENDA = "agenda"
const val TAG_EVENTS_SWITCH = "events-switch"
const val TAG_BATTERY = "battery"
const val TAG_ALARM = "alarm"

/** Description d'état de l'écran de l'horloge, lue par les lecteurs d'écran et les parcours e2e. */
const val STATE_DAY = "jour"
const val STATE_NIGHT = "nuit"

fun calendarChoiceTag(id: Long) = "calendar-choice-$id"

fun clockTag(style: fr.clementvassant.standbyclock.ClockStyle) = "clock-$style"
