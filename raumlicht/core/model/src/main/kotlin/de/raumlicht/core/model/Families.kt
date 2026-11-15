package de.raumlicht.core.model

import de.raumlicht.core.model.Part.Col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/** Ein gegebener Laserstrahl: startet in [a], läuft in Richtung [d] und endet in [end] (Scheibe, Sensor oder Raumrand). */
data class Ray(val name: String, val a: V3, val d: V3, val color: String, val end: V3)

/** Benannte Bedingung für gezielte Rückmeldung („liegt auf g“, „liegt in E“). */
data class Check(val text: String, val ok: (V3) -> Boolean)

/**
 * Spielarten. Jede baut ein [Level] aus den Angaben der Aufgabe; die Lösung wird – wo möglich – berechnet
 * und nicht von Hand eingetragen.
 */
object Families {
    private val X = listOf("x₁", "x₂", "x₃")
    fun col(v: V3, control: String? = null) = Col(v.list.map { Geo.num(it) }, control)

    /** Welche Koordinaten stimmen schon? Verrät nicht die Lösung, zeigt aber, wo man nachrechnen muss. */
    fun matchMessage(p: V3, target: V3, what: String = "Koordinate"): String {
        val ok = (0..2).filter { kotlin.math.abs(p.list[it] - target.list[it]) < Geo.EPS }
        val bad = (0..2) - ok.toSet()
        fun names(l: List<Int>) = l.joinToString(" und ") { X[it] }
        return when (ok.size) {
            0 -> "Noch stimmt keine $what. Rechne jede Zeile einzeln."
            else -> "${names(ok)} ${if (ok.size == 1) "stimmt" else "stimmen"} ✓ – ${names(bad)} noch nicht."
        }
    }

    /** Lux fliegt zu einem Punkt, den man einstellt. [hidden]: Das Ziel sieht man erst nach dem Lösen – man muss rechnen. */
    fun point(
        id: String, chapter: Int, title: String, mission: String, target: V3, hidden: Boolean,
        tip: String, way: List<String>, bounds: Bounds = Bounds(), abi: Boolean = false,
        home: V3 = V3(bounds.max[0].toDouble(), bounds.min[1].toDouble(), 1.0), name: String = "P",
        start: V3 = V3.ZERO, given: List<List<Part>> = emptyList(),
        /** Typische Fehler: falsche Antwort → gezielter Hinweis, was schiefgelaufen ist */
        mistakes: List<Pair<V3, String>> = emptyList(),
        extras: (Phase) -> List<Obj> = { emptyList() },
    ) = Level(
        id, chapter, title, mission, bounds,
        controls = listOf(Control("P", name, ControlKind.POINT, start.ints, bounds.min, bounds.max, "yellow")),
        solution = mapOf("P" to target.ints), tip = tip, way = way, abi = abi, handle = Handle("P", V3.ZERO),
        formula = { v -> given + listOf(listOf(Txt("$name = "), col(v.vec("P"), "P"))) },
        objects = { v, ph ->
            extras(ph) + listOfNotNull(
                Stage.drone(if (ph == Phase.DONE) target else home, if (ph == Phase.DONE) "happy" else "idle"),
                if (!hidden || ph == Phase.DONE) Stage.point("ziel", target, "green", if (ph == Phase.DONE) "$name${target.fmt()}" else null, "target", guides = !hidden) else null,
                if (ph == Phase.AIM) Stage.point("marke", v.vec("P"), "yellow", style = "ghost") else null,
                if (ph == Phase.AIM) Stage.handle("griff", v.vec("P"), "yellow") else null,
            )
        },
        evaluate = { v ->
            val p = v.vec("P")
            if (p.near(target)) Outcome(true, "Punktlandung bei ${target.fmt()}!", flyTo = p)
            else Outcome(false, mistakes.firstOrNull { p.near(it.first) }?.second ?: matchMessage(p, target), flyTo = p)
        },
    )

