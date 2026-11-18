package de.kurvenflug.app.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.subject
import kotlin.math.cos
import kotlin.math.sin

/** Sonden-Farben, freigeschaltet durch Sterne. */
data class Skin(val name: String, val color: Color, val needStars: Int)
val Skins = listOf(Skin("Neon", Q.Cyan, 0), Skin("Plasma", Q.Pink, 10), Skin("Gold", Q.Yellow, 25), Skin("Aurora", Q.Green, 45))

/** Startseite: vier Reiter – Heute, Galaxie, Spiele, Ich – statt einer langen Liste. */
@Composable
fun HomeScreen(
    stars: Int,
    maxStars: Int,
    cells: Int,
    skin: Int,
    onSkin: (Int) -> Unit,
    chapters: List<de.kurvenflug.core.model.Chapter>,
    levelStars: Map<String, Int>,
    onLevel: (de.kurvenflug.core.model.Level) -> Unit,
    onSettings: () -> Unit,
    daily: de.kurvenflug.core.model.Level? = null,
    dailyStars: Int = 0,
    dailiesDone: Int = 0,
    bossStars: Map<Int, Int> = emptyMap(),
    onBoss: ((Int) -> Unit)? = null,
    badges: Int = 0,
    stormBest: Int = 0,
    onDuel: (() -> Unit)? = null,
    onStorm: (() -> Unit)? = null,
    onBadges: (() -> Unit)? = null,
    onBuild: (() -> Unit)? = null,
    onDetective: (() -> Unit)? = null,
    onCodex: (() -> Unit)? = null,
    onWorkshop: (() -> Unit)? = null,
    banner: (@Composable () -> Unit)? = null,
    /** Lernstand-Karte (mit Auffrisch-Hinweis) */
    learning: (@Composable () -> Unit)? = null,
    /** Klassenstufe (null = nicht gewählt → alle Kapitel) */
    grade: Int? = null,
    onGrade: ((Int?) -> Unit)? = null,
    /** Beim ersten Start nach der Klasse fragen */
    askGrade: Boolean = false,
    /** „Weiter, wo du warst“ */
    continueLevel: de.kurvenflug.core.model.Level? = null,
    onLearn: (() -> Unit)? = null,
    initialTab: Int = 0,
    /** Story: gefundene Bauteile der Aurora, Hangar öffnen */
    partsFound: Int = 0,
    onHangar: (() -> Unit)? = null,
    /** Volts Begrüßung (Laune + Satz) – sonst die Standard-Begrüßung */
    greeting: Pair<VoltMood, String>? = null,
    stickers: Int = 0,
    onStickers: (() -> Unit)? = null,
    /** Spieler: „🦊 Lena“, Spieler wechseln, Eltern-Ansicht */
    player: String? = null,
    onProfiles: (() -> Unit)? = null,
    onParent: (() -> Unit)? = null,
    /** Staffel 2: ein Crew-Mitglied sagt etwas, ein offener Notruf, Crew-Seite */
    crewLine: Pair<de.kurvenflug.core.model.CrewMember, String>? = null,
    rescueCall: de.kurvenflug.core.model.CrewMember? = null,
    crewCount: Int = 0,
    onCrew: (() -> Unit)? = null,
    /** Andromeda: aktueller Planet und Nova-Teile (null = noch nicht freigeschaltet) */
    andromeda: Pair<Int, Int>? = null,
    onAndromeda: (() -> Unit)? = null,
    /** Gast-Planet der Jahreszeit (und ob schon geschafft), Raumstation */
    season: Pair<de.kurvenflug.core.model.SeasonEvent, Boolean>? = null,
    onSeason: (() -> Unit)? = null,
    onStation: (() -> Unit)? = null,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val sfx = LocalSfx.current
    // Zurück-Taste: erst zum Reiter „Heute“ (bzw. Klassen-Auswahl schließen), nicht gleich die App verlassen
    de.kurvenflug.app.ui.fx.BackHandler(enabled = tab != 0 || picking) { if (picking) picking = false else tab = 0 }
    Box(Modifier.fillMaxSize()) {
        Starfield()
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            AnimatedContent(tab, Modifier.weight(1f), label = "tab", transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(260)) { it / 4 * dir } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 4 * dir } + fadeOut(tween(200)))
            }) { t ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (t) {
                        0 -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // dasselbe Logo wie das App-Symbol
                                        de.kurvenflug.app.ui.theme.KurvenflugLogo(Modifier.size(40.dp).padding(end = 8.dp))
                                        Text("KURVENFLUG", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Q.Cyan, letterSpacing = 4.sp, maxLines = 1)
                                    }
                                    // Pro freigeschaltet: immer sichtbar im Kopf (Vorgabe Marcel)
                                    de.kurvenflug.app.ui.pro.ProBadge(de.kurvenflug.app.LocalPro.current, Modifier.padding(vertical = 4.dp))
                                    Text("${player?.let { "$it · " } ?: ""}Mathe & Physik im All · ${grade?.let { "Klasse $it" } ?: "Klasse 5–13"}",
                                        color = Q.Muted, style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.clickable(enabled = onProfiles != null) { onProfiles?.invoke() })
                                }
                                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Einstellungen", tint = Q.Muted) }
                            }
                            banner?.invoke()
                            InstallOffer(onHome = true)
                            Volt(greeting?.first ?: if (stars == 0) VoltMood.IDLE else VoltMood.HAPPY, size = 64.dp,
                                speech = greeting?.second ?: if (stars == 0) "Hey! Ich bin Volt. Bereit für deine erste Mission? 🚀" else "Schon $stars ⭐ gesammelt. Weiter so, Commander!")
                            crewLine?.let { (m, line) -> de.kurvenflug.app.ui.story.CrewSays(m, line) }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Stat("⭐", "$stars / $maxStars", Q.Yellow, Modifier.weight(1f))
                                Stat("⚡", "$cells", Q.Yellow, Modifier.weight(1f))
                            }
                            season?.let { (e, done) -> de.kurvenflug.app.ui.story.SeasonCard(e, done) { onSeason?.invoke() } }
                            rescueCall?.let { m -> de.kurvenflug.app.ui.story.RescueCallCard(m) { onCrew?.invoke() } }
                            continueLevel?.let { l -> ContinueCard(l, chapters.firstOrNull { it.number == l.chapter }) { onLevel(l) } }
                            daily?.let { DailyCard(it, dailyStars, dailiesDone) { onLevel(it) } }
                            learning?.invoke()
                            if (onHangar != null) de.kurvenflug.app.ui.story.AuroraCard(partsFound, de.kurvenflug.core.model.Story.parts.size, onHangar)
                            andromeda?.let { (planet, nova) -> de.kurvenflug.app.ui.story.AndromedaCard(planet, nova) { onAndromeda?.invoke() } }
                            Text("Alle Kapitel findest du in der 🗺️ Galaxie.", color = Q.Muted, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.clickable { sfx?.play(Sound.TAP); tab = 1 })
                        }
                        1 -> Galaxy(chapters, levelStars, onLevel, bossStars, onBoss, grade, continueLevel) { picking = true }
                        2 -> {
                            TabTitle("🎮 Spiele", "Zum Wetteifern, Knobeln und Selberbauen")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("⚔️", "Duell", "zu zweit oder per Code", Q.Cyan, Modifier.weight(1f)) { onDuel?.invoke() }
                                ModeTile("☄️", "Sturm", if (stormBest > 0) "Rekord $stormBest" else "Welle um Welle", Q.Orange, Modifier.weight(1f)) { onStorm?.invoke() }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("🔎", "Detektiv", "Formel zur Kurve finden", Q.Violet, Modifier.weight(1f)) { onDetective?.invoke() }
                                ModeTile("🛠️", "Baukasten", "Level bauen & teilen", Q.Green, Modifier.weight(1f)) { onBuild?.invoke() }
                            }
                            Text("🏫 Lehrkräfte: Die Klassen-Challenge steckt im Baukasten.", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {
                            TabTitle("🎒 Ich", "Dein Fortschritt und deine Sammlung")
                            if (onProfiles != null) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("👥", "Spieler", player ?: "wechseln", Q.Cyan, Modifier.weight(1f), onProfiles)
                                ModeTile("👪", "Für Eltern", "Wochenübersicht", Q.Violet, Modifier.weight(1f)) { onParent?.invoke() }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("📊", "Lernstand", "Was sitzt schon?", Q.Green, Modifier.weight(1f)) { onLearn?.invoke() }
                                ModeTile("🏅", "Abzeichen", "$badges/${de.kurvenflug.core.model.Badges.all.size}", Q.Yellow, Modifier.weight(1f)) { onBadges?.invoke() }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("🛸", "Hangar", "$partsFound/${de.kurvenflug.core.model.Story.parts.size} Bauteile", Q.Orange, Modifier.weight(1f)) { onHangar?.invoke() }
                                ModeTile("📒", "Sticker", "$stickers/${de.kurvenflug.core.model.Stickers.all.size}", Q.Pink, Modifier.weight(1f)) { onStickers?.invoke() }
                            }
                            if (onCrew != null) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("🎖️", "Crew", "$crewCount/${de.kurvenflug.core.model.Season2.crew.size} gerettet", Q.Green, Modifier.weight(1f), onCrew)
                                ModeTile("🏠", "Station", "Räume & Deko", Q.Yellow, Modifier.weight(1f)) { onStation?.invoke() }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("🪐", "Lexikon", "${de.kurvenflug.core.model.Codex.unlocked(stars).size}/${de.kurvenflug.core.model.Codex.all.size}", Q.Pink, Modifier.weight(1f)) { onCodex?.invoke() }
                                ModeTile("🎨", "Werkstatt", "Sonde & Volt", Q.Orange, Modifier.weight(1f)) { onWorkshop?.invoke() }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ModeTile("🎓", "Klasse", grade?.let { "Klasse $it" } ?: "alle", Q.Cyan, Modifier.weight(1f)) { picking = true }
                                ModeTile("⚙️", "Einstellungen", "Ton, Musik, Update", Q.Muted, Modifier.weight(1f), onSettings)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            NavigationBar(containerColor = Q.Panel.copy(alpha = 0.96f), tonalElevation = 0.dp) {
                listOf("🚀" to "Heute", "🗺️" to "Galaxie", "🎮" to "Spiele", "🎒" to "Ich").forEachIndexed { i, (icon, label) ->
                    val selected = tab == i
                    val scale by animateFloatAsState(if (selected) 1.25f else 1f, spring(dampingRatio = 0.4f), label = "tabIcon")
                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (tab != i) { sfx?.play(Sound.TICK); tab = i } },
                        icon = { Text(icon, fontSize = 22.sp, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) },
                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Q.Cyan.copy(alpha = 0.2f),
                            selectedTextColor = Q.Cyan, unselectedTextColor = Q.Muted),
                    )
                }
            }
        }
        if ((askGrade || picking) && onGrade != null) GradePicker(grade, first = askGrade && !picking) { g -> picking = false; onGrade(g) }
    }
}

