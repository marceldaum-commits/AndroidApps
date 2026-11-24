package de.experimentopia.app.ui

import androidx.compose.material3.Surface
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.experimentopia.app.ui.experiment.ExperimentScreen
import de.experimentopia.app.ui.experiment.Phase
import de.experimentopia.app.ui.free.FreeLabRun
import de.experimentopia.app.ui.free.FreeLabScreen
import de.experimentopia.app.ui.home.HomeScreen
import de.experimentopia.app.ui.home.IslandScreen
import de.experimentopia.app.ui.journal.BadgesScreen
import de.experimentopia.app.ui.journal.JournalScreen
import de.experimentopia.app.ui.journal.JournalTab
import de.experimentopia.app.ui.settings.ProScreen
import de.experimentopia.app.ui.settings.SettingsScreen
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.ExperimentopiaTheme
import de.experimentopia.core.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rendert die Bildschirme ohne Gerät – für das README und zum Prüfen vor jeder Auslieferung. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}, content: @Composable () -> Unit) {
        // Endlose Animationen (Bläschen, Wellen) würden sonst nie „ruhig“ – die Zeit läuft nur, wenn der Test sie vorstellt
        rule.mainClock.autoAdvance = false
        rule.setContent { ExperimentopiaTheme { Surface(color = E.Night) { content() } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private val someStars = Catalog.all.take(14).mapIndexed { i, e -> e.id to (3 - i % 3) }.toMap() + Catalog.of(Island.STROM).take(3).associate { it.id to 3 }
    private val stats = Stats(done = 17, stars = 38, hits = 9, ahas = 8, explained = 12)

    private fun lab(id: String, name: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}) {
        val e = Catalog.byId(id)!!
        shot(name, settle, prepare) { ExperimentScreen(e, {}, null, {}, startPhase = Phase.LAB, startGuess = 0) }
    }
    /**
     * Sichtbar machen ohne performScrollTo: Das wartet auf eine Scroll-Animation, die bei angehaltener Uhr nie fertig wird.
     * Stattdessen wischen wir selbst nach oben und stellen die Uhr dabei vor.
     */
    private fun ComposeContentTestRule.reveal(node: () -> SemanticsNodeInteraction) {
        var tries = 0
        while (!runCatching { node().assertIsDisplayed() }.isSuccess && tries < 15) {
            onRoot().performTouchInput { swipeUp(startY = bottom * 0.75f, endY = bottom * 0.35f, durationMillis = 200) }
            mainClock.advanceTimeBy(600); tries++
        }
    }
    private fun ComposeContentTestRule.tap(text: String, times: Int = 1) = repeat(times) {
        val find = { onAllNodes(hasText(text, substring = true) and hasClickAction()).onFirst() }
        reveal(find); find().performClick(); mainClock.advanceTimeBy(250)
    }
    private fun ComposeContentTestRule.slide(label: String, v: Float) {
        reveal { onNodeWithTag("regler:$label") }
        onNodeWithTag("regler:$label").performSemanticsAction(SemanticsActions.SetProgress) { it(v) }; mainClock.advanceTimeBy(200)
    }

    @Test fun home() = shot("01_start", settle = 4000) {
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalAllApps provides {}) {
        HomeScreen(someStars, stats, Daily.forDay(20_600), false, 0, true, onIsland = {}, onDaily = {}, onFreeLab = {}, onJournal = {}, onBadges = {}, onSettings = {}, onGrade = {}, onWelcomed = {})
        }
    }
    @Test fun island() = shot("02_insel") { IslandScreen(Island.WASSER, someStars, 4, { false }, {}, {}) }

    @Test fun story() = shot("03_raetsel", settle = 4000) { ExperimentScreen(Catalog.byId("w5")!!, {}, null, {}) }
    @Test fun predict() = shot("04_vermuten", prepare = { it.onNodeWithTag("wahl1").performClick(); it.mainClock.advanceTimeBy(300); it.onNodeWithTag("sicher2").performClick() }) {
        ExperimentScreen(Catalog.byId("w5")!!, {}, null, {}, startPhase = Phase.PREDICT)
    }
    @Test fun water() = lab("w5", "05_ei_im_salzwasser", settle = 3000, prepare = { it.tap("frisches Ei"); it.tap("🧂 Salzwasser"); it.tap("frisches Ei") })
    @Test fun reveal() = shot("06_aha_moment", settle = 4000) { ExperimentScreen(Catalog.byId("w5")!!, {}, null, {}, startPhase = Phase.REVEAL, startGuess = 1) }
    @Test fun explain() = shot("07_blubb_erklaeren", settle = 2500, prepare = { r -> r.reveal { r.onNodeWithTag("grund1") }; r.onNodeWithTag("grund1").performClick() }) {
        ExperimentScreen(Catalog.byId("w5")!!, {}, null, {}, startPhase = Phase.EXPLAIN, startGuess = 1)
    }
    @Test fun storyAgain() = shot("69_raetsel_nochmal", settle = 1500, prepare = { it.onNodeWithTag("raetsel_karte").performClick() }) {
        ExperimentScreen(Catalog.byId("w5")!!, {}, null, {}, startPhase = Phase.LAB, startGuess = 1)
    }
    @Test fun install() = shot("70_app_installieren", settle = 1500, prepare = { it.onNodeWithText("📲 App installieren").performClick() }) {
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalInstaller provides FakeInstaller) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) { de.experimentopia.app.ui.common.InstallCard() }
        }
    }
    @Test fun done() = shot("08_geschafft", settle = 2500) { ExperimentScreen(Catalog.byId("w5")!!, {}, {}, {}, startPhase = Phase.DONE, startGuess = 0) }

    @Test fun tower() = lab("w8", "10_dichteturm", settle = 4000, prepare = { it.tap("Spiritus"); it.tap("🍯 Honig"); it.tap("Speiseöl"); it.tap("💧 Wasser"); it.tap("Spülmittel"); it.mainClock.advanceTimeBy(2000); it.tap("Weintraube"); it.tap("Eiswürfel") })
    @Test fun lever() = lab("k1", "11_wippe", settle = 2500, prepare = { it.tap("Loslassen") })
    @Test fun pulley() = lab("k8", "12_flaschenzug")
    @Test fun circuitSeries() = lab("s5", "13_lampen_in_reihe")
    @Test fun circuitTester() = lab("s1", "14_leitertest", prepare = { it.tap("Bleistift", 0); it.tap("📌 Eisennagel") })
    @Test fun circuitOhm() = lab("s8", "15_ohmsches_gesetz", prepare = { it.slide("Widerstand einstellen", 22.5f) })
    @Test fun magnet() = lab("s10", "16_magnet")
    @Test fun ph() = lab("c2", "17_rotkohl", settle = 2500, prepare = { listOf("Essig", "Cola", "Natron-Lösung", "Waschsoda").forEach { t -> it.tap(t) } })
    @Test fun titration() = lab("c4", "18_ph_sprung", prepare = { it.tap("+1 mL Lauge", 11) })
    @Test fun reaction() = lab("c7", "19_knallgas", settle = 300, prepare = { r -> r.onAllNodesWithText("+")[0].performClick(); r.mainClock.advanceTimeBy(200); r.onAllNodesWithText("+")[2].performClick() })
    @Test fun fizz() = lab("c6", "20_natron_ballon", settle = 2500, prepare = { it.tap("+1 Löffel Natron", 2) })
    @Test fun melt() = lab("h1", "21_heizplatte", settle = 2000, prepare = { it.slide("Heizplatte", 33f) })
    @Test fun curve() = lab("h3", "22_teilchen", settle = 7000, prepare = { it.tap("Heizen") })
    @Test fun mix() = lab("h6", "23_eisen_ins_wasser", settle = 2000, prepare = { it.tap("Hineinwerfen") })
    @Test fun light() = lab("l1", "24_licht_mischen", prepare = { it.slide("💡 Lampe Rot", 1f); it.slide("💡 Lampe Grün", 1f) })
    @Test fun prism() = lab("l5", "25_prisma", prepare = { it.slide("Prisma drehen", 10f) })
    @Test fun tubes() = lab("l9", "26_lied_spielen")
    @Test fun tuner() = lab("l8", "27_stimmen", prepare = { it.slide("Wasserhöhe in der 25-cm-Röhre", 15.3f) })
    @Test fun fall() = lab("f1", "28_kugel_gegen_feder", settle = 1200, prepare = { it.tap("Loslassen") })
    @Test fun pendulum() = lab("f7", "29_pendel", settle = 1300, prepare = { it.tap("Anstoßen") })

    @Test fun freeLab() = shot("30_freies_labor") { FreeLabScreen({ false }, {}, {}) }
    @Test fun freeFloat() = shot("31_eigene_werte") { FreeLabRun(Lab.SCHWIMMEN) {} }
    @Test fun freeCircuit() = shot("32_freier_stromkreis") { FreeLabRun(Lab.STROM) {} }

    private val entries = listOf(
        NoteRow(1, "w5", 1_763_300_000_000, "sinkt", "schwimmt", false, 2, true),
        NoteRow(2, "k1", 1_763_200_000_000, "rechts runter", "rechts runter", true, 1, true),
        NoteRow(3, "f1", 1_763_100_000_000, "Kugel", "Kugel", true, 2, false, daily = true),
        NoteRow(4, "c1", 1_763_000_000_000, "rot", "rot", true, 0, true),
        NoteRow(5, "l1", 1_762_900_000_000, "Braun", "Gelb", false, 2, true),
    )
    @Test fun journal() = shot("33_forscherbuch") { JournalScreen(entries, {}) }
    @Test fun journalExplained() = shot("38_forscherbuch_erklaerung") { JournalScreen(entries, {}, openFirst = true) }
    @Test fun topics() = shot("39_themensuche") { de.experimentopia.app.ui.home.TopicScreen(someStars, 0, { false }, {}, {}) }
    @Test fun topicNewton() = shot("40_thema_newton", prepare = { it.onNodeWithTag("thema_NEWTON").performClick() }) { de.experimentopia.app.ui.home.TopicScreen(someStars, 0, { false }, {}, {}) }
    @Test fun cart() = lab("n3", "41_newton_wagen", settle = 3000, prepare = { it.tap("Los!") })
    @Test fun skate() = lab("n2", "42_skateboards", settle = 1500, prepare = { it.tap("Abstoßen") })
    @Test fun mercury() = lab("w10", "43_quecksilber", settle = 2500, prepare = { it.tap("kleiner Nagel"); it.tap("Goldbarren") })
    @Test fun mendel() = lab("g2", "44_mendel_erbsen", settle = 2500, prepare = { it.tap("Kreuzen"); it.tap("säen") })
    @Test fun bloodGroups() = lab("g6", "45_blutgruppen", prepare = { it.tap("Kreuzen") })
    @Test fun osmosis() = lab("o3", "46_osmose_zwiebel", settle = 3500, prepare = { it.tap("30 Minuten warten") })
    @Test fun food() = lab("e1", "47_staerke_nachweis", settle = 2000, prepare = { listOf("Kartoffel", "Brot", "Butter").forEach { t -> it.tap(t) } })
    @Test fun enzyme() = lab("e6", "48_enzym", settle = 7000, prepare = { it.tap("Start") })
    @Test fun elodea() = lab("p3", "49_wasserpest", settle = 2500, prepare = { it.tap("mit Natron") })
    @Test fun predator() = lab("p5", "50_hasen_und_luchse", settle = 6000, prepare = { it.tap("30 Jahre") })
    @Test fun separate() = lab("t1", "51_stoffe_trennen", prepare = { it.tap("Filtern"); it.tap("Eindampfen") })
    @Test fun chroma() = lab("t6", "52_chromatografie", settle = 5500, prepare = { it.tap("Papier ins Wasser") })
    @Test fun paint() = lab("m1", "53_malkasten", prepare = { it.tap("Gelb"); it.tap("Blau") })
    @Test fun microscope() = lab("u1", "54_mikroskop", prepare = { it.tap("40×") })
    @Test fun microscopeLight() = lab("u4", "55_augentierchen", settle = 3500, prepare = { it.tap("100×"); it.tap("Lampe rechts an") })
    @Test fun germination() = lab("q4", "56_kresse_im_dunkeln", settle = 4500, prepare = { it.tap("Tage vergehen lassen") })
    @Test fun foodChain() = lab("j3", "57_nahrungskette", prepare = { listOf("Gras", "Hase", "Fuchs").forEach { t -> it.tap(t) } })
    @Test fun foodWebGone() = lab("j4", "58_ohne_maeuse", prepare = { it.tap("verschwinden lassen") })
    @Test fun pulse() = lab("v1", "59_herz_und_puls", prepare = { it.tap("Joggen") })
    @Test fun recovery() = lab("v5", "60_erholungspuls", settle = 5500, prepare = { it.tap("Beide sprinten") })
    @Test fun shadow() = lab("x1", "71_schattenbuehne", prepare = { it.slide("💡 Lampe ↔ Figur", 20f) })
    @Test fun mirror() = lab("x4", "72_spiegel_laser", prepare = { it.slide("↩️ Spiegel drehen", 20f) })
    @Test fun refract() = lab("x5", "73_lichtknick", prepare = { it.slide("🔴 Einfallswinkel", 42f) })
    @Test fun total() = lab("x6", "74_totalreflexion", prepare = { it.slide("🔴 Einfallswinkel", 50f) })
    @Test fun lens() = lab("x9", "75_optische_bank", prepare = { it.slide("⬜ Linse ↔ Schirm", 17f) })
    @Test fun loupe() = lab("x11", "76_lupe", prepare = { it.slide("🕯️ Kerze ↔ Linse", 6f) })
    @Test fun comingSoon() = shot("77_kommt_bald", settle = 2500, prepare = { r -> r.reveal { r.onNodeWithTag("bald_${Coming.of(Subject.BIOLOGIE).size - 1}") }; r.onNodeWithTag("bald_1").performClick() }) { de.experimentopia.app.ui.home.SubjectScreen(Subject.BIOLOGIE, someStars, 0, { false }, {}, {}, onBack = {}) }
    @Test fun theory() = shot("61_theorie", settle = 2500, prepare = { it.tap("📖") }) { ExperimentScreen(Catalog.byId("w5")!!, {}, null, {}, startPhase = Phase.PREDICT) }
    @Test fun theoryCross() = shot("83_querverweis", settle = 2500, prepare = { r -> r.tap("📖"); r.mainClock.advanceTimeBy(800); r.reveal { r.onNodeWithTag("querverweis") } }) { ExperimentScreen(Catalog.byId("x2")!!, {}, null, {}, startPhase = Phase.PREDICT) }
    @Test fun theoryPro() = shot("62_theorie_profi", settle = 2500, prepare = { it.tap("📖") }) { ExperimentScreen(Catalog.byId("s8")!!, {}, null, {}, startPhase = Phase.PREDICT) }
    // ── Motor, Dynamo & Trafo (2.16.0)
    @Test fun coilField() = lab("y2", "110_elektromagnet", prepare = { it.slide("🌀 Windungen", 29f) })
    @Test fun coilCore() = lab("y3", "111_eisenkern", prepare = { it.onNodeWithText("🔩 mit Eisenkern").performClick(); it.mainClock.advanceTimeBy(300) })
    @Test fun conductorSwing() = lab("y6", "112_leiterschaukel_kraft", prepare = { it.slide("⚡ Strom", 10f) })
    @Test fun motorSpeed() = lab("y7", "113_elektromotor", prepare = { it.slide("🔋 Spannung", 10f) })
    @Test fun induction() = lab("y9", "114_induktion", prepare = { it.slide("🧲 Tempo des Magneten", 7f) })
    @Test fun generator() = lab("y10", "115_generator", prepare = { it.slide("🚲 Drehfrequenz", 19f) })
    @Test fun trafoRatio() = lab("y11", "116_trafo", prepare = { it.slide("🌀 Windungen sekundär (N₂)", 3f) })
    @Test fun trafoLine() = lab("y13", "117_fernleitung", prepare = { it.onNodeWithText("50 kV").performClick(); it.mainClock.advanceTimeBy(300) })
    @Test fun deepLink() = shot("82_direktlink_optik", settle = 2500) { de.experimentopia.app.ui.home.SubjectScreen(Subject.PHYSIK, someStars, 5, { false }, {}, {}, startOpen = Topic.OPTIK, onBack = {}) }
    @Test fun subject() = shot("63_lernen_nach_fach", settle = 2500) { de.experimentopia.app.ui.home.SubjectScreen(Subject.PHYSIK, someStars, 5, { false }, {}, {}, onBack = {}) }
    @Test fun subjectBio() = shot("64_fach_biologie", settle = 2500, prepare = { it.tap("Alle 8 Versuche") }) { de.experimentopia.app.ui.home.SubjectScreen(Subject.BIOLOGIE, someStars, 9, { false }, {}, {}, onBack = {}) }
    @Test fun soundSettings() = shot("65_musik_und_klaenge", settle = 1500) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) {
            de.experimentopia.app.ui.settings.SoundSettings(AudioPrefs(), {}, true, {}, true, {})
        }
    }
    /** Mit eingetragener Kasse: Preis, Kauf-Knopf, Code-Eingabe – und gesperrte Versuche auf der Insel */
    @Test fun proShop() {
        Pro.configure("979eb515eb05b04dcc42e5fb776f108cdbdc86bb3c7cf28e05721fdde9e6b812", "https://www.mathenachhilfe-online.com/kasse/?add-to-cart=4143")
        try { shot("66_forscher_pass_kaufen") { de.experimentopia.app.ui.settings.ProScreen("Alles", parentsAtStart = true) {} } } finally { Pro.configure("", "") }
    }
    /** Kind tippt ein Schloss an: nur sachlicher Hinweis auf den Elternbereich – kein Preis, kein Kaufknopf, keine Aufforderung (ELTERN-TOR, 19.11.2026) */
    @Test fun proKind() {
        Pro.configure("979eb515eb05b04dcc42e5fb776f108cdbdc86bb3c7cf28e05721fdde9e6b812", "https://www.mathenachhilfe-online.com/kasse/?add-to-cart=4143")
        try {
            shot("66_forscher_pass_kind", prepare = { r ->
                r.onNodeWithTag("pro-kaufen").assertDoesNotExist()
                r.onNodeWithTag("pro-code").assertDoesNotExist()
                r.onNodeWithText(Pro.WEB_PRICE).assertDoesNotExist()
                r.onNodeWithTag("pro-eltern").assertExists()
                r.onNodeWithText("Frag deine Eltern", substring = true).assertDoesNotExist()
                r.onNodeWithText("Deine Eltern können", substring = true).assertDoesNotExist()   // Recht: keine Aufforderung, auch nicht mittelbar über die Eltern
                r.onAllNodesWithText("gehört zum Forscher-Pass.", substring = true).onFirst().assertExists()
            }) { de.experimentopia.app.ui.settings.ProScreen("Knallgas") {} }
        } finally { Pro.configure("", "") }
    }
    @Test fun proIsland() {
        Pro.configure("979eb515eb05b04dcc42e5fb776f108cdbdc86bb3c7cf28e05721fdde9e6b812", "https://www.mathenachhilfe-online.com/kasse/?add-to-cart=4143")
        try { shot("67_insel_mit_pass") { IslandScreen(Island.KRAFT, someStars, 0, { e -> Pro.locked(e, false, done = someStars.keys) }, {}, {}) } } finally { Pro.configure("", "") }
    }
    @Test fun homePass() = shot("68_forscher_pass_aktiv", settle = 2500) {
        HomeScreen(someStars, stats, Daily.forDay(20_600), false, 0, false, onIsland = {}, onDaily = {}, onFreeLab = {}, onJournal = {}, onBadges = {}, onSettings = {}, onGrade = {}, onWelcomed = {}, pass = true)
    }
    @Test fun calibration() = shot("34_selbsteinschaetzung") { JournalScreen(entries, {}, start = JournalTab.ICH) }
    @Test fun badges() = shot("35_abzeichen") { BadgesScreen(stats) {} }
    @Test fun settings() = shot("36_einstellungen") {
        // Kinderansicht: Ton, Musik, Klassenstufe – nichts Kaufbares, nichts zum Zurücksetzen; alles andere hinter dem Eltern-Tor
        SettingsScreen(true, {}, true, {}, 5, {}, "2.14.0", "24.11.2026", {}, {}, {}, parentArea = { extra -> de.experimentopia.app.ui.settings.ParentArea(20_600, 5, extra = extra) })
    }
    @Test fun pro() = shot("37_forscher_pass") { ProScreen("Knallgas") {} }
    @Test fun parentArea() = shot("81_elternbereich", prepare = { r -> r.onNodeWithText("Stromkreise", substring = true).performClick(); r.mainClock.advanceTimeBy(400) }) {
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalCounter provides FakeCounter) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) { de.experimentopia.app.ui.settings.ParentArea(20_600, 8, openAtStart = true, nickname = "Mia", nowMs = 100 * 86_400_000L, journal = struggleJournal()) }
        }
    }
    @Test fun parentsNew() = shot("78_eltern_koppeln", prepare = { r -> r.reveal { r.onNodeWithTag("eltern_koppeln") } }) {
        val sync = FakeParents(null)
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalParentSync provides sync) {
            SettingsScreen(true, {}, true, {}, 5, {}, "2.8.0", "18.11.2026", {}, {}, {}, parentArea = { extra -> de.experimentopia.app.ui.settings.ParentArea(20_600, 5, openAtStart = true, extra = extra) }, parents = { de.experimentopia.app.ui.settings.ParentCard(de.experimentopia.app.ui.settings.ParentCoupling(sync), 20_600) {} })
        }
    }
    @Test fun parentsRestore() = shot("80_fortschritt_wiederherstellen", prepare = { r ->
        r.reveal { r.onNodeWithTag("eltern_link") }; r.onNodeWithTag("eltern_link").performTextReplacement("https://staging.mathenachhilfe-online.com/eltern-bericht/#" + "A".repeat(22) + "." + "B".repeat(43))
        r.onNodeWithTag("eltern_holen").performClick(); r.mainClock.advanceTimeBy(600); r.reveal { r.onNodeWithTag("eltern_problem") }
    }) {
        val sync = FakeParents(null)
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalParentSync provides sync) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) { de.experimentopia.app.ui.settings.ParentCard(de.experimentopia.app.ui.settings.ParentCoupling(sync), 20_600, hasProgress = true, restoreOpenAtStart = true) {} }
        }
    }
    @Test fun parentsLinked() = shot("79_eltern_qr", prepare = { r -> r.onNodeWithTag("eltern_zeigen").performClick(); r.mainClock.advanceTimeBy(800); r.reveal { r.onNodeWithTag("eltern_teilen") } }) {
        val link = ParentLink("AAAAAAAAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBBBBBBBB", "C".repeat(43), test = true, nickname = "Mia", sentAt = 1_795_535_000_000)
        val sync = FakeParents(link.encode())
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalParentSync provides sync) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) { de.experimentopia.app.ui.settings.ParentCard(de.experimentopia.app.ui.settings.ParentCoupling(sync), 20_600) {} }
        }
    }

    /** Objektive Schwierigkeit für den Elternhinweis: 6 Antworten in „Strom“ über 20 Tage, 2 richtig */
    private fun struggleJournal(): List<de.experimentopia.core.model.NoteRow> {
        val id = Catalog.all.first { it.topic == Topic.STROM }.id; val now = 100 * 86_400_000L
        return listOf(20, 17, 14, 9, 5, 2).mapIndexed { i, d -> de.experimentopia.core.model.NoteRow(experimentId = id, at = now - d * 86_400_000L, guess = "", result = "", right = i < 2, sure = 1, explainedFirstTry = false) }
    }
    /** Eltern-Tor: Malaufgabe in Worten, eigene Ziffern-Tastatur, ohne Vorlese-Knopf */
    @Test fun parentGate() = shot("96_eltern_tor", settle = 800, prepare = { r ->
        r.onNodeWithTag("tor_aufgabe").assertExists(); r.onNodeWithTag("tor_taste_1").performClick(); r.onNodeWithTag("tor_taste_7").performClick(); r.mainClock.advanceTimeBy(200)
    }) {
        de.experimentopia.app.ui.settings.ParentGate({}, memory = de.experimentopia.core.model.ParentGateMemory(kotlin.time.Duration.ZERO)) {}
    }
    /** Kinderansicht bei Schwierigkeiten: nur der Hilfesatz, zwei Knöpfe, kein Außenlink */
    @Test fun kindHilfe() = shot("97_kind_hilfe", settle = 1500, prepare = { r ->
        r.onNodeWithTag("kind_hilfe").assertExists(); r.onNodeWithTag("kind_leichter").assertExists(); r.onNodeWithTag("kind_erklaerung").assertExists()
    }) {
        val e = Catalog.all.first { it.topic == Topic.STROM && it.grades.first >= 5 }
        ExperimentScreen(e, onDone = {}, onNext = null, onBack = {}, struggling = true, onEasier = {})
    }
    /** Klasse 9–10: sachlicher Tipp, ohne Tor, wegklickbar */
    @Test fun oberstufeTipp() = shot("98_oberstufe_tipp", settle = 2500, prepare = { r -> r.onNodeWithTag("nachhilfe_tipp").assertExists() }) {
        de.experimentopia.app.ui.home.SubjectScreen(Subject.PHYSIK, someStars, 9, { false }, {}, {}, tip = { s -> de.experimentopia.app.ui.home.NachhilfeTipp(s, 9, null, 1L, forceShow = true) }, onBack = {})
    }

    /** Familien-Speicher: Karte im Elternbereich (verbunden) – mit Sicherheitshinweis und Notfall-Blatt */
    @Test fun familieKarte() = shot("100_familie", settle = 1200, prepare = { r -> r.onNodeWithTag("familie_zeigen").performClick(); r.mainClock.advanceTimeBy(600); r.reveal { r.onNodeWithTag("familie_notfallblatt") } }) {
        val fp = FakeFamily(); fp.write("familie", FamilieJson.Fam("A".repeat(22), "B".repeat(43), "C".repeat(22)).encode()); fp.write("familienummer", "K7QX-2MFA"); fp.write("kind", "k"); fp.write("familiezeit", "1795535000000")
        androidx.compose.runtime.CompositionLocalProvider(de.experimentopia.app.ui.fx.LocalFamily provides fp) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) { de.experimentopia.app.ui.settings.FamilyCard(de.experimentopia.app.ui.settings.FamilyCoupling(fp)) }
        }
    }
    /** Einwilligung: genau der Pflichttext, Link zur Datenschutzerklärung, Knopf nicht vorausgewählt */
    @Test fun familieEinwilligung() = shot("101_familie_einwilligung", settle = 800, prepare = { r -> r.onNodeWithTag("familie_anlegen").performClick(); r.mainClock.advanceTimeBy(400); r.onNodeWithTag("familie_pflichttext").assertExists(); r.onNodeWithTag("familie_einverstanden").assertExists() }) {
        val fp = FakeFamily()
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) { de.experimentopia.app.ui.settings.FamilyCard(de.experimentopia.app.ui.settings.FamilyCoupling(fp)) }
    }
}

