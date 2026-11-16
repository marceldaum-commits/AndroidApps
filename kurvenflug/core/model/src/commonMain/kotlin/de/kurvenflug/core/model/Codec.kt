package de.kurvenflug.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Regler und feste Werte je Bahnart – für den Baukasten und für geteilte Level. */
object Templates {
    val buildable = listOf(Kind.POINT, Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP, Kind.THROW, Kind.ORBIT)

    fun label(k: Kind) = when (k) {
        Kind.POINT -> "📍 Koordinaten"; Kind.LINE -> "📈 Gerade"; Kind.PARABOLA -> "🌙 Parabel"; Kind.SINE -> "🌊 Sinus"
        Kind.EXP -> "🦠 Exponentiell"; Kind.THROW -> "🎯 Wurf"; Kind.ORBIT -> "🪐 Schleuder"; Kind.RENDEZVOUS -> "🤝 Rendezvous"
        Kind.PROP -> "⚖️ Proportional"; Kind.ANTI -> "⚖️ Antiproportional"; Kind.LOG -> "🔢 Logarithmus"; Kind.RADIUS -> "📡 Pythagoras"
        Kind.LASER -> "🔦 Laser"; Kind.RAMP -> "🎢 Rampe"; Kind.MOTION -> "⏱️ Weg & Zeit"
        Kind.TANGENT -> "🏂 Tangente"; Kind.PEAK -> "⛰️ Extrempunkt"; Kind.AREA -> "⛽ Integral"; Kind.VECTOR -> "🧭 Vektor"
        Kind.ACCEL -> "🚗 Beschleunigung"; Kind.POWER -> "📊 Potenz"; Kind.EXPE -> "📈 e-Funktion"
    }

    fun params(k: Kind): List<Param> = when (k) {
        Kind.POINT -> listOf(Param("x", "x", -9.0, 9.0, 1.0, 0.0), Param("y", "y", -9.0, 9.0, 1.0, 0.0))
        Kind.LINE -> listOf(Param("m", "m", -5.0, 5.0, 0.5, 0.0), Param("b", "b", -9.0, 9.0, 1.0, 0.0))
        Kind.PARABOLA -> listOf(Param("a", "a", -3.0, 3.0, 0.25, 1.0), Param("d", "d", -8.0, 8.0, 1.0, 0.0), Param("e", "e", -9.0, 9.0, 1.0, 0.0))
        Kind.SINE -> listOf(Param("a", "a", -5.0, 5.0, 0.5, 1.0), Param("b", "b", 0.25, 3.0, 0.25, 1.0), Param("d", "d", -8.0, 8.0, 1.0, 0.0))
        Kind.EXP -> listOf(Param("a", "a", 0.25, 4.0, 0.25, 1.0), Param("b", "b", 0.25, 4.0, 0.25, 1.0))
        Kind.THROW -> listOf(Param("angle", "Winkel", 5.0, 85.0, 1.0, 45.0, "°"), Param("v", "Tempo", 1.0, 20.0, 0.5, 8.0, "m/s"))
        Kind.ORBIT -> listOf(Param("angle", "Winkel", -80.0, 80.0, 1.0, 0.0, "°"), Param("v", "Tempo", 1.0, 10.0, 0.5, 4.0))
        Kind.RENDEZVOUS -> listOf(Param("x", "x", -9.0, 9.0, 0.5, 0.0), Param("y", "y", -9.0, 9.0, 0.5, 0.0))
        Kind.PROP, Kind.ANTI -> listOf(Param("k", "k", -20.0, 20.0, 0.25, 1.0))
        Kind.LOG -> listOf(Param("b", "b", 0.5, 10.0, 0.5, 10.0), Param("d", "d", -8.0, 8.0, 1.0, 0.0))
        Kind.RADIUS -> listOf(Param("r", "Radius r", 0.5, 15.0, 0.1, 1.0))
        Kind.LASER -> listOf(Param("angle", "Winkel", -89.0, 89.0, 1.0, 0.0, "°"))
        Kind.RAMP -> listOf(Param("h", "Höhe h", 0.5, 17.5, 0.5, 5.0, " m"))
        Kind.MOTION -> listOf(Param("v", "v", -3.0, 3.0, 0.25, 1.0, " m/s"), Param("s0", "s₀", -9.0, 9.0, 1.0, 0.0, " m"))
        Kind.TANGENT -> listOf(Param("m", "m", -8.0, 8.0, 0.5, 0.0))
        Kind.PEAK -> listOf(Param("x", "x", -9.0, 9.0, 0.5, 0.0))
        Kind.AREA -> listOf(Param("A", "Treibstoff A", 0.0, 40.0, 0.25, 1.0))
        Kind.VECTOR -> listOf(Param("v1", "v₁", -9.0, 9.0, 0.5, 1.0), Param("v2", "v₂", -9.0, 9.0, 0.5, 1.0))
        Kind.ACCEL -> listOf(Param("a", "a", -4.0, 4.0, 0.5, 0.5, " m/s²"), Param("v0", "v₀", -2.0, 8.0, 0.5, 1.0, " m/s"))
        Kind.POWER -> listOf(Param("a", "a", -3.0, 3.0, 0.5, 1.0))
        Kind.EXPE -> listOf(Param("a", "a", 0.5, 9.0, 0.5, 1.0), Param("k", "k", -2.0, 2.0, 0.1, 0.0))
    }

