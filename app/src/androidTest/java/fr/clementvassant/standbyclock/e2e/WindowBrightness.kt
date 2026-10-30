package fr.clementvassant.standbyclock.e2e

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * Luminosité de l'horloge vue par le système, en deux temps : ce que la fenêtre de l'app demande au gestionnaire
 * de fenêtres (`sbrt=` dans `dumpsys window`), puis si l'écran l'applique vraiment. Ce second relevé change selon
 * la version d'Android : `dumpsys power` le donne jusqu'à Android 14 ; Android 16 n'a plus cette ligne et l'écrit
 * dans `dumpsys display` (raison de la luminosité « override »).
 */
internal object WindowBrightness {
    private const val POWER_OVERRIDE_KEY = "mScreenBrightnessOverrideFromWindowManager="
    private const val DISPLAY_REASON_KEY = "mBrightnessReason="
    private const val REQUEST_KEY = "sbrt="
    private const val POLL_MS = 500L

    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** Relit les deux relevés jusqu'à ce qu'ils conviennent ou que le délai expire, le temps que Compose signale le jour ou la nuit. */
    fun waitFor(expected: (requested: Float, applied: Boolean) -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + E2e.SCREEN_TIMEOUT_MS
        while (true) {
            if (expected(requested(), appliedBySystem())) return true
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(POLL_MS)
        }
    }

    /** Luminosité demandée par la fenêtre de l'app la plus en avant, NaN si elle ne demande rien. */
    fun requested(): Float {
        val lines = shell("dumpsys window windows").lines()
        val start = lines.indexOfFirst { "Window{" in it && E2e.PACKAGE in it }
        if (start < 0) return Float.NaN
        return lines.drop(start + 1)
            .takeWhile { "Window #" !in it }
            .firstNotNullOfOrNull { requestIn(it) }
            ?: Float.NaN
    }

    /** Une fenêtre impose-t-elle sa luminosité à l'écran ? */
    fun appliedBySystem(): Boolean {
        val power = shell("dumpsys power").lineSequence().map { it.trim() }.firstOrNull { it.startsWith(POWER_OVERRIDE_KEY) }
        if (power != null) return power.substringAfter('=').toFloatOrNull()?.isNaN() == false
        return shell("dumpsys display").lineSequence().any {
            DISPLAY_REASON_KEY in it && "override" in it.substringAfter(DISPLAY_REASON_KEY)
        }
    }

    /** Pour un échec : ce que chaque fenêtre demande, ce que l'écran reçoit et la luminosité appliquée. */
    fun diagnostics(): String {
        val windows = shell("dumpsys window windows").lines()
            .filter { "Window #" in it || REQUEST_KEY in it }
            .joinToString("\n") { line ->
                line.trim().let {
                    if (REQUEST_KEY in
                        it
                    ) {
                        REQUEST_KEY + it.substringAfter(REQUEST_KEY).substringBefore(' ')
                    } else {
                        it
                    }
                }
            }
        val power = shell("dumpsys power").lines().filter { POWER_OVERRIDE_KEY in it }.joinToString("\n") { it.trim() }
        val display = shell("dumpsys display").lines()
            .filter { line -> DISPLAY_KEYS.any { it in line } }
            .joinToString("\n") { it.trim() }
        return "$ dumpsys window windows\n$windows\n$ dumpsys power\n$power\n$ dumpsys display\n$display\n"
    }

    /** Valeur de `sbrt=` dans une ligne d'attributs de fenêtre, null si la ligne n'en a pas. */
    private fun requestIn(line: String): Float? = line.substringAfter(REQUEST_KEY, "")
        .takeWhile { it.isDigit() || it == '.' }
        .toFloatOrNull()

    private val DISPLAY_KEYS = listOf(DISPLAY_REASON_KEY, "mScreenBrightness=", "policy=")

    private fun shell(command: String) = device.executeShellCommand(command)
}
