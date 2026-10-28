package fr.clementvassant.standbyclock

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R8 et le profil de référence ignorent sans rien dire une classe ou un package qui n'existe plus : après un
 * renommage, les parcours e2e sur l'APK release casseraient et le démarrage ne serait plus précompilé.
 */
class ReleaseShrinkingTest {
    // Les tests unitaires tournent depuis le dossier du module app.
    private val keepRules = File("proguard-rules.pro").readLines()
    private val baselineProfile = File("src/main/baseline-prof.txt").readLines()

    @Test
    fun `keep rules name classes that exist`() {
        val keptClasses = keepRules.mapNotNull { keepTarget.find(it)?.groupValues?.get(1) }
        assertTrue("Aucune règle -keep lue dans proguard-rules.pro", keptClasses.isNotEmpty())
        keptClasses.forEach { Class.forName(it) }
    }

    @Test
    fun `baseline profile covers the app package`() {
        val appPackage = "L" + BuildConfig.APPLICATION_ID.replace('.', '/') + "/**;"
        assertTrue(baselineProfile.joinToString("\n"), baselineProfile.any { it.startsWith("HSP$appPackage") })
        assertTrue(baselineProfile.joinToString("\n"), appPackage in baselineProfile)
    }

    private companion object {
        val keepTarget = Regex("""^-keep (?:class|enum) ([\w.]+)""")
    }
}
