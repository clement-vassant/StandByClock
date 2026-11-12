package fr.clementvassant.standbyclock

/**
 * Délai restant avant l'extinction automatique, en millisecondes, ou null si elle est désactivée (0 minute).
 * Partagé par l'app plein écran et l'écran de veille : pas d'image fixe pendant des heures (anti-marquage OLED).
 * Les instants viennent de la même horloge monotone (elapsedRealtime) ; un délai déjà écoulé donne 0,
 * pour éteindre tout de suite au retour sur l'horloge.
 */
fun autoOffDelayMillis(launchedAt: Long, autoOffMinutes: Int, now: Long): Long? {
    if (autoOffMinutes <= 0) return null
    return (launchedAt + autoOffMinutes * MILLIS_PER_MINUTE - now).coerceAtLeast(0L)
}
