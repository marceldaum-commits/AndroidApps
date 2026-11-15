package de.raumlicht.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import de.raumlicht.core.model.*
import kotlinx.browser.document

// Farben wie in der Android-App
private val Space = Color(0xFF0A0E27); private val Deep = Color(0xFF141A3C); private val Panel = Color(0xFF1E2550)
private val TextC = Color(0xFFE8ECFF); private val Muted = Color(0xFF9AA4D6)
private val Cyan = Color(0xFF3CE8FF); private val Yellow = Color(0xFFFFD75A); private val Pink = Color(0xFFFF4FA3); private val Green = Color(0xFF5CFFA0); private val Orange = Color(0xFFFFB45A)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // eigener Bereich für die Oberfläche: Compose blendet alles andere in seinem Element aus – die 3D-Bühne liegt deshalb daneben, nicht darin
    ComposeViewport(document.getElementById("app")!!) {
        // Mitgelieferte Schrift DejaVu Sans: kann die Mathe-Zeichen, die der Standardschrift im Browser fehlen (x₃, ✓, →, √ …)
        var font by remember { mutableStateOf<FontFamily>(FontFamily.Default) }
        LaunchedEffect(Unit) { loadFonts { normal, bold -> font = FontFamily(Font("DejaVuSans", normal, FontWeight.Normal), Font("DejaVuSans-Bold", bold, FontWeight.Bold), Font("DejaVuSans-Black", bold, FontWeight.Black)); hideStartScreen() } }
        val base = Typography()
        fun TextStyle.own() = copy(fontFamily = font)
        MaterialTheme(colorScheme = darkColorScheme(background = Space, surface = Deep, primary = Cyan),
            typography = Typography(bodyLarge = base.bodyLarge.own(), bodyMedium = base.bodyMedium.own(), bodySmall = base.bodySmall.own(), labelLarge = base.labelLarge.own(), labelMedium = base.labelMedium.own(),
                titleLarge = base.titleLarge.own(), titleMedium = base.titleMedium.own())) {
            CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = font)) { App() } }
    }
}

/** Sterne je Aufgabe – liegen nur im Browser (localStorage), nichts geht an einen Server */
private fun loadStars(): Map<String, Int> = storeGet("raumlicht.sterne").orEmpty().split(';').mapNotNull { it.split('=').takeIf { p -> p.size == 2 }?.let { p -> p[0] to (p[1].toIntOrNull() ?: 0) } }.toMap()
private fun saveStars(m: Map<String, Int>) = storeSet("raumlicht.sterne", m.entries.joinToString(";") { "${it.key}=${it.value}" })

@Composable
private fun App() {
    var stars by remember { mutableStateOf(loadStars()) }
    var open by remember { mutableStateOf<Level?>(null) }
    val level = open
    if (level == null) Home(stars) { open = it }
    else LevelView(level, onBack = { open = null }) { s -> if (s > (stars[level.id] ?: 0)) { stars = stars + (level.id to s); saveStars(stars) } }
}

