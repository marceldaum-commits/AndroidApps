package de.raumlicht.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalVibration
import de.raumlicht.app.ui.home.HomeScreen
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.settings.SettingsScreen
import de.raumlicht.app.ui.settings.UpdateState
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Levels
import de.raumlicht.core.model.Badges
import de.raumlicht.core.model.Chapter
import de.raumlicht.core.model.Daily
import de.raumlicht.core.model.Gen
import de.raumlicht.core.model.JamTopic
import de.raumlicht.core.model.Sheet
import de.raumlicht.app.ui.home.*
import de.raumlicht.core.model.*
import de.raumlicht.app.ui.home.JamPicker
import de.raumlicht.app.ui.home.SheetScreen
import androidx.compose.runtime.saveable.rememberSaveable
import de.raumlicht.core.model.MusicMood

object Routes {
    const val HOME = "start"
    const val LEVEL = "level/{id}"
    const val SETTINGS = "einstellungen"
    const val DAILY = "tagesaufgabe"
    const val JAM_HOME = "jam"
    const val JAM = "jam/{topic}"
    const val SHEET = "spickzettel"
    const val BADGES = "abzeichen"
    const val BACKSTAGE = "backstage"
    const val EXAMS = "abi"
    const val EXAM = "abi/{id}/{zeit}"
    const val LAGE = "lage"
    const val DUEL = "duell"
    const val TRAINING = "zweite-chance"
    const val WORKSHOP = "werkstatt"
    const val BUILDER = "baukasten"
    const val GUIDES = "schritte"
    const val GUIDE = "schritte/{id}"
    const val LEARN = "lernstand"
    const val PLANNER = "klausur"
    const val CLASS = "klasse"
    const val MATCH = "zu-zweit"
    const val RIDE = "achterbahn/{id}"
    const val FLIGHT = "raumflug"
    const val GALTON = "galton"
    const val BALLOONS = "ballerbude"
    const val CHANCE = "zufall"
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
}