    fun fixed(k: Kind): Map<String, Double> = if (k == Kind.EXP) mapOf("d" to 0.0) else emptyMap()

    fun launch(k: Kind) = if (k == Kind.ORBIT) Vec(-9.0, 0.0) else Vec(-9.0, -9.0)

    fun tip(k: Kind) = when (k) {
        Kind.POINT -> "Erst x (rechts/links), dann y (oben/unten)."
        Kind.LINE -> "b ist der Wert bei x = 0, m die Steigung pro Kästchen."
        Kind.PARABOLA -> "Der Scheitel ist (d | e), a macht die Form."
        Kind.SINE -> "a ist die Amplitude, b staucht die Welle, d schiebt sie hoch."
        Kind.EXP -> "Bei x = 0 ist y = a. Pro Kästchen wird mit b malgenommen."
        Kind.THROW -> "Wurfweite auf ebenem Boden: w = v² · sin(2α) : g."
        Kind.ORBIT -> "Planeten ziehen dich an – nah vorbei biegt stark."
        Kind.RENDEZVOUS -> "Beide Gleichungen gleichsetzen."
        Kind.PROP -> "Proportional: y : x ist immer gleich k."
        Kind.ANTI -> "Antiproportional: x · y ist immer gleich k."
        Kind.LOG -> "log_b(x) fragt: b hoch wie viel ergibt x?"
        Kind.RADIUS -> "Abstand² = Δx² + Δy²."
        Kind.LASER -> "Einfallswinkel = Ausfallswinkel."
        Kind.RAMP -> "m·g·h = ½·m·v²."
        Kind.MOTION -> "s = v · t + s₀."
        Kind.TANGENT -> "Tangente: m = f'(x₀)."
        Kind.PEAK -> "Extrempunkt: f'(x) = 0."
        Kind.AREA -> "Fläche = F(b) − F(0)."
        Kind.VECTOR -> "v = (Q − P) : t."
        Kind.ACCEL -> "s = ½ · a · t² + v₀ · t."
        Kind.POWER -> "y = a · xⁿ."
        Kind.EXPE -> "y = a · e^(k·x), umkehren mit ln."
    }
}

/** Ein selbst gebautes Level in kompakter Form (so wird es geteilt). */
@Serializable
data class LevelSpec(
    val kind: String,
    val title: String,
    val task: String,
    /** je Objekt [x, y, r] */
    val targets: List<List<Double>>,
    val rocks: List<List<Double>> = emptyList(),
    val cells: List<List<Double>> = emptyList(),
    /** Masse je Asteroid (Schleuder), sonst leer */
    val masses: List<Double> = emptyList(),
    val planet: String = "EARTH",
    val solution: Map<String, Double>,
    val author: String = "",
    val hidden: Boolean = false,
    /** Startpunkt [x, y] (Wurf/Schleuder), leer = Standard */
    val launch: List<Double> = emptyList(),
) {
    fun toLevel(id: String): Level? = runCatching {
        val k = Kind.valueOf(kind)
        if (k !in Templates.buildable) return null
        fun body(l: List<Double>) = Body(l[0], l[1], l.getOrElse(2) { 0.7 })
        Level(
            id = id, chapter = 0, number = 0, title = title.take(40).ifBlank { "Eigenes Level" }, kind = k,
            task = task.take(200).ifBlank { "Triff alle Stationen!" },
            targets = targets.take(MAX_TARGETS).map(::body), params = Templates.params(k), solution = solution,
            fixed = Templates.fixed(k), asteroids = rocks.take(MAX_ROCKS).map(::body), cells = cells.take(MAX_CELLS).map(::body),
            masses = masses.take(MAX_ROCKS), planet = Planet.valueOf(planet), launch = launch.takeIf { it.size == 2 }?.let { Vec(it[0], it[1]) } ?: Templates.launch(k), hidden = hidden, tip = Templates.tip(k),
        )
    }.getOrNull()

    companion object {
        const val MAX_TARGETS = 3
        const val MAX_ROCKS = 6
        const val MAX_CELLS = 4

        fun of(l: Level, author: String = "") = LevelSpec(
            kind = l.kind.name, title = l.title, task = l.task,
            targets = l.targets.map { listOf(it.x, it.y, it.r) }, rocks = l.asteroids.map { listOf(it.x, it.y, it.r) },
            cells = l.cells.map { listOf(it.x, it.y, it.r) }, masses = l.masses, planet = l.planet.name,
            solution = l.solution, author = author, hidden = l.hidden,
            launch = if (l.launch != Templates.launch(l.kind)) listOf(l.launch.x, l.launch.y) else emptyList(),
        )
    }
}