@Composable
private fun Home(stars: Map<String, Int>, onOpen: (Level) -> Unit) {
    LaunchedEffect(Unit) { placeStage(0.0, 0.0, 0.0, 0.0, false) }
    var area by remember { mutableStateOf(Area.GEOMETRIE) }
    Column(Modifier.fillMaxSize().background(Space).padding(horizontal = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Raumlicht", color = TextC, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text("Web-Vorschau · Version ${Version.NAME} vom ${Version.DATE}", color = Muted, fontSize = 12.sp)
            }
            Text("Sterne ${stars.values.sum()}", color = Yellow, fontWeight = FontWeight.Bold)
        }
        Text("Baustelle: Hier laufen schon alle ${Levels.all.size} Aufgaben der Tour mit der echten 3D-Bühne. Kurvenfahrt, Baumeister, Ballerbude und Co. folgen.", Modifier.padding(vertical = 8.dp), color = Orange, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Area.entries.forEach { a -> Chip(a.title, area == a, Cyan) { area = a } }
        }
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Levels.chapters.filter { it.area == area }) { ch ->
                Column(Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Text("${ch.number} · ${ch.title}", color = TextC, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(ch.topic, color = Muted, fontSize = 12.sp)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ch.levels.forEach { l -> val s = stars[l.id] ?: 0
                            Chip(l.id.substringAfter('-') + if (s > 0) " ★$s" else "", s > 0, if (l.abi) Yellow else Cyan) { onOpen(l) } }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            Text("Impressum", Modifier.clickable { openLink("https://www.mathenachhilfe-online.com/impressum/") }, color = Muted, fontSize = 12.sp)
            Text("Datenschutz", Modifier.clickable { openLink("https://www.mathenachhilfe-online.com/datenschutz/") }, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Chip(text: String, on: Boolean, color: Color, onClick: () -> Unit) {
    Text(text, Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) color.copy(alpha = 0.28f) else Panel).border(1.dp, color.copy(alpha = if (on) 1f else 0.4f), RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp), color = TextC, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

/** Eine Aufgabe: Bühne oben, Auftrag, Regler, „Los“. Wertet mit derselben Logik aus wie die Android-App. */
@Composable
private fun LevelView(level: Level, onBack: () -> Unit, onSolved: (Int) -> Unit) {
    var values by remember(level.id) { mutableStateOf(level.start) }
    var phase by remember(level.id) { mutableStateOf(Phase.AIM) }
    var attempts by remember(level.id) { mutableStateOf(0) }
    var says by remember(level.id) { mutableStateOf(level.mission) }
    LaunchedEffect(values, phase) { stageRun("RL.setState(${level.scene(values, phase)})") }
    DisposableEffect(Unit) { onDispose { placeStage(0.0, 0.0, 0.0, 0.0, false) } }
    Column(Modifier.fillMaxSize().background(Space).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ zurück", Modifier.clickable(onClick = onBack).padding(8.dp), color = Cyan, fontWeight = FontWeight.Bold)
            Text("Aufgabe ${level.id} · ${level.title}", Modifier.weight(1f).padding(start = 8.dp), color = TextC, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        // Platzhalter: Genau hier liegt die 3D-Bühne (eigenes Fenster-Element über der Oberfläche)
        Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp).border(1.5.dp, Cyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .onGloballyPositioned { c -> val o = c.positionInWindow(); val d = pixelRatio(); val inset = 3.0     // knapp innerhalb des Rahmens
                placeStage(o.x / d + inset, o.y / d + inset, c.size.width / d - 2 * inset, c.size.height / d - 2 * inset, true) })
        Text(says, Modifier.fillMaxWidth().background(Deep, RoundedCornerShape(14.dp)).padding(12.dp), color = if (phase == Phase.DONE) Green else TextC, fontSize = 15.sp)
        level.controls.forEach { c ->
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(c.name, Modifier.width(46.dp), color = Yellow, fontWeight = FontWeight.Black)
                c.start.indices.forEach { i ->
                    val v = values.getValue(c.key)[i]
                    fun set(n: Int) { if (phase != Phase.DONE) values = level.set(values, c.key, values.getValue(c.key).toMutableList().also { it[i] = n }) }
                    Row(Modifier.weight(1f).background(Deep, RoundedCornerShape(12.dp)).border(1.dp, Yellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
                        Text("−", Modifier.clickable { set(v - 1) }.padding(horizontal = 14.dp, vertical = 8.dp), color = Yellow, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(Geo.num(v * c.step) + c.suffix, Modifier.weight(1f), color = TextC, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("+", Modifier.clickable { set(v + 1) }.padding(horizontal = 14.dp, vertical = 8.dp), color = Yellow, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ says = "Tipp: ${level.tip}" }, colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Yellow)) { Text("Tipp") }
            Button({
                if (phase == Phase.DONE) { values = level.start; phase = Phase.AIM; attempts = 0; says = level.mission }
                else { attempts++; val out = level.evaluate(values); says = out.message
                    if (out.success) { phase = Phase.DONE; stageRun("RL.party(3)"); onSolved(Level.stars(attempts, false)) } }
            }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Cyan.copy(alpha = 0.3f), contentColor = TextC)) { Text(if (phase == Phase.DONE) "Noch einmal" else "Los", fontWeight = FontWeight.Black) }
        }
    }
}
