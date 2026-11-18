package de.raumlicht.app.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.Buzz
import de.raumlicht.app.ui.fx.LocalBuzzer
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalVibration
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.home.Bar
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Ki

/** Zählung, Nachhilfe-Hinweise und Geschenk-Eintrag in den Einstellungen (null = nicht anzeigen) */
class OffersChoice(val count: Boolean, val setCount: (Boolean) -> Unit, val hints: Boolean, val setHints: (Boolean) -> Unit)
val LocalOffers = staticCompositionLocalOf<OffersChoice?> { null }

/** KI-Tutor in den Einstellungen: Ziel des Eintrags (null = kein Eintrag) und bis wann der KI-Abi-Pass gilt (Tag seit 1.1.1970; null = keiner eingetragen) */
class KiEntryState(val open: () -> Unit, val lastDay: Long?, val today: Long) { val active get() = lastDay != null && today <= lastDay }
val LocalKiEntry = staticCompositionLocalOf<KiEntryState?> { null }

/**
 * Seite zum KI-Tutor und zum KI-Abi-Pass: was er kann, was er kostet, bis wann er gilt – und die Code-Eingabe.
 * @param lastDay letzter gültiger Tag des eingetragenen Passes (null = keiner), [today] heute
 * @param onCode prüft und speichert einen eingegebenen Code (auch mitten aus einer E-Mail); true = angenommen
 */
@Composable
fun KiScreen(lastDay: Long?, today: Long, onCode: (String) -> Boolean, onBack: () -> Unit, checkout: String = Ki.CHECKOUT_URL) {
    val uri = LocalUriHandler.current; val sfx = LocalSfx.current; val buzzer = LocalBuzzer.current; val vibration = LocalVibration.current
    val active = lastDay != null && today <= lastDay; val expired = lastDay != null && !active
    var input by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(active) { if (active) { pop.snapTo(0.6f); pop.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow)) } }
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("KI-Tutor", onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Lux(if (wrong && !active) LuxMood.SAD else LuxMood.HAPPY, size = 64.dp)
                Text(when {
                    active -> "Dein KI-Abi-Pass läuft – ich erkläre dir jede Aufgabe so oft anders, bis es klick macht. 🎉"
                    wrong -> "Den Code erkenne ich nicht. Kopiere ihn am besten komplett aus der E-Mail – oder tippe einfach auf den Link darin."
                    expired -> "Dein KI-Abi-Pass ist abgelaufen. Tipp und Theorie helfen dir weiter wie immer."
                    else -> "Hängst du an einer Aufgabe, erkläre ich sie dir mit KI-Hilfe noch einmal anders – und du kannst nachfragen. ${Ki.TRIALS} Proben sind frei."
                }, Modifier.padding(start = 8.dp).testTag("ki-grund").background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            if (lastDay != null) Text(if (active) "✓ KI-Abi-Pass aktiv bis ${Ki.date(lastDay)}" else "KI-Abi-Pass abgelaufen am ${Ki.date(lastDay)}",
                Modifier.fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value }.testTag("ki-aktiv")
                    .background((if (active) Q.Green else Q.Orange).copy(alpha = 0.18f), RoundedCornerShape(18.dp)).border(1.5.dp, if (active) Q.Green else Q.Orange, RoundedCornerShape(18.dp)).padding(16.dp),
                color = if (active) Q.Green else Q.Orange, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, Q.Yellow.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("So hilft dir der KI-Tutor", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 17.sp)
                listOf("🤖 Nach einem Fehlversuch: „Erklär’s mir anders“ – passend zu deiner Eingabe", "💬 Rückfragen zur Aufgabe stellen, so wie bei einer Nachhilfe",
                    "🧭 Er führt dich zum Weg – die Lösung findest du selbst", "🎟️ Jede Erklärung und jede Rückfrage kostet 1 KI-Punkt",
                    "📅 Mit dem KI-Abi-Pass: ${Ki.DAILY} KI-Punkte an jedem Tag, bis zum 31. Juli deines Abi-Jahres").forEach { Text(it, color = Q.Text, fontSize = 14.sp) }
            }
            if (!active) {
                if (checkout.isNotEmpty()) {
                    NeonButton({ runCatching { uri.openUri(de.raumlicht.core.model.Zaehler.link(checkout, "einstellungen")) } }, Modifier.fillMaxWidth().testTag("ki-kaufen"), color = Q.Yellow) { Text("Zum Shop: KI-Abi-Pass · ${Ki.PRICE}", fontWeight = FontWeight.Bold) }
                    Text("Einmalig ${Ki.PRICE} (inkl. MwSt.), kein Abo – der Pass endet von selbst am 31. Juli deines Abi-Jahres (2027, 2028 oder 2029, wählst du im Shop). " +
                        "Gekauft wird im Shop auf mathenachhilfe-online.com. Bist du noch nicht 18, schließen deine Eltern den Kauf ab. Danach kommt eine E-Mail mit Link und Code. " +
                        "Raumlicht Pro brauchst du dafür nicht. Der Pass gilt in der Web-App.", Modifier.testTag("ki-status"), color = Q.Muted, fontSize = 13.sp)
                } else Text("Den KI-Abi-Pass gibt es bald im Shop. Bis dahin kannst du den KI-Tutor mit den Proben ausprobieren. Der Pass gilt in der Web-App.", Modifier.testTag("ki-status"), color = Q.Muted, fontSize = 13.sp)
                Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Cyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Du hast schon einen Code?", color = Q.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    OutlinedTextField(input, { input = it; wrong = false }, Modifier.fillMaxWidth().testTag("ki-code"), placeholder = { Text("RL-…") }, isError = wrong, maxLines = 4,
                        supportingText = { Text("Du kannst auch die ganze E-Mail einfügen.") })
                    NeonButton({
                        if (onCode(input)) { wrong = false; sfx?.play(de.raumlicht.app.ui.fx.Sound.WIN); buzzer.buzz(Buzz.HIT, vibration) } else { wrong = true; buzzer.buzz(Buzz.TICK, vibration) }
                    }, Modifier.fillMaxWidth().testTag("ki-freischalten"), color = Q.Cyan, enabled = input.isNotBlank()) { Text("Freischalten", fontWeight = FontWeight.Bold) }
                }
            }
            Text("🔒 " + Ki.PRIVACY, Modifier.testTag("ki-datenschutz"), color = Q.Muted, fontSize = 12.sp, lineHeight = 16.sp)
            NeonButton(onBack, Modifier.fillMaxWidth().testTag("ki-zurueck"), color = if (active) Q.Green else Q.Cyan) { Text(if (active) "Los geht’s" else "Zurück", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Eintrag „KI-Tutor“ in den Einstellungen – sichtbar, wo es den KI-Tutor gibt */
@Composable
fun KiEntry() {
    val s = LocalKiEntry.current ?: return
    Row(Modifier.fillMaxWidth().testTag("ki-eintrag").background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, (if (s.active) Q.Green else Q.Cyan).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
        .clickable(onClick = s.open).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🤖", fontSize = 26.sp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(if (s.active) "KI-Abi-Pass aktiv" else "KI-Tutor", color = if (s.active) Q.Green else Q.Cyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(s.lastDay?.takeIf { s.active }?.let { "✓ gilt bis ${Ki.date(it)} · ${Ki.DAILY} KI-Punkte am Tag" } ?: "Lux erklärt’s anders – ${Ki.TRIALS} Proben frei, Code eingeben", color = Q.Muted, fontSize = 13.sp)
        }
        Text("›", color = Q.Muted, fontSize = 22.sp)
    }
}
