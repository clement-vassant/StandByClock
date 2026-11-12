package fr.clementvassant.standbyclock

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le profil de référence ignore sans rien dire un package qui n'existe plus : après un renommage du package,
 * le code de l'app ne serait plus précompilé à l'installation.
 */
class BaselineProfileTest {
    // Les tests unitaires tournent depuis le dossier du module app.
    private val baselineProfile = File("src/main/baseline-prof.txt").readLines()

    @Test
    fun `baseline profile covers the app package`() {
        val appPackage = "L" + BuildConfig.APPLICATION_ID.replace('.', '/') + "/**;"
        assertTrue(baselineProfile.joinToString("\n"), baselineProfile.any { it.startsWith("HSP$appPackage") })
        assertTrue(baselineProfile.joinToString("\n"), appPackage in baselineProfile)
    }
}
