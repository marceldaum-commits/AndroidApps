package de.raumlicht.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
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
        rule.onNodeWithText("du bist genau in der Ebene").assertExists()
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
        rule.onNodeWithText("▶ Los geht's").performClick(); rule.mainClock.advanceTimeBy(800)
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
        // daneben getippt: Lux schießt genau dorthin, Punkte bleiben, Serie weg
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

    @Test fun voiceSwitchInSettings() {
        var on = true; val heard = ArrayList<String>()
        show { CompositionLocalProvider(de.raumlicht.app.ui.fx.LocalSpeaker provides de.raumlicht.app.ui.fx.Speaker { heard += it }) {
            de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "2.0.0", de.raumlicht.app.ui.settings.UpdateState.Idle, {}, voice = on, onVoice = { on = it }) } }
        rule.onNodeWithText("🗣️ Lux spricht").assertExists()
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = de.raumlicht.core.data.Settings(ctx); assertTrue(s.voice.value); s.setVoice(false); assertFalse(de.raumlicht.core.data.Settings(ctx).voice.value)
        val v = de.raumlicht.app.ui.fx.LuxVoice(ctx); v.enabled = false; v.say("still"); v.stop(); v.shutdown()      // ohne Stimme oder ausgeschaltet: kein Absturz
    }

    @Test fun ownFlightCanBeDefined() {
        var stored = ""
        show { FlightScreen({}, controller = stage, onCustom = { stored = it }) }
        rule.onNodeWithTag("flug-eigener").performClick(); rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag("richtung-0").performTextReplacement("0"); rule.onNodeWithTag("richtung-1").performTextReplacement("0"); rule.onNodeWithTag("richtung-2").performTextReplacement("0")
        rule.onNodeWithTag("flug-bauen").performClick(); rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("flug-fehler").assertTextEquals("Die Richtung darf nicht (0|0|0) sein – sonst fliegt Lux nirgendwohin."); assertEquals("", stored)
        rule.onNodeWithTag("richtung-0").performTextReplacement("1"); rule.onNodeWithTag("richtung-1").performTextReplacement("1")           // g: (0|0|1) + t·(1|1|0)
        rule.onNodeWithTag("e2-an").performClick()                                                                                         // zweite Ebene dazu: x₁ + x₂ + x₃ = 16 → hier 1,1,1 = 16
        rule.onNodeWithTag("e2-3").performTextReplacement("9")                                                                             // x₁ + x₂ + x₃ = 9 → t = 4
        rule.onNodeWithTag("kugel-an").performClick()                                                                                      // Kugel weg
        rule.onNodeWithTag("flug-bauen").performClick(); rule.mainClock.advanceTimeBy(600)
        assertEquals("0,0,1;1,1,0;1,0,0,4;1,1,1,9;", stored)
        rule.onNodeWithTag("flug-term", useUnmergedTree = true).assertTextEquals("g: x⃗ = (0|0|1) + t · (1|1|0)")
        assertTrue(stage.lastState!!.contains("E₂: x₁ + x₂ + x₃ = 9") && !stage.lastState!!.contains("\"type\":\"sphere\""))
        slide("flug-regler", 0.5f)                                                                                                          // t = 4 von 8: beide Ebenen liegen bei t = 4
        rule.onNodeWithText("du bist genau in der Ebene").assertExists()
        rule.onNodeWithTag("flug-zeile").performClick(); rule.mainClock.advanceTimeBy(300); rule.onNodeWithTag("start-0").assertExists()
    }
}