    /** Lux steht in [a] und fliegt um den eingestellten Vektor weiter – Ziel ist [b]. */
    fun vector(
        id: String, chapter: Int, title: String, mission: String, a: V3, b: V3,
        tip: String, way: List<String>, bounds: Bounds = Bounds(), abi: Boolean = false,
        given: List<List<Part>> = emptyList(), name: String = "v⃗",
        extras: (Phase) -> List<Obj> = { emptyList() },
    ) = Level(
        id, chapter, title, mission, bounds,
        controls = listOf(Control("v", name, ControlKind.VECTOR, listOf(0, 0, 0),
            (bounds.lo - a).ints, (bounds.hi - a).ints, "pink")),
        solution = mapOf("v" to (b - a).ints), tip = tip, way = way, abi = abi, handle = Handle("v", a),
        formula = { v -> given + listOf(listOf(Txt("$name = "), col(v.vec("v"), "v"))) },
        objects = { v, ph ->
            extras(ph) + listOfNotNull(
                Stage.drone(if (ph == Phase.DONE) b else a, if (ph == Phase.DONE) "happy" else "idle"),
                Stage.point("ziel", b, "green", "B${b.fmt()}", "target"),
                Stage.point("start", a, "cyan", "A${a.fmt()}"),
                Stage.vector("pfeil", a, a + v.vec("v"), "pink", name),
                if (ph == Phase.AIM) Stage.handle("griff", a + v.vec("v"), "pink") else null,
            )
        },
        evaluate = { v ->
            val p = a + v.vec("v")
            if (p.near(b)) Outcome(true, "Genau der richtige Vektor!", flyTo = p)
            else Outcome(false, when {
                v.vec("v").near(a - b) -> "Du hast A − B gerechnet. Es heißt Spitze minus Fuß: B − A – alle Vorzeichen drehen sich um."
                !a.isZero && v.vec("v").near(b) -> "Das ist der Ortsvektor von B. Für den Weg von A nach B musst du A noch abziehen."
                else -> matchMessage(p, b, "Komponente")
            }, flyTo = p)
        },
    )

    /** Laser in [a], Richtung einstellen: Der Strahl muss alle [targets] treffen (jedes Vielfache der Richtung zählt). */
    fun aim(
        id: String, chapter: Int, title: String, mission: String, a: V3, targets: List<V3>,
        tip: String, way: List<String>, bounds: Bounds = Bounds(), abi: Boolean = false, hidden: Boolean = false,
        start: V3 = V3(1, 0, 0), name: String = "d⃗", line: String = "g", solution: V3 = targets.first() - a,
        given: List<List<Part>> = emptyList(),
        extras: (Phase) -> List<Obj> = { emptyList() },
    ): Level {
        val far = targets.maxBy { (it - a).length }
        return Level(
            id, chapter, title, mission, bounds,
            controls = listOf(Control("d", name, ControlKind.VECTOR, start.ints,
                (bounds.lo - a).ints.map { maxOf(it, -6) }, (bounds.hi - a).ints.map { minOf(it, 6) }, "cyan")),
            solution = mapOf("d" to solution.ints), tip = tip, way = way, abi = abi, handle = Handle("d", a),
            formula = { v -> given + listOf(listOf(Txt("$line: x⃗ = "), col(a), Txt(" + t · "), col(v.vec("d"), "d"))) },
            objects = { v, ph ->
                extras(ph) + listOfNotNull(
                    Stage.drone(V3(bounds.max[0].toDouble(), bounds.min[1].toDouble(), 1.0), if (ph == Phase.DONE) "happy" else "idle"),
                    Stage.point("quelle", a, "cyan", "A${a.fmt()}"),
                    Stage.vector("pfeil", a, a + v.vec("d"), "cyan", name),
                    if (ph == Phase.AIM) Stage.handle("griff", a + v.vec("d"), "cyan") else null,
                    if (ph == Phase.DONE) Stage.line("strahl", a, far - a, 0.0, 1.0, "cyan") else null,
                ) + (if (!hidden || ph == Phase.DONE) targets.mapIndexed { i, t -> Stage.point("ziel$i", t, "yellow", if (targets.size == 1) "Z${t.fmt()}" else "Z${i + 1}${t.fmt()}", "target") } else emptyList())
            },
            evaluate = { v ->
                val d = v.vec("d")
                when {
                    d.isZero -> Outcome(false, "Der Nullvektor hat keine Richtung – so kommt kein Strahl raus.")
                    targets.all { Geo.sameDirection(d, it - a) } -> Outcome(true, "Volltreffer! Jedes Vielfache dieser Richtung hätte auch getroffen.", listOf(Beam(a, far, "cyan", true)))
                    else -> {
                        val end = a + d * Geo.exit(a, d, bounds.outerLo, bounds.outerHi)
                        val msg = if (Geo.parallel(d, far - a) && targets.size == 1) "Die Richtung stimmt – aber der Strahl läuft rückwärts. Vorzeichen umdrehen!"
                        else if (!a.isZero && Geo.parallel(d, far)) "Das ist die Richtung vom Ursprung zum Ziel. Der Laser steht aber in A – zieh A ab: Z − A."
                        else "Der Strahl geht ${Geo.num(Geo.distPointLine(far, a, d))} Einheiten am Ziel vorbei."
                        Outcome(false, msg, listOf(Beam(a, end, "cyan", false)))
                    }
                }
            },
        )
    }

