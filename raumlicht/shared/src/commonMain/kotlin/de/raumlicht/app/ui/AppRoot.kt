package de.raumlicht.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import de.raumlicht.app.ui.fx.nowMillis
import de.raumlicht.app.ui.home.*
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.level.LocalMistakeSink
import de.raumlicht.app.ui.stage.LocalLuxLook
import de.raumlicht.app.ui.stage.LocalLuxSkin
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.*
import kotlinx.coroutines.flow.StateFlow

/** Alle Ziele in der App. Ein Ziel ist ein Text wie „level/3-2“ – so lässt es sich merken, verlinken und von außen ansteuern (Widget). */
object Routes {
    const val HOME = "start"
    const val SETTINGS = "einstellungen"
    const val PRO = "abi-pass"
    const val DAILY = "tagesaufgabe"
    const val JAM_HOME = "jam"
    const val SHEET = "spickzettel"
    const val BADGES = "abzeichen"
    const val BACKSTAGE = "backstage"
    const val EXAMS = "abi"
    const val LAGE = "lage"
    const val DUEL = "duell"
    const val TRAINING = "zweite-chance"
    const val WORKSHOP = "werkstatt"
    const val BUILDER = "baukasten"
    const val GUIDES = "schritte"
    const val LEARN = "lernstand"
    const val PLANNER = "klausur"
    const val CLASS = "klasse"
    const val MATCH = "zu-zweit"
    const val FLIGHT = "raumflug"
    const val GALTON = "galton"
    const val BALLOONS = "ballerbude"
    const val CHANCE = "zufall"
    const val BINOMIAL = "binomial"
    const val BUILD = "baumeister"
    const val GHOST = "geisterfahrt"
    const val RINGS = "ringflug"
    const val QUICK = "pflichtteil"
    const val DIARY = "fehler-tagebuch"
    const val CALENDAR = "kalender"
    fun ride(id: String) = "achterbahn/$id"
    fun guide(id: String) = "schritte/$id"
    fun exam(id: String, timed: Boolean) = "abi/$id/${if (timed) 1 else 0}"
    fun jam(topic: String) = "jam/$topic"
    fun level(id: String) = "level/$id"
    /** Ziele, in denen gespielt oder gerechnet wird – dort läuft die treibendere Musik */
    fun isAction(route: String) = route.substringBefore('/') in setOf("level", "tagesaufgabe", "abi", "zweite-chance", "duell", "lage", "schritte", "klasse", "zu-zweit", "klausur", "achterbahn", "raumflug", "galton", "pflichtteil") &&
        route !in setOf(EXAMS, GUIDES) || route.startsWith("jam/")
}

/**
 * Der Weg durch die App: ein Stapel von Zielen. Oben liegt der sichtbare Bildschirm, „zurück“ nimmt ihn herunter.
 * Doppeltipper werden abgefangen (zweimal schnell „zurück“ schließt nur eine Ebene, zweimal dasselbe Ziel öffnet es nur einmal).
 */
class Navigator(start: List<String> = listOf(Routes.HOME)) {
    var stack by mutableStateOf(start)
        private set
    val top get() = stack.last()
    val canGoBack get() = stack.size > 1
    private var lastPop = 0L
    fun go(route: String) { if (top != route) stack = stack + route }
    fun back(): Boolean { val now = nowMillis(); if (!canGoBack || now - lastPop < 250) return false; lastPop = now; stack = stack.dropLast(1); return true }
    /** Alles bis zur Startseite schließen und dann [route] öffnen (z. B. „nächste Aufgabe“ oder Sprung vom Widget) */
    fun restart(route: String) { stack = if (route == Routes.HOME) listOf(Routes.HOME) else listOf(Routes.HOME, route) }
}

/**
 * Die ganze App als ein Baustein – derselbe für Android und für die WebApp.
 * [prefs] und die Sterne ([stars], [record]) kommen von der Plattform; ebenso die Einstellungsseite ([settings]), weil sie plattformeigene Abschnitte hat.
 *
 * @param updateVersion neue Version, auf die die Startseite hinweisen soll (nur wo die App sich selbst aktualisiert)
 * @param onRoute meldet jedes neue Ziel (Android stellt danach die Musik ein)
 * @param onPlanChanged der Klausur-Plan wurde geändert (Android frischt das Widget auf)
 */
