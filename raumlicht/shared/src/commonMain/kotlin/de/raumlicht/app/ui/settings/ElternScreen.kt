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
 * @param remove löscht den Bericht auf dem Server (und damit auch die Sicherung)
 * @param load holt die Sicherung von [url] und entschlüsselt sie; [done] bekommt Status (200, 404, 0 = keine Verbindung, -1 = Schlüssel passt nicht) und Klartext
 * @param snapshot der Speicherstand der App für die Sicherung
 * @param apply setzt den Speicherstand aus einer Sicherung samt Kopplung und startet die App neu
 * @param release schickt den Vermerk „vom Kind gelöst“ (statt zu löschen); null = nur Löschen
 */
class ElternPlatform(val server: String, val page: String, val create: () -> String,
                     val send: (url: String, write: String, key: String, json: String, done: (Int) -> Unit) -> Unit,
                     val remove: (url: String, write: String, done: (Int) -> Unit) -> Unit,
                     val load: ((url: String, key: String, done: (Int, String) -> Unit) -> Unit)? = null,
                     val snapshot: (() -> Map<String, String>)? = null,
                     val apply: ((data: Map<String, String>, pairing: String) -> Unit)? = null,
                     val release: ((url: String, write: String, key: String, json: String, done: (Int) -> Unit) -> Unit)? = null)

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
 * @param backup Rückmeldung zur Sicherung (leer = noch keine), null = Sicherung gibt es hier nicht
 * @param restoreAsk nicht null: Es wurde eine Sicherung gefunden (Datum), und hier ist schon ein Lernstand – erst nachfragen
 * @param onRestore „Fortschritt wiederherstellen“ mit dem eingefügten Link (null = gibt es hier nicht)
 */
