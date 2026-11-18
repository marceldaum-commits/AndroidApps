package de.raumlicht.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertTextContains
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.raumlicht.app.ui.home.*
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.level.LocalMistakeSink
import de.raumlicht.app.ui.stage.LocalLuxLook
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.app.widget.LearnWidget
import de.raumlicht.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

/** Die Erlebnis- und Übungsbereiche aus 0.5.0 von vorn bis hinten. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class PlayFlowTest {
    @get:Rule val rule = createComposeRule()
    private val stage = StageController()
    private fun show(content: @Composable () -> Unit) {
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) { content() } } } }
        rule.mainClock.advanceTimeBy(800)
    }
    private fun slide(tag: String, u: Float) { rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(u) }; rule.mainClock.advanceTimeBy(300) }

    @Test fun flightShowsWhereTheLinePiercesThePlane() {
        show { FlightScreen({}, controller = stage) }
        assertTrue(stage.lastState!!.contains("\"style\":\"line\"")); assertTrue(stage.lastRide!!.contains("view:'fp'"))
        rule.onNodeWithText("Abstand 4").assertExists()                                  // Start: 4 von E₁ entfernt
        slide("flug-regler", 0.5f)                                                       // t = 2: genau in E₁
        rule.onNodeWithText("Durchstoßpunkt mit E₁: x₁ = 4").assertExists(); rule.onNodeWithText("t = 2 → S(4|4|3)").assertExists()
        rule.onNodeWithText("Du bist genau in der Ebene").assertExists()
        rule.onNodeWithTag("flug-kugel").performClick(); rule.mainClock.advanceTimeBy(400)
        assertTrue(stage.lastState!!.contains("\"type\":\"sphere\""))
    }

    @Test fun galtonBallsFillTheBins() {
        show { GaltonScreen({}, controller = stage, random = Random(3)) }
        rule.onNodeWithText("0 Kugeln").assertExists(); rule.onNodeWithText("4").assertExists()          // μ = 8 · 0,5
        rule.onNodeWithTag("galton-10").performClick(); rule.mainClock.advanceTimeBy(6000)
        rule.onNodeWithText("10 Kugeln").assertExists()
        assertTrue(stage.lastState!!.contains("\"id\":\"erwartung\""))
        rule.onNodeWithTag("plus-p").performClick(); rule.mainClock.advanceTimeBy(400)                    // neues p → neues Experiment
        rule.onNodeWithText("0 Kugeln").assertExists(); rule.onNodeWithText("4,8").assertExists()
    }

    @Test fun quickModeHasNoTipButton() {
        show { QuickScreen({ _, _, _ -> }, {}, seed = 3) }
        rule.onNodeWithText("▶ Los geht’s").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("▶ Los").assertExists(); rule.onNodeWithText("💡 Tipp").assertDoesNotExist()
        rule.onNodeWithText("✍️ Ohne Hilfsmittel · 1/6 · Geometrie").assertExists()
    }

    @Test fun namedMistakesGoToTheDiary() {
        val seen = ArrayList<Pair<String, String>>()
        val l = Levels.byId("2-1")!!
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }, LocalMistakeSink provides { id, msg -> seen += id to msg }) {
            LevelScreen(l, Levels.chapters[1], false, {}, { _, _ -> }, {}, {}, mapOf("v" to listOf(-3, -4, 0)))
        } } } }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000)
        assertEquals(1, seen.size); assertEquals("2-1", seen[0].first); assertTrue(seen[0].second.startsWith("Du hast A − B gerechnet"))
        // Zählen und Sortieren im Tagebuch
        var text = ""; repeat(3) { text = DiaryEntry.add(text, "2-1", seen[0].second) }; text = DiaryEntry.add(text, "7-5", "Kosinus statt Sinus")
        val entries = DiaryEntry.parse(text); assertEquals(listOf(3, 1), entries.map { it.count }); assertEquals("2-1", entries[0].levelId)
    }

    @Test fun rideCanShowDerivativeTrackAndFillTheArea() {
        show { RideScreen({}, "huegel", controller = stage) }
        assertFalse(stage.lastState!!.contains("ableitung")); rule.onNodeWithTag("flaeche").assertDoesNotExist()
        rule.onNodeWithTag("zeige-ableitung").performClick(); rule.onNodeWithTag("zeige-flaeche").performClick(); rule.mainClock.advanceTimeBy(400)
        slide("fahrt-regler", 0.5f); slide("fahrt-regler", 1f)
        assertTrue(stage.lastState!!.contains("\"id\":\"ableitung\"") && stage.lastState!!.contains("\"id\":\"plus"))
        rule.onNodeWithText("10,67").assertExists()                                       // ∫₀⁴ (−x² + 4x) dx = 32/3
        assertFalse(stage.lastState!!.contains("\"id\":\"zweite\""))
        rule.onNodeWithTag("zeige-zweite").performScrollTo().performClick(); rule.mainClock.advanceTimeBy(400)
        assertTrue(stage.lastState!!.contains("\"id\":\"zweite\"") && stage.lastState!!.contains("\"label\":\"f″\""))
    }

    @Test fun introShowsTheFirstTaskBeingSolved() {
        val l = Levels.byId("1-1")!!; var seenIntro = false
        show { LevelScreen(l, Levels.chapters[0], true, { seenIntro = true }, { _, _ -> }, {}, {}, controller = stage) }
        assertEquals(l.scene(l.start, Phase.AIM), stage.lastState)
        rule.onNodeWithText("Weiter ›").performClick(); rule.mainClock.advanceTimeBy(500)
        assertEquals(l.scene(l.solution, Phase.AIM), stage.lastState)
        rule.onNodeWithText("Weiter ›").performClick(); rule.mainClock.advanceTimeBy(500)
        assertEquals(l.scene(l.solution, Phase.DONE), stage.lastState)
        rule.onNodeWithText("Licht an! ▶").performClick(); rule.mainClock.advanceTimeBy(500)
        assertTrue(seenIntro); assertEquals(l.scene(l.start, Phase.AIM), stage.lastState)
    }

    @Test fun workshopExtrasAndStageLook() {
        var look = "none" to "none"
        show { CompositionLocalProvider(LocalLuxLook provides ("rainbow" to "crown")) { Box { Stage(stage); WorkshopScreen(0, "pink", {}, badges = 2, look = look, onLook = { t, h -> look = t to h }) {} } } }
        assertTrue(stage.log.any { it == "RL.look({trail:'rainbow',hat:'crown'})" })
        rule.onNodeWithTag("zubehoer-sparks-0").performClick(); assertEquals("sparks" to "none", look)
        rule.onNodeWithTag("zubehoer-crown-1").performClick(); assertEquals("sparks" to "none", look)     // Krone braucht 8 Erfolge
    }

    @Test fun learnWidgetAndShareCard() {
        assertEquals(listOf(0, 0, 0), LearnWidget.percents(emptyMap()))
        val all = Levels.all.associate { it.id to 3 }; assertEquals(listOf(100, 100, 100), LearnWidget.percents(all))
        val geo = Levels.chapters.filter { it.area == Area.GEOMETRIE }.flatMap { it.levels }
        assertEquals(listOf(50, 0, 0), LearnWidget.percents(geo.take(geo.size / 2).associate { it.id to 3 }).let { listOf(it[0], it[1], it[2]) })
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val v = LearnWidget.views(ctx, listOf(42, 10, 0)).apply(ctx, android.widget.FrameLayout(ctx))
        assertEquals("42 %", v.findViewById<android.widget.TextView>(de.raumlicht.app.R.id.learn_value_0).text.toString())
        assertEquals(42, v.findViewById<android.widget.ProgressBar>(de.raumlicht.app.R.id.learn_bar_0).progress)
        val card = ShareCard.draw("🎪 Festivalzelt", "11 von 12 BE", Exam.verdict(11, 12)); assertEquals(1080, card.width); assertEquals(1080, card.height)
    }

    @Test fun resetAsksFirst() {
        var resets = 0
        show { de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "0.5.3", de.raumlicht.app.ui.settings.UpdateState.Idle, {}, onReset = { resets++ }) }
        rule.onNodeWithTag("zuruecksetzen").performScrollTo().performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Wirklich alles zurücksetzen?").assertExists()
        rule.onNodeWithTag("zuruecksetzen-nein").performClick(); rule.mainClock.advanceTimeBy(500); assertEquals(0, resets)      // Abbrechen löscht nichts
        rule.onNodeWithTag("zuruecksetzen").performScrollTo().performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("zuruecksetzen-ja").performClick(); rule.mainClock.advanceTimeBy(500); assertEquals(1, resets)
        rule.onNodeWithText("✓ Alles zurückgesetzt – du startest wieder bei null.").assertExists()
    }

    /** Medien-Lautstärke 0 → die Einstellungen sagen, warum nichts zu hören ist. Der Klingelton-Schalter (stumm/Vibration) spielt keine Rolle. */
    @Test fun settingsExplainSilence() {
        val audio = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getSystemService(android.media.AudioManager::class.java)
        audio.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE; audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 5, 0)
        show { de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "2.18.12", de.raumlicht.app.ui.settings.UpdateState.Idle, {}) }
        rule.onNodeWithTag("ton-hinweis").assertDoesNotExist()                       // Handy auf Vibration, Medien laut: kein Hinweis – Raumlicht spielt
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
        rule.mainClock.advanceTimeBy(1500); rule.onNodeWithTag("ton-hinweis").assertTextContains("Medien-Lautstärke", substring = true)
        rule.onRoot().captureRoboImage("../screenshots/80_ton_hinweis.png")
        audio.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL; audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 5, 0)
        rule.mainClock.advanceTimeBy(1500); rule.onNodeWithTag("ton-hinweis").assertDoesNotExist()
    }

    @Test fun volumeSliderInSettings() {
        var v = 100
        show { de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "0.5.4", de.raumlicht.app.ui.settings.UpdateState.Idle, {}, volume = v, onVolume = { v = it }) }
        rule.onNodeWithText("🔉 Lautstärke: 100 %").assertExists()
        slide("lautstaerke", 0.42f); assertEquals(40, v)                                                    // in Schritten von 5 %
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = de.raumlicht.core.data.Settings(ctx); assertEquals(100, s.volume.value); s.setVolume(130); assertEquals(100, s.volume.value); s.setVolume(35); assertEquals(35, de.raumlicht.core.data.Settings(ctx).volume.value)
    }

    @Test fun ownFunctionCanBeTypedBuiltAndContinued() {
        var stored = ""
        show { RideScreen({}, controller = stage, onCustom = { stored = it }) }
        rule.onNodeWithTag("bahn-eigene").performClick(); rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag("eigene-eingabe").performTextReplacement("2*(x+1"); rule.onNodeWithTag("eigene-bauen").performClick(); rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag("eigene-fehler").assertTextEquals("Hier fehlt eine schließende Klammer.")                 // Fehler wird erklärt, nichts gebaut
        assertEquals("", stored)
        rule.onNodeWithTag("eigene-eingabe").performTextReplacement("x^2-"); rule.onNodeWithTag("taste-x").performClick()   // Tasten für schwer zu findende Zeichen
        rule.onNodeWithTag("von-plus").performClick(); rule.onNodeWithTag("bis-plus").performClick(); rule.onNodeWithTag("bis-plus").performClick()   // Abschnitt −1 bis 4
        rule.onNodeWithTag("eigene-bauen").performClick(); rule.mainClock.advanceTimeBy(600)
        assertEquals("x^2-x|-1|4", stored)
        rule.onNodeWithTag("term", useUnmergedTree = true).assertTextEquals("f(x) = x²−x"); rule.onNodeWithTag("abschnitt", useUnmergedTree = true).assertTextEquals("x: −1 bis 4"); rule.onNodeWithTag("eigene-eingabe").assertDoesNotExist()
        assertTrue(stage.lastState!!.contains("\"type\":\"axes2d\"") && stage.lastState!!.contains("\"label\":\"N\"") && stage.lastState!!.contains("\"label\":\"T\""))
        slide("fahrt-regler", 0.3f)                                                                                      // x = 0,5: Tiefpunkt
        rule.onNodeWithText("Tiefpunkt bei x = 0,5").assertExists(); rule.onNodeWithText("→ 0").assertExists()
        rule.onNodeWithTag("weiter-abschnitt").performClick(); rule.mainClock.advanceTimeBy(600)                         // nahtlos weiter: 4 bis 9
        assertEquals("x^2-x|4|9", stored); rule.onNodeWithTag("term", useUnmergedTree = true).assertTextEquals("f(x) = x²−x"); rule.onNodeWithTag("abschnitt", useUnmergedTree = true).assertTextEquals("x: 4 bis 9")
        rule.onNodeWithText("12").assertExists()                                                                         // Start des neuen Abschnitts: f(4) = 12
        rule.onNodeWithTag("eigene-aendern").performClick(); rule.mainClock.advanceTimeBy(300); rule.onNodeWithTag("eigene-eingabe").assertExists()
        // die Zeile über der Bühne öffnet die Eingabe ebenfalls
        rule.onNodeWithTag("eigene-bauen").performClick(); rule.mainClock.advanceTimeBy(500); rule.onNodeWithTag("eigene-eingabe").assertDoesNotExist()
        rule.onNodeWithTag("eigene-zeile").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("eigene-eingabe").assertExists()
    }

    @Test fun ownFunctionIsRememberedAndPolesAreFlagged() {
        show { RideScreen({}, de.raumlicht.core.model.Coaster.CUSTOM, controller = stage, custom = "1/x|-2|2") }
        rule.onNodeWithTag("abgeschnitten").assertExists(); rule.onNodeWithTag("term", useUnmergedTree = true).assertTextEquals("f(x) = 1/x"); rule.onNodeWithTag("abschnitt", useUnmergedTree = true).assertTextEquals("x: −2 bis 2")       // steht sofort wieder da
        rule.onNodeWithTag("eigene-aendern").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("von").assertTextEquals("-2"); rule.onNodeWithTag("bis").assertTextEquals("2")
        // den sichtbaren Abschnitt direkt eintippen – auch weit weg und negativ
        rule.onNodeWithTag("von").performTextReplacement("-40"); rule.onNodeWithTag("bis").performTextReplacement("-10"); rule.onNodeWithTag("eigene-bauen").performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("term", useUnmergedTree = true).assertTextEquals("f(x) = 1/x"); rule.onNodeWithTag("abschnitt", useUnmergedTree = true).assertTextEquals("x: −40 bis −10")
        rule.onNodeWithTag("eigene-zeile").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("bis").performTextReplacement("-50"); rule.onNodeWithTag("eigene-bauen").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("eigene-fehler").assertTextEquals("„von“ muss kleiner sein als „bis“.")
    }

    @Test fun rideSpeedCanBeChanged() {
        show { RideScreen({}, "huegel", controller = stage) }
        fun runFor(ms: Long): Double { rule.mainClock.autoAdvance = false; rule.onNodeWithTag("fahren").performClick(); rule.mainClock.advanceTimeBy(ms); rule.onNodeWithTag("fahren").performClick(); rule.mainClock.advanceTimeBy(100)
            rule.mainClock.autoAdvance = true; return Regex("u:([0-9.E-]+)").find(stage.lastRide!!)!!.groupValues[1].toDouble() }
        val normal = runFor(1000)
        slide("fahrt-regler", 0f)
        rule.onNodeWithText("Tempo 1×").assertExists(); rule.onNodeWithTag("tempo").performClick(); rule.onNodeWithTag("tempo").performClick(); rule.onNodeWithText("⚡ 4×").assertExists()
        val fast = runFor(1000)
        assertTrue("$normal $fast", fast > normal * 2.5)
        rule.onNodeWithTag("tempo").performClick(); rule.onNodeWithText("🐢 ½×").assertExists()
    }

    @Test fun balloonBoothIsJustForFun() {
        val scores = ArrayList<Int>(); val g = BalloonGame(5 + 1)                          // Runde 1 nutzt seed + 1
        show { BalloonScreen(3, { scores += it }, {}, controller = stage, seed = 5) }
        rule.onNodeWithText("Nur zum Spaß 🎈").assertExists(); rule.onNodeWithTag("bestwert").assertTextEquals("Bestwert 3")
        stage.onTap?.invoke("b0"); rule.onNodeWithTag("punkte").assertTextEquals("0")       // vor dem Start zählt nichts
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("ballern").performClick(); rule.mainClock.advanceTimeBy(4000)
        val t = 4.0; val up = g.alive(t - 0.2, emptySet()).filter { it.kind != BalloonKind.DUD }; assertTrue(up.isNotEmpty())
        assertTrue(stage.lastState!!.contains("\"id\":\"b${up[0].id}\""))
        stage.onTap?.invoke("b${up[0].id}"); rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("punkte").assertTextEquals("${up[0].kind.points}")
        assertTrue(stage.log.any { it.startsWith("RL.fire") } && stage.log.any { it.startsWith("RL.burst") })    // Lux ballert
        assertFalse(stage.lastState!!.contains("\"id\":\"b${up[0].id}\""))
        stage.onTap?.invoke("b${up[0].id}"); rule.mainClock.advanceTimeBy(100); rule.onNodeWithTag("punkte").assertTextEquals("${up[0].kind.points}")   // weg ist weg
        // daneben getippt: Lux schießt genau dorthin, Punkte bleiben, Serie beginnt von vorn
        val shots = stage.log.count { it.startsWith("RL.fire") }
        stage.onMiss?.invoke(V3(4.0, 6.5, 5.0)); rule.mainClock.advanceTimeBy(100)
        assertEquals(shots + 1, stage.log.count { it.startsWith("RL.fire") }); assertTrue(stage.log.last { it.startsWith("RL.fire") }.contains("to:[4.0,6.5,5.0]") )
        rule.onNodeWithTag("meldung").assertTextEquals("Daneben!"); rule.onNodeWithTag("punkte").assertTextEquals("${up[0].kind.points}")
        rule.mainClock.advanceTimeBy(45_000)
        rule.onNodeWithTag("ergebnis").assertExists(); rule.onNodeWithText("↻ Noch eine Runde").assertExists()
        assertEquals(listOf(up[0].kind.points), scores)
        rule.mainClock.autoAdvance = true
    }

    @Test fun luxReadsHerSpeechBubblesAloud() {
        val heard = ArrayList<String>(); val l = Levels.byId("13-3")!!
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }, de.raumlicht.app.ui.fx.LocalSpeaker provides de.raumlicht.app.ui.fx.Speaker { heard += it }) {
            LevelScreen(l, Levels.chapters.first { it.number == 13 }, false, {}, { _, _ -> }, {}, {}, mapOf("a" to listOf(2)))
        } } } }
        rule.mainClock.advanceTimeBy(600); assertEquals(listOf(l.mission), heard)                          // Auftrag beim Öffnen
        rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(8000)
        assertTrue(heard.toString(), heard.any { it.startsWith("1. Zug: pink") } && heard.any { it.contains("nur noch 1 von 4") } && heard.last() == "Genau 10 % – Treffer!")
        assertEquals("1. Zug: pink, 2 von 5. Die Kugel bleibt draußen.", Speech.clean(heard[1]))
    }

    /** Seit 2.18.14 ist das Vorlesen abgeschaltet: kein Schalter, kein Sprechtempo, kein Offline-Laden – und niemand spricht. Musik und Geräusche bleiben getrennt schaltbar. */
    @Test fun vorlesenIstAbgeschaltet() {
        assertFalse(de.raumlicht.core.model.Vorlesen.ENABLED)
        val heard = ArrayList<String>()
        show { CompositionLocalProvider(de.raumlicht.app.ui.fx.LocalSpeaker provides de.raumlicht.app.ui.fx.Speaker { heard += it }) {
            de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "2.18.14", de.raumlicht.app.ui.settings.UpdateState.Idle, {}, voice = true) } }
        rule.onNodeWithText("🗣️ Lux spricht").assertDoesNotExist(); rule.onNodeWithTag("stimme-name").assertDoesNotExist(); rule.onNodeWithTag("sprechtempo").assertDoesNotExist()
        rule.onNodeWithTag("hoerprobe").assertDoesNotExist(); rule.onNodeWithTag("offline").assertDoesNotExist()
        rule.onNodeWithText("🎵 Musik").assertExists(); rule.onNodeWithText("🔊 Geräusche").assertExists(); rule.onNodeWithText("Für alles zusammen: Musik und Geräusche").assertExists()
        rule.onNodeWithText("Vorlesen", substring = true).assertDoesNotExist(); rule.onNodeWithText("Nova", substring = true).assertDoesNotExist()
        assertTrue(heard.isEmpty())
    }

    @Test fun voiceSwitchInSettings() {
        var on = true; val heard = ArrayList<String>()
        show { CompositionLocalProvider(de.raumlicht.app.ui.fx.LocalSpeaker provides de.raumlicht.app.ui.fx.Speaker { heard += it }) {
            de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "2.0.0", de.raumlicht.app.ui.settings.UpdateState.Idle, {}, voice = on, onVoice = { on = it }, voiceOffered = true) } }
        rule.onNodeWithText("🗣️ Lux spricht").assertExists()
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = de.raumlicht.core.data.Settings(ctx); assertFalse(s.voice.value) /* aus, bis man es einschaltet */; s.setVoice(true); assertTrue(de.raumlicht.core.data.Settings(ctx).voice.value)
        val v = de.raumlicht.app.ui.fx.LuxVoice(ctx); v.enabled = false; v.say("still"); v.stop(); v.shutdown()      // ohne Stimme oder ausgeschaltet: kein Absturz
    }

    @Test fun ownFlightCanBeDefined() {
        var stored = ""
        show { FlightScreen({}, controller = stage, onCustom = { stored = it }) }
        rule.onNodeWithTag("flug-eigener").performClick(); rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag("richtung-0").performTextReplacement("0"); rule.onNodeWithTag("richtung-1").performTextReplacement("0"); rule.onNodeWithTag("richtung-2").performTextReplacement("0")
        rule.onNodeWithTag("flug-bauen").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("flug-fehler").assertTextEquals("Die Richtung darf nicht (0|0|0) sein – sonst fliege ich nirgendwohin."); assertEquals("", stored)
        rule.onNodeWithTag("richtung-0").performTextReplacement("1"); rule.onNodeWithTag("richtung-1").performTextReplacement("1")           // g: (0|0|1) + t·(1|1|0)
        rule.onNodeWithTag("e2-an").performClick()                                                                                         // zweite Ebene dazu: x₁ + x₂ + x₃ = 16 → hier 1,1,1 = 16
        rule.onNodeWithTag("e2-3").performTextReplacement("9")                                                                             // x₁ + x₂ + x₃ = 9 → t = 4
        rule.onNodeWithTag("kugel-an").performClick()                                                                                      // Kugel weg
        rule.onNodeWithTag("flug-bauen").performClick(); rule.mainClock.advanceTimeBy(600)
        assertEquals("0,0,1;1,1,0;1,0,0,4;1,1,1,9;", stored)
        rule.onNodeWithTag("flug-term", useUnmergedTree = true).assertTextEquals("g: x⃗ = (0|0|1) + t · (1|1|0)")
        assertTrue(stage.lastState!!.contains("E₂: x₁ + x₂ + x₃ = 9") && !stage.lastState!!.contains("\"type\":\"sphere\""))
        slide("flug-regler", 0.5f)                                                                                                          // t = 4 von 8: beide Ebenen liegen bei t = 4
        rule.onNodeWithText("Du bist genau in der Ebene").assertExists()
        rule.onNodeWithTag("flug-zeile").performClick(); rule.mainClock.advanceTimeBy(300); rule.onNodeWithTag("start-0").assertExists()
    }

    @Test fun balloonQuizCountsOnlyFittingNumbers() {
        val scores = ArrayList<Int>(); val g = BalloonGame(8 + 1, mode = BalloonMode.QUIZ)
        show { BalloonScreen(0, { scores += it }, {}, controller = stage, seed = 8) }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("ballern-aufgabe").performClick(); rule.mainClock.advanceTimeBy(5000)
        rule.onNodeWithTag("regel").assertTextEquals("Schieß ab: ${g.rules[0].text}")
        val up = g.alive(4.8, emptySet()); val good = up.first { g.rules[0].ok(g.numbers[it.id]) }; val bad = up.first { !g.rules[0].ok(g.numbers[it.id]) }
        stage.onTap?.invoke("b${good.id}"); rule.mainClock.advanceTimeBy(100); rule.onNodeWithTag("punkte").assertTextEquals("1")
        stage.onTap?.invoke("b${bad.id}"); rule.mainClock.advanceTimeBy(100); rule.onNodeWithTag("punkte").assertTextEquals("0")
        rule.onNodeWithTag("meldung").assertTextEquals("${g.numbers[bad.id]} passt nicht! −1")
        rule.mainClock.advanceTimeBy(13_000); rule.onNodeWithTag("regel").assertTextEquals("Schieß ab: ${g.rules[1].text}")     // nach 15 s die nächste Regel
        // gewertet wird nach der Regel, die JETZT gilt – nicht nach der vom Rundenbeginn
        val later = g.alive(18.0, setOf(good.id, bad.id)); val fits2 = later.first { g.rules[1].ok(g.numbers[it.id]) }; val not2 = later.first { !g.rules[1].ok(g.numbers[it.id]) }
        stage.onTap?.invoke("b${fits2.id}"); rule.mainClock.advanceTimeBy(100); rule.onNodeWithTag("punkte").assertTextEquals("1")
        stage.onTap?.invoke("b${not2.id}"); rule.mainClock.advanceTimeBy(100); rule.onNodeWithTag("meldung").assertTextEquals("${g.numbers[not2.id]} passt nicht! −1")
        // bei 0 Punkten wird nichts abgezogen – dann steht auch kein „−1“ in der Meldung
        rule.onNodeWithTag("punkte").assertTextEquals("0")
        later.firstOrNull { !g.rules[1].ok(g.numbers[it.id]) && it.id != not2.id }?.let { not3 ->
            stage.onTap?.invoke("b${not3.id}"); rule.mainClock.advanceTimeBy(100); rule.onNodeWithTag("meldung").assertTextEquals("${g.numbers[not3.id]} passt nicht!"); rule.onNodeWithTag("punkte").assertTextEquals("0") }
        rule.mainClock.advanceTimeBy(40_000); rule.onNodeWithTag("ergebnis").assertExists(); assertEquals(emptyList<Int>(), scores)   // Bestwert gibt es nur im Spaß-Modus
        rule.mainClock.autoAdvance = true
    }

    @Test fun balloonDuelLeftAgainstRight() {
        val g = BalloonGame(2 + 1, mode = BalloonMode.DUEL)
        show { BalloonScreen(0, {}, {}, controller = stage, seed = 2) }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("ballern-duell").performClick(); rule.mainClock.advanceTimeBy(6000)
        val up = g.alive(5.8, emptySet()).filter { it.kind != BalloonKind.DUD }
        val right = up.first { g.side(g.pos(it, 6.0)) == 1 && g.side(g.pos(it, 5.6)) == 1 }
        stage.onTap?.invoke("b${right.id}"); rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("punkte").assertTextEquals("0"); rule.onNodeWithTag("punkte-rechts").assertTextEquals("${right.kind.points}")
        assertTrue(stage.lastState!!.contains("\"id\":\"mitte\""))
        rule.mainClock.advanceTimeBy(45_000); rule.onNodeWithTag("ausgang").assertTextEquals("Rechts gewinnt!")
        rule.mainClock.autoAdvance = true
    }

    @Test fun chanceMachineBetAndRun() {
        show { ChanceScreen({}, seed = 6) }
        rule.onNodeWithTag("p").assertTextEquals("p = 50 %"); rule.onNodeWithTag("versuch-wuerfel").performClick(); rule.onNodeWithTag("p").assertTextEquals("p = 16,67 %")
        rule.onNodeWithTag("versuch-eigene").performClick()
        rule.onNodeWithTag("p-eingabe").performTextReplacement(""); rule.onNodeWithTag("p-eingabe").assertTextEquals(""); rule.onNodeWithTag("p").assertTextEquals("p = 30 %")       // Feld darf leer sein, der Wert bleibt
        rule.onNodeWithTag("p-eingabe").performTextReplacement("8"); rule.onNodeWithTag("p").assertTextEquals("p = 8 %")
        rule.onNodeWithTag("p-plus").performClick(); rule.onNodeWithTag("p-eingabe").assertTextEquals("13")
        rule.onNodeWithTag("p-eingabe").performTextReplacement("80"); rule.onNodeWithTag("p").assertTextEquals("p = 80 %")   // eigene Chance
        rule.onNodeWithTag("n-1000").performClick(); slide("wett-regler", 0.8f)
        rule.onNodeWithTag("wette").assertTextEquals("Deine Wette: Bei 1000 Versuchen kommt 800-mal ein Treffer.")
        rule.onNodeWithText("800").assertExists()                                                                 // erwartet n · p
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("werfen").performClick(); rule.mainClock.advanceTimeBy(1500)
        rule.onNodeWithText("läuft …").assertExists()
        rule.mainClock.advanceTimeBy(3000); rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(300)
        val run = ChanceRun(0.8, 1000, 6 + 1)
        rule.onNodeWithText("${run.total} von 1000").assertExists(); rule.onNodeWithTag("wette").assertTextEquals(run.verdict(800)); rule.onNodeWithText("↻ Noch einmal").assertExists()
    }

    @Test fun galtonRowsCanBeChosen() {
        show { GaltonScreen({}, controller = stage, random = Random(3)) }
        rule.onNodeWithText("Reihen").assertExists(); assertTrue(stage.lastState!!.contains("nägel8"))
        rule.onNodeWithTag("plus-n").performClick(); rule.onNodeWithTag("plus-n").performClick(); rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithText("5").assertExists(); assertTrue(stage.lastState!!.contains("nägel10"))     // μ = 10 · 0,5
        repeat(8) { rule.onNodeWithTag("minus-n").performClick() }; rule.mainClock.advanceTimeBy(400); assertTrue(stage.lastState!!.contains("nägel4"))   // unten ist bei 4 Schluss
    }

    @Test fun coasterBuilderNeedsEnoughEnergy() {
        show { CoasterBuildScreen({}, controller = stage) }
        assertTrue(stage.lastState!!.contains("\"type\":\"handle\"") && stage.lastState!!.contains("\"flat\":true"))
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("probefahrt").performClick(); rule.mainClock.advanceTimeBy(15_000)                // eben: rollt aus
        rule.onNodeWithTag("bau-meldung").assertTextContains("geht dem Wagen der Schwung aus", substring = true)
        rule.mainClock.autoAdvance = true
        repeat(2) { rule.onNodeWithTag("hoch-0").performClick() }; rule.onNodeWithTag("hoch-1").performClick(); rule.onNodeWithTag("runter-3").performClick(); repeat(2) { rule.onNodeWithTag("runter-4").performClick() }
        stage.onDrag?.invoke("h2", listOf(4, 4, 6)); rule.mainClock.advanceTimeBy(200); rule.onNodeWithTag("hoehe-2").assertTextEquals("6")      // Ziehen in der Bühne
        stage.onDrag?.invoke("h2", listOf(4, 4, 3)); rule.mainClock.advanceTimeBy(200)
        assertEquals(listOf("5", "4", "3", "2", "1"), (0..4).map { i -> rule.onNodeWithTag("hoehe-$i").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text })
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("bau-sicht").performClick()                                                                    // einsteigen
        rule.onNodeWithTag("probefahrt").performClick(); rule.mainClock.advanceTimeBy(1000)
        assertTrue(stage.lastRide!!.contains("view:'fp'"))
        assertFalse(stage.lastState!!.contains("\"type\":\"handle\"")); assertTrue(stage.lastRide!!.contains("id:'bahn'"))                 // während der Fahrt keine Griffe
        rule.mainClock.advanceTimeBy(14_000); rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(300)
        assertTrue(stage.lastRide!!.contains("view:'out'"))                                                                 // nach der Fahrt wieder von außen
        rule.onNodeWithTag("bau-meldung").assertTextContains("Geschafft!", substring = true); rule.onNodeWithText("1/5 ✓").assertExists(); rule.onNodeWithText("✓ Erste Fahrt").assertExists()
        rule.onNodeWithTag("auftrag-frei").performClick(); rule.mainClock.advanceTimeBy(300); rule.onNodeWithTag("bau-meldung").assertTextContains("Bau, was du willst", substring = true)
    }

    @Test fun ghostDrawingIsScoredAgainstTheRealDerivative() {
        show { GhostScreen({}) }
        val r = Coaster.byId("huegel")!!; val (lo, hi) = Ghost.window(r)                                      // f′ = −2x + 4: Gerade von 4 nach −4
        rule.onNodeWithTag("aufloesen").assertIsNotEnabled()
        rule.onNodeWithTag("zeichnen").performTouchInput {
            fun y(v: Double) = (height * (hi - v) / (hi - lo)).toFloat()
            down(Offset(2f, y(4.0))); for (k in 1..40) moveTo(Offset(width * k / 40f - 2f, y(4.0 - 8.0 * k / 40))); up() }
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("aufloesen").assertIsEnabled().performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("geist-meldung").assertTextContains("Wie abgepaust!", substring = true)
        rule.onNodeWithTag("geist-neu").performClick(); rule.mainClock.advanceTimeBy(200); rule.onNodeWithTag("geist-meldung").assertTextContains("Zeichne die ganze Breite", substring = true)
        // waagerechter Strich bei 0 statt der fallenden Geraden: deutlich schlechter
        rule.onNodeWithTag("zeichnen").performTouchInput { val y0 = (height * hi / (hi - lo)).toFloat(); down(Offset(2f, y0)); for (k in 1..40) moveTo(Offset(width * k / 40f - 2f, y0)); up() }
        rule.onNodeWithTag("aufloesen").performClick(); rule.mainClock.advanceTimeBy(300)
        val flat = Ghost.score(r, List(Ghost.COLUMNS) { 0.0 }); assertTrue(flat < 75)
        val said = rule.onNodeWithTag("geist-meldung").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        assertTrue(said, !said.startsWith("Wie abgepaust") && !said.startsWith("Stark") && said.contains(" %"))
    }

    @Test fun ringFlightNeedsTheRightDirection() {
        val run = RingRun(5)
        show { RingScreen({}, controller = stage, seed = 5) }
        rule.onNodeWithTag("ring-daten").assertTextContains("A${run.a.fmt()}", substring = true)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("ring-flug").performClick(); rule.mainClock.advanceTimeBy(5000); rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(200)     // (1|0|0) – daneben
        if (run.d != V3(1, 0, 0)) rule.onNodeWithTag("ring-meldung").assertTextContains("Tipp: Richtung = R₁ − A", substring = true)
        val want = listOf(run.d.x.toInt(), run.d.y.toInt(), run.d.z.toInt()); val have = listOf(1, 0, 0)
        (0..2).forEach { i -> repeat(kotlin.math.abs(want[i] - have[i])) { rule.onNodeWithTag(if (want[i] > have[i]) "plus-d$i" else "minus-d$i").performClick() } }
        rule.mainClock.advanceTimeBy(300); assertTrue(stage.lastState!!.contains("\"color\":\"green\""))
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("ring-flug").performClick(); rule.mainClock.advanceTimeBy(5000); rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(200)
        rule.onNodeWithTag("ring-meldung").assertTextEquals("Alle drei Ringe! Richtung = Ring minus Start.")
        rule.onNodeWithTag("ring-weiter").performClick(); rule.mainClock.advanceTimeBy(300); rule.onNodeWithTag("ring-runde").assertTextEquals("Runde 2")
        rule.onNodeWithTag("ring-daten").assertTextContains("A${RingRun(6).a.fmt()}", substring = true)
    }

    @Test fun luxPicksThePleasantVoice() {
        val all = listOf(de.raumlicht.app.ui.fx.VoiceInfo("de-netz-top", 500, network = true, missing = false), de.raumlicht.app.ui.fx.VoiceInfo("de-lokal-gut", 400, false, false),
            de.raumlicht.app.ui.fx.VoiceInfo("de-lokal-einfach", 200, false, false), de.raumlicht.app.ui.fx.VoiceInfo("de-fehlt", 500, false, missing = true), de.raumlicht.app.ui.fx.VoiceInfo("de-lokal-b", 400, false, false))
        val ok = de.raumlicht.app.ui.fx.usable(all)
        assertEquals(listOf("de-lokal-b", "de-lokal-gut", "de-lokal-einfach"), ok.map { it.name })               // ohne Netz, installiert, beste zuerst
        assertEquals("de-lokal-b", de.raumlicht.app.ui.fx.choose(ok, null)); assertEquals("de-lokal-einfach", de.raumlicht.app.ui.fx.choose(ok, "de-lokal-einfach"))
        assertEquals("de-lokal-b", de.raumlicht.app.ui.fx.choose(ok, "gibt-es-nicht-mehr")); assertNull(de.raumlicht.app.ui.fx.choose(emptyList(), "x"))
    }

    @Test fun rideStaysCalmWhenSpecialPointsComeFast() {
        val heard = ArrayList<String>(); var clock = 0L
        show { CompositionLocalProvider(de.raumlicht.app.ui.fx.LocalSpeaker provides de.raumlicht.app.ui.fx.Speaker { heard += it }) { RideScreen({}, "doppel", controller = stage, now = { clock }) } }
        // nach jedem Schieben abwarten, bis die Meldung dazu wirklich verarbeitet ist – erst dann läuft die Uhr weiter
        fun go(u: Float) { slide("fahrt-regler", u); rule.waitForIdle(); rule.mainClock.advanceTimeBy(400); rule.waitForIdle() }
        fun parties() = stage.log.count { it.startsWith("RL.party") }
        // Die Bahn beginnt auf einer Nullstelle (x = 0, am Rand): Sie bekommt ihr Schild, aber keine Ansage und keine Lichtshow –
        // sonst wäre die Ruhe für die erste Stelle der Fahrt schon verbraucht, bevor es losgeht
        rule.waitForIdle(); rule.onNodeWithText("Nullstelle bei x = 0").assertExists(); assertEquals(0, parties()); assertEquals(emptyList<String>(), heard)
        go(0.25f)                                                    // x = 1: Hochpunkt – in Ruhe erreicht
        rule.onNodeWithText("Hochpunkt bei x = 1").assertExists(); assertEquals("ride=" + stage.lastRide, 1, parties())
        assertEquals(listOf("Hochpunkt bei x = 1"), heard)
        clock += 400; go(0.5f)                                        // 0,4 s später schon der Wendepunkt: Schild ja, aber keine Ansage und keine Lichtshow
        rule.onNodeWithText("Wendepunkt bei x = 2").assertExists(); assertEquals(1, heard.size); assertEquals(1, parties())
        clock += 400; go(0.75f)                                       // und gleich der Tiefpunkt: weiter ruhig
        rule.onNodeWithText("Tiefpunkt und Nullstelle bei x = 3").assertExists()   // fallen zwei Stellen zusammen, werden beide genannt
        assertEquals(1, heard.size); assertEquals(1, parties())
        clock += 3000; go(0.5f)                                       // nach einer Pause wird wieder angesagt
        assertEquals(listOf("Hochpunkt bei x = 1", "Wendepunkt bei x = 2"), heard); assertEquals(2, parties())
    }

    /** Binomial-Labor: n, p, k und die Frage frei einstellbar; das Ergebnis stimmt mit der Formel überein */
    @Test fun binomialLab() {
        show { de.raumlicht.app.ui.home.BinomialScreen({}) }
        rule.onNodeWithTag("ergebnis").assertTextEquals("P(X ≤ 3) = 17,19 %")                      // B(10; 0,5), höchstens 3
        rule.onNodeWithText("Mit 66 % zwischen 4 und 6 Treffern (μ ± σ)").assertExists()            // Streubereich mit der echten Wahrscheinlichkeit statt „meist“
        rule.onNodeWithTag("frage-exact").performClick(); rule.onNodeWithTag("ergebnis").assertTextEquals("P(X = 3) = 11,72 %")
        rule.onNodeWithTag("ansatz").assertTextEquals("P(X = 3) = (10 über 3) · 0,5^3 · 0,5^7")
        rule.onNodeWithTag("frage-at_least").performClick(); rule.onNodeWithTag("ergebnis").assertTextEquals("P(X ≥ 3) = 94,53 %")
        rule.onNodeWithTag("plus-k").performClick(); rule.onNodeWithTag("ergebnis").assertTextEquals("P(X ≥ 4) = 82,81 %")
        rule.onNodeWithTag("n-20").performClick(); rule.onNodeWithTag("p-minus10").performClick(); rule.onNodeWithTag("verteilung").assertTextEquals("B(20; 0,4)")
        rule.onNodeWithTag("mu", useUnmergedTree = true).onChildren().assertAny(hasText("8"))
        rule.onNodeWithTag("saeulen").performClick(); rule.onNodeWithText("P(X ≤ k) aufsummiert").assertExists()
        // Säule antippen wählt k: ganz links ist k = 0
        rule.onNodeWithTag("saeulenbild").performTouchInput { click(androidx.compose.ui.geometry.Offset(3f, height / 2f)) }
        rule.onNodeWithTag("ergebnis").assertTextEquals("P(X ≥ 0) = 100,00 %")
        rule.onNodeWithTag("n-100").performClick(); rule.onNodeWithTag("frage-at_most").performClick(); rule.onNodeWithTag("ergebnis").assertTextContains("P(X ≤ 0)", substring = true)
    }

    /** Endlos üben: Stufe wählen – es bleiben nur Themen mit Aufgaben dieser Stufe, und die Wahl kommt beim Antippen mit */
    @Test fun jamLevelPicker() {
        val picked = ArrayList<Pair<de.raumlicht.core.model.JamTopic, de.raumlicht.core.model.JamLevel?>>(); var saved = "?"
        show { de.raumlicht.app.ui.home.JamPicker(0, {}, {}, onLevel = { saved = it }, onPick = { t, l -> picked += t to l }) }
        rule.onNodeWithTag("stufe-text").assertTextEquals("Alle Stufen gemischt"); rule.onNodeWithTag("thema-SCHNITT").assertExists()
        rule.onNodeWithTag("stufe-profi").performClick(); assertEquals("PROFI", saved)
        rule.onNodeWithTag("thema-SCHNITT").assertDoesNotExist(); rule.onNodeWithTag("thema-BEWEGUNG").assertDoesNotExist(); rule.onNodeWithTag("thema-PUNKTE").assertExists()
        rule.onNodeWithTag("thema-WINKEL").performClick(); rule.onNodeWithTag("thema-MIX").performScrollTo().performClick()
        assertEquals(listOf(de.raumlicht.core.model.JamTopic.WINKEL to de.raumlicht.core.model.JamLevel.PROFI, de.raumlicht.core.model.JamTopic.MIX to de.raumlicht.core.model.JamLevel.PROFI), picked)
        rule.onNodeWithTag("stufe-einstieg").performClick(); rule.onNodeWithTag("thema-PUNKTE").assertExists(); rule.onNodeWithTag("thema-SCHNITT").assertDoesNotExist()
        rule.onNodeWithTag("stufe-alle").performClick(); assertEquals("", saved); rule.onNodeWithTag("thema-SCHNITT").assertExists()
    }
}
