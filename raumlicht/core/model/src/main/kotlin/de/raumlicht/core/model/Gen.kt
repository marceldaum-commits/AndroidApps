package de.raumlicht.core.model

import de.raumlicht.core.model.Part.Txt
import kotlin.random.Random

/** Themen der Jam-Session. */
enum class JamTopic(val title: String, val emoji: String, val color: String, val kinds: List<Gen.Kind>) {
    PUNKTE("Punkte & Vektoren", "🛸", "cyan", listOf(Gen.Kind.MIDPOINT, Gen.Kind.VECTOR)),
    GERADEN("Geraden", "🔦", "green", listOf(Gen.Kind.AIM, Gen.Kind.PARAM, Gen.Kind.TRACE)),
    SCHNITT("Schnittpunkte", "✨", "yellow", listOf(Gen.Kind.CROSS)),
    EBENEN("Ebenen", "🔷", "violet", listOf(Gen.Kind.PIERCE_FLAT, Gen.Kind.PIERCE_SKEW)),
    MIX("Alles gemischt", "🎧", "pink", Gen.Kind.entries),
}

/**
 * Erzeugt aus einer Zahl (Seed) immer dieselbe Aufgabe – mit ganzzahliger Lösung, die im Raum liegt.
 * Grundlage für Jam-Session (endlos üben) und Tagesgig (jeden Tag eine neue Aufgabe für alle).
 */
object Gen {
    enum class Kind { MIDPOINT, VECTOR, AIM, PARAM, TRACE, CROSS, PIERCE_FLAT, PIERCE_SKEW }

    private val B = Bounds()
    private fun inRoom(v: V3) = (0..2).all { v.list[it] >= B.min[it] && v.list[it] <= B.max[it] }
    private fun Random.point(lo: Int = 0, pad: Int = 0) = V3(nextInt(lo + pad, 9 - pad), nextInt(lo + pad, 9 - pad), nextInt(lo, 7 - pad))
    private fun Random.dir(max: Int = 2): V3 { while (true) { val d = V3(nextInt(-max, max + 1), nextInt(-max, max + 1), nextInt(-max, max + 1)); if (!d.isZero) return d } }
    private fun gcd(a: Int, b: Int): Int = if (b == 0) kotlin.math.abs(a) else gcd(b, a % b)
    private fun V3.reduced(): V3 { val g = ints.fold(0, ::gcd); return if (g <= 1) this else V3(ints[0] / g, ints[1] / g, ints[2] / g) }
    private val normals = listOf(V3(1, 1, 1), V3(1, 1, 0), V3(1, 0, 1), V3(0, 1, 1), V3(1, 2, 2), V3(2, 1, 2), V3(1, 1, 2), V3(2, 1, 1))

    fun level(kind: Kind, seed: Long, id: String = "jam-${kind.name.lowercase()}-$seed", chapter: Int = 0): Level {
        val r = Random(seed * 31 + kind.ordinal)
        repeat(10_000) {
            build(kind, r, id, chapter)?.let { return it }
        }
        error("Keine Aufgabe für $kind/$seed gefunden")
    }

