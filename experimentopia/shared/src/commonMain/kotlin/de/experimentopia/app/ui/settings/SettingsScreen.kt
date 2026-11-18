package de.experimentopia.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.core.model.License
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.GlowButton
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.Catalog
import de.experimentopia.core.model.Island
import de.experimentopia.core.model.Pro

val DEFAULT_PRIVACY get() = "Experimentopia speichert nur den Spielstand und das Forscherbuch auf diesem Gerät. Keine Anmeldung, keine Werbung, keine Kamera, kein Mikrofon. " + Pro.purchaseNote + " Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar)" + if (de.experimentopia.core.model.Nova.ENABLED) " und – wenn Vorlesen an ist – um Sätze der Vorlese-Stimme zu laden. " + de.experimentopia.core.model.Nova.PRIVACY else "."

@Composable
fun SettingsScreen(
    sound: Boolean, onSound: (Boolean) -> Unit,
    vibration: Boolean, onVibration: (Boolean) -> Unit,
    grade: Int, onGrade: (Int) -> Unit,
    installed: String, versionDate: String,
    onReset: () -> Unit,
    onPro: () -> Unit,
    onBack: () -> Unit,
    /** oben: Android zeigt hier „App-Update“, die WebApp Impressum/Datenschutz */
    top: @Composable () -> Unit = {},
    privacy: String = DEFAULT_PRIVACY,
    audio: AudioPrefs = AudioPrefs(),
    onAudio: (AudioPrefs) -> Unit = {},
    /** Forscher-Pass freigeschaltet */
    pass: Boolean = false,
    /** Karte „Eltern-Handy koppeln“ (nur WebApp) */
    parents: @Composable () -> Unit = {},
) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Bar("Einstellungen", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            top()
            de.experimentopia.app.ui.common.InstallCard()
            SoundSettings(audio, onAudio, sound, onSound, vibration, onVibration)
            if (de.experimentopia.core.model.Nova.ENABLED) ReadSettings(audio, onAudio)
            Panel(Modifier.fillMaxWidth()) {
                Text("Klassenstufe", style = MaterialTheme.typography.titleMedium, color = E.Mint)
                Text("Passende Versuche werden hervorgehoben. Alle anderen bleiben spielbar.", color = E.Muted, fontSize = 13.sp)
                ChipRow(listOf(0) + (3..10).toList(), grade, { if (it == 0) "alle" else "Klasse $it" }, onGrade)
            }
            if (pass) Panel(Modifier.fillMaxWidth().testTag("pass_status"), border = E.Sun) {
                Text("💛 Forscher-Pass aktiv", style = MaterialTheme.typography.titleMedium, color = E.Sun)
                Text("Für Eltern: Der Forscher-Pass ist auf diesem Gerät freigeschaltet – alle Versuche und Geräte sind offen. Danke!", color = E.Text, fontSize = 13.sp)
            } else if (Pro.ENABLED) Panel(Modifier.fillMaxWidth(), border = E.Sun) {
                Text("💛 Forscher-Pass", style = MaterialTheme.typography.titleMedium, color = E.Sun)
                Text("Noch nicht freigeschaltet. Einmalig ${Pro.WEB_PRICE}, kein Abo – gilt in der Web-App.", color = E.Muted, fontSize = 13.sp)
                GlowButton(onPro, color = E.Sun) { Text("Was ist im Forscher-Pass?") }
            }
            parents()
            Panel(Modifier.fillMaxWidth()) {
                Text("Für Eltern & Lehrkräfte", style = MaterialTheme.typography.titleMedium, color = E.Mint)
                Text(privacy, fontSize = 13.sp, color = E.Text)
                Text("So lernt Ihr Kind: Erst vermuten (mit Selbsteinschätzung), dann ausprobieren, dann Blubb erklären (Lernen durch Lehren), dann eine Knobelfrage. " +
                    "Falsche Vermutungen kosten nichts – sie werden als Aha-Moment gefeiert.", fontSize = 13.sp, color = E.Text)
                Text("${Catalog.all.size} Versuche auf ${Island.entries.size} Inseln (Klasse 3–10): Dichte und Auftrieb, Hebel, Rollen, schiefe Ebene, Newtonsche Gesetze, Stromkreise, Ohmsches Gesetz, Magnetismus, " +
                    "pH-Wert und Neutralisation, Reaktionsgleichungen, Schmelzen und Sieden, Teilchenmodell, Mischtemperatur, Stoffe trennen, Chromatografie, Farbmischung (Licht und Malkasten), Prisma, Akustik, freier Fall, Pendel, " +
                    "Vererbung nach Mendel, Blutgruppen, Osmose, Nährstoffnachweise, Enzyme, Fotosynthese, Räuber-Beute-Beziehungen, Mikroskopieren, Keimung, Nahrungsketten, Herz und Puls.", fontSize = 13.sp, color = E.Muted)
                Text("Alle Ergebnisse werden in Tests mit physikalischen Modellen nachgerechnet. Versuche mit Säuren, Laugen, Feuer und Strom bitte nur hier in der App – oder mit Erwachsenen.", fontSize = 12.sp, color = E.Muted)
                Text("Version $installed vom $versionDate", style = MaterialTheme.typography.labelMedium, color = E.Muted, modifier = Modifier.testTag("version"))
            }
            Panel(Modifier.fillMaxWidth()) {
                Text("Fortschritt", style = MaterialTheme.typography.titleMedium, color = E.Mint)
                GlowButton({ confirm = true }, color = E.Red) { Text("🧽 Alles zurücksetzen") }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Wirklich alles löschen?") },
        text = { Text("Sterne, Forscherbuch und Abzeichen werden gelöscht. Das lässt sich nicht rückgängig machen.") },
        confirmButton = { TextButton({ confirm = false; onReset() }) { Text("Löschen", color = E.Red) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Abbrechen") } },
    )
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = E.Text)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = E.Muted)
        }
        Switch(on, onChange)
    }
}