private object FakeCounter : de.experimentopia.app.ui.fx.Counter {
    override val enabled = kotlinx.coroutines.flow.MutableStateFlow(true)
    override fun setEnabled(on: Boolean) { enabled.value = on }
    override fun count(ev: String, ort: String?, thema: String?) {}
}

/** Eltern-Kopplung ohne Netz für Bildschirmfotos */
private class FakeParents(override var stored: String?) : de.experimentopia.app.ui.fx.ParentSync {
    override fun random(n: Int) = ByteArray(n) { it.toByte() }
    override fun send(method: String, url: String, write: String, body: String?, done: (Int) -> Unit) = done(204)
    override fun share(title: String, url: String, done: (String) -> Unit) = done("kopiert")
    override val test = true
    override fun now() = 1_795_535_000_000L
    override val offsetMs = 3_600_000L
    override fun get(url: String, done: (Int, String) -> Unit) = done(404, "")
    override fun snapshot() = emptyMap<String, String>()
    override fun restore(data: Map<String, String>) {}
    override var months: String? = null
    override val version = "2.9.0"
    override val journalKey = "experimentopia.forscherbuch"
}

private object FakeInstaller : de.experimentopia.app.ui.fx.Installer {
    override val state = de.experimentopia.app.ui.fx.InstallState.IOS
    override fun install() {}
}