    private fun build(kind: Kind, r: Random, id: String, ch: Int): Level? = when (kind) {
        Kind.MIDPOINT -> {
            val m = r.point(1, 1); val h = r.dir(3); val a = m - h; val b = m + h
            if (!inRoom(a) || !inRoom(b)) null else Families.point(id, ch, "Mittelpunkt", "Zwischen A und B hängt ein Kabel. Flieg mich genau in die Mitte.", m, hidden = true, name = "M",
                given = listOf(listOf(Txt("A${a.fmt()}   B${b.fmt()}"))),
                tip = "Jede Koordinate einzeln mitteln: (a + b) : 2.",
                way = listOf("M = ½ · (A + B)", "M${m.fmt()}"),
                extras = { listOf(Stage.point("A", a, "cyan", "A${a.fmt()}"), Stage.point("B", b, "cyan", "B${b.fmt()}"), Stage.edges("kabel", listOf(a to b), "cyan")) })
        }
        Kind.VECTOR -> {
            val a = r.point(); val b = r.point(); val v = b - a
            if (v.ints.count { it != 0 } < 2) null else Families.vector(id, ch, "Verbindungsvektor", "Welcher Vektor bringt mich von A nach B?", a, b,
                tip = "Spitze minus Fuß: B − A, Zeile für Zeile.", way = listOf("v⃗ = B − A", "v⃗ = ${v.fmt()}"))
        }
        Kind.AIM -> {
            val a = r.point(); val z = r.point(); val d = z - a
            if (d.ints.count { it != 0 } < 2 || d.ints.any { kotlin.math.abs(it) > 6 }) null else Families.aim(id, ch, "Zielen", "Gib dem Laser in A die Richtung zum Ring.", a, listOf(z),
                // Startrichtung: liegt im Raum und trifft nicht schon
                start = listOf(V3(1, 0, 0), V3(-1, 0, 0), V3(0, 1, 0), V3(0, -1, 0), V3(0, 0, 1)).first { inRoom(a + it) && !Geo.parallel(it, d) },
                tip = "Richtungsvektor = Ziel − Stützpunkt.", way = listOf("d⃗ = Z − A = ${d.fmt()}") + if (d.reduced() != d) listOf("kürzer: ${d.reduced().fmt()}") else emptyList())
        }
        Kind.PARAM -> {
            val d = r.dir(2); val t = listOf(-3, -2, -1, 2, 3, 4).random(r); val a = r.point(); val z = a + d * t
            if (!inRoom(z) || d.ints.count { it != 0 } < 2) null else {
                val i = d.ints.indexOfFirst { it != 0 }
                Families.param(id, ch, "Parameter", "Für welches t lande ich genau im Ring?", a, d, t, -4..6,
                    tip = "Eine Zeile reicht, um t zu finden. Die anderen sind die Probe.",
                    way = listOf("x${"₁₂₃"[i]}: ${Geo.num(a.ints[i])} + t · ${if (d.ints[i] < 0) "(${Geo.num(d.ints[i])})" else Geo.num(d.ints[i])} = ${Geo.num(z.ints[i])}", "t = ${Geo.num(t)}"))
            }
        }
        Kind.TRACE, Kind.PIERCE_FLAT -> {
            val axis = if (kind == Kind.TRACE) 2 else r.nextInt(3); val k = if (kind == Kind.TRACE) 0 else r.nextInt(2, if (axis == 2) 6 else 8)
            val n = V3(if (axis == 0) 1 else 0, if (axis == 1) 1 else 0, if (axis == 2) 1 else 0)
            val s0 = r.point(); val s = V3(if (axis == 0) k else s0.ints[0], if (axis == 1) k else s0.ints[1], if (axis == 2) k else s0.ints[2])
            val d = r.dir(2); val t = r.nextInt(1, 4); val a = s - d * t
            if (s.isZero || d.ints[axis] == 0 || d.ints.count { it != 0 } < 2 || !inRoom(a)) null else {
                val text = "x${"₁₂₃"[axis]} = $k"
                Families.pierce(id, ch, if (kind == Kind.TRACE) "Spurpunkt" else "Durchstoßpunkt",
                    if (kind == Kind.TRACE) "Wo trifft der Laser den Boden? Stell den Sensor dorthin." else "Wo stößt der Laser durch die Scheibe E?", a, d, n, k,
                    tip = "In E gilt $text. Diese Zeile der Geraden liefert t.",
                    way = listOf("$text: ${Geo.num(a.ints[axis])} + t · ${if (d.ints[axis] < 0) "(${Geo.num(d.ints[axis])})" else Geo.num(d.ints[axis])} = $k → t = $t", "S = A + $t · d⃗ = ${s.fmt()}"),
                    planeText = if (kind == Kind.TRACE) "x₃ = 0 (Boden)" else text)
            }
        }
        Kind.PIERCE_SKEW -> {
            val n = normals.random(r); val s = r.point(0, 1); val rhs = (n dot s).toInt()
            val d = r.dir(2); val t = r.nextInt(1, 4); val a = s - d * t
            if (s.isZero || (n dot d) == 0.0 || d.ints.count { it != 0 } < 2 || !inRoom(a)) null else Families.pierce(id, ch, "Schräge Scheibe", "Setz die Gerade in die Ebenengleichung ein: Wo ist der Durchstoßpunkt?", a, d, n, rhs,
                tip = "Jede Koordinate der Geraden in E einsetzen, nach t auflösen.",
                way = listOf("einsetzen → ${Geo.num((n dot a).toInt())} + ${Geo.num((n dot d).toInt())} · t = $rhs", "t = $t", "S = A + $t · d⃗ = ${s.fmt()}"))
        }
        Kind.CROSS -> {
            val s = r.point(0, 1); val d1 = r.dir(2); val d2 = r.dir(2)
            val u = listOf(-3, -2, -1, 1, 2, 3).random(r); val w = listOf(-3, -2, -1, 1, 2, 3).random(r)
            val a = s - d1 * u; val b = s - d2 * w
            if (s.isZero || Geo.parallel(d1, d2) || !inRoom(a) || !inRoom(b) || d1.ints.count { it != 0 } < 2 || d2.ints.count { it != 0 } < 2 || u < 0 || w < 0) null
            else Families.crossing(id, ch, "Kreuzung", "Wo kreuzen sich die Strahlen g und h?", a, d1, b, d2,
                tip = "Gleichsetzen und aus zwei Zeilen s und t bestimmen. Die dritte Zeile ist die Probe.",
                way = listOf("gleichsetzen → s = $u, t = $w", "S = A + $u · d⃗ = ${s.fmt()}"))
        }
    }