@Composable
fun ElternScreen(pairing: Eltern.Pairing?, page: String, status: String?, onPair: (name: String) -> Unit, onUnpair: (byChild: Boolean) -> Unit, onSendNow: () -> Unit, onBack: () -> Unit,
                 backup: String? = null, restoreStatus: String? = null, restoreAsk: String? = null, onRestore: ((link: String) -> Unit)? = null, onRestoreYes: () -> Unit = {}, onRestoreNo: () -> Unit = {}) {
    val share = LocalShareText.current; val sfx = LocalSfx.current
    var name by rememberSaveable { mutableStateOf("") }
    // Lösen: 0 nichts · 1 Altersfrage · 2 selbst lösen (ab 16) · 3 „Nein“: nur die Eltern · 4 Eltern bestätigen (nach dem Eltern-Tor)
    var step by rememberSaveable { mutableStateOf(0) }
    val gate = LocalElternTor.current
    var link by rememberSaveable { mutableStateOf("") }
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
                if (backup != null) Text("💾 " + Eltern.BACKUP_NOTE, Modifier.testTag("eltern-sicherung-hinweis"), color = Q.Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            if (pairing == null) {
                OutlinedTextField(name, { name = it.take(Eltern.MAX_NAME) }, Modifier.fillMaxWidth().testTag("eltern-name"), label = { Text("Spitzname (freiwillig)") }, singleLine = true,
                    supportingText = { Text("Steht über dem Bericht. Kein voller Name nötig.") })
                NeonButton({ sfx?.play(Sound.TAP); onPair(name) }, Modifier.fillMaxWidth().testTag("eltern-koppeln"), color = Q.Cyan) { Text("👪 Eltern-Handy koppeln", fontWeight = FontWeight.Bold) }
                if (onRestore != null) Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Green.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neues Gerät? Fortschritt wiederherstellen", color = Q.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("War dieses Gerät oder ein altes schon gekoppelt? Auf der Eltern-Seite gibt es den Knopf „Neues Gerät des Kindes? Fortschritt übertragen“. Er kopiert den Kopplungs-Link – füge ihn hier ein.",
                        color = Q.Text, fontSize = 13.sp, lineHeight = 18.sp)
                    OutlinedTextField(link, { link = it.take(400) }, Modifier.fillMaxWidth().testTag("eltern-link"), label = { Text("Kopplungs-Link einfügen") }, singleLine = true)
                    if (restoreAsk == null) NeonButton({ sfx?.play(Sound.TAP); onRestore(link) }, Modifier.fillMaxWidth().testTag("eltern-wiederherstellen"), color = Q.Green) { Text("Fortschritt wiederherstellen", fontWeight = FontWeight.Bold) }
                    else Column(Modifier.fillMaxWidth().background(Q.Space, RoundedCornerShape(14.dp)).border(1.dp, Q.Orange.copy(alpha = 0.7f), RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sicherung gefunden" + (if (restoreAsk.isNotEmpty()) " (Stand $restoreAsk)" else "") + ". Auf diesem Gerät gibt es schon einen Lernstand. Soll er durch die Sicherung ersetzt werden? Das lässt sich nicht rückgängig machen.",
                            Modifier.testTag("eltern-ersetzen-frage"), color = Q.Text, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeonButton(onRestoreYes, Modifier.weight(1f).testTag("eltern-ersetzen-ja"), color = Q.Orange) { Text("Ja, ersetzen") }
                            NeonButton(onRestoreNo, Modifier.weight(1f).testTag("eltern-ersetzen-nein"), color = Q.Muted) { Text("Abbrechen") }
                        }
                    }
                    if (restoreStatus != null) Text(restoreStatus, Modifier.testTag("eltern-wiederherstellen-status"), color = Q.Muted, fontSize = 13.sp)
                }
            } else {
                val link = remember(pairing, page) { Eltern.link(pairing, page) }
                QrImage(link, Modifier.align(Alignment.CenterHorizontally).size(232.dp).testTag("eltern-qr"))
                Text("Mit der Kamera des Eltern-Handys scannen", Modifier.align(Alignment.CenterHorizontally), color = Q.Muted, fontSize = 12.sp)
                NeonButton({ share(link) }, Modifier.fillMaxWidth().testTag("eltern-teilen"), color = Q.Cyan) { Text("Link teilen", fontWeight = FontWeight.Bold) }
                if (status != null) Text(status, Modifier.testTag("eltern-status"), color = Q.Muted, fontSize = 13.sp)
                if (!backup.isNullOrEmpty()) Text(backup, Modifier.testTag("eltern-sicherung-status"), color = Q.Muted, fontSize = 13.sp)
                Text("Der Bericht aktualisiert sich nach dem Lernen von selbst, höchstens alle 10 Minuten.", color = Q.Muted, fontSize = 12.sp)
                NeonButton(onSendNow, Modifier.fillMaxWidth().testTag("eltern-senden"), color = Q.Muted) { Text("Bericht jetzt aktualisieren") }
                if (step == 0) Text("Kopplung aufheben", Modifier.testTag("eltern-aufheben").clickable { step = 1 }.padding(vertical = 10.dp), color = Q.Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                else Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Orange.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (step) {
                        1 -> {   // Altersfrage (Selbstauskunft, wird nicht gespeichert); beide Knöpfe gleich gestaltet, keine Vorauswahl
                            Text(Eltern.RELEASE_ASK, Modifier.testTag("eltern-alter-frage"), color = Q.Text, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NeonButton({ step = 2 }, Modifier.weight(1f).testTag("eltern-alter-ja"), color = Q.Cyan) { Text(Eltern.RELEASE_YES) }
                                NeonButton({ step = 3 }, Modifier.weight(1f).testTag("eltern-alter-nein"), color = Q.Cyan) { Text(Eltern.RELEASE_NO) }
                            }
                        }
                        2 -> {   // ab 16: selbst lösen – die Eltern-Seite zeigt danach „vom Kind gelöst“
                            Text(Eltern.RELEASE_SELF, Modifier.testTag("eltern-loesen-text"), color = Q.Text, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NeonButton({ step = 0 }, Modifier.weight(1f).testTag("eltern-loesen-abbrechen"), color = Q.Muted) { Text("Abbrechen") }
                                NeonButton({ step = 0; onUnpair(true) }, Modifier.weight(1f).testTag("eltern-loesen-ja"), color = Q.Orange) { Text("Verbindung lösen") }
                            }
                        }
                        3 -> {   // unter 16: nur die Eltern – über das Eltern-Tor
                            Text(Eltern.RELEASE_PARENTS, Modifier.testTag("eltern-nur-eltern"), color = Q.Text, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NeonButton({ gate { step = 4 } }, Modifier.weight(1f).testTag("eltern-zum-bereich"), color = Q.Cyan) { Text("Zum Elternbereich") }
                                NeonButton({ step = 0 }, Modifier.weight(1f).testTag("eltern-zurueck"), color = Q.Muted) { Text("Zurück") }
                            }
                        }
                        else -> {   // Eltern haben das Tor geöffnet: löschen wie bisher, ohne Vermerk
                            Text("Wirklich aufheben? Bericht und Sicherung werden gelöscht; der Link deiner Eltern zeigt dann nichts mehr. Dein Lernstand auf diesem Gerät bleibt.", color = Q.Text, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NeonButton({ step = 0; onUnpair(false) }, Modifier.weight(1f).testTag("eltern-aufheben-ja"), color = Q.Orange) { Text("Ja, aufheben") }
                                NeonButton({ step = 0 }, Modifier.weight(1f), color = Q.Muted) { Text("Abbrechen") }
                            }
                        }
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
