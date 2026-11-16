package de.kurvenflug.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.kurvenflug.app.ui.flight.IntroOverlay
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.home.HomeScreen
import de.kurvenflug.app.ui.settings.SettingsScreen
import de.kurvenflug.app.ui.settings.UpdateState
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.theme.KurvenflugTheme
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.Learning
import de.kurvenflug.core.model.Played
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) { content() } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private val someStars = Levels.all.take(9).mapIndexed { i, l -> l.id to (3 - i % 3) }.toMap() +
        Levels.chapters[1].levels.take(3).associate { it.id to 3 }

    private fun level(id: String, values: Map<String, Double>? = null, prepare: (ComposeContentTestRule) -> Unit = {}, name: String, settle: Long = 1500,
                      reward: ((Int) -> de.kurvenflug.core.model.StickerDrop?)? = null) {
        val l = Levels.byId(id)!!
        shot(name, settle, prepare) {
            LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, Q.Cyan, false, {}, { _, _, _ -> }, {}, {}, values, onReward = reward)
        }
    }

    @Test fun home() = shot("01_start") {
        HomeScreen(someStars.values.sum(), Levels.all.size * 3, 7, 0, {}, Levels.chapters, someStars, {}, {},
            daily = de.kurvenflug.core.model.Daily.forDay(20_500), dailiesDone = 4,
            bossStars = mapOf(1 to 2), onBoss = {}, badges = 3, stormBest = 7, onDuel = {}, onStorm = {}, onBadges = {},
            learning = {
                val now = 20_500L * Learning.DAY_MS
                val played = someStars.mapValues { Played(it.value, now - 3 * Learning.DAY_MS) }
                de.kurvenflug.app.ui.learning.LearningCard(Learning.status(played, now), Learning.review(played, now, 20_500L)) {}
            },
            grade = 7, continueLevel = Levels.byId("1-4"))
    }

    @Test fun homeGalaxy() = shot("43_galaxie") {
        HomeScreen(someStars.values.sum(), Levels.all.size * 3, 7, 0, {}, Levels.chapters, someStars, {}, {},
            grade = 7, continueLevel = Levels.byId("2-1"), initialTab = 1)
    }

    @Test fun homeGames() = shot("44_spiele") {
        HomeScreen(someStars.values.sum(), Levels.all.size * 3, 7, 0, {}, Levels.chapters, someStars, {}, {}, stormBest = 7, initialTab = 2)
    }

    @Test fun homeMe() = shot("46_ich") {
        HomeScreen(someStars.values.sum(), Levels.all.size * 3, 7, 0, {}, Levels.chapters, someStars, {}, {}, badges = 3, grade = 7, initialTab = 3)
    }

    @Test fun gradePicker() = shot("45_klasse_waehlen") {
        HomeScreen(0, Levels.all.size * 3, 0, 0, {}, Levels.chapters, emptyMap(), {}, {}, onGrade = {}, askGrade = true)
    }

    @Test fun intro() = shot("03_volt_erklaert", prepare = { it.onNodeWithText("Weiter ›").performClick() }) {
        IntroOverlay(Levels.chapters[1]) {}
    }

    @Test fun coordinates() = level("1-2", name = "04_koordinaten", values = mapOf("x" to -5.0, "y" to 0.0))

    @Test fun lineAim() = level("2-7", name = "05_gerade_zielen", values = mapOf("m" to -1.0, "b" to 2.0))

    @Test fun lineMiss() = level("2-6", name = "06_gerade_daneben", values = mapOf("m" to 1.0, "b" to 0.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun lineFlying() = level("2-6", name = "07_gerade_flug", values = mapOf("m" to 0.5, "b" to 1.0), settle = 900,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun parabolaWin() = level("3-6", name = "08_parabel_geschafft", values = mapOf("a" to -0.25, "d" to 0.0, "e" to 5.0), settle = 4500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun throwHelp() = level("4-5", name = "09_wurf_berg", values = mapOf("angle" to 60.0, "v" to 12.0),
        prepare = { it.onNodeWithText("👁 Bahn").performClick() })

    @Test fun throwMoon() = level("4-6", name = "10_wurf_mond", values = mapOf("angle" to 35.0, "v" to 5.5), settle = 1500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun settings() = shot("11_einstellungen") {
        SettingsScreen(sound = true, onSound = {}, vibration = true, onVibration = {}, autoUpdate = true, onAutoUpdate = {}, installed = "0.9.0", update = UpdateState.Idle, onBack = {}, onReset = {})
    }

    @Test fun sine() = level("5-4", name = "12_sinus", values = mapOf("a" to 2.0, "b" to 1.0, "d" to -1.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun exponential() = level("5-7", name = "13_zerfall", values = mapOf("b" to 0.75))

    @Test fun slingshot() = level("6-4", name = "14_schwerkraft_schleuder", values = mapOf("angle" to 0.0, "v" to 3.5), settle = 1600,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun slingshotHelp() = level("6-3", name = "15_schleuder_ganze_bahn", values = mapOf("angle" to 70.0, "v" to 7.0),
        prepare = { it.onNodeWithText("👁 Bahn").performClick() })

    @Test fun daily() {
        val l = de.kurvenflug.core.model.Daily.forDay(20_501)
        shot("16_tagesmission") {
            LevelScreen(l, de.kurvenflug.core.model.Chapter(0, "Tagesmission", "Jeden Tag neu", "🛰️", emptyList(), listOf(l)),
                Q.Pink, false, {}, { _, _, _ -> }, null, {})
        }
    }

    @Test fun rendezvous() = level("7-4", name = "17_rendezvous", values = mapOf("x" to 1.5, "y" to 5.5), settle = 1000,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun rendezvousMiss() = level("7-2", name = "18_rendezvous_daneben", values = mapOf("x" to 2.0, "y" to 1.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun bossIntro() = shot("19_boss") {
        de.kurvenflug.app.ui.modes.BossScreen(de.kurvenflug.core.model.Bosses.forChapter(3)!!, Q.Cyan, { _, _ -> }, {})
    }

    @Test fun storm() = shot("20_asteroidensturm", prepare = { it.onNodeWithText("Los! 🚀").performClick() }) {
        de.kurvenflug.app.ui.modes.StormScreen(12, Q.Cyan, { false }, {}, seed = 5)
    }

    @Test fun duelMenu() = shot("21_duell") { de.kurvenflug.app.ui.modes.DuelScreen(Q.Cyan, null, {}, {}) }

    @Test fun duelHandover() = shot("22_duell_zu_zweit", prepare = { it.onNodeWithText("Zu zweit an diesem Handy").performClick() }) {
        // fester Startwert, damit das Bild bei jedem Lauf gleich bleibt
        de.kurvenflug.app.ui.modes.DuelScreen(Q.Cyan, null, {}, {}, localSeed = 1_234_567L)
    }

    @Test fun badges() = shot("23_abzeichen") {
        de.kurvenflug.app.ui.modes.BadgesScreen(de.kurvenflug.core.model.Stats(stars = 40, perfect = 12, chaptersDone = 2, cells = 30, stormBest = 11, duelsPlayed = 2), {})
    }

    @Test fun dragMode() = level("3-4", name = "24_kurve_ziehen", values = mapOf("d" to 1.0, "e" to 2.0),
        prepare = { it.onNodeWithText("✋ Ziehen").performClick() })

    @Test fun builder() {
        val spec = de.kurvenflug.core.model.LevelSpec.of(Levels.byId("2-7")!!).copy(title = "Gürtel-Chaos", task = "Schaffst du das?")
        shot("25_baukasten") { de.kurvenflug.app.ui.maker.BuilderScreen(spec, Q.Cyan, {}, {}, {}) }
    }

    @Test fun builderHome() {
        val mine = listOf(de.kurvenflug.core.model.Codec.encodeLevel(de.kurvenflug.core.model.LevelSpec.of(Levels.byId("3-5")!!).copy(title = "Lenas Bogen")))
        shot("26_baukasten_start") { de.kurvenflug.app.ui.maker.BuilderHome(mine, {}, {}, {}, {}, {}, {}, {}) }
    }

    @Test fun challengeComposer() = shot("27_klassen_challenge", prepare = { r ->
        r.onAllNodesWithText("1")[1].performClick(); r.onAllNodesWithText("2")[1].performClick()
    }) { de.kurvenflug.app.ui.maker.ChallengeComposer(emptyList()) {} }

    @Test fun detective() = shot("28_formel_detektiv") {
        de.kurvenflug.app.ui.maker.DetectiveScreen(3, Q.Cyan, {}, {}, seed = 4)
    }

    @Test fun anti() = level("8-5", name = "29_antiproportional", values = mapOf("k" to 12.0), settle = 1200,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun pythagoras() = level("9-3", name = "30_pythagoras", values = mapOf("r" to 7.6), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun laser() = level("11-2", name = "31_laser", values = mapOf("angle" to 4.0), settle = 1500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    /** Zielhilfe: Der Strahl ist bis zum ersten Spiegel zu sehen – den Abprall konstruiert man selbst. */
    @Test fun laserAim() = level("11-1", name = "37_laser_zielen", values = mapOf("angle" to -35.0))

    /** Nach einem Fehlversuch: gelber Pfeil von der Bahn zur Station mit „2 zu tief“. */
    @Test fun gapArrow() = level("3-1", name = "38_abstand_pfeil", values = mapOf("e" to -5.0), settle = 4000,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun trigLaser() = level("14-1", name = "39_trig_laser", values = mapOf("angle" to 20.0))
    @Test fun trigRadio() = level("14-5", name = "40_trig_funk", values = mapOf("r" to 10.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    /** Lernstand: einige Themen sicher, eins wackelig und fällig zum Auffrischen. */
    @Test fun learning() {
        val day = 20_500L; val now = day * Learning.DAY_MS + 43_200_000L
        fun all(n: Int, stars: Int, daysAgo: Int) = Levels.chapters.first { it.number == n }.levels
            .associate { it.id to Played(stars, now - daysAgo * Learning.DAY_MS) }
        val played = all(1, 3, 1) + all(8, 3, 4) + all(2, 2, 3) + mapOf("3-1" to Played(1, now - Learning.DAY_MS))
        shot("41_lernstand", settle = 2500) {
            de.kurvenflug.app.ui.learning.LearningScreen(Learning.status(played, now), Learning.review(played, now, day),
                false, now, {}, {})
        }
    }

    /** Rechenweg: Volt hat den ersten Schritt vorgerechnet, das Kind ist beim zweiten. */
    @Test fun walkthrough() = shot("42_rechenweg") {
        val l = Levels.byId("3-1")!!
        de.kurvenflug.app.ui.flight.WalkthroughOverlay(l, de.kurvenflug.core.model.Walkthrough.of(l)!!, voltSolves = 1) {}
    }

    @Test fun fractions() = level("15-1", name = "49_brueche", values = mapOf("x" to 2.75))
    @Test fun angles() = level("16-4", name = "50_winkel", values = mapOf("angle" to 100.0))
    @Test fun refraction() = level("17-1", name = "51_lichtbrechung", values = mapOf("angle" to 22.0),
        prepare = { it.onNodeWithText("👁 Bahn").performClick() })
    @Test fun zeros() = level("18-3", name = "52_nullstellen", values = mapOf("d" to 2.0, "e" to -5.0),
        prepare = { it.onNodeWithText("👁 Bahn").performClick() })

    private val storyStars = Levels.chapters.filter { it.number in setOf(1, 15, 8, 2) }.flatMap { it.levels }.associate { it.id to 3 } + someStars

    @Test fun galaxyMap() = shot("53_sternenkarte", settle = 1500) {
        HomeScreen(storyStars.values.sum(), Levels.all.size * 3, 7, 0, {}, Levels.chapters, storyStars, {}, {},
            grade = 7, continueLevel = Levels.byId("13-1"), initialTab = 1, partsFound = 4, onHangar = {})
    }

    @Test fun hangar() = shot("54_hangar") {
        de.kurvenflug.app.ui.story.HangarScreen(de.kurvenflug.core.model.Story.found(Levels.chapters, storyStars), {})
    }

    @Test fun partFound() = shot("55_bauteil_gefunden", settle = 1200) {
        val found = de.kurvenflug.core.model.Story.found(Levels.chapters, storyStars)
        de.kurvenflug.app.ui.story.PartFoundOverlay(found.take(1), found.map { it.chapter }.toSet(), {})
    }

    @Test fun storyIntro() = shot("56_geschichte") { de.kurvenflug.app.ui.story.StoryIntroOverlay {} }

    @Test fun stickerAlbum() = shot("57_sticker") {
        de.kurvenflug.app.ui.collection.StickerAlbum(setOf("rocket", "star", "moon", "comet*", "planet", "alien", "robot", "unicorn*", "sat")) {}
    }

    /** Volt schläft abends und tanzt bei 3 ⭐; Ergebnis mit Sticker und Foto-Knopf */
    @Test fun voltMoods() = shot("58_volt_launen") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)) {
            de.kurvenflug.app.ui.volt.Volt(de.kurvenflug.app.ui.volt.VoltMood.SLEEP, size = 96.dp,
                speech = de.kurvenflug.core.model.Greeting.home(22, 0, 12).second)
            de.kurvenflug.app.ui.volt.Volt(de.kurvenflug.app.ui.volt.VoltMood.HAPPY, size = 96.dp,
                speech = de.kurvenflug.core.model.Greeting.home(12, 5, 12).second)
        }
    }

    @Test fun rewardCard() = level("2-1", name = "59_sticker_gewonnen", values = mapOf("b" to 4.0), settle = 5000,
        prepare = { it.onNodeWithText("🚀 Start").performClick() }, reward = { de.kurvenflug.core.model.StickerDrop(de.kurvenflug.core.model.Stickers.byId("unicorn")!!, true, true) })

    @Test fun profiles() = shot("61_spieler") {
        de.kurvenflug.app.ui.together.ProfileScreen(
            listOf(de.kurvenflug.core.data.Profile(0, "Lena", "🦊"), de.kurvenflug.core.data.Profile(1, "Max", "🦖"), de.kurvenflug.core.data.Profile(2, "Papa", "🐼")),
            current = 0, onSwitch = {}, onAdd = { _, _ -> }, onRename = { _, _, _ -> }, onDelete = {}, onBack = {})
    }

    @Test fun parents() = shot("62_eltern", settle = 2000) {
        val today = 20_500L * de.kurvenflug.core.model.Weekly.DAY
        val d = de.kurvenflug.core.model.Weekly.DAY
        val plays = listOf(
            de.kurvenflug.core.model.PlayRecord("2-1", 3, 1, 180, today - 5 * d), de.kurvenflug.core.model.PlayRecord("2-2", 3, 1, 240, today - 5 * d),
            de.kurvenflug.core.model.PlayRecord("2-3", 3, 1, 200, today - 3 * d), de.kurvenflug.core.model.PlayRecord("2-4", 2, 2, 300, today - 3 * d),
            de.kurvenflug.core.model.PlayRecord("3-1", 0, 3, 400, today - d), de.kurvenflug.core.model.PlayRecord("15-1", 3, 1, 120, today),
            de.kurvenflug.core.model.PlayRecord("15-2", 3, 1, 90, today), de.kurvenflug.core.model.PlayRecord("2-5", 3, 1, 260, today))
        de.kurvenflug.app.ui.together.ParentScreen(de.kurvenflug.core.data.Profile(0, "Lena", "🦊"),
            de.kurvenflug.core.model.Weekly.summary(plays, today + 3_600_000L, today), listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")) {}
    }

    @Test fun results() = shot("63_bestenliste") {
        fun r(n: String, a: List<Int>) = de.kurvenflug.core.model.ChallengeResult("Klasse 7b – Geraden", n, a, "15.11.2026 10:15",
            de.kurvenflug.core.model.ChallengeResult.checkOf("Klasse 7b – Geraden", n, a, "15.11.2026 10:15"))
        val text = listOf(r("Mia", listOf(1, 1, 2, 1)), r("Ben", listOf(1, 1, 1, 1)), r("Ali", listOf(2, 1, 3, 1)), r("Emma", listOf(1, 2, 1, 1)))
            .joinToString("\n") { de.kurvenflug.core.model.Codec.encodeResult(it) } + "\n" +
            de.kurvenflug.core.model.Codec.encodeResult(r("Tom", listOf(1, 1, 1, 1)).copy(attempts = listOf(1, 1, 1, 1), check = "0000"))
        de.kurvenflug.app.ui.together.ResultsBoard({}, initial = text)
    }

    private val allFound = Levels.all.associate { it.id to if (it.chapter in setOf(1, 15, 2, 8, 3, 13)) 3 else 2 }

    @Test fun hangarGold() = shot("64_hangar_gold", settle = 1500) {
        de.kurvenflug.app.ui.story.HangarScreen(de.kurvenflug.core.model.Story.found(Levels.chapters, allFound), {},
            gold = de.kurvenflug.core.model.Season2.gold(Levels.chapters, allFound), onCrew = {})
    }

    @Test fun crew() = shot("65_crew") {
        de.kurvenflug.app.ui.story.CrewScreen(setOf(1, 2, 3), parts = 18, gold = 3, onRescue = {}, onBack = {})
    }

    @Test fun rescueIntro() = shot("66_notruf") {
        de.kurvenflug.app.ui.story.RescueScreen(4, Q.Cyan, { _, _ -> }, {})
    }

    @Test fun homeCrew() = shot("67_start_crew") {
        HomeScreen(allFound.values.sum(), Levels.all.size * 3, 40, 0, {}, Levels.chapters, allFound, {}, {},
            daily = de.kurvenflug.core.model.Daily.forDay(20_500), dailiesDone = 12, grade = 8,
            crewLine = de.kurvenflug.core.model.Season2.crew[1] to de.kurvenflug.core.model.Season2.crew[1].lines[0],
            rescueCall = de.kurvenflug.core.model.Season2.crew[3], crewCount = 3, onCrew = {}, partsFound = 18, onHangar = {})
    }

    @Test fun andromeda() = shot("68_andromeda", settle = 1500) {
        de.kurvenflug.app.ui.story.AndromedaScreen((1..5).flatMap { n -> (1..3).map { "andro-$n-$it" to 3 } }.toMap() + ("andro-6-1" to 2), {}, {})
    }

    @Test fun station() = shot("69_raumstation", settle = 1500) {
        de.kurvenflug.app.ui.story.StationScreen(allFound, unlocked = setOf(1, 2), owned = setOf("rocket", "unicorn*", "planet", "alien", "s_halloween"),
            decor = mapOf("0:0" to "rocket", "0:1" to "unicorn*", "0:4" to "planet", "1:0" to "alien", "1:2" to "s_halloween"), rescued = setOf(1, 2, 3, 4),
            onUnlock = {}, onDecor = { _, _, _ -> }, onBack = {})
    }

    @Test fun seasonPlanet() = shot("70_gastplanet", settle = 1500) {
        val e = de.kurvenflug.core.model.SeasonEvent.HALLOWEEN
        de.kurvenflug.app.ui.story.SeasonScreen(e, 2026, de.kurvenflug.core.model.Seasons.levels(e, 2026).take(2).associate { it.id to 3 }, {}, {})
    }

    @Test fun homeSeason() = shot("71_start_gastplanet") {
        HomeScreen(allFound.values.sum(), Levels.all.size * 3, 40, 0, {}, Levels.chapters, allFound, {}, {}, grade = 7,
            season = de.kurvenflug.core.model.SeasonEvent.ADVENT to false, onSeason = {}, andromeda = 6 to 5, onAndromeda = {}, partsFound = 18, onHangar = {})
    }

    @Test fun ramp() = level("12-1", name = "32_energie_rampe", values = mapOf("h" to 8.0), settle = 1100,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun log() = level("10-5", name = "33_logarithmus", values = mapOf("b" to 3.0, "d" to -2.0), prepare = { it.onNodeWithText("👁 Bahn").performClick() })

    @Test fun motion() = level("13-3", name = "34_weg_zeit", values = mapOf("v" to 0.5, "s0" to 5.0), settle = 1300,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun codex() = shot("35_lexikon") { de.kurvenflug.app.ui.collection.CodexScreen(32) {} }

    @Test fun workshop() = shot("36_werkstatt") {
        de.kurvenflug.app.ui.collection.WorkshopScreen(30, 22, 1, Triple(1, 2, 2), {}, { _, _, _ -> }, {})
    }

    // Verstehen statt nur schieben: Live-Rechnung, Rechen-Stern, Lösungsweg
    @Test fun liveCalc() = level("14-2", name = "72_live_rechnung", values = mapOf("angle" to 10.0))

    @Test fun calcCheckQuestion() = level("14-2", name = "73_rechenstern_frage", values = mapOf("angle" to 13.0), settle = 4500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun calcCheckSolved() = level("14-2", name = "74_rechenstern_geschafft", values = mapOf("angle" to 13.0), settle = 6000,
        prepare = {
            it.onNodeWithText("🚀 Start").performClick(); it.mainClock.advanceTimeBy(5000)
            it.onNodeWithText("tan α = 4 : 17").performClick()
            it.mainClock.advanceTimeBy(3000)
            it.onNodeWithText("2. α = tan⁻¹", substring = true).assertIsDisplayed()
        })

    @Test fun calcCheckWrong() = level("14-2", name = "75_rechenstern_daneben", values = mapOf("angle" to 13.0), settle = 3000,
        prepare = {
            it.onNodeWithText("🚀 Start").performClick(); it.mainClock.advanceTimeBy(5000)
            it.onNodeWithText("sin α = 4 : 17").performClick()
        })

    @Test fun taskStaysAfterMiss() = level("1-7", name = "76_aufgabe_nach_fehlversuch", values = mapOf("x" to -3.0, "y" to 2.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    // Oberstufe (Klasse 11–13)
    @Test fun upperTangent() = level("19-1", name = "77_oberstufe_tangente", values = mapOf("m" to 1.0), settle = 1500)
    @Test fun upperTangentWin() = level("19-5", name = "78_oberstufe_tangente_geschafft", values = mapOf("m" to 1.5), settle = 5000,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })
    @Test fun upperPeak() = level("20-3", name = "79_oberstufe_hochpunkt", values = mapOf("x" to -1.0))
    @Test fun upperArea() = level("21-4", name = "80_oberstufe_integral", values = mapOf("A" to 10.0), settle = 5000,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })
    @Test fun upperVector() = level("22-4", name = "81_oberstufe_vektor", values = mapOf("v1" to 2.0, "v2" to 2.0))

    // Inhaltspaket 0.20
    @Test fun moreMap() = level("29-2", name = "82_massstab", values = mapOf("x" to -2.0, "y" to 3.0))
    @Test fun morePercent() = level("23-1", name = "83_prozent", values = mapOf("b" to 1.15))
    @Test fun moreAccel() = level("24-5", name = "84_beschleunigung", values = mapOf("a" to 0.5, "v0" to 1.5))
    @Test fun morePower() = level("25-5", name = "85_wurzel", values = mapOf("a" to 2.0))
    @Test fun moreWave() = level("26-3", name = "86_verschobene_welle", values = mapOf("c" to 1.0, "d" to 0.5))
    @Test fun moreHorizontal() = level("27-3", name = "87_waagerechter_wurf", values = mapOf("v" to 3.0), settle = 7000,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })
    @Test fun moreExpe() = level("28-5", name = "88_e_funktion", values = mapOf("k" to -0.3))
}
