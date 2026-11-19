package de.raumlicht.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.data.Settings
import de.raumlicht.core.model.Levels
import kotlinx.coroutines.flow.MutableStateFlow
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Der gemeinsame App-Rahmen (derselbe für Android und Web): Wege durch die App, „zurück“, Sprünge von außen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class AppRootTest {
    @get:Rule val rule = createComposeRule()
    private val nav = Navigator(); private val stars = MutableStateFlow(mapOf<String, Int>()); private val routes = ArrayList<String>()
    private lateinit var prefs: Settings
    // Schlüssel und Code nur für Tests (Seed 0…31)
    private val PK = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val PASS = "RL-04202-0000F-MGKKT-CDRGB-BYPG5-8XEVQ-M0F02-94P76-M1MXZ-28QYX-86PN1-NEY0T-JNXA5-8S0GJ-HA3X2-C0GQW-AW2K1-BQN3M-4PV5F-ZHQBG-ZKMPE-KJSDT-6VARJ-0W"
    // KI-Pässe aus dem Test-Schlüssel: gültig bis 31.07.2027 und abgelaufen am 31.07.2026 („heute“ ist im Test der 15.11.2026)
    private val KI_PASS = "RL-04208-0000Z-8GNM5-HJFN6-SDW5Y-QPW2T-BSSPN-9DS5K-7Z3A1-PTFJ2-ACM3Q-GX1H4-ZM8EV-3GR33-K3N31-3VN22-58C7D-HP8Y1-1321W-HG25S-PNJ23-FSKM6-JYD9R-GJ"
    private val KI_ALT = "RL-04208-0000Z-90JRS-KVK76-JC5YR-K5AB8-66P5J-B9FQ0-J8M2Q-N9CCS-9YCEM-0XX2C-KCRBZ-8QGFP-J83ZY-RYKBJ-8WK56-H6461-JQTF0-1PN3P-K7Q9A-337T4-N8R7X-GE"
    private val counted = ArrayList<String>()
    /** Zählaufrufe aus den Aufgaben (gestartet, geschafft, Theorie) – getrennt gesammelt */
    private val played = ArrayList<String>()
    private var crash = de.raumlicht.core.model.Nachhilfe.TRIAL_URL
    /** Nachgestellte Plattform fürs Eltern-Koppeln: merkt sich, was hochgeladen und gelöscht würde (hier unverschlüsselt, die Verschlüsselung prüft der Browser-Test) */
    private val uploads = ArrayList<Triple<String, String, String>>(); private val deleted = ArrayList<String>()
    private var eltern: de.raumlicht.app.ui.settings.ElternPlatform? = null
    private var appsOpened = 0
    private fun fakeEltern() = de.raumlicht.app.ui.settings.ElternPlatform("https://server.test/b/", "https://seite.test/#weg/", { "A".repeat(22) + "." + "B".repeat(22) + "." + "C".repeat(43) },
        { url, write, _, json, done -> uploads += Triple(url, write, json); done(204) }, { url, _, done -> deleted += url; done(204) })
    private fun start(pro: Boolean = false, ki: (suspend (String) -> Pair<Int, String>)? = null, kiOn: Boolean = true) {
        prefs = Settings(ApplicationProvider.getApplicationContext())
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) {
            AppRoot(nav, prefs, stars, { id, s, _ -> stars.value = stars.value + (id to s) }, { 20_772L }, settings = { onBack -> androidx.compose.foundation.layout.Column { androidx.compose.material3.Text("EINSTELLUNGEN", androidx.compose.ui.Modifier.testTag("einst")); de.raumlicht.app.ui.settings.ProEntry(); de.raumlicht.app.ui.settings.KiEntry(); de.raumlicht.app.ui.settings.ElternEntry() } },
                onRoute = { routes += it }, proEnabled = pro, proKey = PK, kiSend = ki, kiEnabled = kiOn, version = "test", eltern = eltern, countSend = { if (Regex("\"ev\":\"(level_|theorie)").containsMatchIn(it)) played += it else counted += it }, examUrl = crash, onApps = { appsOpened++ })
        } } } }
        rule.mainClock.advanceTimeBy(600)
    }
    private fun settle() { rule.mainClock.advanceTimeBy(700); rule.waitForIdle() }

    @Test fun walksThroughTheAppAndBack() {
        start()
        rule.onNodeWithTag("gebiet-GEOMETRIE").assertExists(); assertEquals(listOf(Routes.HOME), nav.stack)
        nav.go(Routes.level("1-1")); settle()
        rule.onNodeWithText(Levels.byId("1-1")!!.title, substring = true).assertExists()
        nav.go(Routes.level("1-1")); assertEquals(2, nav.stack.size)                                   // dasselbe Ziel zweimal: nur einmal offen
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); assertTrue(nav.back()); assertFalse(nav.back()); settle()                       // zweimal schnell „zurück“: nur eine Ebene
        rule.onNodeWithTag("gebiet-GEOMETRIE").assertExists()
        nav.go(Routes.BACKSTAGE); settle(); rule.onNodeWithText("Ballerbude").performScrollTo().performClick(); settle()
        assertEquals(listOf(Routes.HOME, Routes.BACKSTAGE, Routes.BALLOONS), nav.stack); rule.onNodeWithTag("ballern").assertExists()
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("einst").assertExists()
        nav.restart(Routes.DAILY); settle()                                                               // Sprung von außen (Widget): Startseite darunter
        assertEquals(listOf(Routes.HOME, Routes.DAILY), nav.stack); rule.onNodeWithText("Tagesaufgabe", substring = true).assertExists()
        assertTrue(routes.containsAll(listOf(Routes.HOME, "level/1-1", Routes.BACKSTAGE, Routes.BALLOONS, Routes.SETTINGS, Routes.DAILY)))
    }

    @Test fun everyDestinationOpens() {
        start()
        val all = listOf(Routes.DAILY, Routes.JAM_HOME, Routes.jam("MIX"), Routes.SHEET, Routes.BADGES, Routes.BACKSTAGE, Routes.EXAMS, Routes.exam("zelt", false), Routes.LAGE, Routes.DUEL, Routes.TRAINING,
            Routes.WORKSHOP, Routes.BUILDER, Routes.GUIDES, Routes.guide(de.raumlicht.core.model.Guides.all.first().id), Routes.LEARN, Routes.PLANNER, Routes.CLASS, Routes.MATCH, Routes.ride("huegel"), Routes.FLIGHT,
            Routes.GALTON, Routes.BALLOONS, Routes.CHANCE, Routes.BUILD, Routes.GHOST, Routes.RINGS, Routes.QUICK, Routes.DIARY, Routes.CALENDAR, Routes.EXPERIENCE, Routes.ROTATION, Routes.level("18-3"))
        all.forEach { r -> nav.restart(r); settle(); assertEquals(r, listOf(Routes.HOME, r), nav.stack); rule.onNodeWithTag("gebiet-GEOMETRIE").assertDoesNotExist() }
        nav.restart("gibt/es/nicht"); settle(); settle(); assertEquals(listOf(Routes.HOME), nav.stack)       // unbekanntes Ziel: zurück zur Startseite
        assertTrue(Routes.isAction("level/3-2") && Routes.isAction("jam/MIX") && Routes.isAction("abi/zelt/1") && !Routes.isAction(Routes.HOME) && !Routes.isAction(Routes.EXAMS) && !Routes.isAction(Routes.BACKSTAGE))
    }

    /** Raumlicht Pro: eingeschaltet sperrt er genau die Pro-Teile und zeigt die Pro-Seite; ein gültiger Code öffnet alles, ein falscher nichts. */
    @Test fun proGate() {
        Settings(ApplicationProvider.getApplicationContext()).setProCode("")
        start(pro = true)
        rule.onNodeWithTag("kapitel-7").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("schloss-7-3", useUnmergedTree = true).assertExists(); rule.onNodeWithTag("schloss-7-2", useUnmergedTree = true).assertDoesNotExist()
        nav.go(Routes.level("7-2")); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist(); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.level("7-3")); settle(); rule.onNodeWithTag("pro-grund").assertTextContains("Aufgabe 7-3", substring = true); rule.onNodeWithTag("pro-status").assertExists()
        rule.onNodeWithTag("pro-zurueck").performScrollTo().performClick(); settle(); assertEquals(listOf(Routes.HOME), nav.stack)
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.BINOMIAL); settle(); rule.onNodeWithTag("pro-grund").assertTextContains("Binomial-Labor", substring = true); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.CHANCE); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist(); nav.back(); settle()            // Zufallsmaschine bleibt frei
        // über die Einstellungen zur Pro-Seite: Kauf-Knopf und Code-Eingabe
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        rule.onNodeWithTag("pro-abzeichen").assertDoesNotExist()                                                               // noch kein Abzeichen
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("pro-eintrag").assertTextContains("Alles freischalten", substring = true).performClick(); settle()
        assertEquals(Routes.PRO, nav.top); rule.onNodeWithTag("pro-kaufen").assertExists()
        // falscher Code: freundlicher Hinweis, nichts geht auf
        rule.onNodeWithTag("pro-code").performScrollTo().performTextInput("RL-12345-ABCDE"); rule.onNodeWithTag("pro-freischalten").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("pro-grund").assertTextContains("erkenne ich nicht", substring = true); rule.onNodeWithTag("pro-aktiv").assertDoesNotExist(); assertEquals("", prefs.proCode.value)
        rule.onRoot().captureRoboImage("../screenshots/76_abi_pass.png")
        // richtiger Code, mitten aus einer E-Mail kopiert
        rule.onNodeWithTag("pro-code").performTextClearance(); rule.onNodeWithTag("pro-code").performTextInput("Hallo! Dein Code:\n$PASS\nViel Erfolg!")
        rule.onNodeWithTag("pro-freischalten").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("pro-aktiv").assertExists(); rule.onNodeWithTag("pro-kaufen").assertDoesNotExist(); assertEquals(PASS, prefs.proCode.value)
        rule.onRoot().captureRoboImage("../screenshots/77_abi_pass_frei.png")
        rule.onNodeWithTag("pro-zurueck").performScrollTo().performClick(); settle(); rule.onNodeWithTag("pro-eintrag").assertTextContains("Raumlicht Pro aktiv", substring = true)
        // das Abzeichen im Kopf des Startbildschirms – Antippen führt zur Pro-Seite
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle(); assertEquals(Routes.HOME, nav.top); rule.onNodeWithTag("pro-abzeichen").assertTextContains("Pro aktiv", substring = true)
        rule.onRoot().captureRoboImage("../screenshots/79_abi_pass_abzeichen.png")
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); rule.onNodeWithTag("pro-abzeichen").performClick(); settle(); assertEquals(Routes.PRO, nav.top)
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle()
        // freigeschaltet: alles offen, Schlösser weg – auch nach einem Neustart (der Code wird neu geprüft)
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.level("7-3")); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist()
        assertEquals(PASS, Settings(ApplicationProvider.getApplicationContext()).proCode.value)
        rule.runOnUiThread { prefs.setProCode("") }
    }

    /** Ein gespeicherter Code, der nicht (mehr) zum Schlüssel passt, schaltet nichts frei */
    /** KI-Tutor: erst nach einem Fehlversuch, erklärt anders, beantwortet Rückfragen, zählt Proben – und führt zum KI-Abi-Pass, wenn sie aufgebraucht sind. */
    @Test fun kiTutor() {
        val asked = ArrayList<String>()
        val answers = ArrayDeque(listOf(200 to """{"text":"Stell dir den Vektor als Weg vor: erst nach vorn, dann zur Seite, dann nach oben.","rest":2}""",
            200 to """{"text":"Genau – die erste Zahl ist der Schritt in x₁-Richtung.","rest":1}""", 402 to """{"fehler":"proben","rest":0}"""))
        start(ki = { body -> asked += body; answers.removeFirst() })
        nav.go(Routes.level("1-2")); settle()
        rule.onNodeWithTag("ki").assertDoesNotExist()                                                                          // vor dem ersten Versuch kein KI-Knopf
        rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000)
        rule.onNodeWithTag("ki").assertTextContains("Erklär’s mir anders", substring = true).performClick(); settle()
        rule.onNodeWithTag("ki-antwort").assertTextContains("Vektor als Weg", substring = true); rule.onNodeWithTag("ki-rest").assertTextContains("Noch 2 Proben frei", substring = true)
        assertTrue(asked[0], asked[0].contains("\"art\":\"erklaerung\"") && asked[0].contains("\"id\":\"1-2\"") && asked[0].contains("\"geraet\":\"${prefs.kiDevice()}\"") && asked[0].contains("\"code\":\"\""))
        // Rückfrage
        rule.onNodeWithTag("ki-frage").performTextInput("Was heißt die erste Zahl?"); rule.onNodeWithTag("ki-senden").performClick(); settle()
        rule.onAllNodesWithTag("ki-antwort").assertCountEquals(2); rule.onNodeWithText("Was heißt die erste Zahl?").assertExists(); rule.onNodeWithTag("ki-rest").assertTextContains("Noch 1 Probe frei", substring = true)
        assertTrue(asked[1], asked[1].contains("\"art\":\"rueckfrage\"") && asked[1].contains("Was heißt die erste Zahl?") && asked[1].contains("\"rolle\":\"lux\""))
        rule.onRoot().captureRoboImage("../screenshots/82_ki_tutor.png")
        // Proben aufgebraucht: nur die sachliche Auskunft – keine Werbung für den Pass, kein Knopf zur Kaufseite; der bisherige Wortwechsel bleibt
        rule.onNodeWithTag("ki-anders").performClick(); settle()
        rule.onNodeWithTag("ki-problem").assertTextEquals("Deine 3 Proben sind aufgebraucht."); rule.onAllNodesWithTag("ki-antwort").assertCountEquals(2)
        rule.onNodeWithTag("ki-dazubuchen").assertDoesNotExist(); rule.onNodeWithText("KI-Abi-Pass", substring = true).assertDoesNotExist()
        // die KI-Seite bleibt über die Einstellungen erreichbar
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.go(Routes.KI); settle(); assertEquals(Routes.KI, nav.top)
        rule.onNodeWithTag("ki-kaufen").assertTextContains("Zum Shop: KI-Abi-Pass · 14,99 €", substring = true); rule.onNodeWithTag("ki-status").assertTextContains("kein Abo", substring = true).assertTextContains("gilt in der Web-App", substring = true); rule.onNodeWithTag("ki-datenschutz").assertExists()
        // abgelaufener Pass, falscher Code, gültiger Pass (mitten aus der E-Mail)
        rule.onNodeWithTag("ki-code").performScrollTo().performTextInput(KI_ALT); rule.onNodeWithTag("ki-freischalten").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("ki-aktiv").assertTextContains("abgelaufen am 31.07.2026", substring = true)
        rule.onNodeWithTag("ki-code").performScrollTo().performTextClearance(); rule.onNodeWithTag("ki-code").performTextInput("RL-12345"); rule.onNodeWithTag("ki-freischalten").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("ki-grund").assertTextContains("erkenne ich nicht", substring = true); assertEquals(KI_ALT, prefs.kiCode.value)
        rule.onNodeWithTag("ki-code").performScrollTo().performTextClearance(); rule.onNodeWithTag("ki-code").performTextInput("Hallo!\n$KI_PASS\nViel Erfolg!"); rule.onNodeWithTag("ki-freischalten").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("ki-aktiv").assertTextContains("aktiv bis 31.07.2027", substring = true); assertEquals(KI_PASS, prefs.kiCode.value); rule.onNodeWithTag("ki-code").assertDoesNotExist()
        rule.onRoot().captureRoboImage("../screenshots/83_ki_abi_pass.png")
        // zurück in der Aufgabe geht der Code mit jeder Frage mit
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle(); assertEquals(Routes.level("1-2"), nav.top)
        answers += 200 to """{"text":"Mit Pass: noch ein anderer Blick.","rest":9}"""
        rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000); rule.onNodeWithTag("ki").performClick(); settle()
        rule.onNodeWithTag("ki-antwort").assertTextContains("Mit Pass", substring = true); rule.onNodeWithTag("ki-rest").assertTextContains("Heute noch 9 KI-Punkte", substring = true)
        assertTrue(asked.last(), asked.last().contains("\"code\":\"$KI_PASS\""))
        rule.onNodeWithTag("ki-zu").performClick(); settle(); rule.onNodeWithTag("ki-karte").assertDoesNotExist()
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("ki-eintrag").assertTextContains("KI-Abi-Pass aktiv", substring = true).assertTextContains("31.07.2027", substring = true)
        rule.runOnUiThread { prefs.setKiCode("") }
    }

    /** Solange der KI-Tutor nicht für alle freigegeben ist, sieht ihn nur, wer schon einen KI-Code hat; ohne Server-Anbindung (Play-Fassung) gibt es ihn gar nicht. */
    @Test fun kiTutorNurWoFreigegeben() {
        start(ki = { 200 to """{"text":"Hallo","rest":9}""" }, kiOn = false)
        nav.go(Routes.level("1-2")); settle(); rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000)
        rule.onNodeWithTag("ki").assertDoesNotExist()
        rule.runOnUiThread { prefs.setKiCode(KI_PASS) }; settle(); rule.onNodeWithTag("ki").assertExists()
        rule.onNodeWithTag("ki").performClick(); settle(); rule.onNodeWithTag("ki-antwort").assertTextContains("Hallo"); rule.onNodeWithTag("ki-rest").assertTextContains("Heute noch 9 KI-Punkte", substring = true)
        rule.runOnUiThread { prefs.setKiCode("") }
    }

    /** Nachhilfe-Hinweis: nur im Lernstand, nur wenn ein Gebiet hakt, ausblendbar und abschaltbar – nie in einer Aufgabe. Gezählt wird anonym und nur bei eingeschaltetem Schalter. */
    @Test fun nachhilfeHinweisUndZaehlung() {
        val geo = Levels.chapters.filter { it.area == de.raumlicht.core.model.Area.GEOMETRIE }.flatMap { it.levels }.take(8)
        stars.value = geo.associate { it.id to 1 }
        start()
        // in der Aufgabe und auf der Startseite: kein Hinweis
        rule.onNodeWithTag("nachhilfe-hinweis").assertDoesNotExist(); nav.go(Routes.level("1-2")); settle(); rule.onNodeWithTag("nachhilfe-hinweis").assertDoesNotExist()
        // die Aufgabe zählt „gestartet“ und „Theorie geöffnet“ – nur mit dem Gebiet, ohne Aufgabe, Sterne oder Versuche
        rule.onNodeWithTag("theorie").performClick(); settle(); rule.onNodeWithTag("theorie-formel", useUnmergedTree = true).assertExists(); rule.onNodeWithTag("theorie-zu", useUnmergedTree = true).performClick(); settle()
        assertEquals(listOf("""{"app":"raumlicht","v":"test","ev":"level_start","thema":"vektoren"}""", """{"app":"raumlicht","v":"test","ev":"theorie","thema":"vektoren"}"""), played.toList())
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle()
        nav.go(Routes.LEARN); settle()
        rule.onNodeWithTag("nachhilfe-hinweis").assertExists(); rule.onNodeWithText("Bei Vektoren hakt es noch?", substring = true).assertExists()
        rule.onNodeWithText("€", substring = true).assertDoesNotExist()
        rule.onRoot().captureRoboImage("../screenshots/84_nachhilfe_hinweis.png")
        assertEquals(listOf("""{"app":"raumlicht","v":"test","ev":"nachhilfe_hinweis","ort":"lernstand","thema":"vektoren"}"""), counted.toList())
        // noch einmal öffnen: am selben Tag sichtbar, aber nicht noch einmal gezählt
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle(); nav.go(Routes.LEARN); settle()
        rule.onNodeWithTag("nachhilfe-hinweis").assertExists(); assertEquals(1, counted.size)
        rule.onNodeWithTag("nachhilfe-probestunde").performClick(); settle(); assertTrue(counted.last().contains("\"ev\":\"probestunde_klick\""))
        // ausblenden: weg, auch beim nächsten Öffnen
        rule.onNodeWithTag("nachhilfe-aus").performClick(); settle(); rule.onNodeWithTag("nachhilfe-hinweis").assertDoesNotExist()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle(); nav.go(Routes.LEARN); settle(); rule.onNodeWithTag("nachhilfe-hinweis").assertDoesNotExist()
        // nichts Gezähltes enthält Kennungen, Gerätedaten oder Lernstände
        for (b in counted) { assertTrue(b, Regex("""^\{"app":"raumlicht","v":"test","ev":"[a-z_]+"(,"ort":"[a-z]+")?(,"thema":"[a-z]+")?\}$""").matches(b)) }
        // Schalter aus: kein Hinweis, kein Zählen
        rule.runOnUiThread { prefs.tutorState = ""; prefs.setTutorHints(false) }
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle(); nav.go(Routes.LEARN); settle(); rule.onNodeWithTag("nachhilfe-hinweis").assertDoesNotExist()
        rule.runOnUiThread { prefs.setTutorHints(true); prefs.setCountUsage(false) }; val before = counted.size
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.back(); settle(); nav.go(Routes.LEARN); settle()
        rule.onNodeWithTag("nachhilfe-hinweis").assertExists(); assertEquals("Zählung aus: nichts wird gesendet", before, counted.size)
        rule.runOnUiThread { prefs.setCountUsage(true); prefs.tutorState = "" }
    }

    /** „Klausur bald“ im Klausur-Planer: erst mit bekannter Adresse, sachlich, ausblendbar, gezählt. */
    @Test fun klausurHinweis() {
        start(); rule.runOnUiThread { prefs.tutorState = ""; prefs.setPlan(de.raumlicht.core.model.Plan(20_772L + 7, setOf(1, 2)).encode()) }
        nav.go(Routes.PLANNER); settle()
        rule.onNodeWithTag("klausur-hinweis").performScrollTo().assertExists(); rule.onNodeWithText("In 7 Tagen ist deine Klausur.", substring = true).assertExists()
        rule.onRoot().captureRoboImage("../screenshots/86_klausur_hinweis.png")
        assertEquals(listOf("""{"app":"raumlicht","v":"test","ev":"nachhilfe_hinweis","ort":"klassenarbeit","thema":"vektoren"}"""), counted.toList())
        rule.onNodeWithTag("klausur-probestunde").assertTextContains("Kostenlose Probestunde anfragen").performClick(); settle(); assertTrue(counted.last(), counted.last().contains("\"ev\":\"probestunde_klick\",\"ort\":\"klassenarbeit\""))
        rule.onNodeWithTag("klausur-aus").performClick(); settle(); rule.onNodeWithTag("klausur-hinweis").assertDoesNotExist()
        rule.runOnUiThread { prefs.setPlan(""); prefs.tutorState = "" }
    }

    /** Ohne Adresse erscheint im Klausur-Planer kein Hinweis. */
    @Test fun klausurHinweisErstMitAdresse() {
        crash = ""; start(); rule.runOnUiThread { prefs.tutorState = ""; prefs.setPlan(de.raumlicht.core.model.Plan(20_772L + 7, setOf(1, 2)).encode()) }
        nav.go(Routes.PLANNER); settle(); rule.onNodeWithTag("klausur-hinweis").assertDoesNotExist(); assertTrue(counted.isEmpty())
        rule.runOnUiThread { prefs.setPlan("") }
    }

    /** Eltern-Handy koppeln: erklären, koppeln (QR-Code, Link), Bericht geht raus, nach dem Lernen frühestens nach 10 Minuten wieder, aufheben löscht alles. */
    @Test fun elternHandyKoppeln() {
        eltern = fakeEltern(); start(); rule.runOnUiThread { prefs.setParentLink(""); prefs.parentSent = 0; prefs.learnLog = "" }
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("eltern-eintrag").assertTextContains("Eltern-Handy koppeln").performClick(); settle(); assertEquals(Routes.PARENTS, nav.top)
        rule.onNodeWithTag("eltern-inhalt").assertExists(); rule.onNodeWithTag("eltern-hinweis").assertTextContains("verschlüsselt", substring = true); rule.onNodeWithTag("eltern-qr").assertDoesNotExist()
        assertTrue("vor dem Koppeln geht nichts raus", uploads.isEmpty())
        rule.onNodeWithTag("eltern-name").performTextInput("Lena"); rule.onNodeWithTag("eltern-koppeln").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("eltern-qr").performScrollTo().assertExists(); rule.onNodeWithTag("eltern-status").performScrollTo().assertTextContains("Bericht gesendet", substring = true)
        rule.onRoot().captureRoboImage("../screenshots/87_eltern_koppeln.png")
        assertEquals(1, uploads.size); assertEquals("https://server.test/b/" + "A".repeat(22), uploads[0].first); assertEquals("B".repeat(22), uploads[0].second)
        assertTrue(uploads[0].third, uploads[0].third.contains("\"name\":\"Lena\"") && uploads[0].third.contains("\"app\":\"raumlicht\"") && !uploads[0].third.contains("C".repeat(43)))
        assertTrue(prefs.parentLink.value.endsWith(".Lena"))
        // Lernen: eine gelöste Aufgabe landet im Protokoll; der nächste Bericht geht erst nach 10 Minuten
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.restart(Routes.level("1-1")); settle()
        rule.runOnUiThread { prefs.learnLog = de.raumlicht.core.model.Eltern.add(prefs.learnLog, 20_772L, tasks = 1, right = 1) }
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.restart(Routes.HOME); settle(); assertEquals("innerhalb von 10 Minuten kein zweiter Bericht", 1, uploads.size)
        rule.runOnUiThread { prefs.parentSent = prefs.parentSent - 11 * 60 * 1000L }
        nav.go(Routes.BACKSTAGE); settle(); assertEquals(2, uploads.size); assertTrue(uploads[1].third.contains("\"aufgaben\":1"))
        // aufheben: Bericht auf dem Server löschen, Werte lokal weg
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.restart(Routes.PARENTS); settle()
        rule.onNodeWithTag("eltern-aufheben").performScrollTo().performClick(); settle(); rule.onNodeWithTag("eltern-aufheben-ja").performScrollTo().performClick(); settle()
        assertEquals(listOf("https://server.test/b/" + "A".repeat(22)), deleted.toList()); assertEquals("", prefs.parentLink.value); rule.onNodeWithTag("eltern-koppeln").assertExists()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); nav.restart(Routes.HOME); settle(); assertEquals("nach dem Aufheben geht nichts mehr raus", 2, uploads.size)
    }

    /** Sicherung und „Fortschritt wiederherstellen“: geht mit dem Bericht raus, nur bei Änderung; ein vorhandener Lernstand wird nie ohne Nachfrage ersetzt. */
    @Test fun sicherungUndWiederherstellen() {
        val E = de.raumlicht.core.model.Eltern; val id = "A".repeat(22); val write = "B".repeat(22); val key = "C".repeat(43)
        var state = mapOf("sterne" to "1-1=3", "proCode" to "RL"); val applied = ArrayList<Pair<Map<String, String>, String>>(); var answer = 404 to ""; val asked = ArrayList<String>()
        eltern = de.raumlicht.app.ui.settings.ElternPlatform("https://server.test/b/", "https://seite.test/eltern-bericht/", { "$id.$write.$key" },
            { url, w, _, json, done -> uploads += Triple(url, w, json); done(204) }, { url, _, done -> deleted += url; done(204) },
            load = { url, _, done -> asked += url; done(answer.first, answer.second) }, snapshot = { state }, apply = { d, p -> applied += d to p })
        start(); rule.runOnUiThread { prefs.setParentLink(""); prefs.parentSent = 0; prefs.learnLog = ""; prefs.learnMonths = ""; prefs.backupMark = "" }
        nav.go(Routes.PARENTS); settle(); rule.onNodeWithTag("eltern-sicherung-hinweis").assertTextContains("verschlüsselt", substring = true)
        // Wiederherstellen: kein Link → verständliche Meldung, nichts wird abgefragt
        rule.onNodeWithTag("eltern-link").performScrollTo().performTextInput("irgendwas"); rule.onNodeWithTag("eltern-wiederherstellen").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("eltern-wiederherstellen-status").performScrollTo().assertTextContains("kein Kopplungs-Link", substring = true); assertTrue(asked.isEmpty())
        rule.onNodeWithTag("eltern-link").performTextClearance(); rule.onNodeWithTag("eltern-link").performTextInput("https://seite.test/eltern-bericht/#$id.$key")
        rule.onNodeWithTag("eltern-wiederherstellen").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("eltern-wiederherstellen-status").performScrollTo().assertTextContains("Keine Sicherung gefunden", substring = true); assertEquals(listOf("https://server.test/s/$id"), asked.toList())
        answer = 200 to """{"format":1,"app":"wissensgarten","schreib":"$write","daten":{}}"""; rule.onNodeWithTag("eltern-wiederherstellen").performClick(); settle()
        rule.onNodeWithTag("eltern-wiederherstellen-status").assertTextContains("anderen App", substring = true)
        // Sicherung gefunden, hier ist noch nichts gelernt → gleich einspielen, samt Kopplung mit Schreibrecht
        val saved = E.backup(de.raumlicht.core.model.Eltern.Pairing.fresh("$id.$write.$key", "Lena")!!, "2.18.27", 1_795_016_400_000L, mapOf("sterne" to "2-1=3", "kiCode" to "KI"))!!
        answer = 200 to saved; rule.onNodeWithTag("eltern-wiederherstellen").performClick(); settle()
        assertEquals(1, applied.size); assertEquals(mapOf("sterne" to "2-1=3", "kiCode" to "KI"), applied[0].first); assertEquals("$id.$write.$key.Lena", applied[0].second)
        // Hier ist schon gelernt → erst fragen; „Abbrechen“ ändert nichts, „Ja, ersetzen“ spielt ein
        rule.runOnUiThread { prefs.learnLog = E.add("", 20_772L, tasks = 1, right = 1) }
        rule.onNodeWithTag("eltern-wiederherstellen").performClick(); settle(); assertEquals(1, applied.size)
        rule.onNodeWithTag("eltern-ersetzen-frage").performScrollTo().assertTextContains("schon einen Lernstand", substring = true); rule.onRoot().captureRoboImage("../screenshots/89_fortschritt_wiederherstellen.png")
        rule.onNodeWithTag("eltern-ersetzen-nein").performScrollTo().performClick(); settle(); assertEquals(1, applied.size); rule.onNodeWithTag("eltern-ersetzen-frage").assertDoesNotExist()
        rule.onNodeWithTag("eltern-wiederherstellen").performScrollTo().performClick(); settle(); rule.onNodeWithTag("eltern-ersetzen-ja").performScrollTo().performClick(); settle(); assertEquals(2, applied.size)
        // Koppeln: Bericht (mit Monatsverlauf) und Sicherung gehen zusammen raus
        rule.onNodeWithTag("eltern-koppeln").performScrollTo().performClick(); settle()
        assertEquals(listOf("https://server.test/b/$id", "https://server.test/s/$id"), uploads.map { it.first }); assertTrue(uploads[0].third, uploads[0].third.contains("\"verlauf\":["))
        assertTrue(uploads[1].third, uploads[1].third.contains("\"schreib\":\"$write\"") && uploads[1].third.contains("\"proCode\":\"RL\"") && !uploads[1].third.contains(key)); assertEquals(write, uploads[1].second)
        rule.onNodeWithTag("eltern-sicherung-status").performScrollTo().assertTextContains("Lernstand gesichert", substring = true)
        // ohne Änderung keine neue Sicherung, mit Änderung schon
        rule.onNodeWithTag("eltern-senden").performScrollTo().performClick(); settle(); assertEquals(3, uploads.size); assertTrue(uploads[2].first.contains("/b/"))
        state = state + ("sterne" to "1-1=3;1-2=3"); rule.onNodeWithTag("eltern-senden").performClick(); settle(); assertEquals(5, uploads.size); assertTrue(uploads[4].first.contains("/s/"))
        // aufheben: der Merker der Sicherung ist weg
        rule.onNodeWithTag("eltern-aufheben").performScrollTo().performClick(); settle(); rule.onNodeWithTag("eltern-aufheben-ja").performScrollTo().performClick(); settle(); assertEquals("", prefs.backupMark)
    }

    /** „← Alle Apps“ oben auf der Startseite: sichtbar, groß genug zum Tippen, ohne Preis – und nur dort, nicht in Aufgaben. */
    @Test fun alleAppsObenAufDerStartseite() {
        start(); settle()
        rule.onNodeWithTag("alle-apps").assertExists().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(44f)).assertWidthIsAtLeast(androidx.compose.ui.unit.Dp(44f)); rule.onNodeWithText("← Alle Apps").assertExists()
        for (bad in listOf("€", "Pro", "kaufen")) assertFalse(bad, de.raumlicht.core.model.Nachhilfe.APPS_LABEL.contains(bad))
        rule.onRoot().captureRoboImage("../screenshots/91_alle_apps.png")
        rule.onNodeWithTag("alle-apps").performClick(); settle(); assertEquals(1, appsOpened)
        nav.go(Routes.level("1-1")); settle(); rule.onNodeWithTag("alle-apps").assertDoesNotExist()
        nav.restart(Routes.BACKSTAGE); settle(); rule.onNodeWithTag("alle-apps").assertDoesNotExist()
    }

    /** Eltern-Tor vor dem Shop: Malaufgabe in Worten, eigene Ziffern-Tastatur, nach einem Fehler eine neue Aufgabe, zehn Minuten offen, jederzeit abbrechen. */
    @Test fun elternTorVorDemShop() {
        val zahl = mapOf("dreizehn" to 13, "vierzehn" to 14, "fünfzehn" to 15, "sechzehn" to 16, "siebzehn" to 17, "achtzehn" to 18, "neunzehn" to 19, "sechs" to 6, "sieben" to 7, "acht" to 8, "neun" to 9)
        fun aufgabe(): String = rule.onNodeWithTag("eltern-tor-aufgabe").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        fun loesung(text: String): Int { val m = Regex("Wie viel ist (\\S+) mal (\\S+)\\?").find(text)!!; return zahl.getValue(m.groupValues[1]) * zahl.getValue(m.groupValues[2]) }
        fun tippe(ziffern: String) { ziffern.forEach { rule.onNodeWithTag("eltern-tor-taste-$it").performClick() }; rule.onNodeWithTag("eltern-tor-taste-OK").performClick(); settle() }
        start(pro = true); nav.go(Routes.PRO); settle()
        // Preis und sachliche Info bleiben sichtbar, der Weg zum Shop führt übers Tor
        rule.onNodeWithTag("pro-kaufen").performScrollTo().assertTextContains("7,99 €", substring = true); rule.onNodeWithTag("pro-status").assertTextContains("Wer unter 18 ist, kauft nicht selbst", substring = true)
        rule.onNodeWithTag("eltern-tor").assertDoesNotExist(); rule.onNodeWithTag("pro-kaufen").performClick(); settle()
        rule.onNodeWithTag("eltern-tor").assertExists(); rule.onNodeWithTag("eltern-tor-hinweis").assertTextContains("Wer unter 18 ist, kauft nicht selbst", substring = true)
        val erste = aufgabe(); assertTrue(erste, Regex("Wie viel ist (dreizehn|vierzehn|fünfzehn|sechzehn|siebzehn|achtzehn|neunzehn) mal (sechs|sieben|acht|neun)\\?").matches(erste)); assertFalse("keine Ziffern in der Aufgabe", erste.any { it.isDigit() })
        rule.onRoot().captureRoboImage("../screenshots/94_eltern_tor.png")
        // falsche Antwort: sofort eine neue Aufgabe, nichts geht auf
        tippe("1"); rule.onNodeWithTag("eltern-tor-falsch").assertExists(); assertNotEquals(erste, aufgabe()); assertTrue(counted.none { it.contains("pro_kasse") || it.contains("elternbereich") })
        // löschen und richtig lösen: Tor auf, gezählt, Weg zum Shop frei
        rule.onNodeWithTag("eltern-tor-taste-9").performClick(); rule.onNodeWithTag("eltern-tor-taste-⌫").performClick(); rule.onNodeWithTag("eltern-tor-eingabe").assertTextEquals("–")
        tippe(loesung(aufgabe()).toString()); rule.onNodeWithTag("eltern-tor").assertDoesNotExist()
        assertEquals(1, counted.count { it.contains("\"ev\":\"elternbereich\"") }); assertEquals(1, counted.count { it.contains("\"ev\":\"pro_kasse\"") })
        // innerhalb von zehn Minuten bleibt das Tor offen
        rule.onNodeWithTag("pro-kaufen").performScrollTo().performClick(); settle(); rule.onNodeWithTag("eltern-tor").assertDoesNotExist(); assertEquals(2, counted.count { it.contains("pro_kasse") })
        // danach wieder zu – und „Abbrechen“ führt nirgendwohin
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMinutes(11))
        rule.onNodeWithTag("pro-kaufen").performClick(); settle(); rule.onNodeWithTag("eltern-tor").assertExists()
        rule.onNodeWithTag("eltern-tor-abbrechen").performClick(); settle(); rule.onNodeWithTag("eltern-tor").assertDoesNotExist(); assertEquals(2, counted.count { it.contains("pro_kasse") }); assertEquals(1, counted.count { it.contains("elternbereich") })
    }

    /** „Analysis & Zufall erleben“: von der Startseite erreichbar, alle Erlebnis-Bereiche samt Rotationskörper-Labor, dazu die Verweise zum Üben im Mathe-Trainer. */
    @Test fun analysisUndZufallErleben() {
        start(); settle(); rule.onNodeWithTag("erleben").performClick(); settle(); assertEquals(Routes.EXPERIENCE, nav.top)
        for (t in listOf("Kurve abfahren (Fahrersicht)", "Achterbahn-Baumeister", "Geisterfahrt gegen f′", "Rotationskörper-Labor", "Zufallsmaschine", "Galton-Brett", "Binomial-Labor")) {
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(t)); rule.onNodeWithText(t).assertExists() }
        rule.onNodeWithText("Flug durch den Raum").assertDoesNotExist()
        rule.onRoot().captureRoboImage("../screenshots/92_analysis_zufall_erleben.png")
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("trainer-links")); rule.onNodeWithTag("trainer-kurve").assertExists(); rule.onNodeWithTag("trainer-s-verteilung").assertExists()
        rule.onNodeWithText("Die Rechenaufgaben zu Analysis und Stochastik sind jetzt im Mathe-Trainer.", substring = true).assertExists()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Rotationskörper-Labor")); rule.onNodeWithText("Rotationskörper-Labor").performClick(); settle(); assertEquals(Routes.ROTATION, nav.top)
        // gestrichene Aufgaben führen nirgendwohin: zurück zur Startseite statt auf einen leeren Bildschirm
        nav.restart(Routes.level("14-2")); settle(); assertEquals(listOf(Routes.HOME), nav.stack)
    }

    /** Rotationskörper-Labor: Vorlage wählen, eigene Zahlen einstellen, Volumen mit ganzem Rechenweg lesen – und eine verständliche Rückmeldung, wenn die Eingabe nicht passt. */
    @Test fun rotationskoerperLabor() {
        start(); nav.go(Routes.ROTATION); settle()
        rule.onNodeWithTag("rotation-satz").assertTextContains("Zylinder mit Radius 2 und Höhe 4", substring = true); rule.onNodeWithTag("rotation-volumen").performScrollTo(); rule.onNodeWithText("16 · π").assertExists()
        rule.onNodeWithTag("rotation-weg-0", useUnmergedTree = true).assertTextContains("V = π · ∫ f(x)² dx von 0 bis 4", substring = true); rule.onNodeWithTag("rotation-weg-5", useUnmergedTree = true).assertTextContains("V = 16 · π ≈ 50,27", substring = true)
        rule.onNodeWithTag("rotation-probe", useUnmergedTree = true).assertTextContains("16π", substring = true)
        rule.onRoot().captureRoboImage("../screenshots/93_rotationskoerper_labor.png")
        // eigene Werte: Kugel mit Radius 3, dann Grenzen verschieben
        rule.onNodeWithTag("rotation-vorlage-Kugel").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("rotation-volumen").performScrollTo(); rule.onNodeWithText("36 · π").assertExists(); rule.onNodeWithTag("rotation-satz").assertTextContains("Kugel mit Radius 3", substring = true)
        rule.onNodeWithTag("plus-rotation-a").performScrollTo().performClick(); rule.onNodeWithTag("plus-rotation-a").performClick(); rule.onNodeWithTag("plus-rotation-a").performClick(); settle()
        rule.onNodeWithTag("rotation-satz").assertTextContains("Halbkugel mit Radius 3", substring = true); rule.onNodeWithTag("rotation-volumen").performScrollTo(); rule.onNodeWithText("18 · π").assertExists()
        // unpassend: linke Grenze über die rechte hinaus → Rückmeldung statt falschem Körper, kein Volumen
        repeat(3) { rule.onNodeWithTag("plus-rotation-a").performScrollTo().performClick() }; settle()
        rule.onNodeWithTag("rotation-satz").assertTextContains("kleiner sein als die rechte", substring = true); rule.onNodeWithTag("rotation-volumen").assertDoesNotExist()
    }

    /** „Kommt bald“ auf der Seite „Mehr“: sichtbar, ohne Datum, ohne Kaufhinweis – und Antippen führt nirgendwohin. */
    @Test fun kommtBald() {
        start(); nav.go(Routes.BACKSTAGE); settle()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Körper im Raum")); settle(); rule.onNodeWithText("KOMMT BALD").assertExists()
        rule.onNodeWithText("Schattenwurf").assertExists().performClick(); settle(); assertEquals(Routes.BACKSTAGE, nav.top); rule.onRoot().captureRoboImage("../screenshots/88_kommt_bald.png")
    }

    @Test fun fremderCodeSchaltetNichtsFrei() {
        Settings(ApplicationProvider.getApplicationContext()).setProCode(PASS.replace("MGKKT", "MGKKV"))
        start(pro = true)
        nav.go(Routes.level("7-3")); settle(); rule.onNodeWithTag("pro-grund").assertExists()
        rule.runOnUiThread { prefs.setProCode("") }
    }

    /** Die Schreibweise lässt sich in den Einstellungen umstellen und wird gemerkt; die Formelsammlung folgt ihr */
    @Test fun schreibweiseUmstellen() {
        Settings(ApplicationProvider.getApplicationContext()).setStrictVectors(false)
        prefs = Settings(ApplicationProvider.getApplicationContext())
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) {
            AppRoot(nav, prefs, stars, { _, _, _ -> }, { 20_772L }, settings = { onBack -> de.raumlicht.app.ui.settings.SettingsPage(voiceOffered = true, sound = true, onSound = {}, vibration = true, onVibration = {}, versionText = "Test", onBack = onBack) })
        } } } }
        rule.mainClock.advanceTimeBy(600)
        nav.go(Routes.SHEET); settle(); rule.onNodeWithText("Verbindungsvektor: AB⃗ = B − A (Spitze minus Fuß)").assertExists(); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.SETTINGS); settle(); rule.onNode(hasAnyAncestor(hasTestTag("schreibweise")) and isToggleable()).performScrollTo().performClick(); settle()
        assertTrue(prefs.strictVectors.value); assertTrue(Settings(ApplicationProvider.getApplicationContext()).strictVectors.value)
        // Klang: Musik und Geräusche haben eigene Lautstärken, die gemerkt werden
        rule.onNodeWithTag("ton-laut-text").performScrollTo().assertTextEquals("Geräusche: 100 %")
        rule.runOnUiThread { prefs.setSoundLevel(40); prefs.setMusicLevel(65) }; settle(); rule.onNodeWithTag("ton-laut-text").assertTextEquals("Geräusche: 40 %")
        Settings(ApplicationProvider.getApplicationContext()).let { assertEquals(40, it.soundLevel.value); assertEquals(65, it.musicLevel.value) }
        rule.runOnUiThread { prefs.setSoundLevel(100); prefs.setMusicLevel(100) }
        // Prüfungsmodus (Musik aus): nur Prüfungen auf Zeit und der Pflichtteil
        assertTrue(Routes.isExam("abi/zelt/1")); assertTrue(Routes.isExam(Routes.QUICK)); assertFalse(Routes.isExam("abi/zelt/0")); assertFalse(Routes.isExam(Routes.EXAMS)); assertFalse(Routes.isExam("level/1-1"))
        // Vorlesen: Stimme ist als KI-Stimme gekennzeichnet, Hörprobe vorhanden
        rule.onNodeWithTag("stimme-name").performScrollTo().assertTextEquals("Stimme: Nova – KI-generierte Stimme"); rule.onNodeWithTag("hoerprobe").assertExists()
        // Sprechtempo in sechs Stufen, gemerkt; krumme Werte rasten auf die nächste Stufe
        rule.onNodeWithTag("sprechtempo-text").performScrollTo().assertTextEquals("🐢🐇 Sprechtempo: 1,0×")
        rule.runOnUiThread { prefs.setSpeechRate(118) }; settle(); rule.onNodeWithTag("sprechtempo-text").assertTextEquals("🐢🐇 Sprechtempo: 1,15×")
        assertEquals(115, Settings(ApplicationProvider.getApplicationContext()).speechRate.value)
        rule.runOnUiThread { prefs.setSpeechRate(75) }; settle(); rule.onNodeWithTag("sprechtempo-text").assertTextEquals("🐢🐇 Sprechtempo: 0,75×"); rule.runOnUiThread { prefs.setSpeechRate(100) }
        rule.onNodeWithText("An: mit Ortsvektoren", substring = true).assertExists(); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.SHEET); settle(); rule.onNodeWithText("Verbindungsvektor: AB⃗ = OB⃗ − OA⃗ (Spitze minus Fuß)").assertExists()
        rule.runOnUiThread { prefs.setStrictVectors(false) }
    }

    @Test fun proOffMeansEverythingOpen() {
        start()                                                                                                             // Auslieferungsstand
        nav.go(Routes.level("26-7")); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist()
        rule.onNodeWithText(Levels.byId("26-7")!!.title, substring = true).assertExists()
        // kein Eintrag in den Einstellungen, kein Wort von einem Kauf
        nav.back(); settle(); org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("einst").assertExists(); rule.onNodeWithTag("pro-eintrag").assertDoesNotExist()
    }
}
