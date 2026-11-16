package de.raumlicht.app.ui.stage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.core.model.Beam
import de.raumlicht.core.model.V3
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Fernbedienung für die 3D-Bühne (assets/scene). Die Bühne zeichnet nur; gerechnet und geprüft wird in core/model.
 * Befehle, die vor dem Laden ankommen, werden gesammelt und danach ausgeführt.
 */
class StageController {
    /** Führt einen Befehl in der Bühne aus – Android: Web-Ansicht, Browser: eingebettetes Fenster. null = gerade keine Bühne da. */
    private var runner: ((String) -> Unit)? = null
    private var ready = false
    private var state: String? = null
    private val queue = ArrayList<String>()

    /** Griff wurde in der Bühne gezogen: neue (ganzzahlige) Position */
    var onDrag: ((id: String, p: List<Int>) -> Unit)? = null
    /** Eine Kugel wurde kurz angetippt */
    var onTap: ((id: String) -> Unit)? = null
    /** Kurz angetippt, aber keine Kugel getroffen: die Stelle in der Ebene x₁ = 4 */
    var onMiss: ((p: V3) -> Unit)? = null
    /** Zuletzt gesetzter Zustand (für Tests) */
    val lastState get() = state
    val log = ArrayList<String>()
    /** Fehlermeldung der Bühne (JavaScript/WebGL) – wird über der Bühne angezeigt, damit man sie melden kann */
    var problem by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    val isReady get() = ready
    /** Selbstauskunft der Bühne (Gerät, Größe, Farben) */
    var info by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    fun setState(json: String) { state = json; if (ready) run("RL.setState($json)") }
    fun fire(b: Beam, ms: Int) = send("RL.fire({from:${js(b.from)},to:${js(b.to)},color:'${b.color}',ms:$ms,hit:${b.hit}})")
    fun burst(p: V3, color: String) = send("RL.burst(${js(p)},'$color')")
    fun look(trail: String, hat: String) = send("RL.look({trail:'$trail',hat:'$hat'})")
    fun skin(color: String) = send("RL.skin('$color')")
    /** Mitfahren auf der Bahn „bahn“: u in 0..1; inside = Fahrersicht, sonst Blick von außen mit Wagen */
    fun ride(u: Double, inside: Boolean) { val cmd = "RL.ride({id:'bahn',u:$u,view:'${if (inside) "fp" else "out"}'})"; lastRide = cmd; send(cmd) }
    fun rideOff() { lastRide = null; send("RL.ride(null)") }
    var lastRide: String? = null
        private set
    fun party(seconds: Double = 4.0) = send("RL.party($seconds)")
    fun camera(az: Double? = null, el: Double? = null, dist: Double? = null) =
        send("RL.setCamera({${listOfNotNull(az?.let { "az:$it" }, el?.let { "el:$it" }, dist?.let { "dist:$it" }).joinToString(",")}})")

    private fun js(v: V3) = "[${v.x},${v.y},${v.z}]"
    private fun send(cmd: String) {
        // Fahrbefehle kommen in jedem Bild – nur der letzte zählt, sie werden nicht protokolliert oder gestapelt
        if (cmd.startsWith("RL.ride({")) { if (ready) run(cmd) else { queue.removeAll { it.startsWith("RL.ride({") }; queue += cmd }; return }
        log += cmd; if (ready) run(cmd) else queue += cmd
    }
    private fun run(cmd: String) { runner?.invoke(cmd) }

    fun attach(run: (String) -> Unit) { runner = run; ready = false }
    fun detach() { runner = null; ready = false }
    /** Zählt hoch, wenn die Bühne neu aufgebaut werden muss */
    var generation by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    /** Der Darstellungsprozess ist weg: alte Ansicht loslassen, neue anfordern. Der letzte Bühnen-Zustand wird nach dem Start wieder gezeigt. */
    fun rendererGone() { runner = null; ready = false; queue.clear(); generation++ }
    fun report(msg: String) { if (problem == null) problem = msg }
    fun tell(json: String) { info = json }
    fun onEvent(json: String) {
        val o = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return
        when (o["type"]?.jsonPrimitive?.content) {
            "ready" -> { ready = true; state?.let { run("RL.setState($it)") }; queue.forEach(::run); queue.clear() }
            "info" -> info = json
            "error" -> if (problem == null) problem = o["msg"]?.jsonPrimitive?.content
            "miss" -> o.getValue("p").jsonArray.map { it.jsonPrimitive.content.toDouble() }.let { onMiss?.invoke(V3(it[0], it[1], it[2])) }
            "tap" -> onTap?.invoke(o.getValue("id").jsonPrimitive.content)
            "drag" -> onDrag?.invoke(o.getValue("id").jsonPrimitive.content, o.getValue("p").jsonArray.map { it.jsonPrimitive.int })
        }
    }
    fun pause(on: Boolean) { if (ready) run(if (on) "RL.pause()" else "RL.resume()") }
}

/** Wie die Bühne gezeichnet wird – in Tests durch ein Standbild ersetzt (Robolectric hat kein WebGL). */
val LocalStageRenderer = staticCompositionLocalOf<@Composable (StageController, Modifier) -> Unit> { { c, m -> PlatformStage(c, m) } }

/** Leuchtspur und Aufsatz von Lux (Werkstatt) */
val LocalLuxLook = staticCompositionLocalOf { "none" to "none" }

/** Farbe von Lux' Leuchtband (Werkstatt) */
val LocalLuxSkin = staticCompositionLocalOf { "pink" }

@Composable
fun Stage(controller: StageController, modifier: Modifier = Modifier) {
    val skin = LocalLuxSkin.current; val look = LocalLuxLook.current
    androidx.compose.runtime.LaunchedEffect(controller, skin, look) { controller.skin(skin); controller.look(look.first, look.second) }
    LocalStageRenderer.current(controller, modifier)
}

/** Die echte Bühne der jeweiligen Plattform: Android bettet eine Web-Ansicht ein, der Browser ein eigenes Fenster-Element. */
@Composable
expect fun PlatformStage(controller: StageController, modifier: Modifier)

/** Bemerkenswertes aus der Bühne (z. B. „neu gestartet“) – die App kann es für den Abbruch-Bericht festhalten */
var stageNote: (String) -> Unit = {}

/**
 * Runde Ecken für die Bühne, ohne die Web-Ansicht zu beschneiden: Die vier Ecken werden in der Hintergrundfarbe
 * übermalt, darüber liegt der leuchtende Rand. So bleibt die Bühne auf jedem Gerät sichtbar.
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.StageFrame(accent: androidx.compose.ui.graphics.Color, radius: androidx.compose.ui.unit.Dp = 22.dp) {
    val bg = de.raumlicht.app.ui.theme.Q.Space
    androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
        val r = radius.toPx()
        val round = androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(r))
        val corners = androidx.compose.ui.graphics.Path().apply {
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)); addRoundRect(round)
        }
        drawPath(corners, bg)
        drawRoundRect(accent.copy(alpha = 0.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
    }
}

/** Kurzer Hinweis über der Bühne, wie man sie dreht und zoomt (Finger oder Maus) */
expect val orbitHint: String
