package de.experimentopia.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Koppeln, Bericht hochladen (höchstens alle 10 Minuten), Kopplung aufheben. Ohne Kopplung geht nichts ins Netz. */
class ParentCoupling(private val p: ParentSync) {
    private val _link = MutableStateFlow(ParentLink.parse(p.stored))
    val link: StateFlow<ParentLink?> = _link
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status
    private fun keep(l: ParentLink?) { _link.value = l; p.stored = l?.encode() }

    fun couple(nickname: String) = ParentLink.create(p::random, p.test, nickname).also { keep(it) }
    fun rename(nickname: String) { _link.value?.let { keep(it.copy(nickname = nickname.trim().take(20))) } }

    /** Wann darf frühestens wieder hochgeladen werden? (ms ab jetzt) */
    fun waitMs(): Long = _link.value?.let { maxOf(0L, it.sentAt + ParentReport.MIN_GAP_MS - p.now()) } ?: Long.MAX_VALUE

    fun upload(stars: Map<String, Int>, journal: List<NoteRow>, grade: Int, done: (Boolean) -> Unit = {}) {
        val l = _link.value ?: return
        val now = p.now()
        // Monatssummen dauerhaft fortschreiben (bleiben, auch wenn das Forscherbuch alte Einträge abgibt)
        val months = Monthly.merge(Monthly.decode(p.months), journal, p.offsetMs)
        p.months = Monthly.encode(months)
        val json = ParentReport.build(stars.map { ResultRow(it.key, it.value, true) }, journal, grade, l.nickname, now, p.offsetMs, months)
        val body = ParentReport.seal(json, l.key, p.random(12))
        if (body.length > ParentReport.MAX_BYTES) { _status.value = "Bericht zu groß – bitte melden."; done(false); return }
        _status.value = "Sende Bericht …"
        p.send("PUT", l.uploadUrl, l.write, body) { st ->
            val ok = st in 200..299
            if (ok) { _link.value?.takeIf { it.id == l.id }?.let { keep(it.copy(sentAt = now)) }; backup(done); return@send }
            _status.value = when { st == 0 -> "Gerade kein Internet – der Bericht wird später geschickt."; st == 403 -> "Der Server kennt diese Kopplung nicht mehr. Bitte neu koppeln."; else -> "Server antwortet nicht (Fehler $st) – später neuer Versuch." }
            done(ok)
        }
    }

    /** Verschlüsselte Sicherung des ganzen Lernstands – nur, wenn sich seit der letzten etwas geändert hat */
    private fun backup(done: (Boolean) -> Unit) {
        val l = _link.value ?: return done(false)
        val now = p.now()
        var data = p.snapshot()
        val h = Backup.hash(l.nickname, data)
        if (h == l.backupHash) { _status.value = "✓ Bericht übertragen · Sicherung aktuell"; return done(true) }
        fun seal(d: Map<String, String>) = ParentReport.seal(Backup.build(p.version, now, p.offsetMs, l.write, l.nickname, d), l.key, p.random(12))
        var body = seal(data)
        if (body.length > Backup.MAX_BYTES) { data = Backup.shrink(data, p.journalKey, now); body = seal(data) }
        if (body.length > Backup.MAX_BYTES) { _status.value = "✓ Bericht übertragen · Sicherung zu groß – bitte melden."; return done(false) }
        p.send("PUT", l.backupUrl, l.write, body) { st ->
            val ok = st in 200..299
            if (ok) _link.value?.takeIf { it.id == l.id }?.let { keep(it.copy(backupHash = h, backupAt = now)) }
            _status.value = if (ok) "✓ Bericht übertragen · Lernstand gesichert" else "✓ Bericht übertragen · Sicherung klappte nicht (${if (st == 0) "kein Internet" else "Fehler $st"}) – nächster Versuch später."
            done(ok)
        }
    }

    /** Was beim Wiederherstellen gefunden wurde */
    sealed interface Found {
        data class Ok(val id: String, val key: String, val test: Boolean, val backup: Backup.Read.Ok) : Found
        data class Problem(val message: String) : Found
    }

