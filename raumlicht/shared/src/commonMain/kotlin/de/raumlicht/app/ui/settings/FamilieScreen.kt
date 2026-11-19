package de.raumlicht.app.ui.settings

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
import de.raumlicht.core.model.Familie

/**
 * Was die Plattform für den Familien-Speicher beisteuert (im Browser: der Baustein familie.js).
 * @param base Adresse der Lern-Apps für den Familien-Link  @param call ein Aufruf an den Speicher, siehe `familieRuf`
 * @param snapshot der Speicherstand der App  @param apply setzt den Lernstand eines Kindes und startet die App neu
 */
class FamiliePlatform(val server: String, val base: String, val test: Boolean,
                      val call: (server: String, what: String, a: String, b: String, c: String, d: String, done: (String, String, String) -> Unit) -> Unit,
                      val snapshot: () -> Map<String, String>, val apply: (data: Map<String, String>) -> Unit)

/** Eintrag in den Einstellungen (null = gibt es hier nicht) */
class FamilieEntryState(val open: () -> Unit, val joined: Boolean)
val LocalFamilieEntry = staticCompositionLocalOf<FamilieEntryState?> { null }

/** Es gibt in der Familie schon einen Stand für dieses Kind, und er ist anders als der auf dem Gerät: Die Eltern entscheiden, nichts wird still überschrieben. */
class FamilieChoice(val day: String, val conflict: Boolean)

@Composable
private fun Card(color: androidx.compose.ui.graphics.Color, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)

/**
 * Seite „Familie“ – für Eltern, sachlich: Familie anlegen oder beitreten, Kind wählen, Familien-Link aufbewahren.
 * @param state die Familie dieses Geräts oder null  @param kids nicht null: gerade beigetreten, jetzt das Kind wählen
 * @param choice nicht null: Stand aus der Familie holen oder den dieses Geräts behalten?
 */