/** Nur zurück, wenn der Bildschirm noch aktiv ist – verhindert, dass ein Doppeltipp zwei Ebenen schließt. */
fun NavController.popSafe(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true && previousBackStackEntry != null && popBackStack()

fun NavController.goOnce(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
    navigate(route) { launchSingleTop = true }
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    /** Wunsch-Ziel von außen (Widget): wird einmal angesteuert und dann gelöscht */
    private val openRoute = mutableStateOf<String?>(null)

    private fun readIntent(intent: android.content.Intent?) {
        if (intent?.getStringExtra(de.raumlicht.app.widget.DailyWidget.EXTRA_OPEN) == de.raumlicht.app.widget.DailyWidget.OPEN_DAILY) openRoute.value = Routes.DAILY
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); readIntent(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as RaumlichtApp
        app.music.prepare(MusicMood.MENU, MusicMood.FLIGHT)
        if (savedInstanceState == null) readIntent(intent)
        setContent {
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            val music by vm.settings.music.collectAsStateWithLifecycle()
            LaunchedEffect(sound) { app.sfx.enabled = sound }
            LaunchedEffect(music) { app.music.setEnabled(music) }
            val volume by vm.settings.volume.collectAsStateWithLifecycle()
            LaunchedEffect(volume) { app.sfx.master = volume / 100f; app.music.setVolume(volume / 100f); app.voice.volume = volume / 100f }
            val voice by vm.settings.voice.collectAsStateWithLifecycle()
            LaunchedEffect(voice) { app.voice.enabled = voice; if (!voice) app.voice.stop() }
            RaumlichtTheme {
                CompositionLocalProvider(LocalSfx provides app.sfx, LocalVibration provides vibration, de.raumlicht.app.ui.fx.LocalSpeaker provides app.voice) {
                    Surface(Modifier.fillMaxSize(), color = Q.Space) { Nav() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as RaumlichtApp).music.onForeground(true)
        de.raumlicht.app.widget.DailyWidget.refresh(this)   // z. B. nach Mitternacht: neuer Tagesaufgabe
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }
    override fun onStop() { (application as RaumlichtApp).music.onForeground(false); (application as RaumlichtApp).voice.stop(); super.onStop() }

    @Composable
    private fun Nav() {
        val nav = rememberNavController()
        // Musik passend zum Bildschirm: ruhig im Tourplan, treibend auf der Bühne
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        LaunchedEffect(route) { (application as RaumlichtApp).music.setMood(if (route in setOf(Routes.LEVEL, Routes.JAM, Routes.DAILY, Routes.EXAM, Routes.TRAINING, Routes.DUEL, Routes.LAGE, Routes.GUIDE, Routes.CLASS, Routes.MATCH, Routes.PLANNER, Routes.RIDE, Routes.FLIGHT, Routes.GALTON, Routes.QUICK)) MusicMood.FLIGHT else MusicMood.MENU) }
        val skin by vm.settings.skin.collectAsStateWithLifecycle()
        val exams by vm.settings.exams.collectAsStateWithLifecycle()
        val lageBest by vm.settings.lageBest.collectAsStateWithLifecycle()
        val planText by vm.settings.plan.collectAsStateWithLifecycle()
        val look by vm.settings.look.collectAsStateWithLifecycle()
        val diaryText by vm.settings.diary.collectAsStateWithLifecycle()
        val diary = remember(diaryText) { DiaryEntry.parse(diaryText) }
        val plan = remember(planText) { Plan.decode(planText) }
        // In den ersten noch offenen Aufgabe eines Kapitels springen
        fun openChapter(n: Int) { Levels.chapters.firstOrNull { it.number == n }?.let { c -> nav.goOnce(Routes.level((c.levels.firstOrNull { (vm.stars.value[it.id] ?: 0) == 0 } ?: c.levels.first()).id)) } }
        val stars by vm.stars.collectAsStateWithLifecycle()
        val update by vm.availableUpdate.collectAsStateWithLifecycle()
        val wanted by openRoute
        LaunchedEffect(wanted) { wanted?.let { r -> nav.navigate(r) { popUpTo(Routes.HOME); launchSingleTop = true }; openRoute.value = null } }
        val jamBest by vm.settings.jamBest.collectAsStateWithLifecycle()
        val badges = remember(stars, jamBest) { Badges.all(stars, jamBest) }
        val tourStars = remember(stars) { Levels.all.sumOf { stars[it.id] ?: 0 } }

        CompositionLocalProvider(de.raumlicht.app.ui.stage.LocalLuxSkin provides skin, de.raumlicht.app.ui.stage.LocalLuxLook provides look,
            de.raumlicht.app.ui.level.LocalMistakeSink provides { id, msg -> vm.settings.setDiary(DiaryEntry.add(vm.settings.diary.value, id, msg)) }) {
        val rideFor = remember { mapOf("14-1" to "schuessel", "14-2" to "huegel", "14-3" to "bergtal", "14-4" to "doppel") }
        NavHost(nav, Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(Levels.chapters, stars, { nav.goOnce(Routes.level(it)) }, { nav.goOnce(Routes.SETTINGS) },
                    updateVersion = update?.version, onUpdate = { nav.goOnce(Routes.SETTINGS) },
                    dailyDone = (stars[Daily.id(vm.today())] ?: 0) > 0, jamBest = jamBest,
                    onDaily = { nav.goOnce(Routes.DAILY) }, onJam = { nav.goOnce(Routes.JAM_HOME) }, onExam = { nav.goOnce(Routes.EXAMS) }, onMore = { nav.goOnce(Routes.BACKSTAGE) })
            }
            composable(Routes.LEVEL) { e ->
                val level = Levels.byId(e.arguments?.getString("id").orEmpty())
                if (level == null) { LaunchedEffect(Unit) { nav.popSafe() }; return@composable }
                val chapter = Levels.chapters.first { it.number == level.chapter }
                val next = Levels.next(level.id)
                LevelScreen(level, chapter,
                    showIntro = remember(level.id) { level.id.endsWith("-1") && !vm.settings.introSeen(level.chapter) },
                    onIntroSeen = { vm.settings.markIntroSeen(level.chapter) },
                    onSolved = { s, a -> vm.record(level.id, s, a) },
                    onNext = next?.let { n -> { nav.navigate(Routes.level(n.id)) { popUpTo(Routes.HOME); launchSingleTop = true } } },
                    onBack = { nav.popSafe() },
                    // Im Achterbahn-Kapitel: dieselbe Kurve aus der Fahrersicht abfahren
                    extra = rideFor[level.id]?.let { id -> "🎢 Mitfahren" to { nav.goOnce(Routes.ride(id)) } })
            }
            composable(Routes.DAILY) {
                val day = remember { vm.today() }
                val level = remember(day) { Daily.forDay(day) }
                LevelScreen(level, Chapter(0, "Tagesaufgabe", level.title, "Tagesaufgabe", "🎟️", "orange", emptyList(), emptyList()), false, {},
                    onSolved = { s, a -> vm.record(level.id, s, a) }, onNext = null, onBack = { nav.popSafe() },
                    caption = "🎟️ Tagesaufgabe · ${level.title}", doneLabel = "Fertig für heute ✓")
            }
            composable(Routes.JAM_HOME) { JamPicker(jamBest, { nav.goOnce(Routes.jam(it.name)) }, { nav.popSafe() }) }
            composable(Routes.JAM) { e ->
                val topic = JamTopic.entries.firstOrNull { it.name == e.arguments?.getString("topic") } ?: JamTopic.MIX
                val session = rememberSaveable { System.currentTimeMillis() % 1_000_000 }
                var n by rememberSaveable { mutableIntStateOf(0) }
                var streak by rememberSaveable { mutableIntStateOf(0) }
                val level = remember(session, n) { Gen.jam(topic, session, n) }
                LevelScreen(level, Chapter(0, topic.title, topic.title, "Jam-Session", topic.emoji, topic.color, emptyList(), emptyList()), false, {},
                    // Serie: nur Treffer beim ersten Versuch zählen
                    onSolved = { s, _ -> streak = if (s == 3) streak + 1 else 0; vm.settings.recordJam(streak) },
                    onNext = { n++ }, onBack = { nav.popSafe() },
                    caption = "${topic.emoji} Üben · ${level.title} · Nr. ${n + 1}", badge = if (streak > 0) "🔥 $streak" else null)
            }
            composable(Routes.SHEET) {
                SheetScreen(Sheet.cards,
                    // direkt in den ersten noch offenen Aufgabe des Kapitels
                    onChapter = ::openChapter,
                    onJam = { nav.goOnce(Routes.jam(it.name)) }, onBack = { nav.popSafe() })
            }
            composable(Routes.BACKSTAGE) {
                val weak = remember(stars) { Training.levels(stars).size }
                BackstageScreen(listOf(
                    BackstageEntry("🎢", "Kurve abfahren (Fahrersicht)", "Analysis erleben: Höhe, Steigung, Krümmung, Ableitungsbahn und Fläche live", Q.Cyan, "Erleben") { nav.goOnce(Routes.ride(Coaster.rides.first().id)) },
                    BackstageEntry("🚀", "Flug durch den Raum", "Geometrie erleben: eine Gerade entlangfliegen, durch Ebenen und Kugeln", Q.Cyan, "Erleben") { nav.goOnce(Routes.FLIGHT) },
                    BackstageEntry("🎈", "Ballerbude", "Ballons antippen, Lux ballert sie ab – zum Spaß, mit Aufgabe oder zu zweit", Q.Pink, "Erleben") { nav.goOnce(Routes.BALLOONS) },
                    BackstageEntry("🏗️", "Achterbahn-Baumeister", "Bahn selbst bauen, Probefahrt mit echter Physik – reicht der Schwung?", Q.Cyan, "Erleben") { nav.goOnce(Routes.BUILD) },
                    BackstageEntry("👻", "Geisterfahrt gegen f′", "Zeichne die Ableitung mit dem Finger – wie nah kommst du?", Q.Pink, "Erleben") { nav.goOnce(Routes.GHOST) },
                    BackstageEntry("⭕", "Ringflug", "Stell den Richtungsvektor ein und flieg durch drei Ringe", Q.Green, "Erleben") { nav.goOnce(Routes.RINGS) },
                    BackstageEntry("🎲", "Zufallsmaschine", "Münze, Würfel oder eigene Chance: erst wetten, dann tausendmal werfen", Q.Pink, "Erleben") { nav.goOnce(Routes.CHANCE) },
                    BackstageEntry("🎰", "Galton-Brett", "Stochastik erleben: Kugeln fallen, die Binomialverteilung wächst", Q.Pink, "Erleben") { nav.goOnce(Routes.GALTON) },
                    BackstageEntry("🧩", "Rechenweg in Schritten", "Durchstoßpunkt, Spiegelpunkt, Schnittpunkt, Abstand – Schritt für Schritt", Q.Green, "Üben") { nav.goOnce(Routes.GUIDES) },
                    BackstageEntry("✍️", "Ohne Hilfsmittel", "6 kurze Aufgaben wie im Pflichtteil – ohne Tipp", Q.Yellow, "Üben") { nav.goOnce(Routes.QUICK) },
                    BackstageEntry("🧭", "Lage von Geraden erkennen", "parallel, identisch, schneidend, windschief? · Rekord $lageBest/${LageCheck.ROUNDS}", Q.Yellow, "Üben") { nav.goOnce(Routes.LAGE) },
                    BackstageEntry("🔁", "Fehler wiederholen", if (weak == 0) "nichts offen" else "$weak Aufgaben, die nicht beim ersten Versuch saßen", Q.Orange, "Üben") { nav.goOnce(Routes.TRAINING) },
                    BackstageEntry("📔", "Fehler-Tagebuch", if (diary.isEmpty()) "deine typischen Fehler – noch leer" else "${diary.size} typische Fehler notiert", Q.Orange, "Üben") { nav.goOnce(Routes.DIARY) },
                    BackstageEntry("📊", "Lernstand", Learn.summary(Learn.topics(stars)), Q.Cyan, "Planen") { nav.goOnce(Routes.LEARN) },
                    BackstageEntry("🗓️", "Klausur-Planer", plan?.let { Planner.headline(it, vm.today(), stars) } ?: "Termin eintragen, Tagespakete bekommen", Q.Orange, "Planen") { nav.goOnce(Routes.PLANNER) },
                    BackstageEntry("🎟️", "Kalender der Tagesaufgaben", "${Calendar.solved(stars).size} gelöst – verpasste Tage kosten nichts", Q.Orange, "Planen") { nav.goOnce(Routes.CALENDAR) },
                    BackstageEntry("⚔️", "Duell per Code", "5 Aufgaben an Freunde schicken – wer trifft öfter?", Q.Pink, "Gemeinsam") { nav.goOnce(Routes.DUEL) },
                    BackstageEntry("👥", "Zu zweit an einem Gerät", "abwechselnd spielen – wer rechnet besser?", Q.Cyan, "Gemeinsam") { nav.goOnce(Routes.MATCH) },
                    BackstageEntry("🏫", "Klassen-Paket (für Lehrkräfte)", "Aufgaben auswählen und als Code an die Klasse geben", Q.Green, "Gemeinsam") { nav.goOnce(Routes.CLASS) },
                    BackstageEntry("🧮", "3D-Rechner & Baukasten", "Schulbuch-Aufgabe eingeben und nachrechnen – oder Lichtshow im Takt bauen", Q.Violet, "Werkzeuge") { nav.goOnce(Routes.BUILDER) },
                    BackstageEntry("📓", "Formelsammlung", "alle Formeln – mit Sprung zu den passenden Aufgaben", Q.Green, "Werkzeuge") { nav.goOnce(Routes.SHEET) },
                    BackstageEntry("🎨", "Lux gestalten", "${Skins.unlocked(tourStars).size} von ${Skins.all.size} Farben frei · Leuchtspur und Aufsatz", Q.Cyan, "Für dich") { nav.goOnce(Routes.WORKSHOP) },
                    BackstageEntry("🏅", "Erfolge", "${badges.count { it.earned }} von ${badges.size} verdient", Q.Yellow, "Für dich") { nav.goOnce(Routes.BADGES) },
                )) { nav.popSafe() }
            }
            composable(Routes.EXAMS) { ExamPicker(Exams.all, exams, { e, timed -> nav.goOnce(Routes.exam(e.id, timed)) }) { nav.popSafe() } }
            composable(Routes.EXAM) { e ->
                val exam = Exams.byId(e.arguments?.getString("id").orEmpty())
                if (exam == null) { LaunchedEffect(Unit) { nav.popSafe() }; return@composable }
                ExamPlay(exam, onFinished = { vm.settings.recordExam(exam.id, it) }, onBack = { nav.popSafe() }, timed = e.arguments?.getString("zeit") == "1")
            }
            composable(Routes.GUIDES) { ExamPicker(Guides.all, emptyMap(), { e, _ -> nav.goOnce(Routes.guide(e.id)) }, guided = true) { nav.popSafe() } }
            composable(Routes.GUIDE) { e ->
                val guide = Guides.byId(e.arguments?.getString("id").orEmpty())
                if (guide == null) { LaunchedEffect(Unit) { nav.popSafe() }; return@composable }
                ExamPlay(guide, onFinished = {}, onBack = { nav.popSafe() }, guided = true)
            }
            composable(Routes.LEARN) { LearnScreen(remember(stars) { Learn.topics(stars) }, ::openChapter) { nav.popSafe() } }
            composable(Routes.PLANNER) {
                PlannerScreen(plan, vm.today(), stars, { vm.settings.setPlan(it?.encode() ?: ""); de.raumlicht.app.widget.DailyWidget.refresh(this@MainActivity) }, { l, s, a -> vm.record(l.id, s, a) }) { nav.popSafe() }
            }
            composable(Routes.CLASS) { ClassScreen({ l, s, a -> vm.record(l.id, s, a) }) { nav.popSafe() } }
            composable(Routes.MATCH) { MatchScreen({ nav.popSafe() }) }
            composable(Routes.RIDE) { e -> RideScreen({ nav.popSafe() }, e.arguments?.getString("id") ?: Coaster.rides.first().id, custom = vm.settings.ride, onCustom = { vm.settings.ride = it }) }
            composable(Routes.LAGE) { LageScreen(lageBest, vm.settings::recordLage, { nav.popSafe() }) }
            composable(Routes.DUEL) { DuelScreen({ nav.popSafe() }) }
            composable(Routes.TRAINING) {
                val list = remember { Training.levels(vm.stars.value) }
                TrainingPlay(list, { l, s, a -> vm.record(l.id, s, a) }) { nav.popSafe() }
            }
            composable(Routes.WORKSHOP) {
                WorkshopScreen(tourStars, skin, vm.settings::setSkin, badges.count { it.earned }, look, { t, h -> vm.settings.setLook(t, h) }) { nav.popSafe() }
            }
            composable(Routes.FLIGHT) { FlightScreen({ nav.popSafe() }, custom = vm.settings.flight, onCustom = { vm.settings.flight = it }) }
            composable(Routes.GALTON) { GaltonScreen({ nav.popSafe() }) }
            composable(Routes.BUILD) { de.raumlicht.app.ui.home.CoasterBuildScreen({ nav.popSafe() }) }
            composable(Routes.GHOST) { de.raumlicht.app.ui.home.GhostScreen({ nav.popSafe() }) }
            composable(Routes.RINGS) { de.raumlicht.app.ui.home.RingScreen({ nav.popSafe() }) }
            composable(Routes.CHANCE) { de.raumlicht.app.ui.home.ChanceScreen({ nav.popSafe() }) }
            composable(Routes.BALLOONS) { val best by vm.settings.balloonBest.collectAsStateWithLifecycle(); de.raumlicht.app.ui.home.BalloonScreen(best, { vm.settings.recordBalloons(it) }, { nav.popSafe() }) }
            composable(Routes.QUICK) { QuickScreen({ l, s, a -> vm.record(l.id, s, a) }, { nav.popSafe() }) }
            composable(Routes.DIARY) { DiaryScreen(diary, { nav.goOnce(Routes.level(it)) }, { vm.settings.setDiary("") }) { nav.popSafe() } }
            composable(Routes.CALENDAR) { CalendarScreen(vm.today(), remember(stars) { Calendar.solved(stars) }, { nav.goOnce(Routes.DAILY) }) { nav.popSafe() } }
            composable(Routes.BUILDER) {
                BuilderScreen(remember { Show.decode(vm.settings.show) ?: Show.demo }, { vm.settings.show = Show.encode(it) }, { nav.popSafe() })
            }
            composable(Routes.BADGES) { BadgesScreen(badges) { nav.popSafe() } }
            composable(Routes.SETTINGS) {
                val sound by vm.settings.sound.collectAsStateWithLifecycle()
                val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
                val music by vm.settings.music.collectAsStateWithLifecycle()
                val auto by vm.settings.autoUpdate.collectAsStateWithLifecycle()
                SettingsScreen(music, vm.settings::setMusic, sound, vm.settings::setSound, vibration, vm.settings::setVibration, auto, vm.settings::setAutoUpdate,
                    BuildConfig.VERSION_NAME, update?.let { UpdateState.Available(it) } ?: UpdateState.Idle, { nav.popSafe() }, onReset = vm::resetAll, voice = vm.settings.voice.collectAsStateWithLifecycle().value, onVoice = vm.settings::setVoice,
                    voiceControls = { val v = (application as RaumlichtApp).voice; de.raumlicht.app.ui.settings.VoiceControls(v.position, vm.settings.voiceBright,
                        onNext = { v.nextVoice()?.let { vm.settings.voiceName = it } }, onBright = { vm.settings.voiceBright = it; v.setBright(it) }) }, voiceAvailable = (application as RaumlichtApp).voice.let { !it.checked || it.available }, volume = vm.settings.volume.collectAsStateWithLifecycle().value, onVolume = vm.settings::setVolume)
            }
        }
        }
    }
}