    /** Gerade gegeben, Parameter t einstellen: Lux fliegt zu A + t·d. [live]: Marke wandert sichtbar mit. */
    fun param(
        id: String, chapter: Int, title: String, mission: String, a: V3, d: V3, tStar: Int, range: IntRange,
        tip: String, way: List<String>, bounds: Bounds = Bounds(), abi: Boolean = false, live: Boolean = false,
        start: Int = 0, symbol: String = "t", vectorName: String = "d⃗", prefix: String = "g: x⃗ = ", lineLabel: String? = "g", hidden: Boolean = false,
        extras: (Phase) -> List<Obj> = { emptyList() },
    ): Level {
        val z = a + d * tStar
        return Level(
            id, chapter, title, mission, bounds,
            controls = listOf(Control("t", symbol, ControlKind.SCALAR, listOf(start), listOf(range.first), listOf(range.last), "pink")),
            solution = mapOf("t" to listOf(tStar)), tip = tip, way = way, abi = abi,
            formula = { v -> listOf(listOf(Txt(prefix), col(a), Txt(" + "), Num(Geo.num(v.int("t")), "t"), Txt(" · "), col(d)), (if (hidden) emptyList() else listOf(Txt("Ziel Z${z.fmt()}")))) },
            objects = { v, ph ->
                extras(ph) + listOfNotNull(
                    Stage.fullLine("g", a, d, bounds, "cyan", lineLabel),
                    Stage.point("A", a, "cyan", "A${a.fmt()}"),
                    Stage.vector("pfeil", a, a + d, "cyan", vectorName),
                    if (!hidden || ph == Phase.DONE) Stage.point("ziel", z, "green", "Z${z.fmt()}", "target") else null,
                    Stage.drone(if (ph == Phase.DONE) z else a, if (ph == Phase.DONE) "happy" else "idle"),
                    if (live && ph == Phase.AIM) Stage.point("marke", a + d * v.int("t"), "pink", style = "ghost") else null,
                )
            },
            evaluate = { v ->
                val t = v.int("t")
                val p = a + d * t
                when {
                    t == tStar -> Outcome(true, "$symbol = ${Geo.num(tStar)} – genau so oft passt der Richtungsvektor hinein.", flyTo = p)
                    t < tStar -> Outcome(false, "Zu kurz gedacht: Lux braucht ein größeres $symbol.", flyTo = p)
                    else -> Outcome(false, "Übers Ziel hinaus: Lux braucht ein kleineres $symbol.", flyTo = p)
                }
            },
        )
    }

