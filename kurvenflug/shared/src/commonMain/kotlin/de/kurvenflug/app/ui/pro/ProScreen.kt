package de.kurvenflug.app.ui.pro

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ProShop
import de.kurvenflug.app.ProState
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.CalDate
import de.kurvenflug.core.model.License
import de.kurvenflug.core.model.Pro

/**
 * Kurvenflug Pro: was drin ist, Preis, Kauf nur nach Eltern-Frage (Google-Familienrichtlinie), Code-Eingabe für Web/GitHub.
 * [locked]: dieser Bildschirm kam statt eines gesperrten Levels.
 */
@Composable
fun ProScreen(state: ProState, shop: ProShop?, onCode: (String) -> Unit, onBack: () -> Unit, locked: Boolean = false) {
    val counter = de.kurvenflug.app.LocalPlatform.current
    LaunchedEffect(Unit) { counter.countAt("pro_ansehen", if (locked) "gesperrt" else "einstellungen") }
    val sfx = LocalSfx.current
    var gate by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var codeMsg by remember { mutableStateOf<String?>(null) }
    val price = shop?.price?.collectAsState()?.value
    val available = shop?.available?.collectAsState()?.value == true
    val t = rememberInfiniteTransition(label = "pro")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
            Text("Kurvenflug Pro", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Text("✨", fontSize = 96.sp, modifier = Modifier.rotate(spin).graphicsLayer { alpha = 0.35f })
                Text("⭐", fontSize = 64.sp, modifier = Modifier.scale(pulse))
            }
            val speech = when {
                state.pro -> "Pro ist freigeschaltet! Jetzt geht's bis ganz nach oben! 🚀"
                locked -> PRO_LOCKED_TEXT
                else -> "Mit Pro gibt's Profi-Stufen, Andromeda und noch mehr!"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Volt(if (state.pro) VoltMood.DANCE else VoltMood.WOW, size = 64.dp)
                Spacer(Modifier.width(8.dp))
                Text(speech, color = Q.Text, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Q.Panel).padding(12.dp))
            }
            Pro.perks.forEachIndexed { i, p ->
                val a by animateFloatAsState(if (shown) 1f else 0f, tween(400, delayMillis = 80 * i), label = "perk$i")
                Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a; translationX = (1 - a) * 60f }.clip(RoundedCornerShape(16.dp))
                    .background(Q.Panel).border(1.dp, Q.Yellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(p.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, fontWeight = FontWeight.Bold, color = Q.Text)
                        Text(p.text, style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                    }
                    if (state.pro) Text("✅", fontSize = 20.sp)
                }
            }
            Text("Frei bleibt alles bis Klasse 10: alle Kapitel, Tagesmission, Duell, Bosse, Sturm bis „Schwer“.",
                style = MaterialTheme.typography.bodySmall, color = Q.Muted, textAlign = TextAlign.Center)

            when {
                state.pro -> {
                    val until = state.license?.until?.let { CalDate.ofEpochDay(it) }
                    Text(if (until != null) "🏫 Schullizenz – gültig bis ${until.dayOfMonth}.${until.monthValue}.${until.year}, im Unterricht und zu Hause"
                        else if (state.viaPlay) "Gekauft über Google Play. Danke! 💛" else if (de.kurvenflug.app.LocalPlatform.current.isWeb) "Freigeschaltet mit Code – gilt in der Web-App. Danke! 💛" else "Freigeschaltet mit Code. Danke! 💛",
                        color = Q.Green, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                !available -> Text("Pro kommt bald – bis dahin ist alles frei spielbar. 🎁", color = Q.Yellow, textAlign = TextAlign.Center)
                else -> {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(Q.Yellow.copy(alpha = 0.18f), Q.Panel))).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(price ?: Pro.WEB_PRICE, fontSize = 34.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
                        Text("einmalig · kein Abo · keine Werbung" + if (shop?.acceptsCodes == true) " · inkl. 19 % MwSt." else "",
                            color = Q.Text, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        NeonButton(onClick = { gate = true }, color = Q.Yellow, modifier = Modifier.fillMaxWidth()) {
                            Text("⭐ Pro freischalten", fontWeight = FontWeight.Bold)
                        }
                        Text("Nur mit Erlaubnis der Eltern – vorher kommt eine Frage für Erwachsene.",
                            color = Q.Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                    if (shop?.acceptsCodes == false) TextButton(onClick = { sfx?.play(Sound.TAP); shop.restore() }) { Text("Schon gekauft? Käufe wiederherstellen", color = Q.Cyan) }
                }
            }
            if (shop?.acceptsCodes == true && !(state.pro && state.license?.until == null)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Q.Panel).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Code aus der E-Mail", fontWeight = FontWeight.Bold, color = Q.Text)
                    Text("Am einfachsten: den Link aus der E-Mail antippen. Oder den Code hier einfügen – auf iPhone und iPad immer dann, " +
                        "wenn Kurvenflug als App vom Home-Bildschirm läuft (sie hat einen eigenen Speicher, der Link öffnet Safari).",
                        style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                    OutlinedTextField(code, { code = it; codeMsg = null }, Modifier.fillMaxWidth(), placeholder = { Text("KF-…") },
                        minLines = 2, maxLines = 5, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                    NeonButton(onClick = {
                        val c = License.extract(code)
                        val ok = c != null && shop.publicKey.isNotEmpty() && License.check(c, shop.publicKey, CalDate.today().toEpochDay()) != null
                        codeMsg = if (ok) "Super – Pro ist freigeschaltet! 🎉" else "Dieser Code passt leider nicht. Bitte genau so einfügen, wie er in der E-Mail steht."
                        if (ok) { onCode(c!!); counter.countAt("code_eingeloest", "eingabe"); sfx?.play(Sound.WIN) } else sfx?.play(Sound.WRONG)
                    }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Code einlösen", fontWeight = FontWeight.Bold) }
                    codeMsg?.let { Text(it, color = if (it.startsWith("Super")) Q.Green else Q.Pink) }
                }
            }
            Text("Für Eltern: Einmalkauf ohne Abo. " + (if (shop?.acceptsCodes == false) "Der Kauf läuft über Google Play (Ihr Google-Konto). "
                else if (de.kurvenflug.app.LocalPlatform.current.isWeb) "Der Kauf gilt in der Web-App (in diesem Browser), ohne Konto. "
                else "Der Kauf gilt für dieses Gerät, ohne Konto. ") +
                "Es werden keine Daten über das Kind gespeichert – nur „Pro: ja“.",
                style = MaterialTheme.typography.labelSmall, color = Q.Muted, textAlign = TextAlign.Center)
        }
    }
    if (gate) ParentGate(onDismiss = { gate = false }) { gate = false; counter.countAt("pro_kasse", if (locked) "gesperrt" else "einstellungen"); shop?.buy() }
}

/**
 * Kinder-Hinweis an jedem Pro-Schloss (UWG Anhang Nr. 28, Korrektur AppZentrale 19.11.): sachlich, kein Preis, kein Kaufknopf,
 * keine Aufforderung an das Kind. Der einzige Weg führt zum Eltern-Tor, nie direkt zur Kasse.
 */
const val PRO_LOCKED_TEXT = "Das gehört zu KurvenFlug Pro."   // ohne Eltern-Zusatz (Recht/Marcel 19.11.: kam einer mittelbaren Kaufaufforderung zu nah)

@Composable
fun ProLockedNotice(onBack: () -> Unit, onParents: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⭐", fontSize = 56.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 520.dp)) {
            Volt(VoltMood.HAPPY, size = 64.dp)
            Spacer(Modifier.width(8.dp))
            Text(PRO_LOCKED_TEXT, color = Q.Text, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Q.Panel).padding(12.dp))
        }
        NeonButton(onClick = onBack, color = Q.Cyan, modifier = Modifier.widthIn(min = 220.dp)) { Text("← Zurück", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onParents) { Text("👪 Elternbereich (für Erwachsene)", color = Q.Muted) }
    }
}

