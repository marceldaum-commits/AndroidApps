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
 * Elternbereich hinter dem Eltern-Tor: Hilfe vor der Klassenarbeit (Hinweis auf die kostenlose Probestunde) und der Schalter für die anonyme Zählung.
 * Nichts davon sehen Kinder ohne die Frage für Erwachsene; Blubb wirbt nie.
 */
@Composable
fun ParentArea(todayDays: Long, grade: Int, openAtStart: Boolean = false) {
    val counter = LocalCounter.current
    val open = LocalOpenLink.current
    var unlocked by remember { mutableStateOf(openAtStart) }   // true nur für Bildschirmfotos
    var gate by remember { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth().testTag("elternbereich"), border = E.Violet) {
        Text("👪 Elternbereich", style = MaterialTheme.typography.titleMedium, color = E.Violet)
        if (!unlocked) {
            Text("Für Eltern: Hilfe vor der Physik-Arbeit und Einstellungen zur anonymen Zählung.", color = E.Muted, fontSize = 13.sp)
            GlowButton({ gate = true }, Modifier.fillMaxWidth().testTag("elternbereich_oeffnen"), color = E.Violet) { Text("🔐 Elternbereich öffnen", fontWeight = FontWeight.Bold) }
        }
        AnimatedVisibility(unlocked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ── Klassenarbeit bald? → kostenlose Probestunde
                // Nachhilfe gibt es nur in Physik (nicht in Chemie und Biologie) – darum nur Physik-Themen
                Text("📝 Physik-Arbeit bald?", color = E.Text, fontWeight = FontWeight.Bold)
                val subject = Subject.PHYSIK
                var topic by remember { mutableStateOf<Topic?>(null) }
                var klasse by remember { mutableIntStateOf(if (grade in 5..10) grade else 7) }
                ChipRow(Topic.entries.filter { it.subject == subject }, topic, { "${it.emoji} ${it.title.substringBefore(" (").substringBefore(",").substringBefore(" &")}" }, { topic = it }, Modifier.testTag("arbeit_thema"), color = subject.color())
                ChipRow((5..10).toList(), klasse, { "Kl. $it" }, { klasse = it }, color = E.Sun)
                Text(Angebote.PROBESTUNDE_TEXT, color = E.Text, fontSize = 14.sp)
                GlowButton({
                    val slug = topic?.let { ParentReport.slug(it) }
                    counter?.count("probestunde_klick", "klassenarbeit", slug)
                    open(Angebote.probestunde(slug, klasse))
                }, Modifier.fillMaxWidth().testTag("probestunde"), color = E.Sun) { Text("📅 Kostenlose Probestunde anfragen", fontWeight = FontWeight.Bold) }
                // ── anonyme Zählung
                if (counter != null) {
                    HorizontalDivider(color = E.Line)
                    val on by counter.enabled.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("📊 Anonyme Nutzungszählung", color = E.Text, fontWeight = FontWeight.Bold)
                            Text("Zählt nur, wie oft die App startet und welche Elternknöpfe genutzt werden – ohne Kennung, ohne Namen, ohne Lernstand. " +
                                "Der Server speichert nur Tagessummen. Hilft uns, Experimentopia zu verbessern.", color = E.Muted, fontSize = 12.sp)
                        }
                        Switch(on, { counter.setEnabled(it) }, Modifier.testTag("zaehlung_schalter"))
                    }
                }
                TextButton({ unlocked = false }) { Text("Elternbereich schließen", color = E.Muted) }
            }
        }
    }
    if (gate) ParentGate(todayDays, onDismiss = { gate = false }) { gate = false; unlocked = true; counter?.count("elternbereich", "einstellungen") }
}
