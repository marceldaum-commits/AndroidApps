package de.raumlicht.app.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
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
import de.raumlicht.app.ui.fx.LocalBuzzer
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalVibration
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.home.Bar
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.fx.Buzz
import de.raumlicht.core.model.Pro

/** Vektorschreibweise: kurz („B − A“) oder streng mit Ortsvektoren („OB⃗ − OA⃗“) – und wie man sie umstellt */
class NotationChoice(val strict: Boolean, val set: (Boolean) -> Unit)
val LocalNotation = staticCompositionLocalOf { NotationChoice(false) {} }
/** Text in der gewählten Schreibweise */
@Composable fun noted(text: String): String = if (LocalNotation.current.strict) remember(text) { de.raumlicht.core.model.Notation.strict(text) } else text

/** Eigene Lautstärke für Musik und für Geräusche (je 0 … 100 %) */
class MixChoice(val music: Int, val setMusic: (Int) -> Unit, val sound: Int, val setSound: (Int) -> Unit)
val LocalMix = staticCompositionLocalOf { MixChoice(100, {}, 100, {}) }

/** Sprechtempo beim Vorlesen (Stufen aus Vorlesen.TEMPOS, in Prozent) und wie man es umstellt */
class SpeechChoice(val rate: Int, val setRate: (Int) -> Unit)
val LocalSpeech = staticCompositionLocalOf { SpeechChoice(100) {} }

/** Raumlicht Pro in den Einstellungen: Ziel des Eintrags (null = kein Eintrag, solange Raumlicht Pro nicht erhältlich ist) und ob er freigeschaltet ist */
val LocalProEntry = staticCompositionLocalOf<(() -> Unit)?> { null }
val LocalProActive = staticCompositionLocalOf { false }

/**
 * Seite zu Raumlicht Pro: freundlich, ohne Druck, mit klarer Liste, was dazugehört und was frei bleibt.
 * Kaufen geht über die Kasse auf der Website ([offered]); freigeschaltet wird mit dem Code aus der E-Mail ([onCode]) – oder gleich über den Link darin.
 *
 * @param what was der Nutzer gerade öffnen wollte (null = über die Einstellungen oder den Freischalt-Link gekommen)
 * @param offered Raumlicht Pro ist erhältlich (Kasse eingetragen)
 * @param onCode prüft und speichert einen eingegebenen Code (auch mitten aus einer E-Mail kopiert); true = freigeschaltet
 */