@Composable
fun FamilieScreen(state: Familie.State?, base: String, test: Boolean, status: String?, busy: Boolean, kids: List<Familie.Kid>?, choice: FamilieChoice?, prefill: String, log: List<String>?, onDelete: () -> Unit,
                  onCreate: (name: String) -> Unit, onJoin: (link: String) -> Unit, onPick: (Familie.Kid) -> Unit, onNewKid: (name: String) -> Unit, onCancelJoin: () -> Unit,
                  onTake: () -> Unit, onKeep: () -> Unit, onSync: () -> Unit, onFetch: () -> Unit, onLeave: () -> Unit, onBack: () -> Unit) {
    val share = LocalShareText.current; val sfx = LocalSfx.current
    var name by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable(prefill) { mutableStateOf(prefill) }
    var sure by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var typed by rememberSaveable { mutableStateOf("") }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Familie", onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Familie.INTRO, Modifier.testTag("familie-text"), style = MaterialTheme.typography.bodyLarge, color = Q.Text)
            Card(Q.Cyan) {
                Text("🔒 " + Familie.NOTE, Modifier.testTag("familie-hinweis"), color = Q.Text, fontSize = 13.sp, lineHeight = 18.sp)
                Text(Familie.PRIVACY_LINK + " ›", Modifier.testTag("familie-datenschutz").clickable { try { uri.openUri(Familie.PRIVACY_URL) } catch (e: Exception) { } }.padding(vertical = 6.dp), color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (test) Text("🧪 " + Familie.TEST_NOTE, Modifier.testTag("familie-test"), color = Q.Orange, fontSize = 12.sp, lineHeight = 16.sp)
            if (choice != null) Card(Q.Orange) {
                Text((if (choice.conflict) "Auf einem anderen Gerät wurde weitergelernt" else "In der Familie gibt es schon einen Lernstand") + (if (choice.day.isNotEmpty()) " (Stand ${choice.day})" else "") +
                    ". Welcher soll gelten? Der andere geht dabei verloren.", Modifier.testTag("familie-frage"), color = Q.Text, fontSize = 14.sp)
                NeonButton({ sfx?.play(Sound.TAP); onTake() }, Modifier.fillMaxWidth().testTag("familie-holen-ja"), color = Q.Green) { Text("Stand aus der Familie holen", fontWeight = FontWeight.Bold) }
                NeonButton({ sfx?.play(Sound.TAP); onKeep() }, Modifier.fillMaxWidth().testTag("familie-behalten"), color = Q.Orange) { Text("Stand dieses Geräts behalten") }
            } else if (kids != null) Card(Q.Green) {
                Text("Wer lernt auf diesem Gerät?", color = Q.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                kids.forEachIndexed { i, k -> NeonButton({ sfx?.play(Sound.TAP); onPick(k) }, Modifier.fillMaxWidth().testTag("familie-kind-$i"), color = Q.Cyan) { Text(k.label, fontWeight = FontWeight.Bold) } }
                OutlinedTextField(name, { name = it.take(Familie.MAX_NAME) }, Modifier.fillMaxWidth().testTag("familie-name"), label = { Text("Neues Kind: Spitzname") }, singleLine = true)
                NeonButton({ sfx?.play(Sound.TAP); onNewKid(name) }, Modifier.fillMaxWidth().testTag("familie-kind-neu"), color = Q.Muted) { Text("Als neues Kind hinzufügen") }
                Text("Abbrechen", Modifier.testTag("familie-abbrechen").clickable(onClick = onCancelJoin).padding(vertical = 10.dp), color = Q.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            } else if (state == null) {
                Card(Q.Cyan) {
                    Text("Neue Familie anlegen", color = Q.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    OutlinedTextField(name, { name = it.take(Familie.MAX_NAME) }, Modifier.fillMaxWidth().testTag("familie-name"), label = { Text("Spitzname des Kindes (freiwillig)") }, singleLine = true,
                        supportingText = { Text("Nur die Familie kann ihn lesen. Kein voller Name nötig.") })
                    NeonButton({ sfx?.play(Sound.TAP); onCreate(name) }, Modifier.fillMaxWidth().testTag("familie-anlegen"), color = Q.Cyan) { Text(Familie.AGREE_CREATE, fontWeight = FontWeight.Bold) }
                }
                Card(Q.Green) {
                    Text("Es gibt schon eine Familie?", color = Q.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("Füge den Familien-Link ein – aus einer anderen Lern-App oder vom Notfall-Blatt.", color = Q.Text, fontSize = 13.sp, lineHeight = 18.sp)
                    OutlinedTextField(link, { link = it.take(400) }, Modifier.fillMaxWidth().testTag("familie-link"), label = { Text("Familien-Link einfügen") }, singleLine = true)
                    NeonButton({ sfx?.play(Sound.TAP); onJoin(link) }, Modifier.fillMaxWidth().testTag("familie-beitreten"), color = Q.Green) { Text(Familie.AGREE_JOIN, fontWeight = FontWeight.Bold) }
                }
            } else {
                val full = remember(state, base) { Familie.link(state.fam, base) }
                Card(Q.Green) {
                    Text("✓ Dieses Gerät gehört zur Familie", Modifier.testTag("familie-dabei"), color = Q.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("Hier lernt: " + state.name.ifBlank { "Kind ohne Namen" }, Modifier.testTag("familie-kind"), color = Q.Text, fontSize = 14.sp)
                    Text("Familien-Nummer: " + state.number, Modifier.testTag("familie-nummer"), color = Q.Text, fontSize = 14.sp)
                    Text("Die Nummer öffnet nichts. Sie hilft nur, die Familie wiederzufinden, falls der Link verloren geht.", color = Q.Muted, fontSize = 12.sp, lineHeight = 16.sp)
                    Text(Familie.DETAILS, Modifier.testTag("familie-einzelheiten"), color = Q.Muted, fontSize = 12.sp, lineHeight = 16.sp)
                }
                QrImage(full, Modifier.align(Alignment.CenterHorizontally).size(232.dp).testTag("familie-qr"))
                Text("Familien-Link: auf einem anderen Gerät scannen oder als Notfall-Blatt aufbewahren", Modifier.align(Alignment.CenterHorizontally), color = Q.Muted, fontSize = 12.sp)
                Card(Q.Orange) { Text("⚠️ " + Familie.SECURITY, Modifier.testTag("familie-sicherheit"), color = Q.Text, fontSize = 13.sp, lineHeight = 18.sp) }
                NeonButton({ share("Familien-Zugang für die Lern-Apps von Mathenachhilfe Online.\nFamilien-Nummer: ${state.number}\nLink: $full\n${Familie.SECURITY}") },
                    Modifier.fillMaxWidth().testTag("familie-teilen"), color = Q.Cyan) { Text("Notfall-Blatt teilen", fontWeight = FontWeight.Bold) }
                Text("Gesichert wird nach dem Lernen von selbst, wenn sich etwas geändert hat.", color = Q.Muted, fontSize = 12.sp)
                NeonButton(onSync, Modifier.fillMaxWidth().testTag("familie-sichern"), color = Q.Muted) { Text("Jetzt sichern") }
                NeonButton(onFetch, Modifier.fillMaxWidth().testTag("familie-holen"), color = Q.Muted) { Text("Stand aus der Familie holen") }
                if (log != null) Card(Q.Muted) {
                    Text("Protokoll", color = Q.Text, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    if (log.isEmpty()) Text("Noch keine Einträge.", Modifier.testTag("familie-protokoll-leer"), color = Q.Muted, fontSize = 13.sp)
                    log.forEachIndexed { i, l -> Text(l, Modifier.testTag("familie-protokoll-$i"), color = Q.Text, fontSize = 13.sp) }
                }
                if (!sure) Text("Dieses Gerät aus der Familie nehmen", Modifier.testTag("familie-verlassen").clickable { sure = true }.padding(vertical = 10.dp), color = Q.Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                else Card(Q.Orange) {
                    Text("Wirklich? Der Lernstand bleibt auf diesem Gerät und in der Familie; nur die Verbindung wird getrennt. Mit dem Familien-Link kommt ihr jederzeit zurück.", color = Q.Text, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton({ sure = false; onLeave() }, Modifier.weight(1f).testTag("familie-verlassen-ja"), color = Q.Orange) { Text("Ja, trennen") }
                        NeonButton({ sure = false }, Modifier.weight(1f), color = Q.Muted) { Text("Abbrechen") }
                    }
                }
            }
            if (state != null && choice == null && kids == null) {
                if (!deleting) Text("Familie löschen", Modifier.testTag("familie-loeschen").clickable { deleting = true; typed = "" }.padding(vertical = 10.dp), color = Q.Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                else Card(Q.Orange) {
                    Text("⚠️ " + Familie.DELETE_WARNING, Modifier.testTag("familie-loeschen-warnung"), color = Q.Text, fontSize = 14.sp, lineHeight = 19.sp)
                    OutlinedTextField(typed, { typed = it.take(12) }, Modifier.fillMaxWidth().testTag("familie-loeschen-nummer"), label = { Text("Zum Bestätigen die Familien-Nummer eintippen") }, singleLine = true,
                        supportingText = { Text("Sie steht oben: " + state.number) })
                    val okay = Familie.deleteConfirmed(typed, state.number)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton({ if (okay) { deleting = false; typed = ""; onDelete() } }, Modifier.weight(1f).testTag("familie-loeschen-ja"), color = if (okay) Q.Orange else Q.Muted) { Text("Endgültig löschen") }
                        NeonButton({ deleting = false; typed = "" }, Modifier.weight(1f).testTag("familie-loeschen-nein"), color = Q.Muted) { Text("Abbrechen") }
                    }
                    if (!okay && typed.isNotBlank()) Text("Die Nummer stimmt noch nicht.", Modifier.testTag("familie-loeschen-falsch"), color = Q.Muted, fontSize = 12.sp)
                }
            }
            if (busy) Text("Einen Moment …", Modifier.testTag("familie-laeuft"), color = Q.Muted, fontSize = 13.sp)
            if (status != null) Text(status, Modifier.testTag("familie-status"), color = Q.Muted, fontSize = 13.sp)
        }
    }
}

/** Eintrag „Familie“ in den Einstellungen – der Weg dorthin führt durch das Eltern-Tor */
@Composable
fun FamilieEntry() {
    val s = LocalFamilieEntry.current ?: return
    Row(Modifier.fillMaxWidth().testTag("familie-eintrag").background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, (if (s.joined) Q.Green else Q.Muted).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
        .clickable(onClick = s.open).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("👨‍👩‍👧", fontSize = 26.sp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(if (s.joined) "Familie verbunden" else "Familie (für Eltern)", color = if (s.joined) Q.Green else Q.Text, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(if (s.joined) "Familien-Link, Sicherung, Gerät trennen" else "Lernstand für alle Lern-Apps sichern – ohne Konto, verschlüsselt", color = Q.Muted, fontSize = 13.sp)
        }
        Text("›", color = Q.Muted, fontSize = 22.sp)
    }
}