@Composable
private fun TabTitle(title: String, sub: String) {
    Column {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Q.Text)
        Text(sub, color = Q.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** Große Karte „Weiter, wo du warst“ – ein Tipp und es geht los. */
@Composable
private fun ContinueCard(l: de.kurvenflug.core.model.Level, c: de.kurvenflug.core.model.Chapter?, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    val bob by rememberInfiniteTransition(label = "rocket").animateFloat(-4f, 4f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Row(
        Modifier.fillMaxWidth().pressBounce(interaction).pulse(true, 0.012f).clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Q.Cyan.copy(alpha = 0.35f), Q.Panel)))
            .border(2.dp, Q.Cyan, RoundedCornerShape(22.dp))
            .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🚀", fontSize = 40.sp, modifier = Modifier.graphicsLayer { translationY = bob })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Weiter geht's!", fontWeight = FontWeight.Black, color = Q.Text, fontSize = 20.sp)
            Text("${c?.emoji ?: ""} ${c?.title ?: ""} · Level ${l.number}: ${l.title}", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Text("Los ›", color = Q.Cyan, fontWeight = FontWeight.Black)
    }
}

/** Galaxie: Kapitel nach Klasse geordnet, eingeklappt mit Fortschrittsring, Filter Mathe/Physik. */
@Composable
private fun Galaxy(
    chapters: List<de.kurvenflug.core.model.Chapter>, stars: Map<String, Int>, onLevel: (de.kurvenflug.core.model.Level) -> Unit,
    bossStars: Map<Int, Int>, onBoss: ((Int) -> Unit)?, grade: Int?, continueLevel: de.kurvenflug.core.model.Level?, onPickGrade: () -> Unit,
) {
    var subject by rememberSaveable { mutableStateOf<de.kurvenflug.core.model.Subject?>(null) }
    var open by rememberSaveable { mutableStateOf(listOfNotNull(continueLevel?.chapter)) }
    val sfx = LocalSfx.current
    var asMap by rememberSaveable { mutableStateOf(true) }
    TabTitle("🗺️ Galaxie", "${chapters.size} Planeten · Level für Level")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = asMap, onClick = { sfx?.play(Sound.TICK); asMap = true }, label = { Text("🪐 Karte") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Violet.copy(alpha = 0.35f), labelColor = Q.Muted, selectedLabelColor = Q.Text))
        FilterChip(selected = !asMap, onClick = { sfx?.play(Sound.TICK); asMap = false }, label = { Text("📋 Liste") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Violet.copy(alpha = 0.35f), labelColor = Q.Muted, selectedLabelColor = Q.Text))
    }
    AssistChip(onClick = { sfx?.play(Sound.TAP); onPickGrade() }, label = { Text("🎓 ${grade?.let { "Klasse $it · ändern" } ?: "Klasse wählen"}") },
        colors = AssistChipDefaults.assistChipColors(labelColor = Q.Text))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(null, de.kurvenflug.core.model.Subject.MATH, de.kurvenflug.core.model.Subject.PHYSICS).forEach { s ->
            FilterChip(selected = subject == s, onClick = { sfx?.play(Sound.TICK); subject = s },
                label = { Text(s?.let { "${it.emoji} ${it.label}" } ?: "Alle") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Cyan.copy(alpha = 0.25f), labelColor = Q.Muted, selectedLabelColor = Q.Text))
        }
    }
    if (asMap) {
        de.kurvenflug.app.ui.story.GalaxyMap(chapters.filter { subject == null || it.subject == subject }, chapters, stars, grade,
            continueLevel?.chapter, onLevel, bossStars, onBoss)
        return
    }
    de.kurvenflug.core.model.Plan.sections(chapters, grade, subject).forEach { sec ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(sec.title, fontWeight = FontWeight.Black, color = Q.Text, fontSize = 18.sp)
            Text(sec.hint, color = Q.Muted, style = MaterialTheme.typography.labelSmall)
        }
        sec.chapters.forEach { c ->
            val index = chapters.indexOf(c)
            val expanded = c.number in open
            CompactChapter(c, index, stars, expanded) { sfx?.play(Sound.TAP); open = if (expanded) open - c.number else open + c.number }
            AnimatedVisibility(expanded, enter = expandVertically(spring(dampingRatio = 0.8f)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                de.kurvenflug.app.ui.flight.ChapterCard(c, index, stars, onLevel, bossStars[c.number] ?: 0, onBoss?.let { b -> { b(c.number) } }, header = false)
            }
        }
    }
}