@Composable
fun ProScreen(what: String?, active: Boolean, offered: Boolean, onCode: (String) -> Boolean, onBack: () -> Unit) {
    val count = de.raumlicht.app.ui.fx.LocalCount.current; val place = if (what != null) "gesperrt" else "einstellungen"
    LaunchedEffect(Unit) { if (!active) count("pro_ansehen", place, null) }
    val uri = LocalUriHandler.current; val sfx = LocalSfx.current; val buzzer = LocalBuzzer.current; val vibration = LocalVibration.current
    var input by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    // Freudensprung, wenn der Pass gerade aufgegangen ist
    val pop = remember { Animatable(1f) }
    LaunchedEffect(active) { if (active) { pop.snapTo(0.6f); pop.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow)) } }
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Raumlicht Pro", onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Lux(if (wrong && !active) LuxMood.SAD else LuxMood.HAPPY, size = 64.dp)
                Text(when {
                    active -> "Raumlicht Pro ist freigeschaltet – alles gehört dir. Viel Erfolg! 🎉"
                    wrong -> "Den Code erkenne ich nicht. Kopiere ihn am besten komplett aus der E-Mail – oder tippe einfach auf den Link darin."
                    what != null -> "$what gehört zu Raumlicht Pro. Damit schaltest du alles frei – einmal, ohne Abo."
                    offered -> "Mit Raumlicht Pro schaltest du alles frei – einmal, ohne Abo."
                    else -> "Raumlicht Pro gibt es noch nicht – bis dahin ist alles frei."
                }, Modifier.padding(start = 8.dp).testTag("pro-grund").background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            if (active) Text("✓ Freigeschaltet", Modifier.fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value }.testTag("pro-aktiv")
                .background(Q.Green.copy(alpha = 0.18f), RoundedCornerShape(18.dp)).border(1.5.dp, Q.Green, RoundedCornerShape(18.dp)).padding(16.dp), color = Q.Green, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, Q.Yellow.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (active) "Das hast du jetzt" else "Das bringt Raumlicht Pro", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Pro.benefits.forEach { Text(it, color = Q.Text, fontSize = 14.sp) }
            }
            if (!active) {
                Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Green.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Das bleibt immer frei", color = Q.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Pro.free.forEach { Text("✓ $it", color = Q.Muted, fontSize = 13.sp) }
                }
                if (offered) {
                    val gate = LocalElternTor.current
                    NeonButton({ gate { count("pro_kasse", place, null); runCatching { uri.openUri(de.raumlicht.core.model.Zaehler.link(Pro.CHECKOUT_URL, place)) } } }, Modifier.fillMaxWidth().testTag("pro-kaufen"), color = Q.Yellow) { Text("Zum Shop: Raumlicht Pro · ${Pro.PRICE}", fontWeight = FontWeight.Bold) }
                    Text("Einmalig ${Pro.PRICE} (inkl. MwSt.), kein Abo, kein Ablauf. Gekauft wird im Shop auf mathenachhilfe-online.com. Wer unter 18 ist, kauft nicht selbst – das machen die Eltern. " +
                        "Danach kommt eine E-Mail mit Link und Code. Raumlicht Pro gilt in der Web-App.", Modifier.testTag("pro-status"), color = Q.Muted, fontSize = 13.sp)
                } else Text("Raumlicht Pro ist noch nicht erhältlich.", Modifier.testTag("pro-status"), color = Q.Muted, fontSize = 13.sp)
                Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Cyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Du hast schon einen Code?", color = Q.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    OutlinedTextField(input, { input = it; wrong = false }, Modifier.fillMaxWidth().testTag("pro-code"), placeholder = { Text("RL-…") }, isError = wrong, maxLines = 4,
                        supportingText = { Text("Du kannst auch die ganze E-Mail einfügen.") })
                    NeonButton({
                        if (onCode(input)) { wrong = false; sfx?.play(de.raumlicht.app.ui.fx.Sound.WIN); buzzer.buzz(Buzz.HIT, vibration) } else { wrong = true; buzzer.buzz(Buzz.TICK, vibration) }
                    }, Modifier.fillMaxWidth().testTag("pro-freischalten"), color = Q.Cyan, enabled = input.isNotBlank()) { Text("Freischalten", fontWeight = FontWeight.Bold) }
                }
            }
            NeonButton(onBack, Modifier.fillMaxWidth().testTag("pro-zurueck"), color = if (active) Q.Green else Q.Cyan) { Text(if (active) "Los geht’s" else "Zurück", fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Abzeichen „Raumlicht Pro aktiv“ für den Kopf des Startbildschirms: nur sichtbar, wenn Raumlicht Pro freigeschaltet ist. Antippen öffnet die Pro-Seite.
 * Ploppt beim Erscheinen auf und schimmert leise – man soll auf einen Blick sehen, dass alles offen ist.
 */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    if (!LocalProActive.current) return
    val open = LocalProEntry.current
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    val glow by androidx.compose.animation.core.rememberInfiniteTransition().animateFloat(0.55f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1600), androidx.compose.animation.core.RepeatMode.Reverse))
    Text("🎓 Pro aktiv", modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }.testTag("pro-abzeichen")
        .background(Q.Green.copy(alpha = 0.2f), RoundedCornerShape(50)).border(1.5.dp, Q.Green.copy(alpha = glow), RoundedCornerShape(50))
        .then(if (open != null) Modifier.clip(RoundedCornerShape(50)).clickable(onClick = open) else Modifier).padding(horizontal = 10.dp, vertical = 4.dp),
        color = Q.Green, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
}

/** Eintrag „Raumlicht Pro“ in den Einstellungen – sichtbar, wenn Raumlicht Pro erhältlich oder schon freigeschaltet ist */
@Composable
fun ProEntry() {
    val open = LocalProEntry.current ?: return; val active = LocalProActive.current
    Row(Modifier.fillMaxWidth().testTag("pro-eintrag").background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, (if (active) Q.Green else Q.Yellow).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
        .clickable(onClick = open).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🎓", fontSize = 26.sp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(if (active) "Raumlicht Pro aktiv" else "Raumlicht Pro", color = if (active) Q.Green else Q.Yellow, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(if (active) "✓ Freigeschaltet – alles gehört dir" else "Alles freischalten oder Code eingeben", color = Q.Muted, fontSize = 13.sp)
        }
        Text("›", color = Q.Muted, fontSize = 22.sp)
    }
}
