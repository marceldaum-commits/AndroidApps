package de.experimentopia.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.GlowButton
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.Catalog
import de.experimentopia.core.model.Island
import de.experimentopia.core.model.Pro

val DEFAULT_PRIVACY get() = "Experimentopia speichert nur den Spielstand und das Forscherbuch auf diesem Gerät. Keine Anmeldung, keine Werbung, keine Kamera, kein Mikrofon. " + Pro.purchaseNote + " Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar)."

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
) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Bar("Einstellungen", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            top()
            Panel {
                Text("Ton & Gefühl", style = MaterialTheme.typography.titleMedium, color = E.Mint)
                Toggle("🔊 Geräusche und Töne", "Blubbern, Zischen, Klangröhren", sound, onSound)
                Toggle("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, onVibration)
            }
            Panel {
                Text("Klassenstufe", style = MaterialTheme.typography.titleMedium, color = E.Mint)
                Text("Passende Versuche werden hervorgehoben. Alle anderen bleiben spielbar.", color = E.Muted, fontSize = 13.sp)
                ChipRow(listOf(0) + (3..10).toList(), grade, { if (it == 0) "alle" else "Klasse $it" }, onGrade)
            }
            if (Pro.ENABLED) Panel(border = E.Sun) {
                Text("💛 Forscher-Pass", style = MaterialTheme.typography.titleMedium, color = E.Sun)
                GlowButton(onPro, color = E.Sun) { Text("Was ist im Forscher-Pass?") }
            }
            Panel {
                Text("Für Eltern & Lehrkräfte", style = MaterialTheme.typography.titleMedium, color = E.Mint)
                Text(privacy, fontSize = 13.sp, color = E.Text)
                Text("So lernt Ihr Kind: Erst vermuten (mit Selbsteinschätzung), dann ausprobieren, dann Blubb erklären (Lernen durch Lehren), dann eine Knobelfrage. " +
                    "Falsche Vermutungen kosten nichts – sie werden als Aha-Moment gefeiert.", fontSize = 13.sp, color = E.Text)
                Text("${Catalog.all.size} Versuche auf ${Island.entries.size} Inseln (Klasse 3–10): Dichte und Auftrieb, Hebel, Rollen, schiefe Ebene, Newtonsche Gesetze, Stromkreise, Ohmsches Gesetz, Magnetismus, " +
                    "pH-Wert und Neutralisation, Reaktionsgleichungen, Schmelzen und Sieden, Teilchenmodell, Mischtemperatur, Farbmischung, Prisma, Akustik, freier Fall, Pendel.", fontSize = 13.sp, color = E.Muted)
                Text("Alle Ergebnisse werden in Tests mit physikalischen Modellen nachgerechnet. Versuche mit Säuren, Laugen, Feuer und Strom bitte nur hier in der App – oder mit Erwachsenen.", fontSize = 12.sp, color = E.Muted)
                Text("Version $installed vom $versionDate", style = MaterialTheme.typography.labelMedium, color = E.Muted, modifier = Modifier.testTag("version"))
            }
            Panel {
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

/** Hinweis-Seite, wenn etwas zum Forscher-Pass gehört: freundlich, ohne Druck. Kaufen kann man noch nicht (Pro.ENABLED = false). */
@Composable
fun ProScreen(what: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Bar("💛 Forscher-Pass", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            de.experimentopia.app.ui.blubb.BlubbSays("„$what“ gehört zum Forscher-Pass. Damit öffnest du alle Inseln und Geräte – einmal, ohne Abo.", de.experimentopia.app.ui.blubb.Mood.HAPPY, speak = false)
            Panel(border = E.Sun) {
                Text("Das bringt der Forscher-Pass", color = E.Sun, fontWeight = FontWeight.Black)
                Pro.benefits.forEach { Text(it, color = E.Text, fontSize = 14.sp) }
            }
            Panel(border = E.Mint) {
                Text("Das bleibt immer frei", color = E.Mint, fontWeight = FontWeight.Black)
                Pro.free.forEach { Text("✓ $it", color = E.Muted, fontSize = 13.sp) }
            }
            Text("Der Forscher-Pass ist noch nicht erhältlich.", color = E.Muted, fontSize = 13.sp, modifier = Modifier.testTag("pro-status"))
            GlowButton(onBack, Modifier.fillMaxWidth()) { Text("Zurück", fontWeight = FontWeight.Bold) }
        }
    }
}
