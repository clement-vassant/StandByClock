package fr.clementvassant.standbyclock

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Viser Android 16 change ce que fait l'app sans toucher au code : ces tests rappellent ce qu'il faut revoir
 * à chaque montée de targetSdk.
 */
class TargetSdkTest {
    // Les tests unitaires tournent depuis le dossier du module app.
    private val buildScript = File("build.gradle.kts").readText()
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    private fun targetSdkOf(script: String): Int? = Regex("""targetSdk = (\d+)""").find(script)?.groupValues?.get(1)?.toInt()

    @Test
    fun `the app targets Android 16`() {
        // À 37, l'exemption d'orientation ci-dessous n'agit plus : il faudra choisir quoi faire de « Toujours paysage ».
        assertEquals(ANDROID_16, targetSdkOf(buildScript))
    }

    @Test
    fun `the smoke tests target the same Android as the app`() {
        assertEquals(targetSdkOf(buildScript), targetSdkOf(File("../release-smoke/build.gradle.kts").readText()))
    }

    @Test
    fun `always landscape still holds on large screens`() {
        // Android 16 ignore l'orientation demandée au-delà de 600 dp, sauf exemption de l'activité.
        assertTrue("android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY" in manifest)
    }

    private companion object {
        const val ANDROID_16 = 36
    }
}
