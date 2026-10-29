package fr.clementvassant.standbyclock.e2e

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * Luminosité que la fenêtre au premier plan impose au système, lue dans `dumpsys power` : c'est ce qu'Android
 * applique vraiment, pas seulement ce que l'app a demandé. NaN quand aucune fenêtre n'impose rien.
 */
internal object WindowBrightness {
    private const val OVERRIDE_KEY = "mScreenBrightnessOverrideFromWindowManager="
    private const val POLL_MS = 500L

    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /**
     * Relit la valeur jusqu'à ce qu'elle convienne ou que le délai expire, le temps que Compose signale le jour
     * ou la nuit et que le gestionnaire de fenêtres transmette la valeur. Renvoie la dernière valeur lue.
     */
    fun waitFor(expected: (Float) -> Boolean): Float {
        val deadline = System.currentTimeMillis() + E2e.SCREEN_TIMEOUT_MS
        var override = current()
        while (!expected(override) && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS)
            override = current()
        }
        return override
    }

    fun current(): Float = shell("dumpsys power").lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith(OVERRIDE_KEY) }
        ?.substringAfter('=')?.toFloatOrNull()
        ?: error("$OVERRIDE_KEY absent de dumpsys power")

    /**
     * Pour un échec : ce que chaque fenêtre demande (`sbrt=`), ce que l'affichage reçoit et la luminosité appliquée,
     * pour savoir si l'app n'a rien demandé ou si Android a ignoré sa demande.
     */
    fun diagnostics(): String {
        val windows = shell("dumpsys window windows").lines()
            .filter { "Window #" in it || "sbrt=" in it }
            .joinToString("\n") { it.trim() }
        val display = shell("dumpsys display").lines()
            .filter { line -> DISPLAY_KEYS.any { it in line } }
            .joinToString("\n") { it.trim() }
        return "$ dumpsys window windows\n$windows\n$ dumpsys display\n$display"
    }

    private val DISPLAY_KEYS = listOf("screenBrightnessOverride", "mScreenBrightness=", "policy=")

    private fun shell(command: String) = device.executeShellCommand(command)
}
