package de.experimentopia.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import de.experimentopia.core.model.FamilieJson.Fam
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Familien-Speicher dieses Kindes: Lernstand verschlüsselt auf unserem Server (Marcel 19.11.2026, Recht V8/A6).
 * Anlegen und Beitreten nur nach Einwilligung (Pflichttext, Knopf nicht vorausgewählt) und hinter dem Eltern-Tor.
 * Gesichert wird automatisch beim Start und bei Änderungen; Konflikte werden nie still überschrieben, sondern sicher zusammengeführt ([Familie.merge]).
 */
class FamilyCoupling(private val p: FamilyPlatform) {
    private val _fam = MutableStateFlow(FamilieJson.famOrNull(p.read("familie")))
    val fam: StateFlow<Fam?> = _fam
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    /** Beitritt: Kinder der Familie zur Auswahl (null = gerade kein Beitritt) */
    private val _kinder = MutableStateFlow<List<FamilieJson.Kind>?>(null)
    val kinder: StateFlow<List<FamilieJson.Kind>?> = _kinder
    private val base get() = FamilieJson.basis(p.test)
    private var pending: Triple<Fam, String, Long>? = null     // Familie, Index (JSON), rev des Index
    private var pendingNummer = ""
    private var lastBackup = 0L

    val nummer: String get() = p.read("familienummer") ?: ""
    val lastSync: Long get() = p.read("familiezeit")?.toLongOrNull() ?: 0L

    private fun fail(msg: String, done: (Boolean) -> Unit) { _busy.value = false; _status.value = msg; done(false) }
    private fun persist(f: Fam, nummer: String, kind: String) { p.write("familie", f.encode()); p.write("familienummer", nummer); p.write("kind", kind); p.write("familierev", null); _fam.value = f }
    /** Nur die Schlüssel dieses Geräts entfernen – der Lernstand bleibt */
    private fun forget() { listOf("familie", "familienummer", "kind", "familierev", "familiezeit").forEach { p.write(it, null) }; _fam.value = null }

    fun create(nickname: String, done: (Boolean) -> Unit = {}) {
        if (_busy.value) return; _busy.value = true; _status.value = "Lege die Familie an …"
        p.call("neu", FamilieJson.neu()) { a ->
            val f = FamilieJson.parse(a); val fam = f.fam ?: return@call fail(FamilieJson.grundText(f.grund), done)
            p.call("anlegen", FamilieJson.anlegen(fam, base)) { b ->
                val r = FamilieJson.parse(b); if (!r.ok) return@call fail(FamilieJson.grundText(r.grund), done)
                p.call("indexNeu", FamilieJson.indexNeu(nickname)) { c ->
                    val i = FamilieJson.parse(c); val idx = i.index ?: return@call fail(FamilieJson.grundText(i.grund), done)
                    persist(fam, f.nummer, i.kindId)                    // ab jetzt gehört die Familie zu diesem Gerät
                    p.call("indexSichern", FamilieJson.indexSichern(fam, base, idx, 0)) { d ->
                        val s = FamilieJson.parse(d); _busy.value = false
                        if (!s.ok) { _status.value = FamilieJson.grundText(s.grund); done(false) } else backup(false, true, done)
                    }
                }
            }
        }
    }

    /** Familien-Link prüfen; danach Kind wählen ([kinder]) und [choose] bzw. [chooseNew] */
    fun join(text: String, done: (Boolean) -> Unit = {}) {
        if (_busy.value) return; _busy.value = true; _status.value = "Suche die Familie …"
        p.call("lesen", FamilieJson.lesen(text)) { a ->
            val f = FamilieJson.parse(a); val fam = f.fam ?: return@call fail("Das ist kein Familien-Link. Er sieht so aus: …#familie=…", done)
            p.call("indexLaden", FamilieJson.indexLaden(fam, base)) { b ->
                val r = FamilieJson.parse(b)
                when {
                    r.ok -> { pending = Triple(fam, r.index ?: EMPTY_INDEX, r.rev); pendingNummer = f.nummer; _kinder.value = r.kinder; _busy.value = false; _status.value = null; done(true) }
                    r.grund == "keine" -> { pending = Triple(fam, EMPTY_INDEX, 0); pendingNummer = f.nummer; _kinder.value = emptyList(); _busy.value = false; _status.value = null; done(true) }
                    else -> fail(if (r.grund == "familie-weg") "Diese Familie gibt es nicht." else FamilieJson.grundText(r.grund), done)
                }
            }
        }
    }
    fun cancelJoin() { pending = null; _kinder.value = null }
    fun choose(kindId: String, done: (Boolean) -> Unit = {}) {
        val (fam, _, _) = pending ?: return; persist(fam, pendingNummer, kindId); cancelJoin(); backup(true, true, done)
    }
    fun chooseNew(name: String, done: (Boolean) -> Unit = {}) {
        val (fam, index, rev) = pending ?: return; _busy.value = true
        p.call("kindHinzu", FamilieJson.kindHinzu(index, name)) { a ->
            val k = FamilieJson.parse(a); val idx = k.index ?: return@call fail(FamilieJson.grundText(k.grund), done)
            p.call("indexSichern", FamilieJson.indexSichern(fam, base, idx, rev)) { b ->
                val s = FamilieJson.parse(b); _busy.value = false
                if (!s.ok) { _status.value = if (s.grund == "konflikt") "Die Familie wurde gerade geändert – bitte noch einmal versuchen." else FamilieJson.grundText(s.grund); done(false) }
                else { persist(fam, pendingNummer, k.kindId); cancelJoin(); backup(true, true, done) }
            }
        }
    }