/** Gemeinsamer 10-Minuten-Speicher des Eltern-Tors (nur Arbeitsspeicher, nie gespeichert). Root stellt eine Instanz bereit. */
val LocalGateMemory = androidx.compose.runtime.staticCompositionLocalOf { de.kurvenflug.core.model.GateMemory() }

/**
 * Eltern-Tor nach ELTERN-TOR.md: Malaufgabe IN WORTEN („siebzehn mal acht“), eigene Ziffern-Tastatur, nach einem Fehler sofort
 * eine neue Aufgabe, kein Zeitdruck, jederzeit abbrechen. Einmal gelöst, bleibt es 10 Minuten offen (nur Arbeitsspeicher).
 * Der Grund [why] ist sachlich („Hier geht es zur Website.“). Kein Vorlese-Knopf, keine Gerätestimme.
 */
@Composable
fun ParentGate(onDismiss: () -> Unit, why: String = "Käufe dürfen nur Erwachsene auslösen.", onPassed: () -> Unit) {
    val memory = LocalGateMemory.current
    // schon offen (10 Minuten nach dem Lösen): gar nicht erst fragen
    if (memory.isOpen(de.kurvenflug.core.model.nowMillis())) {
        LaunchedEffect(Unit) { onPassed() }
        return
    }
    var task by remember { mutableStateOf(de.kurvenflug.core.model.ParentGateTask.next(de.kurvenflug.core.model.nowMillis().toInt())) }
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    fun submit() {
        if (task.isRight(input)) { memory.open(de.kurvenflug.core.model.nowMillis()); onPassed() }
        else { wrong = true; input = ""; task = de.kurvenflug.core.model.ParentGateTask.next(de.kurvenflug.core.model.nowMillis().toInt(), task) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frage für Erwachsene") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(why, textAlign = TextAlign.Center)
                Text("Was ist ${task.words}?", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = "Rechenaufgabe: ${task.words}" })
                Text(input.ifEmpty { "…" }, fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (wrong) MaterialTheme.colorScheme.error else Q.Cyan,
                    modifier = Modifier.semantics { contentDescription = if (input.isEmpty()) "noch keine Eingabe" else "Eingabe $input" })
                if (wrong) Text("Das hat nicht gepasst – hier ist eine neue Aufgabe.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            Button(onClick = {
                                when (k) {
                                    "⌫" -> input = input.dropLast(1)
                                    "OK" -> submit()
                                    else -> if (input.length < 3) { input += k; wrong = false }
                                }
                            }, modifier = Modifier.size(width = 64.dp, height = 48.dp), contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (k == "OK") Q.Green else Q.Panel, contentColor = if (k == "OK") Q.Space else Q.Text)) {
                                Text(k, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.semantics { contentDescription = when (k) { "⌫" -> "Löschen"; "OK" -> "Antwort abschicken"; else -> k } })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
