package de.raumlicht.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.level.stageColor
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.stage.StageFrame
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Coaster
import de.raumlicht.core.model.Geo
import de.raumlicht.core.model.Ride
import de.raumlicht.core.model.Spot
import kotlin.math.abs

/**
 * Achterbahn aus der Fahrersicht: Der Graph ist die Bahn, man sitzt im Wagen. Das Cockpit zeigt live Höhe f(x),
 * Steigung f′(x) und Krümmung f″(x) – so wird spürbar, was die Ableitungen bedeuten.
 * Fahren lassen (▶) oder mit dem Regler selbst vor- und zurückschieben; an besonderen Stellen meldet sich Lux.
 */
@Composable
fun RideScreen(onBack: () -> Unit, startId: String = Coaster.rides.first().id, controller: StageController = remember { StageController() }) {
    var rideId by rememberSaveable { mutableStateOf(startId) }
    val ride = Coaster.byId(rideId) ?: Coaster.rides.first()
    var u by rememberSaveable(rideId) { mutableFloatStateOf(0f) }
    var driving by remember(rideId) { mutableStateOf(false) }
    var inside by rememberSaveable { mutableStateOf(true) }
    var showD by rememberSaveable { mutableStateOf(false) }
    var showFill by rememberSaveable { mutableStateOf(false) }
    var seen by remember(rideId) { mutableStateOf<Spot?>(null) }
    val sfx = LocalSfx.current; val view = LocalView.current; val vib = LocalVibration.current
    val d = ride.data(u.toDouble())

    // Mit Zusätzen wird die Bühne in kleinen Schritten nachgeführt (Punkt auf f′ wandert mit, die Fläche wächst)
    LaunchedEffect(rideId, showD, showFill, if (showD || showFill) (u * 100).toInt() else -1) {
        controller.setState(if (showD || showFill) ride.scene(ride.x(u.toDouble()), showD, showFill) else ride.scene())
    }
    LaunchedEffect(rideId, u, inside) { controller.ride(u.toDouble(), inside) }
    DisposableEffect(controller) { onDispose { controller.rideOff() } }
    // Automatische Fahrt: bergab schneller, bergauf langsamer – wie eine echte Achterbahn
    LaunchedEffect(driving, rideId) {
        var last = 0L
        while (driving) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f); last = now
                val speed = (0.085f * (1f - 0.22f * ride.data(u.toDouble()).slope.toFloat().coerceIn(-3f, 3f))).coerceIn(0.03f, 0.2f)
                u = (u + speed * dt).coerceAtMost(1f)
                if (u >= 1f) driving = false
            }
        }
    }
    // An Hoch-, Tief-, Wendepunkten und Nullstellen: Lux meldet sich, es vibriert kurz
    LaunchedEffect(d.x) {
        val s = ride.spotNear(d.x, if (driving) 0.05 else 0.04)
        if (s != null && s != seen) { seen = s; sfx?.play(Sound.HIT); view.buzz(Buzz.HIT, vib); controller.party(1.0) }
        else if (s == null && seen != null && abs(seen!!.x - d.x) > 0.25) seen = null
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Kurve abfahren", onBack) { Text("f(x) = ${ride.text}", color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Coaster.rides.forEach { r ->
                val interaction = remember { MutableInteractionSource() }
                Text(r.title, Modifier.testTag("bahn-${r.id}").pressBounce(interaction).clip(RoundedCornerShape(12.dp)).background(if (r.id == rideId) Q.Cyan.copy(alpha = 0.3f) else Q.Deep)
                    .border(if (r.id == rideId) 2.dp else 1.dp, if (r.id == rideId) Q.Cyan else Q.Muted.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).clickable(interaction, null) { rideId = r.id }
                    .padding(horizontal = 12.dp, vertical = 7.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
            Stage(controller, Modifier.fillMaxSize())
            StageFrame(Q.Cyan)
            // Meldung an besonderen Stellen
            val s = seen
            if (s != null) Row(Modifier.align(Alignment.TopCenter).padding(12.dp).background(Q.Space.copy(alpha = 0.85f), RoundedCornerShape(16.dp)).border(1.5.dp, stageColor(s.kind.color), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Lux(LuxMood.WOW, size = 40.dp)
                Column(Modifier.padding(start = 8.dp)) {
                    Text("${s.kind.title} bei x = ${Geo.num(s.x)}", fontWeight = FontWeight.Black, color = stageColor(s.kind.color))
                    Text(s.kind.rule, fontSize = 12.sp, color = Q.Text)
                }
            }
        }
        // Cockpit
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Gauge("Stelle x", Geo.num(d.x), "", Q.Text, Modifier.weight(0.8f))
            Gauge("Höhe f(x)", Geo.num(d.height), "", Q.Cyan, Modifier.weight(1f))
            Gauge("Steigung f′(x)", "${d.arrow} ${Geo.num(d.slope)}", "${d.slopeWord} · ${Geo.num(d.angle)}°", if (abs(d.slope) < 0.05) Q.Yellow else if (d.slope > 0) Q.Green else Q.Orange, Modifier.weight(1.5f).testTag("steigung"))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Gauge("Krümmung f″(x)", Geo.num(d.bend), d.bendWord, if (abs(d.bend) < 0.05) Q.Pink else Q.Violet, Modifier.weight(1f).testTag("kruemmung"))
            if (showFill) Gauge("Fläche ∫ f(x) dx", Geo.num(ride.integral(d.x)), "vom Start bis x", Q.Yellow, Modifier.weight(1f).testTag("flaeche"))
        }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pick("f′ als zweite Bahn", showD, Q.Pink, "zeige-ableitung") { showD = !showD }
            Pick("∫ Fläche füllen", showFill, Q.Yellow, "zeige-flaeche") { showFill = !showFill }
        }
        Slider(u, { u = it; driving = false }, Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("fahrt-regler"),
            colors = SliderDefaults.colors(thumbColor = Q.Yellow, activeTrackColor = Q.Cyan, inactiveTrackColor = Q.Panel))
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton({ inside = !inside }, Modifier.testTag("sicht"), color = Q.Violet) { Text(if (inside) "👁 von außen" else "🎢 einsteigen", fontWeight = FontWeight.Bold) }
            NeonButton({ if (u >= 0.999f) u = 0f; driving = !driving; if (driving) sfx?.play(Sound.LAUNCH) }, Modifier.weight(1f).testTag("fahren"), color = Q.Green) {
                Text(if (driving) "⏸ Anhalten" else if (u >= 0.999f) "↻ Noch einmal" else "▶ Losfahren", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun Gauge(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Column(modifier.background(Q.Deep, RoundedCornerShape(14.dp)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = Q.Muted, fontSize = 11.sp)
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1)
        if (sub.isNotEmpty()) Text(sub, color = Q.Text, fontSize = 11.sp, maxLines = 1)
    }
}