    /**
     * Sichern: holen, sicher zusammenführen, nur bei Änderung hochladen. [reload]: Hat der Server etwas Neues gebracht, startet die App neu, damit es sichtbar wird.
     * Mindestens 60 Sekunden zwischen zwei Läufen (außer von Hand).
     */
    fun backup(reload: Boolean = false, manual: Boolean = true, done: (Boolean) -> Unit = {}) {
        val f = _fam.value ?: return done(false); val kind = p.read("kind") ?: return done(false)
        if (_busy.value) return done(false)
        if (!manual && p.now() - lastBackup < 60_000) return done(false)
        _busy.value = true; lastBackup = p.now()
        fun ok(neuLokal: Boolean) {
            _busy.value = false; p.write("familiezeit", p.now().toString())
            _status.value = if (neuLokal && !reload) "✓ Gesichert. Neuer Stand von einem anderen Gerät kommt beim nächsten Start." else "✓ Gesichert"
            done(true); if (neuLokal && reload) p.reload()
        }
        fun round(n: Int) {
            p.call("standLaden", FamilieJson.standLaden(f, base, kind)) { txt ->
                val r = FamilieJson.parse(txt)
                val remote: Map<String, String>; val rev: Long
                when {
                    r.ok -> { remote = r.daten; rev = r.rev }
                    r.grund == "keine" -> { remote = emptyMap(); rev = 0 }
                    r.grund == "familie-weg" -> { forget(); return@call fail(Familie.FAMILIE_WEG, done) }
                    else -> return@call fail(FamilieJson.grundText(r.grund), done)
                }
                val local = Familie.TEILE.mapNotNull { k -> p.read(k)?.let { k to it } }.toMap()
                val merged = Familie.merge(local, remote)
                val neuLokal = merged != local
                if (neuLokal) merged.forEach { (k, v) -> p.write(k, v) }
                if (merged == remote && rev > 0) { p.write("familierev", rev.toString()); return@call ok(neuLokal) }
                p.call("standSichern", FamilieJson.standSichern(f, base, kind, merged, rev)) { t2 ->
                    val s = FamilieJson.parse(t2)
                    when {
                        s.ok -> { p.write("familierev", s.rev.toString()); ok(neuLokal) }
                        s.grund == "konflikt" && n < 3 -> round(n + 1)       // ein anderes Gerät war schneller: neu holen und zusammenführen
                        s.grund == "familie-weg" -> { forget(); fail(Familie.FAMILIE_WEG, done) }
                        else -> fail(FamilieJson.grundText(s.grund), done)
                    }
                }
            }
        }
        _status.value = "Sichere …"; round(0)
    }

    fun blatt(done: (FamilieJson.Antwort) -> Unit) { val f = _fam.value ?: return; p.call("blatt", FamilieJson.blatt(f, base)) { done(FamilieJson.parse(it)) } }

    /** Familie löschen: auf dem Server (ALLE Lern-Apps), hier nur die Schlüssel. 404 heißt: gibt es schon nicht mehr. */
    fun delete(done: (Boolean) -> Unit = {}) {
        val f = _fam.value ?: return done(false); if (_busy.value) return; _busy.value = true; _status.value = "Lösche die Familie …"
        p.call("loeschen", FamilieJson.loeschen(f, base)) { t ->
            val r = FamilieJson.parse(t)
            if (r.ok || r.grund == "keine" || r.grund == "familie-weg") { forget(); _busy.value = false; _status.value = "Die Familie ist gelöscht. Der Lernstand auf diesem Gerät bleibt."; done(true) }
            else fail(FamilieJson.grundText(r.grund), done)
        }
    }