    /** Aufgabe Nr. [n] einer Jam-Session zum Thema (für jede Sitzung andere Aufgaben über [session]). */
    fun jam(topic: JamTopic, session: Long, n: Int): Level {
        val kind = topic.kinds[Random(session * 131 + n).nextInt(topic.kinds.size)]
        return level(kind, session * 1000 + n)
    }
}

/** Tagesgig: jeden Tag eine Aufgabe, für alle dieselbe. Die Themen wechseln reihum. */
object Daily {
    const val PREFIX = "tag-"
    fun id(day: Long) = "$PREFIX$day"
    fun isDaily(id: String) = id.startsWith(PREFIX)
    fun forDay(day: Long): Level {
        val kind = Gen.Kind.entries[(day % Gen.Kind.entries.size).toInt()]
        return Gen.level(kind, day * 7919 + 13, id(day))
    }
}

/** Abzeichen – für Fortschritt, nie fürs bloße Wiederkommen. */
data class Badge(val id: String, val emoji: String, val title: String, val text: String, val earned: Boolean)

object Badges {
    fun all(stars: Map<String, Int>, jamBest: Int): List<Badge> {
        val tour = stars.filterKeys { Levels.byId(it) != null }
        val dailies = stars.keys.count { Daily.isDaily(it) }
        fun chapterDone(n: Int) = Levels.chapters.first { it.number == n }.levels.all { (tour[it.id] ?: 0) > 0 }
        val abi = Levels.all.filter { it.abi }
        return listOf(
            Badge("start", "🎬", "Erster Gig", "Ein Level geschafft", tour.isNotEmpty()),
            Badge("perfekt5", "🎯", "Rechnen statt raten", "5 Level beim ersten Versuch", tour.values.count { it == 3 } >= 5),
            Badge("perfekt20", "💎", "Präzisionsarbeit", "20 Level mit drei Sternen", tour.values.count { it == 3 } >= 20),
            Badge("geraden", "🔦", "Laser-Profi", "Kapitel 3 und 4 gespielt", chapterDone(3) && chapterDone(4)),
            Badge("ebenen", "🔷", "Glasklar", "Kapitel 5 gespielt", chapterDone(5)),
            Badge("kugel", "🪩", "Discokönig:in", "Kapitel 9 gespielt", chapterDone(9)),
            Badge("abi3", "🎓", "Abi-Luft", "3 Abi-Checks bestanden", abi.count { (tour[it.id] ?: 0) > 0 } >= 3),
            Badge("abi", "🏆", "Abi-ready", "Alle Abi-Checks bestanden", abi.all { (tour[it.id] ?: 0) > 0 }),
            Badge("halb", "🚐", "Halbe Tour", "Die Hälfte aller Gigs gespielt", tour.size * 2 >= Levels.all.size),
            Badge("tour", "🎆", "Tour geschafft", "Alle Gigs gespielt", tour.size == Levels.all.size),
            Badge("tag3", "📅", "Stammgast", "3 Tagesgigs gelöst", dailies >= 3),
            Badge("tag10", "🗓️", "Residency", "10 Tagesgigs gelöst", dailies >= 10),
            Badge("jam5", "🎧", "Im Flow", "Jam-Session: 5 in Folge beim ersten Versuch", jamBest >= 5),
            Badge("jam15", "🔥", "Unaufhaltsam", "Jam-Session: 15 in Folge", jamBest >= 15),
        )
    }
}

