package de.experimentopia.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*

/**
 * Tipp für Klasse 9–10 – ohne Tor, sachlich, höchstens 1× pro Woche, wegklickbar (Recht V15 (3); Wortlaut verbindlich).
 * Kein Preis, keine Dringlichkeit, keine Figur. Nur Physik (Nachhilfe gibt es nur dort) und nur, wenn die Klasse bekannt ist.
 * Ob und wann der Tipp gezeigt wurde, merkt sich nur dieses Gerät. Gezählt wird anonym, ohne Thema.
 */
@Composable
fun NachhilfeTipp(subject: Subject, grade: Int, prefs: Prefs?, nowMs: Long, forceShow: Boolean = false) {
    val counter = LocalCounter.current
    val open = LocalOpenLink.current
    val show = remember(subject, grade) {
        forceShow || (subject == Subject.PHYSIK && Nachhilfe.tippFuerKlasse(grade) && nowMs > 0 && prefs?.note("nachhilfe_aus") != "1" &&
            Nachhilfe.darfZeigen(prefs?.note("nachhilfe_tipp")?.toLongOrNull(), nowMs, Nachhilfe.PAUSE_TIPP_TAGE))
    }
    var weg by remember(subject, grade) { mutableStateOf(false) }
    if (show && !weg) {
        LaunchedEffect(Unit) { prefs?.setNote("nachhilfe_tipp", nowMs.toString()) }       // ab jetzt 1 Woche Pause
        Panel(Modifier.fillMaxWidth().testTag("nachhilfe_tipp"), border = E.Line) {
            Text(Nachhilfe.TIPP_OBERSTUFE, color = E.Text, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton({
                    counter?.count(Nachhilfe.ZAEHL_EREIGNIS, "oberstufe_tipp", null)
                    open(Angebote.probestunde("oberstufe_tipp"))
                }, Modifier.testTag("tipp_mehr")) { Text(Nachhilfe.TIPP_KNOPF, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                TextButton({ weg = true }, Modifier.testTag("tipp_zu")) { Text("Schließen", color = E.Muted) }
            }
        }
    }
}
