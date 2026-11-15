package de.raumlicht.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.raumlicht.app.ui.home.HomeScreen
import de.raumlicht.app.ui.level.IntroOverlay
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.settings.SettingsScreen
import de.raumlicht.app.ui.settings.UpdateState
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Levels
import de.raumlicht.core.model.Values
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rendert die Bildschirme ohne Gerät – für das README und zum Prüfen vor jeder Auslieferung.
 * Die 3D-Bühne ist ein vorab gerendertes Bild derselben Szene (siehe SceneDumpTest und scene/test/shot.mjs).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun stageImage(name: String): @Composable (StageController, Modifier) -> Unit = { _, m ->
        val bmp = javaClass.classLoader!!.getResourceAsStream("stage/$name.png")?.use { BitmapFactory.decodeStream(it) }
        if (bmp != null) Image(bmp.asImageBitmap(), null, m, contentScale = ContentScale.Crop) else Box(m.background(Q.Deep))
    }

    private fun shot(name: String, stage: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}, content: @Composable () -> Unit) {
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides stageImage(stage)) { content() } } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private fun level(name: String, id: String, stage: String = id, values: Values? = null, solve: Boolean = false, go: Boolean = solve) {
        val l = Levels.byId(id)!!
        shot(name, stage, if (go) 5000 else 1500, prepare = { if (go) it.onNodeWithText("▶ Los").performClick() }) {
            LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, false, {}, { _, _ -> }, {}, {}, if (solve) l.solution else values)
        }
    }

    private val someStars = Levels.all.take(8).mapIndexed { i, l -> l.id to (3 - i % 3 % 2) }.toMap()

    @Test fun home() = shot("01_start", "start") { HomeScreen(Levels.chapters, someStars, {}, {}, jamBest = 7) }
    @Test fun intro() = shot("02_lux_erklaert", "start", prepare = { it.onNodeWithText("Weiter ›").performClick() }) { IntroOverlay(Levels.chapters[0]) {} }
    @Test fun point() = level("03_punkt", "1-2", values = mapOf("P" to listOf(2, 3, 0)))
    @Test fun pointMiss() = level("04_daneben", "1-2", values = mapOf("P" to listOf(2, 3, 0)), go = true)
    @Test fun pyramid() = level("05_pyramide_geschafft", "1-6", "1-6-fertig", solve = true)
    @Test fun vector() = level("06_vektor", "2-2", values = mapOf("v" to listOf(4, -2, 1)))
    @Test fun laserHit() = level("07_laser_treffer", "3-2", "3-2-fertig", solve = true)
    @Test fun crossing() = level("08_kreuzung", "4-2", values = mapOf("P" to listOf(3, 3, 2)))
    @Test fun skew() = level("09_windschief", "4-4")
    @Test fun pierce() = level("10_durchstosspunkt", "5-1", "5-1-fertig", solve = true)
    @Test fun plane() = level("11_ebene_bauen", "5-3", values = mapOf("n" to listOf(1, 2, 2), "c" to listOf(6)))
    @Test fun normal() = level("12_normalenvektor", "5-4", values = mapOf("d" to listOf(1, 0, -2)))
    @Test fun mirror() = level("13_spiegelpunkt", "6-3", "6-3-fertig", solve = true)
    @Test fun bank() = level("14_ueber_bande", "6-5", "6-5-fertig", solve = true)
    @Test fun angle() = level("16_winkel", "7-2")
    @Test fun skewDistance() = level("17_abstand_windschief", "8-4", "8-4-fertig", solve = true)
    @Test fun discoBall() = level("18_discokugel", "9-1", values = mapOf("a" to listOf(2)))
    @Test fun ballHit() = level("19_kugel_treffer", "9-3", "9-3-fertig", solve = true)
    @Test fun dome() = level("20_lichtkuppel", "9-5", values = mapOf("a" to listOf(2)))
    @Test fun sail() = level("21_lichtsegel", "10-1", "10-1-fertig", solve = true)
    @Test fun tent() = level("22_zeltvolumen", "10-3")
    @Test fun jamPicker() = shot("23_jam_session", "start") { de.raumlicht.app.ui.home.JamPicker(7, {}, {}) }
    @Test fun jamLevel() = shot("24_jam_aufgabe", "jam") {
        val l = de.raumlicht.core.model.Gen.jam(de.raumlicht.core.model.JamTopic.MIX, 1, 0)
        LevelScreen(l, de.raumlicht.core.model.Chapter(0, "Alles gemischt", "Alles gemischt", "Jam-Session", "🎧", "pink", emptyList(), emptyList()), false, {}, { _, _ -> }, {}, {},
            caption = "🎧 Jam · ${l.title} · Nr. 4", badge = "🔥 3")
    }
    @Test fun daily() = shot("25_tagesgig", "tag") {
        val l = de.raumlicht.core.model.Daily.forDay(20_772)
        LevelScreen(l, de.raumlicht.core.model.Chapter(0, "Tagesgig", l.title, "Tagesgig", "🎟️", "orange", emptyList(), emptyList()), false, {}, { _, _ -> }, null, {},
            caption = "🎟️ Tagesgig · ${l.title}", doneLabel = "Fertig für heute ✓")
    }
    @Test fun sheet() = shot("26_spickzettel", "start") { de.raumlicht.app.ui.home.SheetScreen(de.raumlicht.core.model.Sheet.cards, onBack = {}) }
    @Test fun motion() = level("28_bewegung", "11-5", values = mapOf("a" to listOf(1)))
    @Test fun vase() = level("29_rotationskoerper", "12-3", values = mapOf("a" to listOf(3)))
    @Test fun backstage() = shot("30_backstage", "start") {
        de.raumlicht.app.ui.home.BackstageScreen(listOf(
            de.raumlicht.app.ui.home.BackstageEntry("🧭", "Lage-Check", "parallel, identisch, schneidend, windschief? · Rekord 6/8", Q.Yellow) {},
            de.raumlicht.app.ui.home.BackstageEntry("⚔️", "Duell", "5 Aufgaben per Code an Freunde schicken", Q.Pink) {},
            de.raumlicht.app.ui.home.BackstageEntry("🔁", "Zweite Chance", "4 Gigs, die nicht beim ersten Versuch saßen", Q.Orange) {},
            de.raumlicht.app.ui.home.BackstageEntry("🎇", "Show-Baukasten", "eigene Lichtshow aus Formeln bauen und teilen", Q.Violet) {},
            de.raumlicht.app.ui.home.BackstageEntry("🎨", "Lux-Werkstatt", "3 von 6 Farben frei", Q.Cyan) {},
            de.raumlicht.app.ui.home.BackstageEntry("📓", "Spickzettel", "alle Formeln – mit Sprung zu den Aufgaben", Q.Green) {},
            de.raumlicht.app.ui.home.BackstageEntry("🏅", "Abzeichen", "9 von 14 verdient", Q.Yellow) {})) {}
    }
    @Test fun examPicker() = shot("31_abi_simulation", "start") { de.raumlicht.app.ui.home.ExamPicker(de.raumlicht.core.model.Exams.all, mapOf("zelt" to 10), { _, _ -> }) {} }
    @Test fun examPart() = shot("32_abi_teilaufgabe", "abi") {
        val e = de.raumlicht.core.model.Exams.all[0]; val part = e.parts[1]
        LevelScreen(part.level, de.raumlicht.core.model.Chapter(0, e.title, e.title, "Abi-Simulation", "🎓", "orange", emptyList(), emptyList()), false, {}, { _, _ -> }, {}, {},
            mapOf("n" to listOf(0, 3, -2), "c" to listOf(3)), caption = "🎓 ${e.title} · Teil b) · 4 BE", badge = "2 BE")
    }
    @Test fun lage() = shot("33_lage_check", "lage", prepare = { it.onNodeWithTag("lage-SKEW").performClick() }) {
        de.raumlicht.app.ui.home.LageScreen(5, { false }, {}, session = 0)
    }
    @Test fun duel() = shot("34_duell", "start") { de.raumlicht.app.ui.home.DuelScreen({}, incoming = de.raumlicht.core.model.Duel(de.raumlicht.core.model.JamTopic.MIX, 4711).code) }
    @Test fun workshop() = shot("35_lux_werkstatt", "start") { de.raumlicht.app.ui.home.WorkshopScreen(42, "green", {}) {} }
    @Test fun builder() = shot("36_show_baukasten", "baukasten") { de.raumlicht.app.ui.home.BuilderScreen(de.raumlicht.core.model.Show.demo, {}, {}) }
    @Test fun badges() = shot("27_abzeichen", "start") {
        de.raumlicht.app.ui.home.BadgesScreen(de.raumlicht.core.model.Badges.all(Levels.all.take(30).associate { it.id to 3 } + mapOf("tag-1" to 3, "tag-2" to 2, "tag-3" to 3), 6)) {}
    }
    @Test fun lottery() = level("38_stochastik", "13-2")
    @Test fun coaster() = level("39_achterbahn", "14-2", values = mapOf("a" to listOf(1)))
    @Test fun area() = level("40_leuchtflaeche", "14-5")
    @Test fun guides() = shot("41_schritt_fuer_schritt", "start") { de.raumlicht.app.ui.home.ExamPicker(de.raumlicht.core.model.Guides.all, emptyMap(), { _, _ -> }, guided = true) {} }
    @Test fun guideStep() = shot("42_schritt", "schritt") {
        val g = de.raumlicht.core.model.Guides.all[0]
        LevelScreen(g.parts[0].level, de.raumlicht.core.model.Chapter(0, g.title, g.title, "Schritt für Schritt", "🧩", "green", emptyList(), emptyList()), false, {}, { _, _ -> }, {}, {},
            mapOf("a" to listOf(1)), caption = "🧩 ${g.title} · Schritt 1 von 2")
    }
    @Test fun learn() = shot("43_lernstand", "start") {
        de.raumlicht.app.ui.home.LearnScreen(de.raumlicht.core.model.Learn.topics(Levels.all.take(9).associate { it.id to 3 } + Levels.all.drop(12).take(8).associate { it.id to 1 }), {}) {}
    }
    @Test fun planner() = shot("44_klausur_planer", "start") {
        de.raumlicht.app.ui.home.PlannerScreen(de.raumlicht.core.model.Plan(20_776, setOf(3, 4, 5)), 20_772, mapOf("3-1" to 3, "3-2" to 2), {}, { _, _, _ -> }) {}
    }
    @Test fun classroom() = shot("45_klasse", "start", prepare = { it.onNodeWithTag("wahl-1-4").performClick(); it.onNodeWithTag("wahl-1-6").performClick() }) {
        de.raumlicht.app.ui.home.ClassScreen({ _, _, _ -> }) {}
    }
    @Test fun match() = shot("46_zu_zweit", "start") { de.raumlicht.app.ui.home.MatchScreen({}) }
    @Test fun calculator() = shot("47_rechner", "baukasten", prepare = { it.onNodeWithTag("rechner").performClick() }) {
        de.raumlicht.app.ui.home.BuilderScreen(de.raumlicht.core.model.Show.demo, {}, {})
    }
    @Test fun rideInside() = shot("49_achterbahn_fahrersicht", "fahrt-gipfel", prepare = { it.onNodeWithTag("fahrt-regler").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { f -> f(0.2f) } }) {
        de.raumlicht.app.ui.home.RideScreen({}, "bergtal")
    }
    @Test fun rideOutside() = shot("50_achterbahn_aussen", "fahrt-aussen", prepare = {
        it.onNodeWithTag("sicht").performClick(); it.onNodeWithTag("fahrt-regler").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { f -> f(0.5f) }
    }) { de.raumlicht.app.ui.home.RideScreen({}, "doppel") }
    @Test fun settings() = shot("15_einstellungen", "start") {
        SettingsScreen(true, {}, true, {}, true, {}, true, {}, "0.1.0", UpdateState.Idle, {})
    }
}