private val ringColors = listOf(Q.Cyan, Q.Pink, Q.Yellow, Q.Green, Q.Violet, Q.Orange)

/** Eingeklapptes Kapitel: Fortschrittsring, Titel, Klasse – antippen klappt die Level auf. */
@Composable
private fun CompactChapter(c: de.kurvenflug.core.model.Chapter, index: Int, stars: Map<String, Int>, expanded: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val color = ringColors[index % ringColors.size]
    val got = c.levels.sumOf { stars[it.id] ?: 0 }
    val share by animateFloatAsState(got.toFloat() / (c.levels.size * 3), tween(700), label = "ring")
    val turn by animateFloatAsState(if (expanded) 90f else 0f, spring(dampingRatio = 0.6f), label = "chevron")
    Row(
        Modifier.fillMaxWidth().pressBounce(interaction).clip(RoundedCornerShape(18.dp)).background(Q.Panel.copy(alpha = 0.9f))
            .border(1.5.dp, color.copy(alpha = if (expanded) 0.9f else 0.35f), RoundedCornerShape(18.dp))
            .clickable(interaction, null, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.08f), style = Stroke(5.dp.toPx()))
                drawArc(color, -90f, 360f * share, false, style = Stroke(5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            Text(c.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.title, fontWeight = FontWeight.Bold, color = Q.Text)
            Text("${c.grades} · $got/${c.levels.size * 3} ⭐", style = MaterialTheme.typography.labelSmall, color = color)
        }
        Text("›", fontSize = 24.sp, color = color, fontWeight = FontWeight.Black, modifier = Modifier.graphicsLayer { rotationZ = turn })
    }
}

