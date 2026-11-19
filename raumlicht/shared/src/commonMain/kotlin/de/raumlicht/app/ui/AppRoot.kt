package de.raumlicht.app.ui

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
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
    const val PRO = "pro"
    const val KI = "ki"
    const val PARENTS = "eltern"
    const val FAMILY = "familie"
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
    /** „Analysis & Zufall erleben“ (seit 2.18.32) und das Rotationskörper-Labor */
    const val EXPERIENCE = "erleben"
    const val ROTATION = "rotation"
    /** Seiten, die auf breiten Bildschirmen als Spalte in der Mitte stehen (Listen, Texte, Einstellungen) – alles mit Bühne oder Spielfeld nutzt die volle Breite */
    val COLUMN_PAGES = setOf(SETTINGS, PRO, KI, PARENTS, FAMILY, JAM_HOME, SHEET, BADGES, BACKSTAGE, EXAMS, GUIDES, LEARN, PLANNER, DIARY, CALENDAR, EXPERIENCE)
    fun ride(id: String) = "achterbahn/$id"
    fun guide(id: String) = "schritte/$id"
    fun exam(id: String, timed: Boolean) = "abi/$id/${if (timed) 1 else 0}"
    fun jam(topic: String) = "jam/$topic"
    fun level(id: String) = "level/$id"
    /** Prüfungsmodus: Prüfungsaufgabe auf Zeit oder Pflichtteil ohne Hilfsmittel – dort läuft keine Musik */
    fun isExam(route: String) = route == QUICK || (route.startsWith("$EXAMS/") && route.endsWith("/1"))
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
    /** Raumlicht Pro erhältlich und Pro-Teile gesperrt? Erst wenn die Kasse eingetragen ist ([Pro.ENABLED]); die Play-Fassung schaltet es aus, Tests schalten es ein. */
    proEnabled: Boolean = Pro.ENABLED,
    /** Öffentlicher Schlüssel zum Prüfen der Freischalt-Codes (Tests nehmen einen eigenen) */
    proKey: String = Pro.PUBLIC_KEY,
    /** Schickt eine Anfrage (JSON) an den KI-Tutor und liefert Status (0 = keine Verbindung) und Antworttext; null = hier gibt es keinen KI-Tutor (Play-Fassung) */
    kiSend: (suspend (String) -> Pair<Int, String>)? = null,
    /** KI-Tutor für alle sichtbar? Sonst nur mit eingetragenem KI-Code (siehe [de.raumlicht.core.model.Ki.ENABLED]) */
    kiEnabled: Boolean = de.raumlicht.core.model.Ki.ENABLED,
    /** Versionsname, geht mit jeder KI-Anfrage mit */
    version: String = "",
    /** Sendet einen Zählaufruf (fertiger Text) an unseren Server; null = diese Plattform zählt nicht (Android pausiert, Tests) */
    countSend: ((String) -> Unit)? = null,
    /** Schalter für Zählung und Nachhilfe-Hinweise samt Geschenk-Eintrag und Fußzeile in den Einstellungen zeigen */
    offers: Boolean = true,
    /** „Eltern-Handy koppeln“: was die Plattform dafür kann (null = hier nicht verfügbar) */
    eltern: de.raumlicht.app.ui.settings.ElternPlatform? = null,
    /** Adresse, auf die der Hinweis „Klausur bald“ führt – die kostenlose Probestunde (leer = Hinweis gibt es nicht) */
    examUrl: String = de.raumlicht.core.model.Nachhilfe.TRIAL_URL,
    /** „← Alle Apps“ oben auf der Startseite: zurück zur App-Auswahl der Website (null = gibt es hier nicht) */
    onApps: (() -> Unit)? = null,
    /** Familien-Speicher: was die Plattform dafür kann (null = hier nicht verfügbar) */
    familie: de.raumlicht.app.ui.settings.FamiliePlatform? = null,
    /** Mit einem Familien-Link („#familie=…“) geöffnet: führt durch das Eltern-Tor auf die Familien-Seite */
    familieLink: String? = null,
    /** Nur für die automatische Prüfung auf dem eigenen Rechner: Das Eltern-Tor steht offen */
    gateFree: Boolean = false,
) {
    // ── Lern-Protokoll (nur auf dem Gerät) und Eltern-Bericht ──
    val parentText by prefs.parentLink.collectAsState()
    val pairing = remember(parentText) { de.raumlicht.core.model.Eltern.Pairing.decode(parentText) }
    var parentStatus by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var restoreStatus by remember { mutableStateOf<String?>(null) }
    var restoreFound by remember { mutableStateOf<de.raumlicht.core.model.Eltern.Restore.Ok?>(null) }
    // Sicherung des ganzen Lernstands: mit demselben Schlüssel verschlüsselt, nur wenn sich etwas geändert hat
    fun sendBackup(p: de.raumlicht.core.model.Eltern.Pairing, e: de.raumlicht.app.ui.settings.ElternPlatform, now: Long) { val data = e.snapshot?.invoke() ?: return; val mark = de.raumlicht.core.model.Eltern.mark(data)
        if (mark == prefs.backupMark) return
        val json = de.raumlicht.core.model.Eltern.backup(p, version, now, data) ?: run { backupStatus = "Die Sicherung ist zu groß geworden und wurde nicht gesendet."; return }
        e.send(de.raumlicht.core.model.Eltern.backupUrl(p.id, e.server), p.write, p.key, json) { status ->
            if (status == 204 || status == 200) { prefs.backupMark = mark; backupStatus = "✓ Lernstand gesichert" } else backupStatus = if (status == 0) null else "Die Sicherung ließ sich gerade nicht senden (Fehler $status)." } }
    // Bericht bauen, verschlüsseln lassen und hochladen – frühestens alle 10 Minuten, außer es wird ausdrücklich verlangt
    fun sendReport(force: Boolean) { val p = de.raumlicht.core.model.Eltern.Pairing.decode(prefs.parentLink.value) ?: return; val e = eltern ?: return
        val now = de.raumlicht.app.ui.fx.nowMillis(); if (!force && now - prefs.parentSent < de.raumlicht.core.model.Eltern.MIN_GAP_MS) return
        prefs.parentSent = now
        val json = de.raumlicht.core.model.Eltern.report(p.name, stars.value, prefs.learnLog, de.raumlicht.core.model.Plan.decode(prefs.plan.value), today(), now, prefs.learnMonths)
        e.send(de.raumlicht.core.model.Eltern.uploadUrl(p, e.server), p.write, p.key, json) { status ->
            parentStatus = when (status) { 204, 200 -> "✓ Bericht gesendet"; 0 -> "Keine Verbindung – der Bericht wird beim nächsten Mal gesendet."; else -> "Der Bericht ließ sich gerade nicht senden (Fehler $status)." }
            if (status != 204 && status != 200) prefs.parentSent = 0 }
        sendBackup(p, e, now) }
    // Lernen festhalten: im Tages-Protokoll (60 Tage) und in den Monatssummen (dauerhaft; beim ersten Mal aus dem Protokoll übernommen)
    fun learn(min: Int = 0, tasks: Int = 0, right: Int = 0) {
        if (prefs.learnMonths.isEmpty()) prefs.learnMonths = de.raumlicht.core.model.Eltern.monthsFromLog(prefs.learnLog)
        prefs.learnLog = de.raumlicht.core.model.Eltern.add(prefs.learnLog, today(), min, tasks, right); prefs.learnMonths = de.raumlicht.core.model.Eltern.addMonth(prefs.learnMonths, today(), min, tasks, right) }
    // Fortschritt wiederherstellen: Sicherung mit dem Kopplungs-Link holen; einen vorhandenen Lernstand nie ohne Nachfrage überschreiben
    fun applyRestore(r: de.raumlicht.core.model.Eltern.Restore.Ok) { restoreFound = null; restoreStatus = "✓ Lernstand wiederhergestellt"; eltern?.apply?.invoke(r.data, r.pairing.encode()) }
    fun restore(link: String) { val e = eltern ?: return; val load = e.load ?: return; restoreFound = null
        val (id, key) = de.raumlicht.core.model.Eltern.fromLink(link) ?: run { restoreStatus = "Das ist kein Kopplungs-Link. Er sieht so aus: …/eltern-bericht/#… – am besten auf dem Eltern-Handy kopieren."; return }
        restoreStatus = "Suche die Sicherung …"
        load(de.raumlicht.core.model.Eltern.backupUrl(id, e.server), key) { status, plain -> when (status) {
            200 -> when (val r = de.raumlicht.core.model.Eltern.restore(plain, id, key)) {
                is de.raumlicht.core.model.Eltern.Restore.Ok -> if (stars.value.isNotEmpty() || prefs.learnLog.isNotEmpty()) { restoreFound = r; restoreStatus = null } else applyRestore(r)
                de.raumlicht.core.model.Eltern.Restore.OtherApp -> restoreStatus = "Dieser Link gehört zu einer anderen App."
                de.raumlicht.core.model.Eltern.Restore.Broken -> restoreStatus = "Die Sicherung ließ sich nicht lesen." }
            404 -> restoreStatus = "Keine Sicherung gefunden."
            0 -> restoreStatus = "Keine Verbindung – bitte später noch einmal versuchen."
            -1 -> restoreStatus = "Der Schlüssel im Link passt nicht zur Sicherung. Bitte den Link vollständig kopieren."
            else -> restoreStatus = "Die Sicherung ließ sich gerade nicht holen (Fehler $status)." } } }
    // Jede gelöste Aufgabe ins Lern-Protokoll; danach ggf. den Bericht auffrischen
    // ── Familien-Speicher (läuft neben der Eltern-Kopplung): Lernstand des Kindes ohne Freischalt-Codes; bei zwei Ständen entscheiden die Eltern ──
    var famText by remember { mutableStateOf(prefs.family) }
    val famState = remember(famText) { de.raumlicht.core.model.Familie.State.decode(famText) }
    var famStatus by remember { mutableStateOf<String?>(null) }
    var famBusy by remember { mutableStateOf(false) }
    var famJoin by remember { mutableStateOf<Triple<String, String, List<de.raumlicht.core.model.Familie.Kid>>?>(null) }   // Familie, Nummer, Kinder
    var famOther by remember { mutableStateOf<Triple<de.raumlicht.core.model.Familie.State, Map<String, String>, de.raumlicht.app.ui.settings.FamilieChoice>?>(null) }
    fun famSet(st: de.raumlicht.core.model.Familie.State?) { prefs.family = st?.encode() ?: ""; famText = prefs.family }
    fun famCall(what: String, a: String = "", b: String = "", c: String = "", d: String = "", done: (String, String, String) -> Unit) { val f = familie ?: return; famBusy = true
        f.call(f.server, what, a, b, c, d) { st, x, y -> famBusy = false; done(st, x, y) } }
    fun famOtherFrom(st: de.raumlicht.core.model.Familie.State, json: String, stand: String, conflict: Boolean): Boolean { val data = de.raumlicht.core.model.Familie.data(json) ?: return false
        famOther = Triple(st, data, de.raumlicht.app.ui.settings.FamilieChoice(de.raumlicht.core.model.Eltern.germanDay(stand), conflict)); return true }
    /** Lernstand in die Familie sichern – nur wenn sich etwas geändert hat (oder ausdrücklich verlangt) */
    fun famSync(force: Boolean, from: de.raumlicht.core.model.Familie.State? = null) { val f = familie ?: return; val st = from ?: de.raumlicht.core.model.Familie.State.decode(prefs.family) ?: return
        if (famBusy || famOther != null && from == null) return
        val data = de.raumlicht.core.model.Familie.child(f.snapshot()); val mark = de.raumlicht.core.model.Eltern.mark(data); if (!force && mark == st.mark) return
        famCall("sichern", st.fam, st.kid, de.raumlicht.core.model.Familie.json(data), st.rev.toString()) { r, x, y -> when (r) {
            "ok" -> { famSet(st.copy(rev = x.toIntOrNull() ?: st.rev, mark = mark)); famStatus = "✓ In der Familie gesichert" }
            "konflikt" -> { val rev = x.toIntOrNull() ?: st.rev; val now = st.copy(rev = rev)
                if (!famOtherFrom(now, y.substringBefore('\n'), y.substringAfter('\n', ""), true)) famStatus = "Auf einem anderen Gerät wurde weitergelernt. Bitte „Stand aus der Familie holen“ antippen." else famStatus = null
                famSet(now.copy(mark = st.mark)) }
            "netz" -> if (force) famStatus = de.raumlicht.core.model.Familie.problem(r)
            else -> famStatus = de.raumlicht.core.model.Familie.problem(r) } } }
    /** Stand des Kindes aus der Familie holen: gibt es dort keinen, wird der dieses Geräts gesichert; ist hier schon gelernt worden, erst fragen */
    fun famFetch(st: de.raumlicht.core.model.Familie.State, ask: Boolean) { val f = familie ?: return
        famCall("laden", st.fam, st.kid) { r, x, y -> when (r) {
            "ok" -> { val now = st.copy(rev = y.substringBefore('|').toIntOrNull() ?: 0); val data = de.raumlicht.core.model.Familie.data(x)
                val mine = de.raumlicht.core.model.Familie.child(f.snapshot())
                if (data == null) famStatus = de.raumlicht.core.model.Familie.problem("schluessel")
                else if (de.raumlicht.core.model.Eltern.mark(data) == de.raumlicht.core.model.Eltern.mark(mine)) { famSet(now.copy(mark = de.raumlicht.core.model.Eltern.mark(mine))); famStatus = "✓ Dieses Gerät ist auf dem Stand der Familie" }
                else if (ask && de.raumlicht.core.model.Familie.hasProgress(mine)) { famSet(now); famOtherFrom(now, x, y.substringAfter('|', ""), false); famStatus = null }
                else { famSet(now.copy(mark = de.raumlicht.core.model.Eltern.mark(data))); famStatus = "✓ Lernstand aus der Familie geholt"; f.apply(data) } }
            "keine" -> { famSet(st.copy(rev = 0, mark = "")); famSync(true, st.copy(rev = 0, mark = "")) }
            else -> famStatus = de.raumlicht.core.model.Familie.problem(r) } } }
    fun famPick(fam: String, number: String, kid: de.raumlicht.core.model.Familie.Kid) { famJoin = null; val st = de.raumlicht.core.model.Familie.State(fam, kid.id, number, name = kid.name); famSet(st); famFetch(st, true) }
    @Suppress("NAME_SHADOWING") val record: (String, Int, Int) -> Unit = { id, s, a -> record(id, s, a); learn(tasks = 1, right = if (s == 3) 1 else 0); sendReport(false); famSync(false) }
    // Lernzeit: jede volle Minute in einer Aufgabe oder Übung zählt
    LaunchedEffect(nav.top) { if (Routes.isAction(nav.top)) while (true) { kotlinx.coroutines.delay(60_000); learn(min = 1) } else { sendReport(false); famSync(false) } }
    val countOn by prefs.countUsage.collectAsState(); val hintsOn by prefs.tutorHints.collectAsState()
    // Zählen: nur mit eingeschaltetem Schalter, nur Ereignisse von der Liste – sonst geschieht nichts
    val count: (String, String?, String?) -> Unit = { ev, place, topic -> if (prefs.countUsage.value) de.raumlicht.core.model.Zaehler.body(ev, place, topic, version)?.let { countSend?.invoke(it) } }
    val kiCode by prefs.kiCode.collectAsState()
    val kiLast = remember(kiCode, proKey) { de.raumlicht.core.model.Ki.info(kiCode, proKey)?.let(de.raumlicht.core.model.Ki::lastDay) }
    val kiShown = kiSend != null && (kiEnabled || kiLast != null)
    val proCode by prefs.proCode.collectAsState()
    val proActive = remember(proCode, proKey) { Pro.active(proCode, proKey) }
    // Was zu Pro gehört und (noch) nicht freigeschaltet ist
    fun needsPro(route: String) = proEnabled && !proActive && Pro.locked(route)
    // Code prüfen und merken – auch wenn er mitten in einer kopierten E-Mail steht
    val enterCode: (String) -> Boolean = { text -> val code = de.raumlicht.core.model.License.extract(text) ?: text.trim()
        // ein KI-Code im Pro-Feld (oder umgekehrt) landet trotzdem an der richtigen Stelle
        val ki = if (kiSend != null) de.raumlicht.core.model.Ki.fromText(text, proKey) else null
        if (ki != null) { prefs.setKiCode(ki); nav.go(Routes.KI); true } else Pro.active(code, proKey).also { if (it) { prefs.setProCode(code); count("code_eingeloest", "eingabe", null) } } }
    val enterKi: (String) -> Boolean = { text -> val ki = de.raumlicht.core.model.Ki.fromText(text, proKey); val pro = de.raumlicht.core.model.License.extract(text) ?: text.trim()
        if (ki != null) { prefs.setKiCode(ki); true } else if (Pro.active(pro, proKey)) { prefs.setProCode(pro); nav.go(Routes.PRO); true } else false }
    val kiTutor = remember(kiShown, kiCode, kiLast) { if (!kiShown) null else de.raumlicht.app.ui.level.KiTutor(kiLast != null && today() <= kiLast,
        { level, values, feedback, kind, question, history ->
            val (status, body) = kiSend!!(de.raumlicht.core.model.Ki.request(level, values, feedback, kind, question, history, kiCode, prefs.kiDevice(), version))
            de.raumlicht.core.model.Ki.answer(status, body) }, { nav.go(Routes.KI) }) }
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
    // Eltern-Tor: steht vor dem Weg zum Shop. Einmal gelöst bleibt es zehn Minuten offen – nur im Arbeitsspeicher, nichts wird gespeichert.
    var gateOpenedAt by remember { mutableStateOf(0L) }
    var gatePending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val gate: (() -> Unit) -> Unit = { action -> if (gateFree || de.raumlicht.core.model.ElternTor.open(gateOpenedAt, de.raumlicht.app.ui.fx.nowMillis())) action() else gatePending = action }
    LaunchedEffect(familieLink) { if (familieLink != null && familie != null) gate { nav.go(Routes.FAMILY) } }
    CompositionLocalProvider(LocalLuxSkin provides skin, LocalLuxLook provides look,
        de.raumlicht.app.ui.settings.LocalMix provides de.raumlicht.app.ui.settings.MixChoice(prefs.musicLevel.collectAsState().value, prefs::setMusicLevel, prefs.soundLevel.collectAsState().value, prefs::setSoundLevel),
        de.raumlicht.app.ui.settings.LocalSpeech provides de.raumlicht.app.ui.settings.SpeechChoice(prefs.speechRate.collectAsState().value, prefs::setSpeechRate),
        de.raumlicht.app.ui.level.LocalOpenSheet provides { nav.go(Routes.SHEET) },
        de.raumlicht.app.ui.settings.LocalNotation provides de.raumlicht.app.ui.settings.NotationChoice(strictVectors, prefs::setStrictVectors),
        de.raumlicht.app.ui.settings.LocalProEntry provides (if (proEnabled || proActive) ({ nav.go(Routes.PRO) }) else null), de.raumlicht.app.ui.settings.LocalProActive provides proActive,
        de.raumlicht.app.ui.level.LocalKi provides kiTutor,
        de.raumlicht.app.ui.settings.LocalKiEntry provides (if (kiShown) de.raumlicht.app.ui.settings.KiEntryState({ nav.go(Routes.KI) }, kiLast, today()) else null),
        de.raumlicht.app.ui.fx.LocalCount provides count,
        de.raumlicht.app.ui.settings.LocalElternTor provides gate,
        de.raumlicht.app.ui.settings.LocalOffers provides (if (offers) de.raumlicht.app.ui.settings.OffersChoice(countOn, prefs::setCountUsage, hintsOn, prefs::setTutorHints) else null),
        de.raumlicht.app.ui.settings.LocalFamilieEntry provides (if (familie != null) de.raumlicht.app.ui.settings.FamilieEntryState({ gate { nav.go(Routes.FAMILY) } }, famState != null) else null),
        de.raumlicht.app.ui.settings.LocalElternEntry provides (if (eltern != null) de.raumlicht.app.ui.settings.ElternEntryState({ nav.go(Routes.PARENTS) }, pairing != null) else null),
        LocalMistakeSink provides { id, msg -> prefs.setDiary(DiaryEntry.add(prefs.diary.value, id, msg)) }) {
        // Erkennungsmelodie: einmal je Sitzung, bei der ersten Berührung (nie vorher) – leise, damit sie niemanden erschreckt
        val jingle = de.raumlicht.app.ui.fx.LocalSfx.current
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope {
            // wirklich eine Berührung (oder ein Mausklick) – nicht schon der Mauszeiger, der nur über die Seite fährt
            while (awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial).type != androidx.compose.ui.input.pointer.PointerEventType.Press) { }
            jingle?.play(de.raumlicht.app.ui.fx.Sound.JINGLE, volume = 0.6f) } }) {
        Crossfade(keys.last(), animationSpec = tween(180), label = "ziel") { key -> holder.SaveableStateProvider(key) {
            val route = key.substringAfter(':'); val parts = route.split('/')
            // „Analysis & Zufall erleben“: dieselben Einträge stehen auf der Seite „Mehr“ und auf der eigenen Seite
            val erlebenEntries = listOf(
            BackstageEntry("🎢", "Kurve abfahren (Fahrersicht)", "Analysis erleben: Höhe, Steigung, Krümmung, Ableitungsbahn und Fläche live", Q.Cyan, "Analysis & Zufall erleben") { nav.go(Routes.ride(Coaster.rides.first().id)) },
            BackstageEntry("🏗️", "Achterbahn-Baumeister", "Bahn selbst bauen, Probefahrt mit echter Physik – reicht der Schwung?", Q.Cyan, "Analysis & Zufall erleben") { nav.go(Routes.BUILD) },
            BackstageEntry("👻", "Geisterfahrt gegen f′", "Zeichne die Ableitung mit dem Finger – wie nah kommst du?", Q.Pink, "Analysis & Zufall erleben") { nav.go(Routes.GHOST) },
            BackstageEntry("🏺", "Rotationskörper-Labor", "Kurve drehen, Körper sehen: Zylinder, Kegel, Kugel, Vase – mit Rechenweg", Q.Violet, "Analysis & Zufall erleben") { nav.go(Routes.ROTATION) },
            BackstageEntry("🎲", "Zufallsmaschine", "Münze, Würfel oder eigene Chance: erst wetten, dann tausendmal werfen", Q.Pink, "Analysis & Zufall erleben") { nav.go(Routes.CHANCE) },
            BackstageEntry("🎰", "Galton-Brett", "Zufall erleben: Kugeln fallen, die Binomialverteilung wächst", Q.Pink, "Analysis & Zufall erleben") { nav.go(Routes.GALTON) },
            BackstageEntry("📊", "Binomial-Labor", "n, p und k frei einstellen: genau, höchstens, mindestens – einzeln und aufsummiert", Q.Pink, "Analysis & Zufall erleben") { nav.go(Routes.BINOMIAL) },
            )
            // Breiter Bildschirm (PC): Seiten zum Lesen und Auswählen nicht über die ganze Breite ziehen, sondern als Spalte in die Mitte setzen
            val column = needsPro(route) || route in Routes.COLUMN_PAGES
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(Q.Space), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                androidx.compose.foundation.layout.Box(if (column) androidx.compose.ui.Modifier.widthIn(max = 860.dp).fillMaxSize() else androidx.compose.ui.Modifier.fillMaxSize()) {
            if (needsPro(route)) de.raumlicht.app.ui.settings.ProScreen(Pro.what(route), false, true, enterCode, back) else
            when (parts[0]) {
                Routes.HOME -> HomeScreen(Levels.chapters, starMap, { nav.go(Routes.level(it)) }, { nav.go(Routes.SETTINGS) }, locked = { needsPro(Routes.level(it)) }, onApps = onApps, onExperience = { nav.go(Routes.EXPERIENCE) },
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
                        BackstageEntry("🚀", "Flug durch den Raum", "Geometrie erleben: eine Gerade entlangfliegen, durch Ebenen und Kugeln", Q.Cyan, "Raum erleben") { nav.go(Routes.FLIGHT) },
                        BackstageEntry("🎈", "Ballerbude", "Ballons antippen, Lux ballert sie ab – zum Spaß, mit Aufgabe oder zu zweit", Q.Pink, "Raum erleben") { nav.go(Routes.BALLOONS) },
                        BackstageEntry("⭕", "Ringflug", "Stell den Richtungsvektor ein und flieg durch drei Ringe", Q.Green, "Raum erleben") { nav.go(Routes.RINGS) },
                    ) + erlebenEntries + listOf(
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
                    ) + de.raumlicht.core.model.Soon.topics.map { t -> BackstageEntry(t.emoji, t.title, t.sub, Q.Muted, "Kommt bald") { } }, onBack = back)
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
                Routes.LEARN -> {
                    val topics = remember(starMap) { Learn.topics(starMap) }
                    val uri = androidx.compose.ui.platform.LocalUriHandler.current
                    var state by remember { mutableStateOf(prefs.tutorState) }
                    val day = remember { today() }
                    val hint = remember(topics, state, hintsOn) { de.raumlicht.core.model.Nachhilfe.hint(topics, starMap, day, state, offers && hintsOn) }
                    // beim ersten Erscheinen zählen und den Tag merken (danach ruht dieses Gebiet 14 Tage)
                    LaunchedEffect(hint?.topic) { hint?.let { h -> if (de.raumlicht.core.model.Nachhilfe.isNew(state, h.topic, day)) { count("nachhilfe_hinweis", "lernstand", h.topic)
                        prefs.tutorState = de.raumlicht.core.model.Nachhilfe.shown(prefs.tutorState, h.topic, day); state = prefs.tutorState } } }
                    LearnScreen(topics, ::openChapter, back, hint,
                        onHint = { hint?.let { h -> count("probestunde_klick", "lernstand", h.topic); runCatching { uri.openUri(h.link) } } },
                        onHideHint = { hint?.let { h -> prefs.tutorState = de.raumlicht.core.model.Nachhilfe.hide(prefs.tutorState, h.topic); state = prefs.tutorState } })
                }
                Routes.PLANNER -> {
                    val uri = androidx.compose.ui.platform.LocalUriHandler.current
                    var state by remember { mutableStateOf(prefs.tutorState) }
                    val day = remember { today() }
                    val hint = remember(plan, state, hintsOn) { de.raumlicht.core.model.Nachhilfe.exam(plan, day, state, offers && hintsOn, examUrl) }
                    // je Klausur einmal am Tag zählen
                    LaunchedEffect(hint?.key) { hint?.let { h -> if (de.raumlicht.core.model.Nachhilfe.isNew(state, h.key, day)) { count("nachhilfe_hinweis", "klassenarbeit", h.topic)
                        prefs.tutorState = de.raumlicht.core.model.Nachhilfe.shown(prefs.tutorState, h.key, day); state = prefs.tutorState } } }
                    PlannerScreen(plan, today(), starMap, { prefs.setPlan(it?.encode() ?: ""); onPlanChanged() }, { l, s, a -> record(l.id, s, a) }, back, hint,
                        onHint = { hint?.let { h -> count("probestunde_klick", "klassenarbeit", h.topic); runCatching { uri.openUri(h.link) } } },
                        onHideHint = { hint?.let { h -> prefs.tutorState = de.raumlicht.core.model.Nachhilfe.hide(prefs.tutorState, h.key); state = prefs.tutorState } })
                }
                Routes.CLASS -> ClassScreen({ l, s, a -> record(l.id, s, a) }, back)
                Routes.MATCH -> MatchScreen(back)
                "achterbahn" -> RideScreen(back, parts.getOrNull(1) ?: Coaster.rides.first().id, custom = prefs.ride, onCustom = { prefs.ride = it })
                Routes.LAGE -> LageScreen(lageBest, prefs::recordLage, back)
                Routes.DUEL -> DuelScreen(back)
                Routes.TRAINING -> { val list = remember { Training.levels(stars.value) }; TrainingPlay(list, { l, s, a -> record(l.id, s, a) }, onBack = back) }
                Routes.WORKSHOP -> WorkshopScreen(tourStars, skin, prefs::setSkin, badges.count { it.earned }, look, { t, h -> prefs.setLook(t, h) }, back)
                Routes.FLIGHT -> FlightScreen(back, custom = prefs.flight, onCustom = { prefs.flight = it })
                Routes.GALTON -> GaltonScreen(back)
                Routes.EXPERIENCE -> { val uri = androidx.compose.ui.platform.LocalUriHandler.current
                    BackstageScreen(erlebenEntries.map { it.copy(group = "") }, onBack = back, title = de.raumlicht.core.model.Erleben.TITLE, intro = de.raumlicht.core.model.Erleben.INTRO,
                        footer = { TrainerLinks { url -> runCatching { uri.openUri(url) } } }) }
                Routes.ROTATION -> { val uri = androidx.compose.ui.platform.LocalUriHandler.current
                    RotationScreen(back, onTrainer = { runCatching { uri.openUri(de.raumlicht.core.model.Erleben.TRAINER + "a-rotation") } }) }
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
                Routes.PARENTS -> if (eltern == null) LaunchedEffect(Unit) { nav.back() } else de.raumlicht.app.ui.settings.ElternScreen(pairing, eltern.page, parentStatus,
                    backup = if (eltern.snapshot != null) backupStatus ?: "" else null, restoreStatus = restoreStatus, restoreAsk = restoreFound?.let { de.raumlicht.core.model.Eltern.germanDay(it.stand) },
                    onRestore = if (eltern.load != null) ({ link -> restore(link) }) else null, onRestoreYes = { restoreFound?.let { applyRestore(it) } }, onRestoreNo = { restoreFound = null; restoreStatus = "Nichts geändert." },
                    onPair = { name -> de.raumlicht.core.model.Eltern.Pairing.fresh(eltern.create(), name)?.let { p -> prefs.setParentLink(p.encode()); parentStatus = null; sendReport(true) } },
                    onUnpair = { pairing?.let { p -> eltern.remove(de.raumlicht.core.model.Eltern.uploadUrl(p, eltern.server), p.write) { } }; prefs.setParentLink(""); prefs.parentSent = 0; prefs.backupMark = ""; parentStatus = null; backupStatus = null },
                    onSendNow = { sendReport(true) }, onBack = back)
                Routes.FAMILY -> if (familie == null) LaunchedEffect(Unit) { nav.back() }
                    // direkt angesteuert (z. B. „?ziel=familie“): erst durchs Eltern-Tor
                    else if (!gateFree && !de.raumlicht.core.model.ElternTor.open(gateOpenedAt, de.raumlicht.app.ui.fx.nowMillis())) LaunchedEffect(Unit) { nav.restart(Routes.HOME); gate { nav.go(Routes.FAMILY) } }
                    else de.raumlicht.app.ui.settings.FamilieScreen(famState, familie.base, familie.test, famStatus, famBusy, famJoin?.third, famOther?.third, familieLink.orEmpty(),
                    onCreate = { name -> famStatus = null; val n = de.raumlicht.core.model.Familie.cleanName(name); famCall("neu", n) { r, x, y -> if (r == "ok") { val p = x.split('|')
                        de.raumlicht.core.model.Familie.State.decode("${p[0]}|${p.getOrElse(1) { "" }}|$y|0||$n")?.let { st -> famSet(st); famSync(true, st) } ?: run { famStatus = de.raumlicht.core.model.Familie.problem("fehler") } }
                        else famStatus = de.raumlicht.core.model.Familie.problem(r) } },
                    onJoin = { link -> famStatus = null; val fam = de.raumlicht.core.model.Familie.fromLink(link)
                        if (fam == null) famStatus = de.raumlicht.core.model.Familie.problem("link") else famCall("kinder", fam) { r, x, y -> if (r == "ok") famJoin = Triple(fam, y, de.raumlicht.core.model.Familie.kids(x)) else famStatus = de.raumlicht.core.model.Familie.problem(r) } },
                    onPick = { kid -> famJoin?.let { j -> famPick(j.first, j.second, kid) } },
                    onNewKid = { name -> famJoin?.let { j -> val n = de.raumlicht.core.model.Familie.cleanName(name); famCall("kind", j.first, n) { r, x, _ -> if (r == "ok") famPick(j.first, j.second, de.raumlicht.core.model.Familie.Kid(x, n)) else famStatus = de.raumlicht.core.model.Familie.problem(r) } } },
                    onCancelJoin = { famJoin = null; famStatus = "Nichts geändert." },
                    onTake = { famOther?.let { (st, data, _) -> famOther = null; famSet(st.copy(mark = de.raumlicht.core.model.Eltern.mark(data))); famStatus = "✓ Lernstand aus der Familie geholt"; familie.apply(data) } },
                    onKeep = { famOther?.let { (st, _, _) -> famOther = null; famSet(st); famSync(true, st) } },
                    onSync = { famStatus = null; famSync(true) }, onFetch = { famStatus = null; famState?.let { famFetch(it, true) } },
                    onLeave = { famSet(null); famOther = null; famStatus = "Dieses Gerät ist nicht mehr mit der Familie verbunden." }, onBack = back)
                Routes.KI -> if (kiSend == null) LaunchedEffect(Unit) { nav.back() } else de.raumlicht.app.ui.settings.KiScreen(kiLast, today(), enterKi, back)
                else -> LaunchedEffect(Unit) { nav.restart(Routes.HOME) }
            }
            } }
        } }
        // dezente Hinweise zur Stimme (lädt … / gerade nicht erreichbar)
        de.raumlicht.app.ui.fx.VoiceHints(androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.TopCenter).systemBarsPadding())
        gatePending?.let { action -> de.raumlicht.app.ui.settings.ElternTorDialog(
            onSolved = { gateOpenedAt = de.raumlicht.app.ui.fx.nowMillis(); gatePending = null; count("elternbereich", null, null); action() }, onCancel = { gatePending = null }) }
        }
    }
}