    private companion object { const val EMPTY_INDEX = "{\"v\":1,\"kinder\":[],\"apps\":[]}" }
}

/** Beim Start sichern und danach bei Änderungen (höchstens alle 60 Sekunden, nur mit Familie) */
@Composable
fun FamilyAutoSync(c: FamilyCoupling?, stars: Map<String, Int>, journal: List<NoteRow>) {
    c ?: return
    val fam by c.fam.collectAsState()
    LaunchedEffect(fam?.famId) { if (fam != null) { delay(2500); c.backup(reload = true, manual = false) } }
    LaunchedEffect(stars, journal) { if (fam != null) { delay(30_000); c.backup(reload = false, manual = false) } }
}

@Composable
fun FamilyCard(c: FamilyCoupling?, nickname: String = "", incoming: String? = null, onIncomingUsed: () -> Unit = {}) {
    c ?: return
    val fam by c.fam.collectAsState(); val status by c.status.collectAsState(); val busy by c.busy.collectAsState(); val kinder by c.kinder.collectAsState()
    val open = LocalOpenLink.current; val sfx = LocalSfx.current; val share = LocalParentSync.current
    var consent by remember { mutableStateOf<String?>(null) }      // "anlegen" | "beitreten"
    var linkText by remember { mutableStateOf(incoming.orEmpty()) }       // Familien-Link aus der Adresszeile: vorausgefüllt, aber nichts passiert ohne Tor und Einwilligung
    LaunchedEffect(incoming) { if (!incoming.isNullOrBlank()) onIncomingUsed() }
    var showLink by remember { mutableStateOf(false) }
    var blatt by remember { mutableStateOf<FamilieJson.Antwort?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var typedNr by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var shared by remember { mutableStateOf("") }
    Panel(Modifier.fillMaxWidth().testTag("familie_karte"), border = E.Mint) {
        Text("👨‍👩‍👧 Familie – Lernstand sichern", style = MaterialTheme.typography.titleMedium, color = E.Mint)
        Text("Für Eltern: Der Lernstand wird verschlüsselt auf unserem Server gesichert. So geht auf einem neuen Gerät nichts verloren, und ihr könnt mehrere Geräte und Lern-Apps verbinden. " +
            "Ohne Familie verlässt nichts dieses Gerät.", color = E.Text, fontSize = 13.sp)
        val f = fam
        if (f == null) {
            if (kinder == null) {
                GlowButton({ consent = "anlegen" }, Modifier.fillMaxWidth().testTag("familie_anlegen"), color = E.Mint, enabled = !busy) { Text("🏠 Familie anlegen", fontWeight = FontWeight.Bold) }
                OutlinedTextField(linkText, { linkText = it }, Modifier.fillMaxWidth().testTag("familie_link"), label = { Text("Familien-Link einfügen") }, minLines = 2, maxLines = 4,
                    placeholder = { Text("https://…#familie=…", fontSize = 12.sp) })
                GlowButton({ consent = "beitreten" }, Modifier.fillMaxWidth().testTag("familie_beitreten"), color = E.Sky, enabled = linkText.isNotBlank() && !busy) { Text("🔗 Familie beitreten", fontWeight = FontWeight.Bold) }
            } else {
                Text("Wer lernt hier?", color = E.Text, fontWeight = FontWeight.Bold)
                kinder.orEmpty().forEach { k -> GlowButton({ c.choose(k.kindId) }, Modifier.fillMaxWidth().testTag("familie_kind"), color = E.Sky) { Text(k.name.ifBlank { "Kind ohne Namen" }) } }
                OutlinedTextField(newName, { newName = it.take(20) }, Modifier.fillMaxWidth().testTag("familie_neuer_name"), singleLine = true, label = { Text("Neues Kind (Spitzname, freiwillig)") })
                GlowButton({ c.chooseNew(newName) }, Modifier.fillMaxWidth().testTag("familie_neues_kind"), color = E.Mint, enabled = !busy) { Text("➕ Neues Kind anlegen") }
                TextButton({ c.cancelJoin() }) { Text("Abbrechen", color = E.Muted) }
            }
        } else {
            Text("✓ Verbunden · Familien-Nummer ${c.nummer}" + (c.lastSync.takeIf { it > 0 }?.let { " · zuletzt gesichert ${dateText(it + (share?.offsetMs ?: 0))}" } ?: ""), color = E.Green, fontSize = 13.sp, modifier = Modifier.testTag("familie_status_ok"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton({ c.backup() }, Modifier.weight(1f).testTag("familie_sichern"), color = E.Mint, enabled = !busy) { Text("💾 Jetzt sichern", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                GlowButton({ showLink = !showLink; if (showLink && blatt == null) c.blatt { blatt = it } }, Modifier.weight(1f).testTag("familie_zeigen"), color = E.Sky) { Text(if (showLink) "Ausblenden" else "📱 Link & QR-Code", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            // Sicherheitshinweis dauerhaft auf der Familien-Seite (Recht V8)
            Text(Familie.SICHERHEIT, color = E.Muted, fontSize = 12.sp, modifier = Modifier.testTag("familie_sicherheit"))
            AnimatedVisibilityBox(showLink) {
                val b = blatt
                if (b == null || b.link.isBlank()) Text("Einen Moment …", color = E.Muted)
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(Familie.SICHERHEIT, color = E.Pink, fontSize = 12.sp)       // direkt bei QR-Code und Link teilen
                    QrImage(b.link, Modifier.widthIn(max = 260.dp).fillMaxWidth())
                    GlowButton({ share?.share("Familien-Zugang Lern-Apps", b.link) { shared = it; if (it.isNotEmpty()) sfx?.play(Sound.DING) } }, Modifier.fillMaxWidth().testTag("familie_teilen"), color = E.Sky) { Text("📨 Link teilen", fontWeight = FontWeight.Bold) }
                    if (shared == "kopiert") Text("Link kopiert.", color = E.Green, fontSize = 12.sp)
                    Panel(Modifier.fillMaxWidth().testTag("familie_notfallblatt"), border = E.Line) {
                        Text("🧾 Notfall-Blatt", color = E.Text, fontWeight = FontWeight.Bold)
                        Text(b.blatt, color = E.Text, fontSize = 12.sp)
                        Text(Familie.SICHERHEIT, color = E.Pink, fontSize = 12.sp)     // direkt beim Notfall-Blatt
                        Text("Ausdrucken oder abfotografieren und sicher aufbewahren: Mit dem Link (oder der Familien-Nummer über uns) lässt sich der Zugang wiederherstellen.", color = E.Muted, fontSize = 11.sp)
                    }
                }
            }
            TextButton({ confirmDelete = true; typedNr = "" }, Modifier.testTag("familie_loeschen")) { Text("Familie löschen …", color = E.Pink) }
        }
        status?.let { Text(it, color = if (it.startsWith("✓")) E.Green else E.Muted, fontSize = 12.sp, modifier = Modifier.testTag("familie_status")) }
    }
    // Einwilligung: NUR dieser eine Text, kein Kästchen vorausgewählt, der Knopf selbst ist die Einwilligung; Abbrechen lädt nichts hoch
    consent?.let { art ->
        AlertDialog(
            onDismissRequest = { consent = null },
            title = { Text(if (art == "anlegen") "Familie anlegen" else "Familie beitreten") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Familie.PFLICHTTEXT, modifier = Modifier.testTag("familie_pflichttext"))
                TextButton({ open(Familie.DATENSCHUTZ_URL) }, Modifier.testTag("familie_datenschutz")) { Text(Familie.DATENSCHUTZ_LINK_TEXT) }
            } },
            confirmButton = { TextButton({
                consent = null
                if (art == "anlegen") c.create(nickname) else c.join(linkText)
            }, Modifier.testTag("familie_einverstanden")) { Text(if (art == "anlegen") Familie.KNOPF_ANLEGEN else Familie.KNOPF_BEITRETEN, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ consent = null }, Modifier.testTag("familie_abbrechen")) { Text("Abbrechen") } },
        )
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Familie wirklich löschen?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Familie.LOESCHEN_WARNUNG)
            Text("Zur Sicherheit bitte die Familien-Nummer eintippen: ${c.nummer}", fontWeight = FontWeight.Bold)
            OutlinedTextField(typedNr, { typedNr = it }, Modifier.testTag("familie_nummer_eingabe"), singleLine = true, label = { Text("Familien-Nummer") })
        } },
        confirmButton = { TextButton({ confirmDelete = false; c.delete() }, Modifier.testTag("familie_loeschen_ja"), enabled = Familie.nummerGleich(typedNr, c.nummer)) { Text("Familie löschen", color = E.Red) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Abbrechen") } },
    )
}

@Composable
private fun AnimatedVisibilityBox(visible: Boolean, content: @Composable () -> Unit) = androidx.compose.animation.AnimatedVisibility(visible) { Column { content() } }