/**
 * Forscher-Pass: was drin ist, was frei bleibt, Preis, Kauf nur nach einer Frage für Erwachsene, Code-Eingabe (E-Mail/Link).
 * Solange keine Kasse eingetragen ist ([Pro.ENABLED] falsch), ist alles frei und es gibt nichts zu kaufen.
 */
@Composable
fun ProScreen(what: String, state: de.experimentopia.app.ui.AppState? = null, onBack: () -> Unit) {
    val sfx = LocalSfx.current; val open = LocalOpenLink.current
    val code by (state?.prefs?.proCode ?: kotlinx.coroutines.flow.MutableStateFlow<String?>(null)).collectAsState()
    val has = state?.let { Pro.valid(code, it.todayDay()) || it.prefs.pro.value } ?: false
    var gate by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Bar("💛 Forscher-Pass", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            de.experimentopia.app.ui.blubb.BlubbSays(when {
                has -> de.experimentopia.core.model.Say.PRO_HAS
                what == "Alles" -> de.experimentopia.core.model.Say.PRO_ALL
                else -> de.experimentopia.core.model.Say.proWhat(what)
            }, if (has) de.experimentopia.app.ui.blubb.Mood.CHEER else de.experimentopia.app.ui.blubb.Mood.HAPPY, speak = false)
            Panel(Modifier.fillMaxWidth(), border = E.Sun) {
                Text("Das bringt der Forscher-Pass", color = E.Sun, fontWeight = FontWeight.Black)
                Pro.benefits.forEach { Text(it, color = E.Text, fontSize = 14.sp) }
            }
            Panel(Modifier.fillMaxWidth(), border = E.Mint) {
                Text("Das bleibt immer frei", color = E.Mint, fontWeight = FontWeight.Black)
                Pro.free.forEach { Text("✓ $it", color = E.Muted, fontSize = 13.sp) }
            }
            when {
                has -> Text("✅ Freigeschaltet. Danke! 💛", color = E.Green, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("pro-status"))
                !Pro.ENABLED -> Text("Der Forscher-Pass kommt bald – bis dahin ist alles frei. 🎁", color = E.Sun, modifier = Modifier.testTag("pro-status"))
                else -> Panel(Modifier.fillMaxWidth(), border = E.Sun) {
                    Text(Pro.WEB_PRICE, color = E.Sun, fontWeight = FontWeight.Black, fontSize = 34.sp)
                    Text("einmalig · kein Abo · keine Werbung · inkl. 19 % MwSt. · gilt in der Web-App", color = E.Text, fontSize = 13.sp)
                    GlowButton({ gate = true }, Modifier.fillMaxWidth().testTag("pro-kaufen"), color = E.Sun) { Text("💛 Forscher-Pass holen", fontWeight = FontWeight.Bold) }
                    Text("Nur mit Erlaubnis der Eltern – vorher kommt eine Frage für Erwachsene.", color = E.Muted, fontSize = 12.sp)
                }
            }
            if (!has && state != null && Pro.publicKey.length == 64) Panel(Modifier.fillMaxWidth()) {
                Text("Code aus der E-Mail", color = E.Text, fontWeight = FontWeight.Bold)
                Text("Am einfachsten: den Link aus der E-Mail antippen. Oder den Code hier einfügen.", color = E.Muted, fontSize = 13.sp)
                OutlinedTextField(input, { input = it; msg = null }, Modifier.fillMaxWidth().testTag("pro-code"), placeholder = { Text("EX-…") }, minLines = 2, maxLines = 5)
                GlowButton({
                    val c = License.extract(input)
                    val ok = c != null && Pro.valid(c, state.todayDay())
                    msg = if (ok) "Super – der Forscher-Pass ist freigeschaltet! 🎉" else "Dieser Code passt leider nicht. Bitte genau so einfügen, wie er in der E-Mail steht."
                    if (ok) { state.prefs.setProCode(c); sfx?.play(Sound.WIN) } else sfx?.play(Sound.SOFT)
                }, Modifier.fillMaxWidth(), enabled = input.isNotBlank()) { Text("Code einlösen", fontWeight = FontWeight.Bold) }
                msg?.let { Text(it, color = if (it.startsWith("Super")) E.Green else E.Pink) }
            }
            Text("Für Eltern: Einmalkauf ohne Abo. Der Kauf gilt in der Web-App auf diesem Gerät bzw. in diesem Browser, ohne Konto. Gespeichert wird nur der Freischalt-Code – nichts über das Kind. " +
                "Schon gemachte Versuche bleiben immer offen.", color = E.Muted, fontSize = 12.sp)
            GlowButton(onBack, Modifier.fillMaxWidth()) { Text("Zurück", fontWeight = FontWeight.Bold) }
        }
    }
    if (gate) ParentGate(state?.todayDay() ?: 0L, onDismiss = { gate = false }) { gate = false; open(Pro.checkoutUrl) }
}

/** Frage für Erwachsene vor jedem Kauf: Geburtsjahr eingeben, mindestens 18 Jahre (Familienrichtlinie). */
@Composable
fun ParentGate(todayDays: Long, onDismiss: () -> Unit, onPassed: () -> Unit) {
    var year by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val now = remember(todayDays) { 1970 + (todayDays / 365.2425).toInt() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frage für Erwachsene") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bitte gib dein Geburtsjahr ein (vierstellig).")
            OutlinedTextField(year, { year = it.filter { c -> c.isDigit() }.take(4); wrong = false }, singleLine = true, modifier = Modifier.testTag("geburtsjahr"))
            if (wrong) Text("Das passt leider nicht. Den Kauf dürfen nur Erwachsene erledigen.", color = E.Pink)
        } },
        confirmButton = { TextButton({ val y = year.toIntOrNull(); if (y != null && y in 1900..(now - 18)) onPassed() else wrong = true }) { Text("Weiter") } },
        dismissButton = { TextButton(onDismiss) { Text("Abbrechen") } },
    )
}
