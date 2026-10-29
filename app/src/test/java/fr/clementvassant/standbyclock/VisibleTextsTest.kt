package fr.clementvassant.standbyclock

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L'app marche sur tout appareil Android : aucun texte visible ne doit la présenter comme réservée au Fold. */
class VisibleTextsTest {
    // Les tests unitaires tournent depuis le dossier du module app.
    private val strings = File("src/main/res/values/strings.xml").readText()
    private val settings = File("src/main/java/fr/clementvassant/standbyclock/SettingsActivity.kt").readText()

    @Test
    fun `no device-specific wording in visible texts`() {
        for (word in listOf("Fold", "Galaxy")) {
            assertFalse("strings.xml mentionne $word", word in strings)
            assertFalse("SettingsActivity mentionne $word", word in settings)
        }
    }

    @Test
    fun `calendar setting does not promise a fixed side`() {
        // Le calendrier échange sa place avec l'horloge chaque heure : un libellé « à droite » faisait croire
        // à un réglage de côté, alors que l'interrupteur masque le calendrier.
        for (side in listOf("à droite", "à gauche")) {
            assertFalse("strings.xml promet un calendrier $side", Regex("[Cc]alendrier[^<]*$side").containsMatchIn(strings))
        }
        assertTrue("Afficher le calendrier" in strings)
    }

    @Test
    fun `visible texts come from the resources`() {
        // Un texte écrit en dur ne serait pas traduit : tout libellé passe par strings.xml. Seuls restent les
        // formats de nombres (« %02d ») et la ponctuation (« : »), identiques dans toutes les langues.
        val sources = listOf(
            "SettingsActivity.kt",
            "ui/Clocks.kt",
            "ui/Agenda.kt",
            "ui/MediaCard.kt",
            "ui/StandByScreen.kt",
            "ui/StatusRow.kt"
        )
            .map { File("src/main/java/fr/clementvassant/standbyclock/$it") }
        val hardcoded = sources.flatMap { file ->
            Regex("""(Text|Section)\("([^"]*)"""").findAll(file.readText())
                .filter { Regex("""\p{L}""").containsMatchIn(it.groupValues[2].replace(Regex("%[0-9$]*[a-z]"), "")) }
                .map { "${file.name} : ${it.value}" }
                .toList()
        }
        assertEquals(emptyList<String>(), hardcoded)
    }

    @Test
    fun `routines help is generic`() {
        val routines = Regex("<string name=\"help_routines\">(.*)</string>").find(strings)!!.groupValues[1]
        assertTrue(routines, "Tasker" in routines)
        assertTrue(routines, "routines ou outils d\\'automatisation" in routines)
    }
}