/** Eine Klassen-Challenge: mehrere Level (eingebaute per Nummer oder eigene) mit Titel. */
@Serializable
data class ChallengeSpec(
    val title: String,
    /** eingebaute Level-IDs („2-6“) */
    val refs: List<String> = emptyList(),
    val custom: List<LevelSpec> = emptyList(),
    /** Reihenfolge: „r0“ = refs[0], „c1“ = custom[1] */
    val order: List<String> = emptyList(),
) {
    fun levels(): List<Level> = order.mapNotNull { o ->
        val i = o.drop(1).toIntOrNull() ?: return@mapNotNull null
        when (o.firstOrNull()) {
            'r' -> refs.getOrNull(i)?.let { Levels.byId(it) }
            'c' -> custom.getOrNull(i)?.toLevel("challenge-c$i")
            else -> null
        }
    }
}

/** Ergebnis einer Klassen-Challenge zum Teilen an die Lehrkraft. [check] = Prüfcode wie auf der Ergebnis-Karte. */
@kotlinx.serialization.Serializable
data class ChallengeResult(val title: String, val name: String, val attempts: List<Int>, val date: String, val check: String) {
    val stars get() = attempts.sumOf { Flight.stars(it, false) }
    val tries get() = attempts.sum()
    /** Stimmt der Prüfcode? (Schutz gegen Abtippen/Ändern) */
    val valid get() = check == checkOf(title, name, attempts, date)

    companion object {
        fun checkOf(title: String, name: String, attempts: List<Int>, date: String) =
            (stableHash(title + name + attempts.joinToString() + date) and 0xFFFF).toString(16).uppercase().padStart(4, '0')

        /** Bestenliste: gültige zuerst, dann mehr Sterne, bei Gleichstand weniger Versuche; doppelte Einsendungen nur einmal */
        fun ranking(results: List<ChallengeResult>): List<ChallengeResult> =
            results.distinctBy { it.name.trim().lowercase() to it.check }
                .sortedWith(compareByDescending<ChallengeResult> { it.valid }.thenByDescending { it.stars }.thenBy { it.tries })
    }
}

/**
 * Codes zum Teilen: „KFL1:…“ für ein Level, „KFC1:…“ für eine Challenge, „KFR1:…“ für ein Challenge-Ergebnis.
 * JSON → zusammengedrückt (Deflate) → Base64 ohne Sonderzeichen. Kein Server nötig.
 */
object Codec {
    private const val LEVEL = "KFL1:"
    private const val CHALLENGE = "KFC1:"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun encodeLevel(s: LevelSpec) = LEVEL + pack(json.encodeToString(LevelSpec.serializer(), round(s)))
    fun encodeChallenge(c: ChallengeSpec) = CHALLENGE + pack(json.encodeToString(ChallengeSpec.serializer(), c.copy(custom = c.custom.map(::round))))

    fun decodeLevel(text: String): LevelSpec? = find(text, LEVEL)?.let { runCatching { json.decodeFromString(LevelSpec.serializer(), unpack(it)) }.getOrNull() }
    fun decodeChallenge(text: String): ChallengeSpec? = find(text, CHALLENGE)?.let { runCatching { json.decodeFromString(ChallengeSpec.serializer(), unpack(it)) }.getOrNull() }

