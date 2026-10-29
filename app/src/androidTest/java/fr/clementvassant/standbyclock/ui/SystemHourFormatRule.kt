package fr.clementvassant.standbyclock.ui

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/**
 * Règle le téléphone en 12 h ou en 24 h pendant un test, puis remet le réglage de l'appareil : l'heure affichée
 * suit ce réglage, et celui de l'émulateur dépend de sa langue.
 */
class SystemHourFormatRule(private val initial: String = TWENTY_FOUR_HOURS) : ExternalResource() {
    private var previous = ""

    override fun before() {
        previous = shell("settings get system $SETTING").trim()
        set(initial)
    }

    override fun after() {
        if (previous == "null" || previous.isEmpty()) shell("settings delete system $SETTING") else set(previous)
    }

    /** « 12 » ou « 24 ». */
    fun set(value: String) {
        shell("settings put system $SETTING $value")
    }

    /** Commande lancée avec les droits du shell : une app ne peut pas écrire les réglages système elle-même. */
    private fun shell(command: String): String {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText() }
    }

    companion object {
        private const val SETTING = "time_12_24"
        const val TWELVE_HOURS = "12"
        const val TWENTY_FOUR_HOURS = "24"
    }
}
