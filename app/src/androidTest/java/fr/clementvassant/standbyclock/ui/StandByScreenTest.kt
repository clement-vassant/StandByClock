package fr.clementvassant.standbyclock.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.DeviceStatus
import fr.clementvassant.standbyclock.NowPlaying
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Marge des comparaisons de positions : arrondis de la conversion pixels → dp. */
private val TOLERANCE = 0.5.dp

/**
 * Ni batterie ni réveil : l'émulateur est toujours « en charge », et la ligne sous le cadran changerait sa taille.
 * Ces tests mesurent la mise en page sans elle ; `DeviceStatusScreenTest` couvre la ligne.
 */
internal val NO_DEVICE_STATUS = DeviceStatus()

/** Tailles d'écran simulées : elles tiennent dans l'émulateur de la CI, en portrait. */
private val LANDSCAPE_WIDTH = 400.dp
private val LANDSCAPE_HEIGHT = 240.dp
private val PORTRAIT_WIDTH = 300.dp
private val PORTRAIT_HEIGHT = 600.dp

@RunWith(AndroidJUnit4::class)
class StandByScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    // Mardi 20 octobre 2026, 14:15 : heure paire (horloge à gauche), en plein jour.
    private val clock = Clock.fixed(
        ZonedDateTime.of(2026, 10, 20, 14, 15, 0, 0, ZoneId.of("Europe/Paris")).toInstant(),
        ZoneId.of("Europe/Paris")
    )
    private val music = NowPlaying(
        title = "One More Night",
        artist = "Maroon 5",
        art = null,
        isPlaying = true,
        pausedAt = 0L,
        liked = false
    )

    @Before
    fun resetPrefs() {
        context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context)
    }

    private fun show(media: NowPlaying? = null, modifier: Modifier = Modifier) = rule.setContent {
        StandByScreen(
            prefs = prefs,
            forcedTheme = null,
            onNightModeChange = {},
            onOpenSettings = null,
            modifier = modifier,
            clock = clock,
            nowPlayingSource = { media },
            deviceStatusSource = { NO_DEVICE_STATUS }
        )
    }

    private fun screenIs(state: String) =
        rule.onNodeWithTag(TAG_SCREEN).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))

    // Écran de veille sur un écran en portrait : l'horloge est mise en page en paysage puis tournée.
    @Test
    fun portraitScreenSaverIsLaidOutInLandscape() {
        rule.setContent {
            Box(Modifier.size(width = 300.dp, height = 600.dp)) {
                StandByScreen(
                    prefs = prefs,
                    forcedTheme = null,
                    onNightModeChange = {},
                    onOpenSettings = null,
                    modifier = Modifier.forceLandscape(),
                    clock = clock,
                    nowPlayingSource = { null },
                    deviceStatusSource = { NO_DEVICE_STATUS }
                )
            }
        }
        val size = rule.onNodeWithTag(TAG_SCREEN).fetchSemanticsNode().size
        assertTrue("mise en page attendue en paysage : $size", size.width > size.height)
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
    }

    // Orientation automatique sur un écran en portrait : à 14 h (heure paire), l'horloge est au-dessus du calendrier.
    @Test
    fun portraitScreenStacksClockAboveCalendar() {
        rule.setContent {
            Box(Modifier.size(width = 300.dp, height = 600.dp)) {
                StandByScreen(
                    prefs = prefs,
                    forcedTheme = null,
                    onNightModeChange = {},
                    onOpenSettings = null,
                    clock = clock,
                    nowPlayingSource = { null },
                    deviceStatusSource = { NO_DEVICE_STATUS }
                )
            }
        }
        val clockBounds = rule.onNodeWithTag(clockTag(ClockStyle.DIGITAL)).getBoundsInRoot()
        val calendarBounds = rule.onNodeWithTag(TAG_CALENDAR).getBoundsInRoot()
        assertTrue("horloge attendue au-dessus du calendrier", clockBounds.bottom <= calendarBounds.top)
    }

    // Portrait : calendrier dessous, le cadran analogique remplit son panneau au lieu de suivre la hauteur du calendrier.
    @Test
    fun portraitAnalogDialFillsItsPane() {
        prefs.style = ClockStyle.ANALOG
        rule.setContent {
            Box(Modifier.size(width = 300.dp, height = 600.dp)) {
                StandByScreen(
                    prefs = prefs,
                    forcedTheme = null,
                    onNightModeChange = {},
                    onOpenSettings = null,
                    clock = clock,
                    nowPlayingSource = { music },
                    deviceStatusSource = { NO_DEVICE_STATUS }
                )
            }
        }
        val dial = rule.onNodeWithTag(TAG_DIAL).fetchSemanticsNode().size
        val screen = rule.onNodeWithTag(TAG_SCREEN).fetchSemanticsNode().size
        assertTrue("cadran $dial trop petit pour l'écran $screen", dial.width * 10 >= screen.width * 6)
    }

    @Test
    fun calendarIsShownByDefault() {
        show()
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(clockTag(ClockStyle.DIGITAL)).assertIsDisplayed()
    }

    // Non-régression : la musique en cours ne doit plus faire disparaître le calendrier.
    @Test
    fun calendarAndMusicAreShownTogether() {
        show(media = music)
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(TAG_MEDIA_BAR).assertIsDisplayed()
    }

    // Paysage (boîte plus large que haute, l'émulateur étant en portrait) : à 14 h l'horloge est à gauche,
    // la barre musique est dessous, dans sa colonne, et ne touche ni l'heure ni le calendrier.
    @Test
    fun landscapeMusicBarSitsUnderDigitalClock() = assertMusicBarUnderClockInLandscape(ClockStyle.DIGITAL)

    @Test
    fun landscapeMusicBarSitsUnderAnalogClock() = assertMusicBarUnderClockInLandscape(ClockStyle.ANALOG)

    @Test
    fun landscapeMusicBarSitsUnderMinimalClock() = assertMusicBarUnderClockInLandscape(ClockStyle.MINIMAL)

    private fun assertMusicBarUnderClockInLandscape(style: ClockStyle) {
        prefs.style = style
        showIn(width = LANDSCAPE_WIDTH, height = LANDSCAPE_HEIGHT, media = music)
        val clockBounds = rule.onNodeWithTag(clockTag(style)).getBoundsInRoot()
        val barBounds = rule.onNodeWithTag(TAG_MEDIA_BAR).getBoundsInRoot()
        val calendarBounds = rule.onNodeWithTag(TAG_CALENDAR).getBoundsInRoot()
        assertBelow("barre sous l'horloge", clockBounds, barBounds)
        assertTrue("barre dans la colonne de l'horloge : $barBounds", barBounds.left >= clockBounds.left - TOLERANCE)
        assertTrue("barre hors du calendrier : $barBounds / $calendarBounds", barBounds.right <= calendarBounds.left + TOLERANCE)
        rule.onNodeWithTag(TAG_MEDIA_BAR).assertIsDisplayed()
    }

    // Portrait : horloge, barre musique puis calendrier, de haut en bas, sans chevauchement.
    @Test
    fun portraitMusicBarSitsUnderDigitalClock() = assertMusicBarBetweenClockAndCalendarInPortrait(ClockStyle.DIGITAL)

    @Test
    fun portraitMusicBarSitsUnderAnalogClock() = assertMusicBarBetweenClockAndCalendarInPortrait(ClockStyle.ANALOG)

    @Test
    fun portraitMusicBarSitsUnderMinimalClock() = assertMusicBarBetweenClockAndCalendarInPortrait(ClockStyle.MINIMAL)

    private fun assertMusicBarBetweenClockAndCalendarInPortrait(style: ClockStyle) {
        prefs.style = style
        showIn(width = PORTRAIT_WIDTH, height = PORTRAIT_HEIGHT, media = music)
        val clockBounds = rule.onNodeWithTag(clockTag(style)).getBoundsInRoot()
        val barBounds = rule.onNodeWithTag(TAG_MEDIA_BAR).getBoundsInRoot()
        val calendarBounds = rule.onNodeWithTag(TAG_CALENDAR).getBoundsInRoot()
        assertBelow("barre sous l'horloge", clockBounds, barBounds)
        assertBelow("calendrier sous la barre", barBounds, calendarBounds)
    }

    // Analogique en paysage : le cadran réduit tient au-dessus de la barre, et pas plus haut que le calendrier.
    @Test
    fun landscapeAnalogDialLeavesRoomForMusicBar() {
        prefs.style = ClockStyle.ANALOG
        showIn(width = LANDSCAPE_WIDTH, height = LANDSCAPE_HEIGHT, media = music)
        val dialBounds = rule.onNodeWithTag(TAG_DIAL).getBoundsInRoot()
        assertBelow("cadran au-dessus de la barre", dialBounds, rule.onNodeWithTag(TAG_MEDIA_BAR).getBoundsInRoot())
        assertDialFitsCalendar()
    }

    // Calendrier masqué par l'appui long : la grande carte musique le remplace, et plus de barre sous l'heure.
    @Test
    fun hiddenCalendarShowsMusicCardInsteadOfBar() {
        show(media = music)
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { longClick(center) }
        rule.onNodeWithTag(TAG_MEDIA_CARD).assertIsDisplayed()
        rule.onNodeWithTag(TAG_MEDIA_BAR).assertDoesNotExist()
        rule.onNodeWithTag(TAG_CALENDAR).assertDoesNotExist()
    }

    private fun showIn(width: Dp, height: Dp, media: NowPlaying?) = rule.setContent {
        Box(Modifier.size(width = width, height = height)) {
            StandByScreen(
                prefs = prefs,
                forcedTheme = null,
                onNightModeChange = {},
                onOpenSettings = null,
                clock = clock,
                nowPlayingSource = { media },
                deviceStatusSource = { NO_DEVICE_STATUS }
            )
        }
    }

    private fun assertBelow(message: String, upper: DpRect, lower: DpRect) =
        assertTrue("$message : $upper / $lower", upper.bottom <= lower.top + TOLERANCE)

    @Test
    fun analogDialIsNotTallerThanCalendar() {
        prefs.style = ClockStyle.ANALOG
        show(modifier = Modifier.forceLandscape())
        assertDialFitsCalendar()
    }

    @Test
    fun analogDialIsNotTallerThanCalendarWithMusic() {
        prefs.style = ClockStyle.ANALOG
        show(media = music, modifier = Modifier.forceLandscape())
        assertDialFitsCalendar()
    }

    // Calendrier à côté (paysage, forcé : l'émulateur est en portrait) : le cadran ne dépasse pas sa hauteur.
    // Tailles de mise en page, avant la rotation du paysage forcé.
    private fun assertDialFitsCalendar() {
        val dialHeight = rule.onNodeWithTag(TAG_DIAL).fetchSemanticsNode().size.height
        val calendarHeight = rule.onNodeWithTag(TAG_CALENDAR).fetchSemanticsNode().size.height
        assertTrue("cadran $dialHeight > calendrier $calendarHeight", dialHeight <= calendarHeight)
    }

    @Test
    fun tapTogglesNightMode() {
        show()
        screenIs("jour")
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { click(center) }
        screenIs("nuit")
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { click(center) }
        screenIs("jour")
    }

    @Test
    fun longPressHidesCalendar() {
        show()
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { longClick(center) }
        rule.onNodeWithTag(TAG_CALENDAR).assertDoesNotExist()
    }

    @Test
    fun swipeChangesStyle() {
        show()
        rule.onNodeWithTag(clockTag(ClockStyle.DIGITAL)).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag(clockTag(ClockStyle.ANALOG)).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag(clockTag(ClockStyle.MINIMAL)).assertIsDisplayed()
    }
}
