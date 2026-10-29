package fr.clementvassant.standbyclock

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import fr.clementvassant.standbyclock.ui.StandByScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Luminosité de la fenêtre la nuit : le minimum, pour ne pas éblouir. */
private const val NIGHT_BRIGHTNESS = 0.01f

/** Plafond de luminosité le jour : un écran OLED s'use moins vite à faible luminosité. */
private const val DAY_MAX_BRIGHTNESS = 0.35f

/** Horloge plein écran, écran maintenu allumé ; s'éteint seule après le délai réglé. */
class MainActivity : ComponentActivity() {

    /** Mode imposé par le lancement (raccourci « Horloge jour/nuit » de l'appui long), sinon les réglages. */
    private val forcedTheme = mutableStateOf<ThemeMode?>(null)

    private lateinit var prefs: ClockPrefs

    /** Instant du lancement, pour l'extinction automatique (le délai se relit à chaque reprise). */
    private var launchedAt = 0L
    private var autoOff: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        launchedAt = SystemClock.elapsedRealtime()
        forcedTheme.value = themeFromLaunch(intent)
        prefs = ClockPrefs(this)
        requestedOrientation = activityOrientation(prefs.orientation)
        setContent {
            StandByScreen(
                prefs = prefs,
                forcedTheme = forcedTheme.value,
                onNightModeChange = ::applyBrightness,
                onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forcedTheme.value = themeFromLaunch(intent)
        launchedAt = SystemClock.elapsedRealtime()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.preferLowestRefreshRate()
    }

    // Écran pliable plié ou déplié : on change d'écran, donc de modes d'affichage.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window.preferLowestRefreshRate()
    }

    override fun onResume() {
        super.onResume()
        // Le réglage a pu changer pendant un passage par les réglages : on le relit à chaque reprise.
        requestedOrientation = activityOrientation(prefs.orientation)
        val remaining = autoOffDelayMillis(launchedAt, prefs.autoOffMinutes, SystemClock.elapsedRealtime()) ?: return
        autoOff = lifecycleScope.launch {
            delay(remaining)
            // Pas d'image fixe pendant des heures : on laisse l'écran s'éteindre et on ferme l'horloge.
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            finish()
        }
    }

    override fun onPause() {
        autoOff?.cancel()
        super.onPause()
    }

    private fun themeFromLaunch(intent: Intent): ThemeMode? = themeFromExtra(intent.getStringExtra(EXTRA_THEME))

    /** Luminosité de la fenêtre : minimale la nuit, plafonnée à 35 % le jour (usure OLED). */
    private fun applyBrightness(night: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (night) NIGHT_BRIGHTNESS else DAY_MAX_BRIGHTNESS
        }
    }
}
