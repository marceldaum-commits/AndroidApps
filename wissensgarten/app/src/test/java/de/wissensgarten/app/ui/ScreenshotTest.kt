package de.wissensgarten.app.ui

import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.wissensgarten.app.ui.family.FamilyScreen
import de.wissensgarten.app.ui.garden.BedScreen
import de.wissensgarten.app.ui.garden.GardenScreen
import de.wissensgarten.app.ui.onboarding.OnboardingScreen
import de.wissensgarten.app.ui.onboarding.TutorialScreen
import de.wissensgarten.app.ui.review.ReviewScreen
import de.wissensgarten.app.ui.shop.ShopScreen
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import de.wissensgarten.app.ui.play.*
import de.wissensgarten.app.ui.adventure.*
import de.wissensgarten.app.ui.fx.Brummi
import de.wissensgarten.app.ui.fx.Mood
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import de.wissensgarten.app.ui.klasse.ClassShareDialog
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.store.StoreScreen
import de.wissensgarten.app.ui.duel.DuelHubScreen
import de.wissensgarten.app.ui.duel.DuelPlayScreen
import de.wissensgarten.core.data.ThemeMode
import de.wissensgarten.core.data.db.*
import de.wissensgarten.core.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Rendert die Bildschirme mit echten Paketen und einem „gelebten“ Garten – ohne Gerät. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
open class ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val testName = org.junit.rules.TestName()

    /** Zielordner – die Store-Varianten (Unterklassen unten) schreiben woanders hin */
    protected open val outDir = "../screenshots"

    /** Store-Fotos: nur diese Bildschirme (die schönsten 8) */
    private val storeSet = setOf("gardenHome", "reviewKids", "adventurePath", "chest", "album", "shop", "family", "duelHub")

    /** Fester Startwert für Konfetti, Wassertropfen und gemischte Runden – sonst flackern die Bilder bei jedem Lauf */
    @org.junit.Before fun fixedRandom() { de.wissensgarten.app.ui.fx.Zufall.quelle = kotlin.random.Random(42) }
    @org.junit.After fun resetRandom() { de.wissensgarten.app.ui.fx.Zufall.quelle = kotlin.random.Random.Default }

    @org.junit.Before fun onlyStoreShots() {
        if (outDir != "../screenshots") org.junit.Assume.assumeTrue(testName.methodName in storeSet)
    }

    private val now = 1_763_031_600_000L // 11:00 UTC – Tageshimmel
    private val night = 1_763_071_200_000L // 22:00 UTC
    private val day = Scheduler.DAY_MS
    private val packs = File("../content/src/main/assets/packs").listFiles()!!.filter { it.extension == "json" }
        .map { PackJson.parse(it.readText()) }
    private val profile = ProfileEntity("p1", "Lena", "🦊", 7, dewdrops = 214, createdAt = 0)

    private fun pack(id: String) = packs.first { it.id == id }

    /** Beet mit Pflanzen in allen Stufen, manche durstig. */
    private fun bed(p: Pack, grownCount: Int, profileId: String = profile.id): BedWithCards {
        val bed = BedEntity(p.id + profileId, profileId, p.title, p.emoji, p.subjectInfo.hue, p.id, p.version, 0)
        val cards = p.cards.mapIndexed { i, c ->
            var m = Memory()
            if (i < grownCount) {
                m = Scheduler.review(m, Rating.GOOD, now - 400 * day)
                repeat(i % 6) { val t = Scheduler.dueAt(m); if (t < now) m = Scheduler.review(m, Rating.GOOD, t) }
                m = m.copy(lastReview = now - (m.stability * (if (i % 3 == 1) 4.0 else 0.3) * day).toLong())
            }
            CardEntity("${bed.id}-$i", bed.id, profileId, c.id, c.q, c.a, CardEntity.joinChoices(c.choices), c.info,
                m.stability, m.difficulty, m.lastReview, m.reps, m.lapses, 0, i)
        }
        return BedWithCards(bed, cards)
    }

    private val garden by lazy {
        listOf(
            bed(pack("s1-englisch-irregular-verbs"), 14),
            bed(pack("s1-bio-zelle-mensch"), 9),
            bed(pack("s1-erdkunde-europa"), 5),
            bed(pack("s1-mathe-brueche-prozent"), 2),
        )
    }

    private fun shot(name: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}, content: @Composable () -> Unit) {
        rule.setContent { WissensgartenTheme(ThemeMode.LIGHT) { Surface { content() } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        val n = if (outDir == "../screenshots") name else "${storeSet.indexOf(testName.methodName) + 1}_$name"
        rule.onRoot().captureRoboImage("$outDir/$n.png")
    }

    @Test fun onboardingWelcome() = shot("01_willkommen") {
        OnboardingScreen(packs, false, {}) { _, _, _, _, _ -> }
    }

    @Test fun onboardingBeds() = shot("02_beete_waehlen", prepare = { r ->
        r.onNodeWithText("Los geht's").performClick(); r.mainClock.advanceTimeBy(800)
        r.onNodeWithText("Mein Kind").performClick()
        r.onNodeWithText("Weiter").performClick(); r.mainClock.advanceTimeBy(800)
        r.onNode(hasSetTextAction()).performTextInput("Lena")
        r.onNodeWithText("Weiter").performClick(); r.mainClock.advanceTimeBy(800)
        r.onNodeWithText("7").performClick()
        r.onNodeWithText("Weiter").performClick(); r.mainClock.advanceTimeBy(800)
        r.onAllNodesWithText("Wählen")[0].performClick()
        r.onAllNodesWithText("Wählen")[0].performClick()
    }) {
        OnboardingScreen(packs, false, {}) { _, _, _, _, _ -> }
    }

    @Test fun tutorial() = shot("03_anleitung", prepare = { r ->
        r.onNodeWithText("Ersten Samen säen").performClick()
        r.onNodeWithText("8").performClick()
    }) { TutorialScreen {} }

    @Test fun garden() = shot("04_garten") {
        GardenScreen(profile, garden, now, 10, {}, {}, {}, {})
    }

    @Test fun gardenNight() = shot("04b_garten_nacht") {
        GardenScreen(profile, garden, night, 10, {}, {}, {}, {})
    }

    @Test fun gardenEvening() = shot("04c_garten_abend") {
        GardenScreen(profile, garden.map { b -> b.copy(cards = b.cards.map { it.copy(lastReview = if (it.reps > 0) now else 0) }) }, now + 7 * 3_600_000L, 10, {}, {}, {}, {})
    }

    @Test fun gardenDecorated() = shot("04d_garten_geschmueckt") {
        val decorated = garden.mapIndexed { i, b -> b.copy(bed = b.bed.copy(species = listOf("sonnenblume", "kirschbaum", "tulpe", "kaktus")[i])) }
        GardenScreen(profile, decorated, now, 10, {}, {}, {}, {}, decos = ShopItems.decos.map { it.id }.toSet())
    }

    @Test fun gardenDecoratedNight() = shot("04e_garten_geschmueckt_nacht") {
        val decorated = garden.mapIndexed { i, b -> b.copy(bed = b.bed.copy(species = listOf("zauberblume", "tanne", "rose", "kaktus")[i])) }
        GardenScreen(profile, decorated, night, 10, {}, {}, {}, {}, decos = setOf("laterne", "zwerg", "teich", "zaun", "vogelhaus"))
    }

    @Test fun storePlants() = shot("11_laden_pflanzen") {
        StoreScreen(profile, listOf(InventoryEntity("p1", "sonnenblume", true, 0)), garden, emptyList(), { _, _ -> }, { _, _ -> }, { _, _ -> }, {})
    }

    @Test fun storeDeco() = shot("12_laden_deko") {
        StoreScreen(profile, listOf(InventoryEntity("p1", "zwerg", true, 0), InventoryEntity("p1", "windrad", false, 0)), garden,
            emptyList(), { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, initialTab = 1)
    }

    private val duel by lazy {
        Duel("s1-erdkunde-europa", 1, 4242, listOf(DuelResult("Lena", "🦊", 8, 74_000, 1012), DuelResult("Papa", "🐻", 7, 98_000, 841)))
    }

    @Test fun duelHub() = shot("13_duell_start") {
        DuelHubScreen("Ben", packs, listOf(duel.copy(seed = 77)), duel, {}, {}, { true }, {}, {})
    }

    @Test fun duelQuestion() = shot("14_duell_frage", settle = 0, prepare = { r ->
        r.mainClock.autoAdvance = false
        r.mainClock.advanceTimeBy(2300)
        r.mainClock.advanceTimeBy(5000)
    }) {
        DuelPlayScreen(duel, pack("s1-erdkunde-europa"), "Ben", { _, _, _ -> duel to 0 }, {})
    }

    @Test fun duelResult() = shot("15_duell_ergebnis", settle = 0, prepare = { r ->
        r.mainClock.autoAdvance = false
        r.mainClock.advanceTimeBy(2300)
        val qs = Duels.questions(pack("s1-erdkunde-europa"), duel.seed)
        qs.forEachIndexed { i, q ->
            r.mainClock.advanceTimeBy(600L + i * 150)
            r.onNodeWithText(q.answer, useUnmergedTree = true).performClick()
            r.mainClock.advanceTimeBy(900)
        }
        r.mainClock.advanceTimeBy(1600)
    }) {
        var d = duel
        DuelPlayScreen(duel, pack("s1-erdkunde-europa"), "Ben", { c, ms, sc ->
            d = duel.withResult(DuelResult("Ben", "🐸", c, ms, sc)); d to (c * 2 + 10)
        }, {})
    }

    private val verbs get() = pack("s1-englisch-irregular-verbs")
    private fun seedCard(id: String = "001"): StudyCard {
        val c = verbs.cards.first { it.id == id }
        return StudyCard("seed-$id", garden[0].bed.id, c.q, c.a, c.choices, c.info, Memory(), c.img)
    }
    private fun grownCard(id: String): StudyCard = seedCard(id).copy(id = "g-$id", memory = Memory(3.0, 5.0, now - 5 * day, 2, 0))
    private val verbPeers get() = mapOf(garden[0].bed.id to verbs.cards.map { it.a })

    @Test fun reviewDiscover() = shot("06_entdecken") {
        ReviewScreen(listOf(seedCard()), garden.associate { it.bed.id to it.bed }, false, { _, _, _ -> }, {}, verbPeers)
    }

    @Test fun reviewReveal() = shot("06b_merkbild_gegossen", settle = 1100, prepare = { r ->
        r.onNodeWithText("Gemerkt! 👍").performClick()
        r.mainClock.advanceTimeBy(800)
        r.mainClock.autoAdvance = false
        r.onNodeWithText("went – gone").performClick()
        r.mainClock.advanceTimeBy(600)
    }) {
        ReviewScreen(listOf(seedCard()), garden.associate { it.bed.id to it.bed }, false, { _, _, _ -> }, {}, verbPeers)
    }

    @Test fun sessionDone() = shot("10_runde_fertig", settle = 1000, prepare = { r ->
        r.onNodeWithText("Gemerkt! 👍").performClick()
        r.mainClock.advanceTimeBy(800)
        r.mainClock.autoAdvance = false
        r.onNodeWithText("went – gone").performClick()
        r.mainClock.advanceTimeBy(4500)
    }) {
        ReviewScreen(listOf(seedCard()), garden.associate { it.bed.id to it.bed }, false, { _, _, _ -> }, {}, verbPeers)
    }

    @Test fun gameBalloons() = shot("16_ballons", settle = 0, prepare = { r ->
        r.mainClock.autoAdvance = false; r.mainClock.advanceTimeBy(2500)
    }) {
        val c = grownCard("003")
        Column(Modifier.padding(16.dp)) { BalloonGame(c, Games.options(c, verbs.cards.map { it.a })!!) {} }
    }

    @Test fun gameLetters() = shot("17_buchstabensalat", prepare = { r ->
        r.onAllNodesWithText("P")[0].performClick()
        r.onAllNodesWithText("a")[0].performClick()
    }) {
        val c = StudyCard("cap", "b", "Hauptstadt von Frankreich?", "Paris", null, null, Memory(3.0, 5.0, 0, 2, 0), "🗼🥐")
        Column(Modifier.padding(16.dp)) { LetterGame(c) {} }
    }

    @Test fun gameSwipe() = shot("18_stimmts") {
        val c = grownCard("010")
        Column(Modifier.padding(16.dp)) { SwipeGame(c, Games.options(c, verbs.cards.map { it.a })!!) {} }
    }

    @Test fun gameMatch() = shot("19_zuordnen", prepare = { r ->
        r.onNodeWithText("„to go“", substring = true).performClick()
        r.onNodeWithText("went – gone").performClick()
        r.onNodeWithText("„to be“", substring = true).performClick()
    }) {
        val cs = listOf("001", "002", "003", "004").map { grownCard(it) }
        Column(Modifier.padding(16.dp)) { MatchBonus(cs) {} }
    }

    @Test fun brummiMoods() = shot("20_brummi") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Brummi(Mood.HAPPY, size = 110.dp, speech = "Super! 🌟")
            Brummi(Mood.SAD, size = 110.dp, speech = "Fehler sind Dünger fürs Wissen 🌱")
            Brummi(Mood.THINK, size = 110.dp, speech = "Stimmt das wirklich? 🤔")
            Brummi(Mood.CHEER, size = 110.dp, speech = "🔥 Du bist on fire!")
            Brummi(Mood.SLEEP, size = 110.dp, speech = "Zzz … bis morgen!")
        }
    }

    @Test fun intro() = shot("21_erklaerung", settle = 2500, prepare = { r ->
        r.onNodeWithText("Überspringen").assertExists()
        r.mainClock.advanceTimeBy(5000)
        r.onNodeWithText("Weiter").performClick()
    }) { IntroScreen(pack("s1-mathe-brueche-prozent")) {} }

    @Test fun adventurePath() = shot("22_abenteuerpfad") {
        val cards = garden[0].cards.sortedBy { it.sortIndex }.map { it.toStudy() }
        AdventurePathView(AdventurePath.stations(cards, now), {}, {}, Modifier.fillMaxSize())
    }

    @Test fun chestClosed() = shot("23a_truhe_zu", settle = 900) { ChestReveal(onOpen = { Friends.all[12] }, onClose = {}) }

    @Test fun chest() = shot("23_truhe", settle = 0, prepare = { r ->
        r.onNodeWithText("Tippe, um sie zu öffnen 👆").performClick()
        r.mainClock.advanceTimeBy(2500)
    }) { ChestReveal(onOpen = { Friends.all[12] }, onClose = {}) }

    @Test fun album() = shot("24_album") {
        Column { FriendsAlbum(opened = 7, earned = 8, inGarden = setOf("marie", "quaki", "ignaz"), onToggle = {}, onOpenChest = {}) }
    }

    @Test fun gardenWithFriends() = shot("04f_garten_freunde") {
        GardenScreen(profile, garden, now, 10, {}, {}, {}, {}, decos = setOf("teich", "zaun"),
            friends = Friends.all.take(6), chestsWaiting = 1)
    }

    @Test fun bed() = shot("05_beet") {
        BedScreen(garden[0], now, 10, {}, {}, { _, _, _ -> }, {}, {})
    }

    @Test fun reviewKids() = shot("07_giessen_grundschule") {
        val kid = bed(pack("gs-tiere"), 6, "kid")
        val tiere = pack("gs-tiere")
        CompositionLocalProvider(de.wissensgarten.app.ui.common.LocalTheory provides { _: String -> de.wissensgarten.app.ui.common.Theory("${tiere.emoji} ${tiere.title}", tiere.intro) }) {
            ReviewScreen(listOf(kid.cards[2].toStudy()), mapOf(kid.bed.id to kid.bed), true, { _, _, _ -> }, {})
        }
    }

    @Test fun theory() = shot("66_theorie", settle = 1500) {
        val tiere = pack("gs-tiere")
        de.wissensgarten.app.ui.common.TheorySheet(de.wissensgarten.app.ui.common.Theory("${tiere.emoji} ${tiere.title}", tiere.intro)) {}
    }

    @Test fun shop() = shot("08_samenhandlung") {
        ShopScreen(profile, packs, garden, { true }, {}, {}, { _, _ -> })
    }

    @Test fun family() = shot("09_familie") {
        val kid = profile.copy(id = "kid", name = "Ben", avatar = "🐸", level = 3, dewdrops = 88)
        val mama = profile.copy(id = "mama", name = "Mama", avatar = "🦉", level = Levels.ADULT, dewdrops = 40)
        val allBeds = garden + bed(pack("gs-einmaleins"), 20, "kid") + bed(pack("aw-erste-hilfe"), 8, "mama")
        val logs = (0 until 60).map { i ->
            ReviewLogEntity(i.toLong(), listOf("p1", "kid", "kid", "mama")[i % 4], "c", now - (i % 6) * day - i * 1000L, 2, 3.0)
        }
        FamilyScreen(listOf(profile, kid, mama), "p1", allBeds, logs, now, {}, {})
    }

    // ---------------------------------------------------------------- 0.8.0

    private val advent = 1_796_900_400_000L // 10.12.2026, 11:00 UTC

    @Test fun teach() = shot("25_brummi_beibringen") {
        val c = garden[0].cards[3].toStudy().copy(img = "🏃💨")
        Column(Modifier.padding(16.dp)) { TeachGame(c, Games.options(c, garden[0].cards.map { it.back }) ?: listOf(c.back, "x")) {} }
    }

    @Test fun tilt() {
        // Das Kipp-Spiel läuft Bild für Bild – Uhr von Hand weiterdrehen
        rule.mainClock.autoAdvance = false
        val c = garden[1].cards[2].toStudy()
        rule.setContent { WissensgartenTheme(ThemeMode.LIGHT) { Surface {
            Column(Modifier.padding(16.dp)) { TiltGame(c, Games.options(c, garden[1].cards.map { it.back })?.let { o -> listOf(c.back, o.first { it != c.back }) } ?: listOf(c.back, "x")) {} }
        } } }
        rule.mainClock.advanceTimeBy(1800)
        rule.onRoot().captureRoboImage("../screenshots/26_kipp_spiel.png")
    }

    @Test fun festivalGarden() = shot("27_garten_advent") {
        GardenScreen(profile, garden, advent, 10, {}, {}, {}, {}, decos = setOf("teich"),
            friends = Friends.all.take(3), surprises = listOf(0 to "🎁", 1 to "⭐", 2 to "🍪"),
            festivalGreeting = Seasons.festival(kotlinx.datetime.LocalDate(2026, 12, 10))?.greeting,
            onGoodNight = {},
            extraCards = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StoryEntryCard(Story.chapters[2]) {}
                    MixEntryCard("Englisch", "Biologie") {}
                }
            })
    }

    @Test fun story() = shot("28_abenteuer_geschichte") { StoryScreen(grown = 40, read = setOf(1, 2), onBack = {}, onRead = {}) }

    @Test fun hybrid() = shot("29_kreuzung", settle = 2500) {
        HybridReveal(Hybrids.of(Subject.MATHE, Subject.MUSIK)) {}
    }

    @Test fun albumHybrids() = shot("30_album_kreuzungen") {
        Column { FriendsAlbum(opened = 3, earned = 3, inGarden = setOf("marie"), onToggle = {}, onOpenChest = {},
            hybrids = listOf(Hybrids.of(Subject.MATHE, Subject.MUSIK), Hybrids.of(Subject.BIOLOGIE, Subject.ENGLISCH), Hybrids.of(Subject.GESCHICHTE, Subject.DEUTSCH))) }
    }

    @Test fun familyTree() = shot("31_familienbaum") {
        val kid = profile.copy(id = "kid", name = "Ben", avatar = "🐸", level = 3, dewdrops = 88)
        val logs = (0 until 31).map { i -> ReviewLogEntity(i.toLong(), listOf("p1", "kid", "kid")[i % 3], "c", now - (i % 6) * day, 2, 3.0) }
        FamilyScreen(listOf(profile, kid), "p1", garden, logs, now, {}, {})
    }

    @Test fun drawDialog() = shot("32_merkbild_malen", prepare = { r ->
        r.onNode(isDialog()).performTouchInput {
            // Zickzack-Strich auf der Malfläche
            swipe(Offset(width * 0.25f, height * 0.3f), Offset(width * 0.75f, height * 0.42f), 300)
        }
        r.waitForIdle()
        r.onNodeWithText("Fertig! 🎨").assertIsEnabled()
    }) {
        DrawDialog("demo", "dog → Hund", "🐶") {}
    }

    @Test fun classQr() = shot("33_klassen_code", settle = 2000) {
        ClassShareDialog(ClassSet("Englisch Unit 3", "🎩", garden[0].cards.take(25).map { ClassCard(it.front, it.back, "🏃") })) {}
    }

    @Test fun classImport() = shot("34_klassen_beet") {
        val set = ClassSet("Tiere auf Englisch", "🐾", listOf(
            ClassCard("dog", "Hund", "🐶"), ClassCard("cat", "Katze", "🐱"), ClassCard("horse", "Pferd", "🐴"),
            ClassCard("bird", "Vogel", "🐦"), ClassCard("cow", "Kuh", "🐮"), ClassCard("fish", "Fisch", "🐟")))
        de.wissensgarten.app.ui.klasse.ClassImportScreen(set, { null }, {}, {})
    }

    @Test fun widget() {
        val flowers = listOf(de.wissensgarten.app.widget.Flower(330f, 1f, true), de.wissensgarten.app.widget.Flower(200f, 0.3f, true),
            de.wissensgarten.app.widget.Flower(45f, 0.8f, true), de.wissensgarten.app.widget.Flower(120f, 1f, false))
        val day = de.wissensgarten.app.widget.GardenBitmap.render(480, 240, flowers, 11, false)
        val party = de.wissensgarten.app.widget.GardenBitmap.render(480, 240, flowers, 22, true)
        val out = android.graphics.Bitmap.createBitmap(480, 490, android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(out).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(day, 0f, 0f, null); drawBitmap(party, 0f, 250f, null) }
        File("../screenshots/36_widget_garten.png").outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun chapterEnd() = shot("28b_kapitel_geschafft") { ChapterEnd(Story.chapters[0], grown = 2, onNext = {}, onWater = {}, onList = {}) }

    @Test fun appUpdate() = shot("37_app_update") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            de.wissensgarten.app.ui.settings.UpdateSection("0.8.2", de.wissensgarten.app.ui.settings.UpdateState.Available(
                AppRelease("0.8.3", "Malen hält jetzt die Runde an. Neuer Knopf für App-Updates.", "https://example.org/a.apk", 3_484_701)))
            de.wissensgarten.app.ui.settings.UpdateSection("0.8.2", de.wissensgarten.app.ui.settings.UpdateState.Downloading(
                AppRelease("0.8.3", "", "https://example.org/a.apk", 3_484_701), 0.62f))
            de.wissensgarten.app.ui.settings.UpdateSection("0.8.3", de.wissensgarten.app.ui.settings.UpdateState.UpToDate)
        }
    }

    /** Seitenwechsel „Wie heißt du?“ → „In welcher Klasse?“ mitten in der Animation: nichts darf übereinanderliegen. */
    @Test fun onboardingTransition() {
        rule.setContent { WissensgartenTheme(ThemeMode.LIGHT) { Surface { OnboardingScreen(packs, false, {}) { _, _, _, _, _ -> } } } }
        rule.onNodeWithText("Los geht's").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("Mein Kind").performClick()
        rule.onNodeWithText("Weiter").performClick(); rule.mainClock.advanceTimeBy(800)
        rule.onNode(hasSetTextAction()).performTextInput("Lena")
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Weiter").performClick()
        rule.mainClock.advanceTimeBy(100)
        rule.onRoot().captureRoboImage("build/uebergang_100ms.png")
        rule.mainClock.advanceTimeBy(100)
        rule.onRoot().captureRoboImage("build/uebergang_200ms.png")
    }

    @Test fun settings() {
        rule.setContent { WissensgartenTheme(ThemeMode.LIGHT) { Surface {
            de.wissensgarten.app.ui.settings.SettingsScreen(profile, true, ThemeMode.LIGHT, true, {}, packCount = 43, cardCount = 1479,
                version = "0.8.4", onTheme = {}, onUpdateProfile = {}, onDeleteProfile = {}, onTutorial = {},
                update = AppRelease("0.8.5", "Brummi schaut beim Start nach Updates.", "https://example.org/a.apk", 3_500_000))
        } } }
        rule.mainClock.advanceTimeBy(800)
        rule.onRoot().captureRoboImage("../screenshots/38_einstellungen_update.png")
    }

    @Test fun gardenUpdateBanner() = shot("39_garten_update") {
        GardenScreen(profile, garden, now, 10, {}, {}, {}, {}, extraCards = {
            de.wissensgarten.app.ui.settings.UpdateBanner("0.8.5") {}
        })
    }

    @Test fun syncCards() = shot("40_geraete_sync") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            de.wissensgarten.app.ui.settings.SyncCard(de.wissensgarten.app.sync.SyncStatus(), null, {}, {}, {})
            de.wissensgarten.app.ui.settings.SyncCard(de.wissensgarten.app.sync.SyncStatus(connected = true, account = "familie.daum@gmail.com",
                lastSync = System.currentTimeMillis() - 3 * 60_000), "Garten zusammengeführt! 🎉", {}, {}, {})
        }
    }

    @Test fun welcomeRestore() = shot("01b_willkommen_wiederherstellen") {
        OnboardingScreen(packs, false, {}, welcomeExtra = {
            androidx.compose.material3.OutlinedButton(onClick = {}) { androidx.compose.material3.Text("☁️ Schon einen Garten? Mit Google wiederherstellen") }
        }) { _, _, _, _, _ -> }
    }

    @Test fun examCards() = shot("41_klassenarbeit") {
        val cards = garden[3].cards.map { it.toStudy() }
        val bedId = garden[3].bed.id
        val today = ExamPlanner.day(now)
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(6, 1, -1).forEach { d ->
                val plan = ExamPlanner.plan(Exam("e$d", bedId, if (d < 0) "Vokabeltest" else "Mathe-Arbeit", today + d), cards, now)
                de.wissensgarten.app.ui.exam.ExamCard(plan, "Brüche & Prozent", {}, {}, {})
            }
            de.wissensgarten.app.ui.exam.ExamPromptCard {}
        }
    }

    @Test fun radio() = shot("42_brummi_radio") {
        val cards = garden[0].cards.take(10).map { it.toStudy() }
        de.wissensgarten.app.ui.radio.RadioScreen(cards, emptyMap(), emptyMap(), { _, _, _ -> }, {}, preview = true)
    }

    @Test fun clapGame() = shot("43_silben_klatschen", settle = 300, prepare = { r ->
        r.mainClock.advanceTimeBy(3500) // Brummi macht vor
        repeat(2) { r.onNodeWithText("👏").performClick(); r.mainClock.advanceTimeBy(120) }
    }) {
        Column(Modifier.padding(16.dp)) { ClapBonus(listOf("Giraffe", "Elefant")) {} }
    }

    @Test fun syllablesInReview() = shot("44_silben_farbig") {
        val kid = bed(pack("gs-tiere"), 6, "kid")
        androidx.compose.runtime.CompositionLocalProvider(de.wissensgarten.app.ui.fx.LocalSyllables provides true) {
            ReviewScreen(listOf(kid.cards[2].toStudy()), mapOf(kid.bed.id to kid.bed), true, { _, _, _ -> }, {}, subjects = mapOf(kid.bed.id to "sachunterricht"))
        }
    }

    @Test fun safari() = shot("45_wissens_safari", prepare = { r -> r.onNodeWithText("🐶  der Hund").performClick(); r.mainClock.advanceTimeBy(1200) }) {
        // Testbild: ein gemalter „Hund“ (brauner Kreis mit Ohren) auf Wiese
        val bmp = android.graphics.Bitmap.createBitmap(600, 600, android.graphics.Bitmap.Config.ARGB_8888).apply {
            val c = android.graphics.Canvas(this); val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            c.drawColor(0xFF9CCC65.toInt()); p.color = 0xFF8D6E63.toInt(); c.drawCircle(300f, 320f, 170f, p)
            c.drawOval(100f, 160f, 220f, 380f, p); c.drawOval(380f, 160f, 500f, 380f, p)
            p.color = 0xFF000000.toInt(); c.drawCircle(240f, 300f, 18f, p); c.drawCircle(360f, 300f, 18f, p); c.drawCircle(300f, 380f, 28f, p)
        }
        de.wissensgarten.app.ui.safari.SafariScreen(6, true, { _, _, _ -> }, {}, previewPhoto = bmp,
            previewWords = Safari.candidates(listOf("Dog" to 0.9f, "Pet" to 0.8f, "Bear" to 0.6f)))
    }

    @Test fun keepsakes() {
        val d = de.wissensgarten.app.ui.share.KeepsakeData("Lena", "🦊", 86, 12, 1234, 47, 9,
            listOf("🐘" to "Größtes Landtier → Elefant", "🏃" to "to run → ran – run", "➗" to "1/2 + 1/4 → 3/4"),
            listOf(de.wissensgarten.app.widget.Flower(340f, 1f, true), de.wissensgarten.app.widget.Flower(120f, 0.7f, true),
                de.wissensgarten.app.widget.Flower(210f, 1f, true), de.wissensgarten.app.widget.Flower(45f, 0.9f, false)),
            listOf("Irregular Verbs", "Zelle & Mensch", "Europa", "Brüche"))
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val png = de.wissensgarten.app.ui.share.Keepsakes.postcard(d, hour = 11)
        File("../screenshots/46_postkarte.png").outputStream().use { png.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        // PDF gibt es nur auf dem Gerät – hier dieselbe Zeichnung als Bild (doppelte Auflösung)
        val cert = android.graphics.Bitmap.createBitmap(1190, 1684, android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(cert).apply { scale(2f, 2f); de.wissensgarten.app.ui.share.Keepsakes.drawCertificate(this, d) }
        File("../screenshots/47_urkunde.png").outputStream().use { cert.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(ctx != null)
    }

    @Test fun localDuel() {
        // Der 15-Sekunden-Timer würde bei laufender Test-Uhr sofort ablaufen – Uhr von Hand weiterdrehen
        rule.mainClock.autoAdvance = false
        val p = pack("gs-tiere")
        val qs = Duels.questions(p, 4)
        rule.setContent { WissensgartenTheme(ThemeMode.LIGHT) { Surface { de.wissensgarten.app.ui.duel.LocalDuelScreen(qs, "${p.emoji} ${p.title}", {}) } } }
        rule.mainClock.advanceTimeBy(1200)
        rule.onAllNodesWithText(qs[0].answer)[1].performClick() // unten (Blau) tippt richtig
        rule.mainClock.advanceTimeBy(400)
        rule.onRoot().captureRoboImage("../screenshots/48_zu_zweit.png")
    }

    @Test fun albumCrystals() = shot("49_kristalle") {
        Column { FriendsAlbum(opened = 2, earned = 2, inGarden = emptySet(), onToggle = {}, onOpenChest = {},
            crystals = listOf("to choose" to "chose – chosen", "Hauptstadt von Australien?" to "Canberra", "7 × 8" to "56", "Mitochondrium" to "Kraftwerk der Zelle"),
            crystalsOnTheWay = 5) }
    }

    @Test fun familyReward() = shot("50_familienbelohnung") {
        val kid = profile.copy(id = "kid", name = "Ben", avatar = "🐸", level = 3, dewdrops = 88)
        val logs = (0 until 40).map { i -> ReviewLogEntity(i.toLong(), listOf("p1", "kid")[i % 2], "c", now - (i % 6) * day, 2, 3.0) }
        FamilyScreen(listOf(profile, kid), "p1", garden, logs, now, {}, {}, familyClaimed = true, familyReward = "Eis essen 🍦")
    }

    @Test fun gardenHome() = shot("04g_garten_start") {
        GardenScreen(profile, garden, now, 10, {}, {}, {}, {}, extraCards = {
            de.wissensgarten.app.ui.garden.DiscoverRow(listOf(
                de.wissensgarten.app.ui.garden.DiscoverItem("📖", "Abenteuer", "Neues Kapitel: Schnecki weiß es besser", androidx.compose.ui.graphics.Color(0xFFEDE7F6), "NEU") {},
                de.wissensgarten.app.ui.garden.DiscoverItem("🔀", "Mischrunde", "Englisch × Biologie – züchte eine Kreuzung!", androidx.compose.ui.graphics.Color(0xFFE0F7FA)) {},
                de.wissensgarten.app.ui.garden.DiscoverItem("📝", "Klassenarbeit?", "Eintragen – Brummi plant, damit am Tag X alles sitzt.", androidx.compose.ui.graphics.Color(0xFFFFF8E1)) {},
                de.wissensgarten.app.ui.garden.DiscoverItem("🎧", "Brummi-Radio", "Lernen ohne Bildschirm.", androidx.compose.ui.graphics.Color(0xFFE8EAF6)) {},
            ))
        })
    }

    @Test fun parentGate() = shot("51_eltern_tor") {
        de.wissensgarten.app.ui.common.ParentGateDialog("Die Einstellungen sind für Eltern: Sync mit Google, Updates, Daten löschen.", {}, {}, kotlin.random.Random(4))
    }

    @Test fun privacy() = shot("52_datenschutz") { de.wissensgarten.app.ui.settings.PrivacyScreen {} }

    @Test fun storePlusSoon() = shot("53_plus_kommt_bald") {
        StoreScreen(profile, emptyList(), garden, packs.filter { it.isPlus }, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, initialTab = 3)
    }

    @Test fun typeAnswer() = shot("54_antwort_tippen", prepare = { r ->
        r.onNode(hasSetTextAction()).performTextInput("Pariss")
        r.onNodeWithText("Prüfen ✓").performClick()
    }) {
        val c = StudyCard("cap", "b", "Hauptstadt von Frankreich?", "Paris", null, null, Memory(3.0, 5.0, 0, 2, 0), "🗼🥐")
        Column(Modifier.padding(16.dp)) { de.wissensgarten.app.ui.play.TypeGame(c, strictSpelling = false) {} }
    }

    @Test fun weekReport() = shot("55_wochenbericht") {
        val day = de.wissensgarten.core.model.Scheduler.DAY_MS
        val now = System.currentTimeMillis()
        val cards = mapOf(
            "a" to de.wissensgarten.core.model.WeekReport.CardInfo("7 × 8", "mathe", 3.0, 2),
            "b" to de.wissensgarten.core.model.WeekReport.CardInfo("Hauptstadt von Frankreich?", "erdkunde", 30.0, 0),
            "c" to de.wissensgarten.core.model.WeekReport.CardInfo("the dog – der …?", "englisch", 12.0, 0),
            "d" to de.wissensgarten.core.model.WeekReport.CardInfo("Wie viele Beine hat eine Spinne?", "sachunterricht", 8.0, 1),
        )
        val e = (0..40).map { i -> de.wissensgarten.core.model.WeekReport.Entry(listOf("a", "b", "c", "d", "c", "b")[i % 6],
            now - (listOf(0, 1, 3, 4, 6)[i % 5]) * day, if (i % 7 == 0 && i % 6 == 0) 0 else if (i % 6 == 0 || i % 9 == 3) 0 else 2) }
        val r = de.wissensgarten.core.model.WeekReport.build(e, cards, now)
        de.wissensgarten.app.ui.parents.WeekReportScreen(listOf(profile, profile.copy(id = "p2", name = "Ben", avatar = "🐻")), "p1", { r }) {}
    }

    @Test fun restTime() = shot("56_garten_ruht") { de.wissensgarten.app.ui.parents.RestOverlay("Lena") {} }

    private fun pool() = verbs.cards.take(12).mapIndexed { i, c ->
        StudyCard("v$i", "b", c.q, c.a, c.choices, c.info, Memory(2.0 + i, 5.0, now - 3 * day, 3, if (i < 3) 3 else 0), c.img)
    }
    private fun peerMap() = mapOf("b" to verbs.cards.map { it.a })

    @Test fun weeds() = shot("58_unkraut") {
        de.wissensgarten.app.ui.play.WeedScreen(pool().take(3), peerMap(), 7, {}, {}) {}
    }

    @Test fun blitz() = shot("59_blitzrunde", prepare = { r -> r.onNodeWithText("Los geht's! ⚡").performClick(); r.mainClock.advanceTimeBy(300) }) {
        de.wissensgarten.app.ui.play.BlitzScreen(pool(), peerMap(), {}) {}
    }

    @Test fun rhythm() = shot("60_rhythmus", prepare = { r -> r.onNodeWithText("Musik ab! 🎶").performClick(); r.mainClock.advanceTimeBy(Rhythm.BEAT_MS + 50) }) {
        de.wissensgarten.app.ui.play.RhythmScreen(pool(), peerMap(), { _, _ -> }) {}
    }

    @Test fun hive() = shot("61_bienenstock") {
        de.wissensgarten.app.ui.garden.HiveScreen(187, { if (it == "eingang") 0 else if (it == "schlafen") 1 else -1 }, { _, _ -> }) {}
    }

    @Test fun questsAndCritters() = shot("62_aufgaben_tiere") {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            de.wissensgarten.app.ui.garden.QuestCard(
                listOf(Quests.Quest(Quests.Kind.WATER, 10), Quests.Quest(Quests.Kind.WEED, 1), Quests.Quest(Quests.Kind.RHYTHM, 1)),
                mapOf("WATER" to 6, "WEED" to 1), 8, false) {}
            de.wissensgarten.app.ui.garden.CrittersCard(9, 16)
        }
    }

    @Test fun loci() = shot("63_gedaechtnis_spaziergang", prepare = { r -> r.onAllNodesWithText("🌲")[0].performClick() }) {
        val cards = pool()
        de.wissensgarten.app.ui.garden.LociScreen(cards.take(6), cards.associateBy { it.id }, mapOf(0 to "v0", 2 to "v1"), peerMap(), {}, { _, _ -> }, {}) {}
    }

    @Test fun film() = shot("64_garten_film", settle = 200) {
        val plants = (0 until 36).map { i -> GardenFilm.Plant(now - (60 - i) * day, now - (i % 5) * day, (i % 9) * 8.0 + 0.5, if (i % 7 == 0) 0 else 3 + i % 4) }
        de.wissensgarten.app.ui.share.GardenFilmScreen(GardenFilm.frames(plants, now).takeLast(1), "Lena", {}) {}
    }

    @Test fun parentSettings() = shot("57_eltern_einstellungen") {
        de.wissensgarten.app.ui.settings.SettingsScreen(profile, true, ThemeMode.LIGHT, true, {}, packCount = 47, cardCount = 1607,
            version = "0.17.0", onTheme = {}, onUpdateProfile = {}, onDeleteProfile = {}, onTutorial = {},
            dailyLimit = 20, holiday = true)
    }

    @Test fun soundSettings() = shot("70_ton") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp)) {
            androidx.compose.material3.Text("Ton", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            de.wissensgarten.app.ui.settings.SoundSettingsContent(45, 65, music = true, sound = true, onMusicVol = {}, onSfxVol = {}, onProbe = {})
        }
    }

    /** Stimm-Einstellungen unter „Brummi liest vor“: Nova (Kennzeichnung), Sprechtempo-Stufen, Probe-Taste */
    private fun fakeVoice() = object : de.wissensgarten.app.ui.fx.Voice {
        override var onSpeaking: (Boolean) -> Unit = {}
        override val isReady = true
        override fun speak(text: String, locale: de.wissensgarten.app.ui.fx.Locale) {}
        override fun stop() {}
        override suspend fun speakAndWait(text: String, locale: de.wissensgarten.app.ui.fx.Locale) {}
    }
    /** „Alle Nova-Texte offline laden“ in drei Zuständen (feste Zahlen, ohne Netz) */
    private fun offlineShot(name: String, z: de.wissensgarten.app.ui.fx.OfflineZustand) = shot(name) {
        val settings = de.wissensgarten.core.data.Settings(de.wissensgarten.core.data.MemoryKeyValueStore())
        val ctl = object : de.wissensgarten.app.ui.fx.NovaOfflineSteuerung {
            override val zustand = kotlinx.coroutines.flow.MutableStateFlow(z)
            override fun pruefen() {}
            override fun laden(q: de.wissensgarten.core.model.NovaQualitaet) {}
            override fun abbrechen() {}
            override fun loeschen() {}
        }
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) {
            androidx.compose.material3.Text("Brummi liest vor", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            de.wissensgarten.app.ui.settings.VoiceSettingsPanel(settings, fakeVoice(), ctl)
        }
    }
    private val std = de.wissensgarten.core.model.NovaQualitaet.STANDARD
    private val best = de.wissensgarten.core.model.NovaQualitaet.BEST
    @Test fun offlineButton() = offlineShot("73_offline_laden", de.wissensgarten.app.ui.fx.OfflineZustand(
        geprueft = true, gesamt = 7802, fehlend = mapOf(std to 7650, best to 7802), rest = mapOf(std to 348_170_000L, best to 429_110_000L), belegt = 7_000_000L))
    @Test fun offlineLoading() = offlineShot("74_offline_laedt", de.wissensgarten.app.ui.fx.OfflineZustand(
        geprueft = true, gesamt = 7802, fehlend = mapOf(std to 7650, best to 7802), laedt = std, fertig = 3120, ziel = 7802))
    @Test fun offlineDone() = offlineShot("75_offline_fertig", de.wissensgarten.app.ui.fx.OfflineZustand(
        geprueft = true, gesamt = 7802, fehlend = mapOf(std to 0, best to 7802), rest = mapOf(std to 0L, best to 429_110_000L), belegt = 355_090_000L,
        hinweis = "✅ Fertig! Brummi liest jetzt auch ohne Internet mit Nova vor."))

    @Test fun voice() = shot("67_stimme") {
        val settings = de.wissensgarten.core.data.Settings(de.wissensgarten.core.data.MemoryKeyValueStore()).apply { setVoiceRate(115) }
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) {
            androidx.compose.material3.Text("Brummi liest vor", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            de.wissensgarten.app.ui.settings.VoiceSettingsPanel(settings, fakeVoice())
        }
    }

    @Test fun gardenPlusBadge() = shot("71_garten_plus_aktiv") {
        androidx.compose.runtime.CompositionLocalProvider(de.wissensgarten.app.ui.plus.LocalPlusStatus provides
            de.wissensgarten.core.data.PlusStatus(active = true, product = 1, until = 21138, enforced = true)) {
            GardenScreen(profile, garden, now, 10, {}, {}, {}, {})
        }
    }

    @Test fun parentPlusBadge() = shot("72_eltern_plus_aktiv") {
        androidx.compose.runtime.CompositionLocalProvider(de.wissensgarten.app.ui.plus.LocalPlusStatus provides
            de.wissensgarten.core.data.PlusStatus(active = true, product = de.wissensgarten.core.model.License.PRODUCT_FOREVER, enforced = true)) {
            de.wissensgarten.app.ui.settings.SettingsScreen(profile, true, ThemeMode.LIGHT, true, {}, packCount = 72, cardCount = 2472,
                version = "2.0.2", onTheme = {}, onUpdateProfile = {}, onDeleteProfile = {}, onTutorial = {}, onPlus = {}, plusActive = true)
        }
    }

    @Test fun speakButton() = shot("76_vorlesen_knopf") {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            de.wissensgarten.app.ui.fx.SilbenText("Wie heißt die Hauptstadt von Frankreich?", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            de.wissensgarten.app.ui.play.SpeakButton("Frage vorlesen", androidx.compose.ui.Modifier.padding(top = 8.dp)) {}
        }
    }

    /** Plattform-Attrappe wie in der WebApp: „App installieren“ wird angeboten ([mode]). */
    private fun installHost(mode: de.wissensgarten.app.InstallMode) = object : de.wissensgarten.app.AppHost {
        override val features = de.wissensgarten.app.Features(label = "WebApp", keepsakes = false, plusPurchase = true, tilt = true)
        override val sfx = object : de.wissensgarten.app.ui.fx.Sfx {
            override var enabled = false; override var level = 0f
            override fun play(s: de.wissensgarten.app.ui.fx.Sound, pitch: Float, volume: Float) {}
        }
        override val music = object : de.wissensgarten.app.ui.fx.Music {
            override fun setEnabled(on: Boolean) {}; override fun setMood(m: de.wissensgarten.core.model.MusicMood) {}
            override fun onForeground(on: Boolean) {}; override fun duck(on: Boolean) {}
            override fun prepare(vararg moods: de.wissensgarten.core.model.MusicMood) {}; override fun setVolume(v: Float) {}; override fun unlock() {}
        }
        override val voice: de.wissensgarten.app.ui.fx.Voice? = null
        override val listener: de.wissensgarten.app.ui.fx.Listener? = null
        override val images = object : de.wissensgarten.app.ImageStore {
            override fun save(name: String, image: androidx.compose.ui.graphics.ImageBitmap) {}
            override fun load(name: String): androidx.compose.ui.graphics.ImageBitmap? = null
            override fun exists(name: String) = false
            override fun delete(name: String) {}
        }
        override val installOffer = kotlinx.coroutines.flow.MutableStateFlow(de.wissensgarten.app.InstallOffer(mode))
        override fun toast(message: String) {}
        override fun shareText(text: String) {}
        override fun shareImage(image: androidx.compose.ui.graphics.ImageBitmap, fileName: String, text: String) {}
        override fun printCraftSheet(bedName: String, emoji: String, items: List<de.wissensgarten.app.ui.share.CraftItem>) {}
        override suspend fun wipeAll(): String? = null
        override fun restart() {}
    }

    @Test fun installGarden() = shot("77_garten_app_installieren") {
        val host = installHost(de.wissensgarten.app.InstallMode.PROMPT)
        androidx.compose.runtime.CompositionLocalProvider(de.wissensgarten.app.ui.fx.LocalHost provides host) {
            GardenScreen(profile, garden, now, 10, {}, {}, {}, {},
                installCard = { de.wissensgarten.app.ui.settings.InstallAppCard(host, inGarden = true) })
        }
    }

    @Test fun installSettings() = shot("78_einstellungen_app_installieren") {
        val host = installHost(de.wissensgarten.app.InstallMode.PROMPT)
        androidx.compose.runtime.CompositionLocalProvider(de.wissensgarten.app.ui.fx.LocalHost provides host) {
            de.wissensgarten.app.ui.settings.SettingsScreen(profile, true, ThemeMode.LIGHT, true, {}, packCount = 72, cardCount = 2472,
                version = "2.2.7", onTheme = {}, onUpdateProfile = {}, onDeleteProfile = {}, onTutorial = {}, onPlus = {})
        }
    }

    @Test fun installIphone() = shot("79_iphone_app_installieren", prepare = { r ->
        r.onNodeWithText("So geht's").performClick(); r.mainClock.advanceTimeBy(600)
    }) {
        val host = installHost(de.wissensgarten.app.InstallMode.IOS_GUIDE)
        androidx.compose.runtime.CompositionLocalProvider(de.wissensgarten.app.ui.fx.LocalHost provides host) {
            GardenScreen(profile, garden, now, 10, {}, {}, {}, {},
                installCard = { de.wissensgarten.app.ui.settings.InstallAppCard(host, inGarden = true) })
        }
    }

    @Test fun plus() = shot("65_plus") {
        de.wissensgarten.app.ui.plus.PlusScreen(
            status = de.wissensgarten.core.data.PlusStatus(active = false, enforced = true), today = 20773,
            purchaseAllowed = true, checkoutYear = "https://shop/jahr", checkoutForever = "https://shop/immer",
            onTrial = { true }, onRedeem = { de.wissensgarten.core.data.PlusEntitlements.Redeem.INVALID }, onBack = {},
        )
    }
}

/** Play-Store-Fotos Handy: 1080 × 1920 (9:16, Store verlangt höchstens 2:1). */
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
class StorePhoneShots : ScreenshotTest() { override val outDir = "../store/handy" }

/** Play-Store-Fotos Tablet: 1600 × 2560. */
@Config(sdk = [35], qualifiers = "w800dp-h1280dp-xhdpi")
class StoreTabletShots : ScreenshotTest() { override val outDir = "../store/tablet"
}
