package de.wissensgarten.app.ui

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
import de.wissensgarten.app.ui.store.StoreScreen
import de.wissensgarten.app.ui.duel.DuelHubScreen
import de.wissensgarten.app.ui.duel.DuelPlayScreen
import de.wissensgarten.core.data.ThemeMode
import de.wissensgarten.core.data.db.*
import de.wissensgarten.core.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Rendert die Bildschirme mit echten Paketen und einem „gelebten“ Garten – ohne Gerät. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

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
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
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

    @Test fun levelUp() = shot("06b_aufgestiegen", settle = 1150, prepare = { r ->
        r.onNodeWithText("went – gone").performClick()
        r.mainClock.advanceTimeBy(300)
        r.mainClock.autoAdvance = false
        r.onNodeWithText("Gießen 💧").performClick()
    }) {
        // Karte kurz vor dem Aufstieg zum Busch
        val c = garden[0].cards.first { it.sourceId == "001" }
        val m = Memory(6.5, 5.0, now - 7 * day, 3, 0)
        ReviewScreen(listOf(c.withMemory(m).toStudy()), garden.associate { it.bed.id to it.bed }, false, { _, _, _ -> }, {})
    }

    @Test fun sessionDone() = shot("10_runde_fertig", settle = 1000, prepare = { r ->
        r.onNodeWithText("went – gone").performClick()
        r.mainClock.advanceTimeBy(300)
        r.mainClock.autoAdvance = false
        r.onNodeWithText("Gießen 💧").performClick()
        r.mainClock.advanceTimeBy(2000)
    }) {
        val c = garden[0].cards.first { it.sourceId == "001" }.toStudy()
        ReviewScreen(listOf(c), garden.associate { it.bed.id to it.bed }, false, { _, _, _ -> }, {})
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

    @Test fun bed() = shot("05_beet") {
        BedScreen(garden[0], now, 10, {}, {}, { _, _, _ -> }, {}, {})
    }

    @Test fun reviewChoice() = shot("06_giessen_auswahl", prepare = { r ->
        r.onNodeWithText("went – gone").performClick()
    }) {
        val card = garden[0].cards.first { it.sourceId == "001" }.toStudy()
        ReviewScreen(listOf(card), garden.associate { it.bed.id to it.bed }, false, { _, _, _ -> }, {})
    }

    @Test fun reviewKids() = shot("07_giessen_grundschule") {
        val kid = bed(pack("gs-tiere"), 6, "kid")
        ReviewScreen(listOf(kid.cards[2].toStudy()), mapOf(kid.bed.id to kid.bed), true, { _, _, _ -> }, {})
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
}
