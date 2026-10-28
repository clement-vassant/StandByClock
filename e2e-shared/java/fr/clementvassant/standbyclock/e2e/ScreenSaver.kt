package fr.clementvassant.standbyclock.e2e

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Pilote l'écran de veille d'Android comme le ferait la charge : on choisit StandBy Clock dans les réglages
 * système, puis on le démarre. Les réglages touchés sont rendus à la fin (émulateur du Mac partagé).
 *
 * Partagé par les parcours e2e de l'app (APK debug) et le module release-smoke (APK release) : il ne passe que
 * par le shell et l'écran, jamais par les classes de l'app, que R8 renomme dans l'APK release.
 */
internal class ScreenSaver(
    private val device: UiDevice,
    private val packageName: String,
    private val capturesDir: String,
    private val screenTimeoutMs: Long,
    /** testTag de l'écran de l'horloge (`TAG_SCREEN`), passé en paramètre : release-smoke ne voit pas l'app. */
    private val screenTag: String
) {
    private val touched = listOf(
        "secure" to "screensaver_enabled",
        "secure" to "screensaver_components",
        "secure" to "screensaver_activate_on_sleep",
        "system" to "screen_off_timeout",
        "global" to "stay_on_while_plugged_in"
    )
    private val previous = touched.associateWith { (namespace, name) -> shell("settings get $namespace $name").trim() }

    /**
     * Démarre l'écran de veille et attend qu'il s'affiche, par trois voies, chacune si la précédente ne donne rien :
     * « Démarrer maintenant » de SystemUI, la commande de test `cmd dreams` (réservée à root selon la version),
     * puis le vrai chemin de la charge : appareil branché, écran laissé sans toucher jusqu'à sa mise en veille.
     * En cas d'échec, le message dit ce qu'Android a répondu et une capture montre ce qui était à l'écran.
     */
    fun start() {
        shell("settings put secure screensaver_enabled 1")
        shell("settings put secure screensaver_components $packageName/.StandByDreamService")
        val answers = mutableListOf<String>()
        for (command in listOf("am start -W -n com.android.systemui/.Somnambulator", "cmd dreams start-dreaming")) {
            answers += "$ $command\n" + shell(command).trim()
            if (isShown(ATTEMPT_TIMEOUT_MS)) return
        }
        // Sur l'émulateur de la CI, une fois sur trois environ, l'écran reste allumé sans lancer l'écran de veille :
        // on éteint puis rallume l'écran avant un second essai, ce qui repart d'un état de veille propre.
        repeat(CHARGING_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                shell("input keyevent KEYCODE_SLEEP")
                device.waitForIdle()
            }
            startAsWhileCharging()
            if (isShown(SLEEP_TIMEOUT_MS)) return
        }

        shell("mkdir -p $capturesDir")
        shell("screencap -p $capturesDir/ecran-de-veille-echec.png")
        val dreams = shell("dumpsys dreams").lines().take(DUMP_LINES).joinToString("\n")
        val power = shell("dumpsys power").lines().filter { line -> POWER_KEYS.any { it in line } }.joinToString("\n")
        throw AssertionError(
            "L'écran de veille ne s'est pas affiché.\n${answers.joinToString("\n")}\n$ dumpsys dreams\n$dreams\n$ dumpsys power\n$power"
        )
    }

    /** Retour : un écran de veille interactif se ferme au bouton retour, comme pour l'utilisateur. */
    fun stop() {
        if (device.hasObject(By.res(screenTag))) {
            device.pressBack()
            device.wait(Until.gone(By.res(screenTag)), screenTimeoutMs)
        }
        shell("dumpsys battery reset")
        for ((setting, value) in previous) {
            val (namespace, name) = setting
            shell(if (value.isEmpty() || value == "null") "settings delete $namespace $name" else "settings put $namespace $name $value")
        }
    }

    /** Branché, « pendant la charge », écran qui s'éteint au plus vite : Android lance l'écran de veille au lieu d'éteindre. */
    private fun startAsWhileCharging() {
        shell("dumpsys battery set ac 1")
        shell("settings put secure screensaver_activate_on_sleep 1")
        shell("settings put global stay_on_while_plugged_in 0")
        shell("settings put system screen_off_timeout $SHORTEST_SCREEN_OFF_MS")
        device.wakeUp()
    }

    private fun isShown(timeoutMs: Long) = device.wait(Until.hasObject(By.res(screenTag)), timeoutMs)

    private fun shell(command: String): String = device.executeShellCommand(command)

    private companion object {
        const val ATTEMPT_TIMEOUT_MS = 5_000L

        /** Android impose au moins 10 s avant la mise en veille, plus la phase où l'écran baisse. */
        const val SHORTEST_SCREEN_OFF_MS = 10_000
        const val SLEEP_TIMEOUT_MS = 35_000L
        const val CHARGING_ATTEMPTS = 2
        const val DUMP_LINES = 40

        /** Lignes de `dumpsys power` qui disent pourquoi Android n'a pas lancé l'écran de veille. */
        val POWER_KEYS = listOf("mWakefulness", "mIsPowered", "mStayOn", "mDream", "mScreenOffTimeout", "mLastUserActivityTime")
    }
}
