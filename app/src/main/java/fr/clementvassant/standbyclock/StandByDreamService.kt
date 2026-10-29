package fr.clementvassant.standbyclock

import android.os.SystemClock
import android.service.dreams.DreamService
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import fr.clementvassant.standbyclock.ui.StandByScreen
import fr.clementvassant.standbyclock.ui.forceLandscape
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Écran de veille Android ("Daydream"). À activer dans
 * Paramètres > Affichage > Écran de veille, avec "Pendant la charge".
 * Un DreamService n'est pas une Activity : on lui fournit nous-mêmes le
 * cycle de vie dont Compose a besoin.
 */
class StandByDreamService :
    DreamService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private var autoOff: Job? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = true // toucher : jour/nuit, appui long : calendrier, balayage : style
        isScreenBright = false // le jour, niveau atténué du système ; la nuit, la fenêtre impose le minimum

        val prefs = ClockPrefs(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StandByDreamService)
            setViewTreeSavedStateRegistryOwner(this@StandByDreamService)
            setViewTreeViewModelStoreOwner(this@StandByDreamService)
            setContent {
                // Un écran de veille ne peut pas imposer le paysage : si l'utilisateur le veut, on tourne nous-mêmes l'horloge.
                val orientation = if (prefs.orientation == OrientationMode.LANDSCAPE) Modifier.forceLandscape() else Modifier
                StandByScreen(
                    prefs = prefs,
                    forcedTheme = null,
                    onNightModeChange = { night -> window.applyClockBrightness(night, ClockSurface.SCREEN_SAVER) },
                    onOpenSettings = null,
                    modifier = orientation
                )
            }
        }
        setContentView(view)
        window.preferLowestRefreshRate()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        // Extinction automatique : pas d'image fixe pendant des heures.
        val now = SystemClock.elapsedRealtime()
        val remaining = autoOffDelayMillis(launchedAt = now, ClockPrefs(this).autoOffMinutes, now) ?: return
        autoOff = lifecycleScope.launch {
            delay(remaining)
            finish()
        }
    }

    override fun onDreamingStopped() {
        autoOff?.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