    /** Strahlen sind gegeben, man sieht nur ihren Anfang. Der Sensor muss genau dorthin, wo es passiert (Schnitt-, Durchstoßpunkt). */
    fun sensor(
        id: String, chapter: Int, title: String, mission: String, rays: List<Ray>, answer: V3, checks: List<Check>,
        given: List<List<Part>>, tip: String, way: List<String>, bounds: Bounds = Bounds(), abi: Boolean = false,
        start: V3 = V3.ZERO, name: String = "S", mistakes: List<Pair<V3, String>> = emptyList(),
        shots: ((V3, Boolean) -> List<Beam>)? = null,
        extras: (Phase) -> List<Obj> = { emptyList() },
    ) = Level(
        id, chapter, title, mission, bounds,
        controls = listOf(Control("P", name, ControlKind.POINT, start.ints, bounds.min, bounds.max, "pink")),
        solution = mapOf("P" to answer.ints), tip = tip, way = way, abi = abi, handle = Handle("P", V3.ZERO),
        formula = { v -> given + listOf(listOf(Txt("$name = "), col(v.vec("P"), "P"))) },
        objects = { v, ph ->
            extras(ph) + rays.flatMap { r ->
                listOf(
                    Stage.point("q-${r.name}", r.a, r.color),
                    if (ph == Phase.AIM) Stage.stub("s-${r.name}", r.a, r.d, r.color, r.name) else Stage.line("s-${r.name}", r.a, r.end - r.a, 0.0, 1.0, r.color, r.name),
                )
            } + listOfNotNull(
                Stage.drone(V3(bounds.max[0].toDouble(), bounds.min[1].toDouble(), 1.0), if (ph == Phase.DONE) "happy" else "idle"),
                Stage.point("sensor", if (ph == Phase.DONE) answer else v.vec("P"), "pink", if (ph == Phase.DONE) "$name${answer.fmt()}" else null, "target"),
                if (ph == Phase.AIM) Stage.handle("griff", v.vec("P"), "pink") else null,
            )
        },
        evaluate = { v ->
            val p = v.vec("P")
            val ok = p.near(answer)
            val beams = shots?.invoke(p, ok) ?: rays.map { Beam(it.a, if (ok) it.end else it.a + it.d * Geo.exit(it.a, it.d, bounds.outerLo, bounds.outerHi), it.color, ok) }
            if (ok) Outcome(true, "Der Sensor leuchtet – genau bei ${answer.fmt()}!", beams)
            else {
                val (good, bad) = checks.partition { it.ok(p) }
                val msg = mistakes.firstOrNull { p.near(it.first) }?.second ?: when {
                    bad.isEmpty() || checks.isEmpty() -> matchMessage(p, answer)
                    good.isEmpty() -> "Dein Punkt ${bad.joinToString(" und ") { it.text }} noch nicht ✗."
                    else -> "Dein Punkt ${good.joinToString(" und ") { it.text }} ✓, aber ${bad.joinToString(" und ") { it.text }} nicht ✗."
                }
                Outcome(false, msg, beams)
            }
        },
    )