    private const val RESULT = "KFR1:"
    fun encodeResult(r: ChallengeResult) = RESULT + pack(json.encodeToString(ChallengeResult.serializer(), r))
    /** Alle Ergebnis-Codes in einem Text (z. B. viele WhatsApp-Nachrichten hintereinander eingefügt) */
    fun decodeResults(text: String): List<ChallengeResult> {
        val out = ArrayList<ChallengeResult>(); var from = 0
        while (true) {
            val i = text.indexOf(RESULT, from); if (i < 0) break
            val code = text.substring(i + RESULT.length).takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
            runCatching { json.decodeFromString(ChallengeResult.serializer(), unpack(code)) }.getOrNull()?.let(out::add)
            from = i + RESULT.length
        }
        return out
    }

    /** Welche Art Code steckt im Text? */
    enum class Type { LEVEL, CHALLENGE, DUEL }
    fun detect(text: String): Type? = when {
        LEVEL in text -> Type.LEVEL
        CHALLENGE in text -> Type.CHALLENGE
        DuelCode.decode(text) != null -> Type.DUEL
        else -> null
    }

    /** Ist das geteilte Level spielbar und fair (Lösung trifft, Startwerte nicht)? */
    fun check(l: Level): String? {
        val win = Flight.fly(l, l.solution)
        return when {
            l.targets.isEmpty() -> "Setz mindestens eine Station."
            !win.success && win.crash != null -> "Deine Lösung kracht in einen Asteroiden."
            !win.success -> "Deine Lösung trifft noch nicht alle Stationen."
            Flight.fly(l, l.start).success -> "Zu leicht: Schon die Startwerte treffen. Verschieb die Station."
            else -> null
        }
    }

    private fun round(s: LevelSpec): LevelSpec {
        fun r(v: Double) = roundHalfUp(v * 1000) / 1000.0
        fun rl(l: List<List<Double>>) = l.map { it.map(::r) }
        return s.copy(targets = rl(s.targets), rocks = rl(s.rocks), cells = rl(s.cells), solution = s.solution.mapValues { r(it.value) })
    }

    private fun find(text: String, prefix: String): String? {
        val i = text.indexOf(prefix); if (i < 0) return null
        return text.substring(i + prefix.length).takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private val b64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    @OptIn(ExperimentalEncodingApi::class)
    private fun pack(s: String): String = b64.encode(zipDeflate(s.encodeToByteArray())).trimEnd('=')

    @OptIn(ExperimentalEncodingApi::class)
    private fun unpack(s: String): String = zipInflate(b64.decode(s)).decodeToString(throwOnInvalidSequence = true)
}

/**
 * Formel-Detektiv: Die Kurve ist zu sehen – du tippst die Formel.
 * Die Umkehrung des Spiels: vom Bild zur Gleichung.
 */
object Detective {
    val kinds = listOf(Kind.LINE, Kind.PARABOLA, Kind.EXP, Kind.SINE)

    /** Runde i eines Falls: Bahnart wechselt, Kurve aus dem Generator. */
    fun case(seed: Long, round: Int): Level = Gen.level(kinds[round % kinds.size], seed * 53 + round, 0, "detektiv-$seed-$round")

    /** Deutsche Eingabe („−0,5“, „-1/2“, „,25“) → Zahl. */
    fun parse(text: String): Double? {
        val t = text.trim().replace('−', '-').replace(',', '.').replace(" ", "")
        if (t.isEmpty()) return null
        if ('/' in t) {
            val (a, b) = t.split('/', limit = 2)
            val x = a.toDoubleOrNull() ?: return null; val y = b.toDoubleOrNull() ?: return null
            return if (y == 0.0) null else x / y
        }
        return t.toDoubleOrNull()
    }

    /** Stimmt die getippte Formel? Verglichen wird die Kurve, nicht die Schreibweise. */
    fun matches(l: Level, guess: Map<String, Double>): Boolean {
        if (l.params.any { it.key !in guess }) return false
        val xs = (-8..8).map { it.toDouble() + 0.37 }
        return xs.all { x ->
            val a = Flight.f(l, l.solution, x); val b = Flight.f(l, guess, x)
            kotlin.math.abs(a) > 50 || kotlin.math.abs(a - b) < 1e-6 * (1 + kotlin.math.abs(a))
        }
    }
}
