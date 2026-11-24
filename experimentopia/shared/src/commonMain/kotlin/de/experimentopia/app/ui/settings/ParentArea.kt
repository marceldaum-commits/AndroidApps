package de.experimentopia.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.home.color
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*

/**
 * Elternbereich hinter dem Eltern-Tor: alles, was nicht für Kinder ist – Forscher-Pass (Preis, Kauf, Code), Familie/Kopplung, Zählung,
 * Zurücksetzen und die Hinweise auf die kostenlose Probestunde (Recht V15: nur hier, nur bei objektiven Schwierigkeiten, nur auf dem Gerät berechnet,
 * höchstens 1× in 4 Wochen je Thema, wegklickbar und abschaltbar). Blubb wirbt nie.
 */
@Composable
fun ParentArea(
    todayDays: Long, grade: Int, openAtStart: Boolean = false,
    journal: List<NoteRow> = emptyList(), nickname: String? = null, prefs: Prefs? = null,
    nowMs: Long = LocalParentSync.current?.now() ?: 0L,
    extra: @Composable () -> Unit = {},
) {
    val counter = LocalCounter.current
    val open = LocalOpenLink.current
    var unlocked by remember { mutableStateOf(openAtStart) }   // true nur für Bildschirmfotos
    var gate by remember { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth().testTag("elternbereich"), border = E.Violet) {
        Text("👪 Elternbereich", style = MaterialTheme.typography.titleMedium, color = E.Violet)
        if (!unlocked) {
            Text("Für Eltern: Forscher-Pass, Eltern-Handy, Familie, Zählung und Zurücksetzen.", color = E.Muted, fontSize = 13.sp)
            GlowButton({ gate = true }, Modifier.fillMaxWidth().testTag("elternbereich_oeffnen"), color = E.Violet) { Text("🔐 Elternbereich öffnen", fontWeight = FontWeight.Bold) }
        }
        AnimatedVisibility(unlocked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var aus by remember { mutableStateOf(prefs?.note("nachhilfe_aus") == "1") }
                // ── Hinweis bei objektiven Schwierigkeiten (Recht V15 (1)); nur Physik, nur ein Thema auf einmal
                val hinweis = remember(unlocked, journal, nowMs, aus) {
                    if (!unlocked || aus || nowMs <= 0L) null
                    else Nachhilfe.schwierigeThemen(journal, nowMs) { Catalog.byId(it)?.topic }
                        .firstOrNull { Nachhilfe.darfZeigen(prefs?.note("nachhilfe_" + ParentReport.slug(it))?.toLongOrNull(), nowMs, Nachhilfe.PAUSE_THEMA_TAGE) }
                }
                var weg by remember(hinweis) { mutableStateOf(false) }
                if (hinweis != null) {
                    LaunchedEffect(hinweis) { prefs?.setNote("nachhilfe_" + ParentReport.slug(hinweis), nowMs.toString()) }   // ab jetzt 4 Wochen Pause für dieses Thema
                    AnimatedVisibility(!weg) {
                        Panel(Modifier.fillMaxWidth().testTag("nachhilfe_hinweis"), border = E.Sun) {
                            Text(Nachhilfe.elternHinweis(hinweis.title.substringBefore(" (").substringBefore(",").substringBefore(" &"), nickname), color = E.Text, fontSize = 14.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlowButton({
                                    counter?.count(Nachhilfe.ZAEHL_EREIGNIS, "elternhinweis", null)
                                    open(Angebote.probestunde("elternhinweis"))
                                }, Modifier.fillMaxWidth().testTag("probestunde_hinweis"), color = E.Sun) { Text("📅 ${Nachhilfe.ELTERN_KNOPF}", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                TextButton({ weg = true }, Modifier.testTag("hinweis_ausblenden")) { Text(Nachhilfe.ELTERN_AUSBLENDEN, fontSize = 12.sp, color = E.Muted) }
                            }
                        }
                    }
                }
                // ── Klassenarbeit bald? (Hinweis auf die kostenlose Probestunde; nur Physik – Nachhilfe gibt es nur dort)
                if (!aus) {
                    Text("📝 Physik-Arbeit bald?", color = E.Text, fontWeight = FontWeight.Bold)
                    Text(Angebote.PROBESTUNDE_TEXT, color = E.Text, fontSize = 14.sp)
                    GlowButton({
                        counter?.count(Nachhilfe.ZAEHL_EREIGNIS, "klassenarbeit", null)
                        open(Angebote.probestunde("klassenarbeit"))
                    }, Modifier.fillMaxWidth().testTag("probestunde"), color = E.Sun) { Text("📅 Kostenlose Probestunde anfragen", fontWeight = FontWeight.Bold) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Nachhilfe.SCHALTER, Modifier.weight(1f), color = E.Muted, fontSize = 13.sp)
                    Switch(aus, { aus = it; prefs?.setNote("nachhilfe_aus", if (it) "1" else null) }, Modifier.testTag("nachhilfe_schalter"))
                }
                HorizontalDivider(color = E.Line)
                extra()
                // ── anonyme Zählung
                if (counter != null) {
                    HorizontalDivider(color = E.Line)
                    val on by counter.enabled.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("📊 Anonyme Nutzungszählung", color = E.Text, fontWeight = FontWeight.Bold)
                            Text("Zählt nur, wie oft die App startet und welche Elternknöpfe genutzt werden – ohne Kennung, ohne Namen, ohne Thema, ohne Lernstand. " +
                                "Der Server speichert nur Tagessummen. Hilft uns, Experimentopia zu verbessern.", color = E.Muted, fontSize = 12.sp)
                        }
                        Switch(on, { counter.setEnabled(it) }, Modifier.testTag("zaehlung_schalter"))
                    }
                }
                TextButton({ unlocked = false; ParentGateMemory.shared.close() }) { Text("Elternbereich schließen", color = E.Muted) }
            }
        }
    }
    if (gate) ParentGate(onDismiss = { gate = false }) { gate = false; unlocked = true; counter?.count("elternbereich", "einstellungen") }
}