    /** Ebene in Koordinatenform einstellen (n₁x₁ + n₂x₂ + n₃x₃ = c), bis alle Punkte darin liegen. Die Glasscheibe dreht sich live mit. */
    fun plane(
        id: String, chapter: Int, title: String, mission: String, points: List<Pair<String, V3>>, n: V3, c: Int,
        tip: String, way: List<String>, bounds: Bounds = Bounds(), abi: Boolean = false, maxC: Int = 24,
        normal: V3? = null, normalHint: String = "", given: List<List<Part>> = emptyList(),
        extras: (Phase) -> List<Obj> = { emptyList() },
    ) = Level(
        id, chapter, title, mission, bounds,
        controls = listOf(
            Control("n", "n⃗", ControlKind.VECTOR, listOf(0, 0, 1), listOf(-5, -5, -5), listOf(6, 6, 6), "violet"),
            Control("c", "c", ControlKind.SCALAR, listOf(3), listOf(-maxC), listOf(maxC), "yellow"),
        ),
        solution = mapOf("n" to n.ints, "c" to listOf(c)), tip = tip, way = way, abi = abi,
        formula = { v ->
            val nn = v.getValue("n")
            given + listOf(buildList {
                add(Txt("E: "))
                nn.forEachIndexed { i, k ->
                    if (i > 0) add(Txt(if (k < 0) " − " else " + "))
                    add(Num(if (i > 0) Geo.num(kotlin.math.abs(k)) else Geo.num(k), "n")); add(Txt(X[i]))
                }
                add(Txt(" = ")); add(Num(Geo.num(v.int("c")), "c"))
            })
        },
        objects = { v, ph ->
            extras(ph) + points.map { (name, p) -> Stage.point("p-$name", p, "yellow", "$name${p.fmt()}") } + listOfNotNull(
                Stage.drone(V3(bounds.max[0].toDouble(), bounds.min[1].toDouble(), 1.0), if (ph == Phase.DONE) "happy" else "idle"),
                if (!v.vec("n").isZero) Stage.plane("E", v.vec("n"), v.int("c").toDouble(), "violet", "E") else null,
            )
        },
        evaluate = { v ->
            val nn = v.vec("n"); val cc = v.int("c").toDouble()
            if (nn.isZero) Outcome(false, "Mit n⃗ = (0|0|0) gibt es keine Ebene.")
            else {
                val (good, bad) = points.partition { Geo.onPlane(it.second, nn, cc) }
                if (bad.isEmpty() && normal != null && !Geo.parallel(nn, normal)) Outcome(false, normalHint)
                else if (bad.isEmpty()) Outcome(true, "Die Scheibe sitzt genau richtig!")
                else Outcome(false, (if (good.isEmpty()) "" else good.joinToString(", ") { it.first } + " ✓  ") + bad.joinToString(", ") { it.first } + " ✗ – setz die Punkte ein und prüfe die Gleichung.")
            }
        },
    )

    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))

    /** Durchstoßpunkt: Gerade trifft Ebene n·x = rhs. */
    fun pierce(
        id: String, chapter: Int, title: String, mission: String, a: V3, d: V3, n: V3, rhs: Int,
        tip: String, way: List<String>, planeText: String = Geo.planeText(n, rhs), abi: Boolean = false, extras: List<Obj> = emptyList(),
    ): Level {
        val s = a + d * Geo.linePlane(a, d, n, rhs.toDouble())!!
        return sensor(id, chapter, title, mission, listOf(Ray("g", a, d, "cyan", s)), s,
            listOf(Check("liegt auf g") { Geo.onLine(it, a, d) }, Check("liegt in E") { Geo.onPlane(it, n, rhs.toDouble()) }),
            given = listOf(lineText("g", a, d), listOf(Txt("E: $planeText"))), tip = tip, way = way, abi = abi,
            extras = { extras + Stage.plane("E", n, rhs.toDouble(), "violet", "E") })
    }

    /** Schnittpunkt zweier Geraden. */
    fun crossing(id: String, chapter: Int, title: String, mission: String, a: V3, d1: V3, b: V3, d2: V3, tip: String, way: List<String>, abi: Boolean = false): Level {
        val s = Geo.lineLine(a, d1, b, d2).point!!
        return sensor(id, chapter, title, mission, listOf(Ray("g", a, d1, "cyan", s), Ray("h", b, d2, "yellow", s)), s,
            listOf(Check("liegt auf g") { Geo.onLine(it, a, d1) }, Check("liegt auf h") { Geo.onLine(it, b, d2) }),
            given = listOf(lineText("g", a, d1, "s"), lineText("h", b, d2)), tip = tip, way = way, abi = abi)
    }

    /** Eine einzelne Zahl ist gesucht (Winkel, Abstand, Fläche, Radius …). */
    fun scalar(
        id: String, chapter: Int, title: String, mission: String, name: String, range: IntRange, solution: Int, start: Int,
        tip: String, way: List<String>, color: String = "pink", step: Int = 1, suffix: String = "", bounds: Bounds = Bounds(), abi: Boolean = false,
        /** Wo Lux steht (z. B. auf einer Kurve); sonst parkt sie am Rand */
        lux: ((Int, Phase) -> V3)? = null,
        formula: (Int) -> List<List<Part>>, objects: (Int, Phase) -> List<Obj>, check: (Int) -> Outcome,
    ) = Level(id, chapter, title, mission, bounds,
        listOf(Control("a", name, ControlKind.SCALAR, listOf(start), listOf(range.first), listOf(range.last), color, step, suffix)), mapOf("a" to listOf(solution)),
        tip, way, abi, formula = { formula(it.int("a")) },
        objects = { v, ph -> objects(v.int("a"), ph) + Stage.drone(lux?.invoke(v.int("a"), ph) ?: V3(bounds.max[0].toDouble(), bounds.min[1].toDouble(), 1.0), if (ph == Phase.DONE) "happy" else "idle") },
        evaluate = { check(it.int("a")) })

    /** Rückmeldung für Zahlen-Level: zu klein / zu groß, ohne die Lösung zu verraten. */
    fun compare(value: Int, solution: Int, what: String, ok: String, mistakes: Map<Int, String> = emptyMap(), beams: (Boolean) -> List<Beam> = { emptyList() }) = when {
        value == solution -> Outcome(true, ok, beams(true))
        value in mistakes -> Outcome(false, mistakes.getValue(value), beams(false))
        value < solution -> Outcome(false, "$what ist größer. Rechne noch einmal nach.", beams(false))
        else -> Outcome(false, "$what ist kleiner. Rechne noch einmal nach.", beams(false))
    }
}
