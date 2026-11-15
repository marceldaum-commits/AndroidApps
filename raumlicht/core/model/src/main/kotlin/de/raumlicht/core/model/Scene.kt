package de.raumlicht.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Sichtbarer Raum (Raster am Boden, Achsenlängen). */
@Serializable
data class Bounds(val min: List<Int> = listOf(0, 0, 0), val max: List<Int> = listOf(8, 8, 6)) {
    val lo get() = V3.of(min); val hi get() = V3.of(max)
    /** Etwas größer – Strahlen dürfen sichtbar aus dem Raum laufen */
    val outerLo get() = V3(min[0] - 1.0, min[1] - 1.0, 0.0); val outerHi get() = V3(max[0] + 1.5, max[1] + 1.5, max[2] + 1.5)
}

/** Ein Ding auf der 3D-Bühne. Die Felder entsprechen dem, was die Szene (scene/src/main.js) versteht. */
@Serializable
data class Obj(
    val id: String, val type: String,
    val p: List<Double>? = null, val d: List<Double>? = null,
    val from: List<Double>? = null, val to: List<Double>? = null,
    val t0: Double? = null, val t1: Double? = null,
    val n: List<Double>? = null, val rhs: Double? = null,
    val corners: List<List<Double>>? = null,
    val segments: List<List<List<Double>>>? = null, val faces: List<List<List<Double>>>? = null,
    val color: String? = null, val label: String? = null, val style: String? = null,
    val guides: Boolean? = null, val drop: Boolean? = null, val dashed: Boolean? = null, val thin: Boolean? = null,
    val mood: String? = null, val axes: List<Int>? = null,
)

@Serializable data class SceneState(val bounds: Bounds, val objects: List<Obj>)

object Stage {
    private val json = Json { explicitNulls = false; encodeDefaults = true }
    fun encode(bounds: Bounds, objects: List<Obj>): String = json.encodeToString(SceneState(bounds, objects))

    fun point(id: String, p: V3, color: String = "yellow", label: String? = null, style: String = "orb", guides: Boolean = false, drop: Boolean = true) =
        Obj(id, "point", p = p.list, color = color, label = label, style = style, guides = guides, drop = drop)
    fun vector(id: String, from: V3, to: V3, color: String = "cyan", label: String? = null) =
        Obj(id, "vector", from = from.list, to = to.list, color = color, label = label)
    fun line(id: String, p: V3, d: V3, t0: Double, t1: Double, color: String = "cyan", label: String? = null, dashed: Boolean = false, thin: Boolean = false) =
        Obj(id, "line", p = p.list, d = d.list, t0 = t0, t1 = t1, color = color, label = label, dashed = dashed, thin = thin)
    /** Ganze Gerade, soweit sie im Raum liegt */
    fun fullLine(id: String, p: V3, d: V3, b: Bounds, color: String = "cyan", label: String? = null, thin: Boolean = true) =
        line(id, p, d, -Geo.exit(p, -d, b.outerLo, b.outerHi), Geo.exit(p, d, b.outerLo, b.outerHi), color, label, thin = thin)
    /** Nur der Anfang eines Strahls – wer rechnet, weiß, wo er landet */
    fun stub(id: String, p: V3, d: V3, color: String = "cyan", label: String? = null, length: Double = 1.6) =
        line(id, p, d, 0.0, length / d.length, color, label)
    fun plane(id: String, n: V3, rhs: Double, color: String = "violet", label: String? = null) =
        Obj(id, "plane", n = n.list, rhs = rhs, color = color, label = label)
    fun pane(id: String, corners: List<V3>, color: String = "violet", label: String? = null) =
        Obj(id, "plane", corners = corners.map { it.list }, color = color, label = label)
    fun edges(id: String, segments: List<Pair<V3, V3>>, color: String = "white", faces: List<List<V3>>? = null) =
        Obj(id, "edges", segments = segments.map { listOf(it.first.list, it.second.list) }, faces = faces?.map { f -> f.map { it.list } }, color = color)
    fun drone(p: V3, mood: String = "idle") = Obj("lux", "drone", p = p.list, mood = mood)
    fun handle(id: String, p: V3, color: String) = Obj(id, "handle", p = p.list, color = color)
}

/** Bühnenbild der Startseite: Lux schwebt zwischen Lasern und einer Glasscheibe. */
object Showreel {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    fun scene(): String = Stage.encode(Bounds(), listOf(
        Stage.drone(V3(4.0, 4.0, 2.6)),
        Stage.pane("wand", listOf(p(0, 1, 0), p(0, 7, 0), p(0, 7, 5), p(0, 1, 5)), "violet"),
        Stage.line("a", p(8, 0, 0), p(-8, 5, 4), 0.0, 1.0, "cyan"),
        Stage.line("b", p(8, 8, 0), p(-8, -4, 3), 0.0, 1.0, "pink"),
        Stage.line("c", p(4, 0, 5), p(-1, 8, -3), 0.0, 1.0, "yellow", thin = true),
        Stage.point("z1", p(0, 5, 4), "cyan", style = "target", drop = false),
        Stage.point("z2", p(0, 4, 3), "pink", style = "target", drop = false),
    ))
}