@Composable
fun AppRoot(
    nav: Navigator, prefs: Prefs, stars: StateFlow<Map<String, Int>>, record: (levelId: String, stars: Int, attempts: Int) -> Unit, today: () -> Long,
    settings: @Composable (onBack: () -> Unit) -> Unit,
    updateVersion: String? = null, onRoute: (String) -> Unit = {}, onPlanChanged: () -> Unit = {},
    /** Abi-Pass erhältlich und Pro-Teile gesperrt? Erst wenn die Kasse eingetragen ist ([Pro.ENABLED]); die Play-Fassung schaltet es aus, Tests schalten es ein. */
    proEnabled: Boolean = Pro.ENABLED,
    /** Öffentlicher Schlüssel zum Prüfen der Freischalt-Codes (Tests nehmen einen eigenen) */
    proKey: String = Pro.PUBLIC_KEY,
) {
    val proCode by prefs.proCode.collectAsState()
    val proActive = remember(proCode, proKey) { Pro.active(proCode, proKey) }
    // Was zu Pro gehört und (noch) nicht freigeschaltet ist
    fun needsPro(route: String) = proEnabled && !proActive && Pro.locked(route)
    // Code prüfen und merken – auch wenn er mitten in einer kopierten E-Mail steht
    val enterCode: (String) -> Boolean = { text -> val code = de.raumlicht.core.model.License.extract(text) ?: text.trim()
        Pro.active(code, proKey).also { if (it) prefs.setProCode(code) } }
    val starMap by stars.collectAsState()
    val skin by prefs.skin.collectAsState(); val look by prefs.look.collectAsState()
    val exams by prefs.exams.collectAsState(); val lageBest by prefs.lageBest.collectAsState(); val jamBest by prefs.jamBest.collectAsState()
    val planText by prefs.plan.collectAsState(); val diaryText by prefs.diary.collectAsState()
    val diary = remember(diaryText) { DiaryEntry.parse(diaryText) }
    val plan = remember(planText) { Plan.decode(planText) }
    val badges = remember(starMap, jamBest) { Badges.all(starMap, jamBest) }
    val tourStars = remember(starMap) { Levels.all.sumOf { starMap[it.id] ?: 0 } }
    val rideFor = remember { mapOf("14-1" to "schuessel", "14-2" to "huegel", "14-3" to "bergtal", "14-4" to "doppel") }
    val back: () -> Unit = { nav.back() }
    // In die erste noch offene Aufgabe eines Kapitels springen
    fun openChapter(n: Int) { Levels.chapters.firstOrNull { it.number == n }?.let { c -> nav.go(Routes.level((c.levels.firstOrNull { (stars.value[it.id] ?: 0) == 0 } ?: c.levels.first()).id)) } }
    LaunchedEffect(nav.top) { onRoute(nav.top) }

    // Was ein Bildschirm sich merkt (rememberSaveable), bleibt erhalten, solange er im Stapel liegt
    val holder = rememberSaveableStateHolder()
    var known by remember { mutableStateOf(emptySet<String>()) }
    val keys = nav.stack.mapIndexed { i, r -> "$i:$r" }
    LaunchedEffect(keys) { (known - keys.toSet()).forEach { holder.removeState(it) }; known = keys.toSet() }

    val strictVectors by prefs.strictVectors.collectAsState()
    CompositionLocalProvider(LocalLuxSkin provides skin, LocalLuxLook provides look,
        de.raumlicht.app.ui.level.LocalOpenSheet provides { nav.go(Routes.SHEET) },
        de.raumlicht.app.ui.settings.LocalNotation provides de.raumlicht.app.ui.settings.NotationChoice(strictVectors, prefs::setStrictVectors),
        de.raumlicht.app.ui.settings.LocalProEntry provides (if (proEnabled) ({ nav.go(Routes.PRO) }) else null), de.raumlicht.app.ui.settings.LocalProActive provides proActive,
        LocalMistakeSink provides { id, msg -> prefs.setDiary(DiaryEntry.add(prefs.diary.value, id, msg)) }) {
        Crossfade(keys.last(), animationSpec = tween(180), label = "ziel") { key -> holder.SaveableStateProvider(key) {
            val route = key.substringAfter(':'); val parts = route.split('/')
            if (needsPro(route)) de.raumlicht.app.ui.settings.ProScreen(Pro.what(route), false, true, enterCode, back) else
            when (parts[0]) {
                Routes.HOME -> HomeScreen(Levels.chapters, starMap, { nav.go(Routes.level(it)) }, { nav.go(Routes.SETTINGS) }, locked = { needsPro(Routes.level(it)) },
                    updateVersion = updateVersion, onUpdate = { nav.go(Routes.SETTINGS) },
                    dailyDone = (starMap[Daily.id(today())] ?: 0) > 0, jamBest = jamBest,
                    onDaily = { nav.go(Routes.DAILY) }, onJam = { nav.go(Routes.JAM_HOME) }, onExam = { nav.go(Routes.EXAMS) }, onMore = { nav.go(Routes.BACKSTAGE) })
                "level" -> {
                    val level = Levels.byId(parts.getOrElse(1) { "" })
                    if (level == null) LaunchedEffect(Unit) { nav.back() } else {
                        val chapter = Levels.chapters.first { it.number == level.chapter }
                        val next = Levels.next(level.id)
                        LevelScreen(level, chapter,
                            showIntro = remember(level.id) { level.id.endsWith("-1") && !prefs.introSeen(level.chapter) },
                            onIntroSeen = { prefs.markIntroSeen(level.chapter) },
                            onSolved = { s, a -> record(level.id, s, a) },
                            onNext = next?.let { n -> { nav.restart(Routes.level(n.id)) } },
                            onBack = back,
                            // Im Achterbahn-Kapitel: dieselbe Kurve aus der Fahrersicht abfahren
                            extra = rideFor[level.id]?.let { id -> "🎢 Mitfahren" to { nav.go(Routes.ride(id)) } })
                    }
                }
                Routes.DAILY -> {
                    val day = remember { today() }
                    val level = remember(day) { Daily.forDay(day) }
                    LevelScreen(level, Chapter(0, "Tagesaufgabe", level.title, "Tagesaufgabe", "🎟️", "orange", emptyList(), emptyList()), false, {},
                        onSolved = { s, a -> record(level.id, s, a) }, onNext = null, onBack = back,
                        caption = "🎟️ Tagesaufgabe · ${level.title}", doneLabel = "Fertig für heute ✓")
                }
                Routes.JAM_HOME -> if (parts.size == 1) JamPicker(jamBest, { nav.go(Routes.jam(it.name)) }, back, startLevel = prefs.jamLevel, onLevel = { prefs.jamLevel = it }, lockedLevel = { needsPro(Routes.jam("MIX") + (it?.let { l -> "/${l.name}" } ?: "")) },
                    onPick = { t, l -> nav.go(Routes.jam(t.name) + (l?.let { "/${it.name}" } ?: "")) }) else {
                    val topic = JamTopic.entries.firstOrNull { it.name == parts[1] } ?: JamTopic.MIX
                    val session = rememberSaveable { nowMillis() % 1_000_000 }
                    var n by rememberSaveable { mutableIntStateOf(0) }
                    var streak by rememberSaveable { mutableIntStateOf(0) }
                    val stufe = JamLevel.entries.firstOrNull { it.name == parts.getOrNull(2) }
                    val level = remember(session, n) { Practice.level(topic, stufe, session, n) }
                    LevelScreen(level, Chapter(0, topic.title, topic.title, "Endlos üben", topic.emoji, topic.color, emptyList(), emptyList()), false, {},
                        // Serie: nur Treffer beim ersten Versuch zählen
                        onSolved = { s, _ -> streak = if (s == 3) streak + 1 else 0; prefs.recordJam(streak) },
                        onNext = { n++ }, onBack = back,
                        caption = "${topic.emoji} Üben${stufe?.let { " ${it.emoji} ${it.title}" } ?: ""} · ${level.title} · Nr. ${n + 1}", badge = if (streak > 0) "🔥 $streak" else null)
                }
                Routes.SHEET -> SheetScreen(Sheet.cards, onChapter = ::openChapter, onJam = { nav.go(Routes.jam(it.name)) }, onBack = back)
                Routes.BACKSTAGE -> {
                    val weak = remember(starMap) { Training.levels(starMap).size }
                    BackstageScreen(listOf(
                        BackstageEntry("🎢", "Kurve abfahren (Fahrersicht)", "Analysis erleben: Höhe, Steigung, Krümmung, Ableitungsbahn und Fläche live", Q.Cyan, "Erleben") { nav.go(Routes.ride(Coaster.rides.first().id)) },
                        BackstageEntry("🚀", "Flug durch den Raum", "Geometrie erleben: eine Gerade entlangfliegen, durch Ebenen und Kugeln", Q.Cyan, "Erleben") { nav.go(Routes.FLIGHT) },
                        BackstageEntry("🎈", "Ballerbude", "Ballons antippen, Lux ballert sie ab – zum Spaß, mit Aufgabe oder zu zweit", Q.Pink, "Erleben") { nav.go(Routes.BALLOONS) },
                        BackstageEntry("🏗️", "Achterbahn-Baumeister", "Bahn selbst bauen, Probefahrt mit echter Physik – reicht der Schwung?", Q.Cyan, "Erleben") { nav.go(Routes.BUILD) },
                        BackstageEntry("👻", "Geisterfahrt gegen f′", "Zeichne die Ableitung mit dem Finger – wie nah kommst du?", Q.Pink, "Erleben") { nav.go(Routes.GHOST) },
                        BackstageEntry("⭕", "Ringflug", "Stell den Richtungsvektor ein und flieg durch drei Ringe", Q.Green, "Erleben") { nav.go(Routes.RINGS) },
                        BackstageEntry("🎲", "Zufallsmaschine", "Münze, Würfel oder eigene Chance: erst wetten, dann tausendmal werfen", Q.Pink, "Erleben") { nav.go(Routes.CHANCE) },
                        BackstageEntry("🎰", "Galton-Brett", "Stochastik erleben: Kugeln fallen, die Binomialverteilung wächst", Q.Pink, "Erleben") { nav.go(Routes.GALTON) },
                        BackstageEntry("📊", "Binomial-Labor", "n, p und k frei einstellen: genau, höchstens, mindestens – einzeln und aufsummiert", Q.Pink, "Erleben") { nav.go(Routes.BINOMIAL) },
                        BackstageEntry("🧩", "Rechenweg in Schritten", "Durchstoßpunkt, Spiegelpunkt, Schnittpunkt, Abstand – Schritt für Schritt", Q.Green, "Üben") { nav.go(Routes.GUIDES) },
                        BackstageEntry("✍️", "Ohne Hilfsmittel", "6 kurze Aufgaben wie im Pflichtteil – ohne Tipp", Q.Yellow, "Üben") { nav.go(Routes.QUICK) },
                        BackstageEntry("🧭", "Lage von Geraden erkennen", "parallel, identisch, schneidend, windschief? · Rekord $lageBest/${LageCheck.ROUNDS}", Q.Yellow, "Üben") { nav.go(Routes.LAGE) },
                        BackstageEntry("🔁", "Fehler wiederholen", if (weak == 0) "nichts offen" else if (weak == 1) "1 Aufgabe, die nicht beim ersten Versuch saß" else "$weak Aufgaben, die nicht beim ersten Versuch saßen", Q.Orange, "Üben") { nav.go(Routes.TRAINING) },
                        BackstageEntry("📔", "Fehler-Tagebuch", if (diary.isEmpty()) "deine typischen Fehler – noch leer" else if (diary.size == 1) "1 typischer Fehler notiert" else "${diary.size} typische Fehler notiert", Q.Orange, "Üben") { nav.go(Routes.DIARY) },
                        BackstageEntry("📊", "Lernstand", Learn.summary(Learn.topics(starMap)), Q.Cyan, "Planen") { nav.go(Routes.LEARN) },
                        BackstageEntry("🗓️", "Klausur-Planer", plan?.let { Planner.headline(it, today(), starMap) } ?: "Termin eintragen, Tagespakete bekommen", Q.Orange, "Planen") { nav.go(Routes.PLANNER) },
                        BackstageEntry("🎟️", "Kalender der Tagesaufgaben", "${Calendar.solved(starMap).size} gelöst – verpasste Tage kosten nichts", Q.Orange, "Planen") { nav.go(Routes.CALENDAR) },
                        BackstageEntry("⚔️", "Duell per Code", "5 Aufgaben an Freunde schicken – wer trifft öfter?", Q.Pink, "Gemeinsam") { nav.go(Routes.DUEL) },
                        BackstageEntry("👥", "Zu zweit an einem Gerät", "abwechselnd spielen – wer rechnet besser?", Q.Cyan, "Gemeinsam") { nav.go(Routes.MATCH) },
                        BackstageEntry("🏫", "Klassen-Paket (für Lehrkräfte)", "Aufgaben auswählen und als Code an die Klasse geben", Q.Green, "Gemeinsam") { nav.go(Routes.CLASS) },
                        BackstageEntry("🧮", "3D-Rechner & Baukasten", "Schulbuch-Aufgabe eingeben und nachrechnen – oder Lichtshow im Takt bauen", Q.Violet, "Werkzeuge") { nav.go(Routes.BUILDER) },
                        BackstageEntry("📓", "Formelsammlung", "alle Formeln – mit Sprung zu den passenden Aufgaben", Q.Green, "Werkzeuge") { nav.go(Routes.SHEET) },
                        BackstageEntry("🎨", "Lux gestalten", "${Skins.unlocked(tourStars).size} von ${Skins.all.size} Farben frei · Leuchtspur und Aufsatz", Q.Cyan, "Für dich") { nav.go(Routes.WORKSHOP) },
                        BackstageEntry("🏅", "Erfolge", "${badges.count { it.earned }} von ${badges.size} verdient", Q.Yellow, "Für dich") { nav.go(Routes.BADGES) },
                    ), back)
                }
                Routes.EXAMS -> if (parts.size == 1) ExamPicker(Exams.all, exams, { e, timed -> nav.go(Routes.exam(e.id, timed)) }, onBack = back) else {
                    val exam = Exams.byId(parts[1])
                    if (exam == null) LaunchedEffect(Unit) { nav.back() }
                    else ExamPlay(exam, onFinished = { prefs.recordExam(exam.id, it) }, onBack = back, timed = parts.getOrNull(2) == "1")
                }
                Routes.GUIDES -> if (parts.size == 1) ExamPicker(Guides.all, emptyMap(), { e, _ -> nav.go(Routes.guide(e.id)) }, guided = true, onBack = back) else {
                    val guide = Guides.byId(parts[1])
                    if (guide == null) LaunchedEffect(Unit) { nav.back() } else ExamPlay(guide, onFinished = {}, onBack = back, guided = true)
                }
                Routes.LEARN -> LearnScreen(remember(starMap) { Learn.topics(starMap) }, ::openChapter, back)
                Routes.PLANNER -> PlannerScreen(plan, today(), starMap, { prefs.setPlan(it?.encode() ?: ""); onPlanChanged() }, { l, s, a -> record(l.id, s, a) }, back)
                Routes.CLASS -> ClassScreen({ l, s, a -> record(l.id, s, a) }, back)
                Routes.MATCH -> MatchScreen(back)
                "achterbahn" -> RideScreen(back, parts.getOrNull(1) ?: Coaster.rides.first().id, custom = prefs.ride, onCustom = { prefs.ride = it })
                Routes.LAGE -> LageScreen(lageBest, prefs::recordLage, back)
                Routes.DUEL -> DuelScreen(back)
                Routes.TRAINING -> { val list = remember { Training.levels(stars.value) }; TrainingPlay(list, { l, s, a -> record(l.id, s, a) }, onBack = back) }
                Routes.WORKSHOP -> WorkshopScreen(tourStars, skin, prefs::setSkin, badges.count { it.earned }, look, { t, h -> prefs.setLook(t, h) }, back)
                Routes.FLIGHT -> FlightScreen(back, custom = prefs.flight, onCustom = { prefs.flight = it })
                Routes.GALTON -> GaltonScreen(back)
                Routes.BUILD -> CoasterBuildScreen(back)
                Routes.GHOST -> GhostScreen(back)
                Routes.RINGS -> RingScreen(back)
                Routes.CHANCE -> ChanceScreen(back)
                Routes.BINOMIAL -> BinomialScreen(back)
                Routes.BALLOONS -> { val best by prefs.balloonBest.collectAsState(); BalloonScreen(best, { prefs.recordBalloons(it) }, back) }
                Routes.QUICK -> QuickScreen({ l, s, a -> record(l.id, s, a) }, back)
                Routes.DIARY -> DiaryScreen(diary, { nav.go(Routes.level(it)) }, { prefs.setDiary("") }, back)
                Routes.CALENDAR -> CalendarScreen(today(), remember(starMap) { Calendar.solved(starMap) }, { nav.go(Routes.DAILY) }, back)
                Routes.BUILDER -> BuilderScreen(remember { Show.decode(prefs.show) ?: Show.demo }, { prefs.show = Show.encode(it) }, back)
                Routes.BADGES -> BadgesScreen(badges, back)
                Routes.SETTINGS -> settings(back)
                Routes.PRO -> de.raumlicht.app.ui.settings.ProScreen(null, proActive, proEnabled, enterCode, back)
                else -> LaunchedEffect(Unit) { nav.restart(Routes.HOME) }
            }
        } }
    }
}