    /** Sicherung zum eingefügten Kopplungs-Link holen und entschlüsseln (noch nichts überschreiben) */
    fun fetchBackup(text: String, done: (Found) -> Unit) {
        val pl = Backup.parseLink(text) ?: return done(Found.Problem("Das sieht nicht wie ein Kopplungs-Link aus. Bitte den ganzen Link aus der Eltern-Nachricht einfügen (…/eltern-bericht/#…)."))
        val test = pl.test ?: p.test
        p.get(Backup.url(test, pl.id)) { st, text2 ->
            done(when {
                st == 404 -> Found.Problem("Keine Sicherung gefunden. Gesichert wird erst, wenn das alte Gerät gekoppelt war und einmal etwas gesendet hat.")
                st == 0 -> Found.Problem("Gerade kein Internet – bitte später noch einmal.")
                st !in 200..299 -> Found.Problem("Der Server antwortet nicht (Fehler $st) – bitte später noch einmal.")
                else -> when (val r = Backup.open(text2, pl.key)?.let { Backup.read(it) }) {
                    null -> Found.Problem("Die Sicherung lässt sich mit diesem Link nicht öffnen. Ist der Link vollständig?")
                    Backup.Read.OtherApp -> Found.Problem("Dieser Link gehört zu einer anderen App.")
                    Backup.Read.Broken -> Found.Problem("Die Sicherung ist beschädigt.")
                    is Backup.Read.Ok -> Found.Ok(pl.id, pl.key, test, r)
                }
            })
        }
    }

    /** Lernstand übernehmen, Kopplung übernehmen (das Gerät schreibt danach weiter) und die App neu starten */
    fun applyRestore(f: Found.Ok) {
        val b = f.backup
        keep(ParentLink(f.id, b.write, f.key, f.test, b.nickname, sentAt = 0, backupHash = Backup.hash(b.nickname, b.data), backupAt = p.now()))
        p.restore(b.data)
    }

    fun unlink(done: () -> Unit = {}) {
        val l = _link.value ?: return
        _status.value = "Kopplung wird aufgehoben …"
        p.send("DELETE", l.uploadUrl, l.write, null) { st ->
            keep(null)
            _status.value = if (st in 200..299 || st == 404) "Kopplung aufgehoben – Bericht und Sicherung sind gelöscht." else "Kopplung auf diesem Gerät aufgehoben. Der Server war nicht erreichbar; dort wird der Bericht nach 90 Tagen von selbst gelöscht."
            done()
        }
    }
}

/** QR-Code als Bild (weißer Rand, schwarze Felder) */
@Composable
fun QrImage(text: String, modifier: Modifier = Modifier) {
    val qr = remember(text) { Qr.encode(text) } ?: return
    Canvas(modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).testTag("qr")) {
        val quiet = 3; val n = qr.size + 2 * quiet; val c = size.width / n
        for (y in 0 until qr.size) for (x in 0 until qr.size) if (qr[x, y]) drawRect(Color.Black, Offset((x + quiet) * c, (y + quiet) * c), Size(c + 0.5f, c + 0.5f))
    }
}

/**
 * Karte „Eltern-Handy koppeln“ in den Einstellungen. Koppeln und Aufheben nur nach der Frage für Erwachsene ([ParentGate]).
 * Auf Plattformen ohne [ParentSync] erscheint nichts.
 */
