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
        val json = ParentReport.build(stars.map { ResultRow(it.key, it.value, true) }, journal, grade, l.nickname, now, p.offsetMs)
        val body = ParentReport.seal(json, l.key, p.random(12))
        if (body.length > ParentReport.MAX_BYTES) { _status.value = "Bericht zu groß – bitte melden."; done(false); return }
        _status.value = "Sende Bericht …"
        p.send("PUT", l.uploadUrl, l.write, body) { st ->
            val ok = st in 200..299
            if (ok) _link.value?.takeIf { it.id == l.id }?.let { keep(it.copy(sentAt = now)) }
            _status.value = when { ok -> "✓ Bericht übertragen"; st == 0 -> "Gerade kein Internet – der Bericht wird später geschickt."; st == 403 -> "Der Server kennt diese Kopplung nicht mehr. Bitte neu koppeln."; else -> "Server antwortet nicht (Fehler $st) – später neuer Versuch." }
            done(ok)
        }
    }

    fun unlink(done: () -> Unit = {}) {
        val l = _link.value ?: return
        _status.value = "Kopplung wird aufgehoben …"
        p.send("DELETE", l.uploadUrl, l.write, null) { st ->
            keep(null)
            _status.value = if (st in 200..299 || st == 404) "Kopplung aufgehoben – der Bericht ist gelöscht." else "Kopplung auf diesem Gerät aufgehoben. Der Server war nicht erreichbar; dort wird der Bericht nach 90 Tagen von selbst gelöscht."
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
fun ParentCard(coupling: ParentCoupling?, todayDays: Long, report: () -> Unit) {
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
        Text("Übertragen werden nur Zahlen zum Lernen (Klasse, Lernzeit, Versuche, richtige Vermutungen, Themen) und – wenn gewünscht – ein Spitzname. " +
            "Der Server speichert nur verschlüsselten Text, den Schlüssel kennt nur der Link. Nach 90 Tagen ohne Aktualisierung wird alles gelöscht.", color = E.Muted, fontSize = 11.sp)
    }
    when (gate) {
        "koppeln" -> ParentGate(todayDays, onDismiss = { gate = null }) { gate = null; coupling.couple(nick); open = true; sfx?.play(Sound.WIN); report() }
        "aufheben" -> ParentGate(todayDays, onDismiss = { gate = null }) { gate = null; confirmUnlink = true }
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