/** Spickzettel: die wichtigsten Formeln zum Nachschlagen, nach Kapiteln sortiert. */
data class SheetCard(val emoji: String, val title: String, val color: String, val lines: List<String>)

object Sheet {
    val cards = listOf(
        SheetCard("🛸", "Punkte & Vektoren", "cyan", listOf(
            "Verbindungsvektor: AB⃗ = B − A (Spitze minus Fuß)",
            "Mittelpunkt: M = ½ · (A + B)",
            "Schwerpunkt Dreieck: S = ⅓ · (A + B + C)",
            "Länge: |a⃗| = √(a₁² + a₂² + a₃²)")),
        SheetCard("🔦", "Geraden", "green", listOf(
            "Parameterform: g: x⃗ = A + t · d⃗",
            "Richtungsvektor durch A und B: d⃗ = B − A",
            "Punktprobe: Punkt einsetzen – alle drei Zeilen müssen dasselbe t liefern",
            "Spurpunkt: eine Koordinate null setzen, t ausrechnen")),
        SheetCard("✨", "Lage zweier Geraden", "yellow", listOf(
            "Richtungsvektoren Vielfache? → parallel oder identisch",
            "Sonst gleichsetzen: Lösung → Schnittpunkt, Widerspruch → windschief",
            "Abstand windschief: d = |(B − A) · n⃗| : |n⃗| mit n⃗ = d⃗₁ × d⃗₂")),
        SheetCard("🔷", "Ebenen", "violet", listOf(
            "Koordinatenform: E: n₁x₁ + n₂x₂ + n₃x₃ = c",
            "Normalenvektor aus Spannvektoren: n⃗ = u⃗ × v⃗",
            "c bestimmen: einen Punkt der Ebene einsetzen",
            "Durchstoßpunkt: Gerade in E einsetzen, t ausrechnen",
            "Schnittgerade zweier Ebenen: Richtung n⃗₁ × n⃗₂")),
        SheetCard("📐", "Skalarprodukt & Winkel", "orange", listOf(
            "a⃗ · b⃗ = a₁b₁ + a₂b₂ + a₃b₃",
            "senkrecht ⇔ a⃗ · b⃗ = 0",
            "Vektoren / Ebenen: cos α = |a⃗ · b⃗| : (|a⃗| · |b⃗|)",
            "Gerade und Ebene: sin α = |n⃗ · d⃗| : (|n⃗| · |d⃗|)")),
        SheetCard("📏", "Abstände & Spiegeln", "pink", listOf(
            "Punkt–Ebene: d = |n⃗ · p⃗ − c| : |n⃗|",
            "Lotfußpunkt auf Ebene: Lotgerade P + t · n⃗ in E einsetzen",
            "Lotfußpunkt auf Gerade: (P − A − t · d⃗) · d⃗ = 0",
            "Spiegelpunkt: P′ = P + 2 · PF⃗")),
        SheetCard("🪩", "Kugeln", "cyan", listOf(
            "K: (x₁ − m₁)² + (x₂ − m₂)² + (x₃ − m₃)² = r²",
            "Gerade trifft Kugel: einsetzen → quadratische Gleichung (0, 1 oder 2 Treffer)",
            "Tangentialebene in B: n⃗ = MB⃗, dann B einsetzen",
            "Schnittkreis: r′² = r² − d² (d = Abstand der Ebene von M)")),
        SheetCard("🎆", "Kreuzprodukt", "green", listOf(
            "a⃗ × b⃗ = (a₂b₃ − a₃b₂ | a₃b₁ − a₁b₃ | a₁b₂ − a₂b₁)",
            "steht senkrecht auf a⃗ und b⃗",
            "Parallelogramm: A = |a⃗ × b⃗|, Dreieck: die Hälfte",
            "Pyramide (Parallelogramm-Grundfläche): V = ⅓ · |(a⃗ × b⃗) · c⃗|, Tetraeder: ⅙")),
    )
}
