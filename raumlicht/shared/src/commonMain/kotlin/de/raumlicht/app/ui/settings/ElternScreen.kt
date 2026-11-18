package de.raumlicht.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalShareText
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.fx.Sound
import de.raumlicht.app.ui.home.Bar
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Eltern
import de.raumlicht.core.model.Qr

/**
 * Was die Plattform für „Eltern-Handy koppeln“ beisteuert (im Browser: Zufallswerte, Verschlüsseln mit AES-GCM, Hochladen, Löschen).
 * @param create liefert frische Zufallswerte „id.schreib.key“
 * @param send verschlüsselt [json] mit [key] und lädt es nach [url] hoch; [done] bekommt den Status (204 = angekommen, 0 = keine Verbindung)
 * @param remove löscht den Bericht auf dem Server
 */
class ElternPlatform(val server: String, val page: String, val create: () -> String,
                     val send: (url: String, write: String, key: String, json: String, done: (Int) -> Unit) -> Unit,
                     val remove: (url: String, write: String, done: (Int) -> Unit) -> Unit)

/** Eintrag in den Einstellungen: Ziel und ob gerade gekoppelt ist (null = gibt es hier nicht) */
class ElternEntryState(val open: () -> Unit, val paired: Boolean)
val LocalElternEntry = staticCompositionLocalOf<ElternEntryState?> { null }

/** Der QR-Code als Bild: dunkle Felder auf weißem Grund, mit dem vorgeschriebenen hellen Rand von vier Feldern */
@Composable
fun QrImage(text: String, modifier: Modifier = Modifier) {
    val q = remember(text) { Qr.encode(text) }
    Canvas(modifier.background(Color.White, RoundedCornerShape(12.dp))) {
        val m = q ?: return@Canvas; val n = m.size + 8; val cell = size.minDimension / n
        for (y in m.indices) for (x in m.indices) if (m[y][x]) drawRect(Color.Black, Offset((x + 4) * cell, (y + 4) * cell), Size(cell + 0.5f, cell + 0.5f))
    }
}

/**
 * Seite „Eltern-Handy koppeln“ – sachlich, ohne Figur: erst erklären, dann koppeln, dann QR-Code und Link zum Weitergeben.
 * @param pairing die bestehende Kopplung oder null
 * @param status Rückmeldung zum letzten Hochladen („Bericht gesendet …“, „Keine Verbindung …“) oder null
 */
@Composable
fun ElternScreen(pairing: Eltern.Pairing?, page: String, status: String?, onPair: (name: String) -> Unit, onUnpair: () -> Unit, onSendNow: () -> Unit, onBack: () -> Unit) {
    val share = LocalShareText.current; val sfx = LocalSfx.current
    var name by rememberSaveable { mutableStateOf("") }
    var sure by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Eltern-Handy koppeln", onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (pairing == null) "Wenn du magst, können deine Eltern auf ihrem Handy sehen, wie es bei dir in Mathe läuft – ohne Konto und ohne Anmeldung."
                 else "Gekoppelt. Zeig deinen Eltern den QR-Code oder schick ihnen den Link – dann sehen sie deinen Bericht.",
                Modifier.testTag("eltern-text"), style = MaterialTheme.typography.bodyLarge, color = Q.Text)
            Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Cyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Was deine Eltern sehen", color = Q.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(Eltern.CONTENT, Modifier.testTag("eltern-inhalt"), color = Q.Text, fontSize = 13.sp, lineHeight = 18.sp)
                Text("🔒 " + Eltern.NOTE, Modifier.testTag("eltern-hinweis"), color = Q.Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            if (pairing == null) {
                OutlinedTextField(name, { name = it.take(Eltern.MAX_NAME) }, Modifier.fillMaxWidth().testTag("eltern-name"), label = { Text("Spitzname (freiwillig)") }, singleLine = true,
                    supportingText = { Text("Steht über dem Bericht. Kein voller Name nötig.") })
                NeonButton({ sfx?.play(Sound.TAP); onPair(name) }, Modifier.fillMaxWidth().testTag("eltern-koppeln"), color = Q.Cyan) { Text("👪 Eltern-Handy koppeln", fontWeight = FontWeight.Bold) }
            } else {
                val link = remember(pairing, page) { Eltern.link(pairing, page) }
                QrImage(link, Modifier.align(Alignment.CenterHorizontally).size(232.dp).testTag("eltern-qr"))
                Text("Mit der Kamera des Eltern-Handys scannen", Modifier.align(Alignment.CenterHorizontally), color = Q.Muted, fontSize = 12.sp)
                NeonButton({ share(link) }, Modifier.fillMaxWidth().testTag("eltern-teilen"), color = Q.Cyan) { Text("Link teilen", fontWeight = FontWeight.Bold) }
                if (status != null) Text(status, Modifier.testTag("eltern-status"), color = Q.Muted, fontSize = 13.sp)
                Text("Der Bericht aktualisiert sich nach dem Lernen von selbst, höchstens alle 10 Minuten.", color = Q.Muted, fontSize = 12.sp)
                NeonButton(onSendNow, Modifier.fillMaxWidth().testTag("eltern-senden"), color = Q.Muted) { Text("Bericht jetzt aktualisieren") }
                if (!sure) Text("Kopplung aufheben", Modifier.testTag("eltern-aufheben").clickable { sure = true }.padding(vertical = 10.dp), color = Q.Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                else Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Orange.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Wirklich aufheben? Der Bericht wird gelöscht; der Link deiner Eltern zeigt dann nichts mehr.", color = Q.Text, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton({ sure = false; onUnpair() }, Modifier.weight(1f).testTag("eltern-aufheben-ja"), color = Q.Orange) { Text("Ja, aufheben") }
                        NeonButton({ sure = false }, Modifier.weight(1f), color = Q.Muted) { Text("Abbrechen") }
                    }
                }
            }
        }
    }
}

/** Eintrag „Eltern-Handy koppeln“ in den Einstellungen */
@Composable
fun ElternEntry() {
    val s = LocalElternEntry.current ?: return
    Row(Modifier.fillMaxWidth().testTag("eltern-eintrag").background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, (if (s.paired) Q.Green else Q.Muted).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
        .clickable(onClick = s.open).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("👪", fontSize = 26.sp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(if (s.paired) "Eltern-Handy gekoppelt" else "Eltern-Handy koppeln", color = if (s.paired) Q.Green else Q.Text, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(if (s.paired) "Bericht ansehen, teilen oder Kopplung aufheben" else "Freiwillig: Eltern sehen deinen Lernstand – ohne Konto, verschlüsselt", color = Q.Muted, fontSize = 13.sp)
        }
        Text("›", color = Q.Muted, fontSize = 22.sp)
    }
}