/** Vorlese-Attrappe für Bildschirmfotos */
private object FakeReader : de.experimentopia.app.ui.fx.Reader {
    override val speaking = false
    override fun say(text: String) {}
    override fun stop() {}
    override fun configure(p: AudioPrefs) {}
}

/** Familien-Attrappe für Bildschirmfotos: Aufrufe antworten mit festen Texten, Merkzettel im Speicher */
private class FakeFamily : de.experimentopia.app.ui.fx.FamilyPlatform {
    private val m = mutableMapOf<String, String>()
    override fun call(op: String, args: String, done: (String) -> Unit) = done(if (op == "blatt") """{"ok":true,"link":"https://lernen.mathenachhilfe-online.com/experimentopia/#familie=AAAAAAAAAAAAAAAAAAAAAA.BBBB.CCCC","nummer":"K7QX-2MFA","blatt":"Familien-Zugang für die Lern-Apps von Mathenachhilfe Online.\nFamilien-Nummer: K7QX-2MFA\nLink: https://lernen.mathenachhilfe-online.com/experimentopia/#familie=…"}""" else """{"ok":false,"grund":"netz"}""")
    override val test = true
    override fun now() = 1_795_536_000_000
    override fun read(key: String) = m[key]
    override fun write(key: String, value: String?) { if (value == null) m.remove(key) else m[key] = value }
    override fun reload() {}
}