@Composable
fun ParentCard(coupling: ParentCoupling?, todayDays: Long, hasProgress: Boolean = false, restoreOpenAtStart: Boolean = false, report: () -> Unit) {
    coupling ?: return
    val link by coupling.link.collectAsState()
    val status by coupling.status.collectAsState()
    val sync = LocalParentSync.current
    val sfx = LocalSfx.current
    var gate by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf(false) }
    var nick by remember(link?.id) { mutableStateOf(link?.nickname ?: "") }
    var shared by remember { mutableStateOf("") }
    var confirmUnlink by remember { mutableStateOf(false) }
    var restoreOpen by remember { mutableStateOf(restoreOpenAtStart) }   // true nur für Bildschirmfotos
    var pasted by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<ParentCoupling.Found?>(null) }
    Panel(Modifier.fillMaxWidth().testTag("eltern_karte"), border = E.Sky) {
        Text("👨‍👩‍👧 Eltern-Handy koppeln", style = MaterialTheme.typography.titleMedium, color = E.Sky)
        Text("Für Eltern: Sehen Sie auf Ihrem eigenen Handy, was Ihr Kind geschafft hat und wo es hakt – ohne Konto, Ende-zu-Ende verschlüsselt. " +
            "Ohne Kopplung verlässt nichts dieses Gerät.", color = E.Text, fontSize = 13.sp)
        val l = link
        if (l == null) {
            GlowButton({ gate = "koppeln" }, Modifier.fillMaxWidth().testTag("eltern_koppeln"), color = E.Sky) { Text("🔗 Eltern-Handy koppeln", fontWeight = FontWeight.Bold) }
        } else {
            Text("✓ Gekoppelt" + (if (l.nickname.isNotBlank()) " – „${l.nickname}“" else "") + (if (l.sentAt > 0) " · zuletzt gesendet ${dateText(l.sentAt + (sync?.offsetMs ?: 0))}" else " · noch kein Bericht gesendet") +
                if (l.test) " · Test-Stufe" else "", color = E.Green, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton({ open = !open }, Modifier.weight(1f).testTag("eltern_zeigen"), color = E.Sky) { Text(if (open) "QR-Code ausblenden" else "📱 QR-Code zeigen", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                GlowButton({ report() }, Modifier.weight(1f).testTag("eltern_senden"), color = E.Mint) { Text("📤 Jetzt senden", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            AnimatedVisibility(open) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mit der Kamera des Eltern-Handys scannen – oder den Link teilen:", color = E.Muted, fontSize = 13.sp)
                    QrImage(l.parentUrl, Modifier.widthIn(max = 260.dp).fillMaxWidth())
                    GlowButton({ sync?.share("Experimentopia – Bericht für Eltern", l.parentUrl) { shared = it; if (it.isNotEmpty()) sfx?.play(Sound.DING) } }, Modifier.fillMaxWidth().testTag("eltern_teilen"), color = E.Sky) {
                        Text("📨 Link teilen", fontWeight = FontWeight.Bold)
                    }
                    if (shared == "kopiert") Text("Link kopiert – jetzt z. B. per Messenger an das Eltern-Handy schicken.", color = E.Green, fontSize = 12.sp)
                    OutlinedTextField(nick, { nick = it.take(20) }, Modifier.fillMaxWidth().testTag("eltern_name"), singleLine = true, label = { Text("Spitzname (freiwillig)") },
                        supportingText = { Text("Nur ein Spitzname, damit Eltern mehrere Kinder auseinanderhalten. Kein echter Name nötig.", fontSize = 11.sp) })
                    if (nick.trim() != l.nickname) TextButton({ coupling.rename(nick); report() }) { Text("Spitzname übernehmen") }
                    Text("Der Link enthält den Schlüssel – bitte nur an Eltern geben. Wer den Link hat, kann den Bericht lesen.", color = E.Muted, fontSize = 12.sp)
                }
            }
            TextButton({ gate = "aufheben" }, Modifier.testTag("eltern_aufheben")) { Text("Kopplung aufheben", color = E.Pink) }
        }
        status?.let { Text(it, color = if (it.startsWith("✓")) E.Green else E.Muted, fontSize = 12.sp, modifier = Modifier.testTag("eltern_status")) }
        Text("Der Bericht enthält nur Zahlen zum Lernen (Klasse, Lernzeit, Versuche, richtige Vermutungen, Themen) und – wenn gewünscht – einen Spitznamen. " +
            "Bei gekoppeltem Eltern-Handy wird zusätzlich eine Ende-zu-Ende-verschlüsselte Sicherung des Lernstands gespeichert (siehe Datenschutzerklärung 10i) – " +
            "so geht auf einem neuen Gerät nichts verloren. Der Server speichert nur verschlüsselten Text, den Schlüssel kennt nur der Link. Nach 90 Tagen ohne Aktualisierung wird alles gelöscht.", color = E.Muted, fontSize = 11.sp)
        HorizontalDivider(color = E.Line)
        Text("💾 Fortschritt wiederherstellen", color = E.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Neues Gerät oder Browser-Daten gelöscht? Mit dem Kopplungs-Link vom Eltern-Handy kommt der gesicherte Lernstand zurück.", color = E.Muted, fontSize = 12.sp)
        if (!restoreOpen) TextButton({ gate = "wiederherstellen" }, Modifier.testTag("eltern_wiederherstellen")) { Text("Fortschritt wiederherstellen …", color = E.Sky) }
        AnimatedVisibility(restoreOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(pasted, { pasted = it; found = null }, Modifier.fillMaxWidth().testTag("eltern_link"), label = { Text("Kopplungs-Link einfügen") }, minLines = 2, maxLines = 4,
                    placeholder = { Text("https://…/eltern-bericht/#…", fontSize = 12.sp) })
                GlowButton({ busy = true; found = null; coupling.fetchBackup(pasted) { f -> busy = false; found = f; if (f is ParentCoupling.Found.Ok && !hasProgress) { sfx?.play(Sound.WIN); coupling.applyRestore(f) } } },
                    Modifier.fillMaxWidth().testTag("eltern_holen"), enabled = pasted.isNotBlank() && !busy, color = E.Sky) { Text(if (busy) "Suche Sicherung …" else "💾 Sicherung holen", fontWeight = FontWeight.Bold) }
                (found as? ParentCoupling.Found.Problem)?.let { Text("🙈 ${it.message}", color = E.Pink, fontSize = 13.sp, modifier = Modifier.testTag("eltern_problem")) }
            }
        }
    }
    when (gate) {
        "koppeln" -> ParentGate(onDismiss = { gate = null }) { gate = null; coupling.couple(nick); open = true; sfx?.play(Sound.WIN); report() }
        "aufheben" -> ParentGate(onDismiss = { gate = null }) { gate = null; confirmUnlink = true }
        "wiederherstellen" -> ParentGate(onDismiss = { gate = null }) { gate = null; restoreOpen = true }
    }
    // Vorhandenen Lernstand nie ohne Nachfrage überschreiben
    (found as? ParentCoupling.Found.Ok)?.takeIf { hasProgress }?.let { f ->
        val n = f.backup.data.size
        AlertDialog(
            onDismissRequest = { found = null },
            title = { Text("Lernstand ersetzen?") },
            text = { Text("Sicherung gefunden" + (if (f.backup.nickname.isNotBlank()) " („${f.backup.nickname}“)" else "") + (if (f.backup.stand.length >= 10) " vom ${f.backup.stand.take(10).split('-').reversed().joinToString(".")}" else "") +
                ". Auf diesem Gerät gibt es schon einen Lernstand – er wird durch die Sicherung ersetzt und lässt sich nicht zurückholen. ($n Einträge)") },
            confirmButton = { TextButton({ found = null; sfx?.play(Sound.WIN); coupling.applyRestore(f) }, Modifier.testTag("eltern_ersetzen")) { Text("Ersetzen", color = E.Pink) } },
            dismissButton = { TextButton({ found = null }) { Text("Abbrechen") } },
        )
    }
    if (confirmUnlink) AlertDialog(
        onDismissRequest = { confirmUnlink = false },
        title = { Text("Kopplung aufheben?") },
        text = { Text("Der Bericht wird vom Server gelöscht, der Eltern-Link funktioniert dann nicht mehr. Man kann jederzeit neu koppeln.") },
        confirmButton = { TextButton({ confirmUnlink = false; coupling.unlink(); open = false }) { Text("Aufheben", color = E.Pink) } },
        dismissButton = { TextButton({ confirmUnlink = false }) { Text("Abbrechen") } },
    )
}

/** Nach jeder Sitzung hochladen – höchstens alle 10 Minuten; wartet sonst, bis die Pause vorbei ist. */
@Composable
fun ParentAutoUpload(coupling: ParentCoupling?, stars: Map<String, Int>, journal: List<NoteRow>, grade: Int) {
    coupling ?: return
    val link by coupling.link.collectAsState()
    val latest by rememberUpdatedState(Triple(stars, journal, grade))
    LaunchedEffect(journal.size, link?.id) {
        if (link == null || journal.isEmpty()) return@LaunchedEffect
        // erst ein wenig warten: mehrere Versuche hintereinander ergeben einen Bericht
        delay(maxOf(60_000L, coupling.waitMs()))
        val (s, j, g) = latest
        coupling.upload(s, j, g)
    }
}