/** Volt fragt nach der Klasse – beim ersten Start und jederzeit über 🎓 änderbar. */
@Composable
private fun GradePicker(current: Int?, first: Boolean, onPick: (Int?) -> Unit) {
    val sfx = LocalSfx.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(20.dp).clip(RoundedCornerShape(24.dp)).background(Q.Panel).border(2.dp, Q.Cyan, RoundedCornerShape(24.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Volt(VoltMood.HAPPY, size = 56.dp, speech = if (first) "Hi! In welcher Klasse bist du? Dann zeige ich dir zuerst, was gerade dran ist. 🎓"
                else "In welcher Klasse bist du jetzt?")
            de.kurvenflug.core.model.Plan.grades.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { g ->
                        NeonButton(onClick = { sfx?.play(Sound.HIT); onPick(g) }, color = if (g == current) Q.Green else Q.Cyan, modifier = Modifier.weight(1f)) {
                            Text("$g", fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }
                    }
                }
            }
            Text("Alle Klassen zeigen", color = Q.Muted, modifier = Modifier.align(Alignment.End)
                .clickable { sfx?.play(Sound.TAP); onPick(null) }.padding(6.dp))
            Text("Die Klasse sperrt nichts, sie sortiert nur. Eltern: Die Angabe bleibt auf dem Gerät.",
                color = Q.Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Stat(icon: String, value: String, color: Color, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(Q.Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp); Spacer(Modifier.width(8.dp))
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

/** Tagesmission: jeden Tag eine neue, für alle gleich. Kein Druck – verpasste Tage verfallen einfach. */
@Composable
private fun DailyCard(l: de.kurvenflug.core.model.Level, stars: Int, done: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    val kind = when (l.kind) {
        de.kurvenflug.core.model.Kind.LINE -> "Gerade 📈"
        de.kurvenflug.core.model.Kind.PARABOLA -> "Parabel 🌙"
        de.kurvenflug.core.model.Kind.EXP -> "Wachstum 🦠"
        de.kurvenflug.core.model.Kind.THROW -> "Wurf ${l.planet.emoji}"
        else -> "Überraschung"
    }
    val wobble by rememberInfiniteTransition(label = "sat").animateFloat(-10f, 10f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w")
    Row(
        Modifier.fillMaxWidth().pressBounce(interaction).pulse(stars == 0, 0.015f).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Q.Violet.copy(alpha = 0.35f), Q.Panel)))
            .border(2.dp, if (stars == 0) Q.Violet else Q.Green.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🛰️", fontSize = 36.sp, modifier = Modifier.graphicsLayer { rotationZ = wobble })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Tagesmission", style = MaterialTheme.typography.titleMedium, color = Q.Text)
            Text("Heute: $kind · jeden Tag neu", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
            if (done > 0) Text("Schon $done Missionen geschafft", style = MaterialTheme.typography.labelSmall, color = Q.Violet)
        }
        Text(if (stars > 0) "★".repeat(stars) + "☆".repeat(3 - stars) else "Los ›", color = if (stars > 0) Q.Yellow else Q.Violet, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ModeTile(emoji: String, title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    Column(
        modifier.pressBounce(interaction).clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.14f))
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 28.sp)
        Text(title, fontWeight = FontWeight.Bold, color = Q.Text)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = Q.Muted)
    }
}
