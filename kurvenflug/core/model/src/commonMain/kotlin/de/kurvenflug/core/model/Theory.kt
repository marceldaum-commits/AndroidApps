package de.kurvenflug.core.model

/**
 * „📖 Theorie“-Knopf in jeder Übung: die passende Erklärung zum Nachlesen, jederzeit und ohne Sterne-Abzug.
 * Kapitel-Level zeigen die Einführung ihres Kapitels; Missionen ohne eigenes Kapitel (Sturm, Duell, Andromeda …)
 * die Einführung des Kapitels mit derselben Bahnart. Dazu die Merkformel (genau die, die man zum Lösen braucht)
 * und ein durchgerechnetes Beispiel (Marcel 18.11.: „die Formel, die man braucht, erklären und zeigen“).
 */
data class Theory(val emoji: String, val title: String, val grades: String, val points: List<String>, val formula: String?, val example: String? = null)

object Theories {
    fun of(level: Level, chapter: Chapter?): Theory? {
        val own = chapter?.takeIf { it.intro.isNotEmpty() && it.levels.any { l -> l.kind == level.kind } }
            ?: Levels.chapters.firstOrNull { it.number == level.chapter && it.intro.isNotEmpty() }
        val c = own ?: Levels.chapters.firstOrNull { c -> c.intro.isNotEmpty() && c.levels.any { it.kind == level.kind } } ?: return null
        val formula = TheoryFormulas.formula(c.number, level.kind)
            ?: Templates.tip(level.kind).takeIf { it.isNotBlank() }?.let { "${Templates.label(level.kind)}: $it" }
        return Theory(c.emoji, c.title, c.grades, c.intro, formula, TheoryFormulas.example(c.number, level.kind))
    }
}

/**
 * Merkformel und Rechenbeispiel je Kapitel (bei Kapiteln mit zwei Aufgabenarten je Bahnart).
 * Geprüft am 18.11.: Jede Größe, die man einstellt, steht in der Formel; das Beispiel rechnet genau so, wie die Level es verlangen.
 */
object TheoryFormulas {
    private class Entry(val formula: String, val example: String)
    private fun e(f: String, x: String) = Entry(f, x)

    private val byChapter: Map<Int, Map<Kind?, Entry>> = mapOf(
        1 to mapOf(null to e("Punkt (x | y): erst x waagerecht (rechts +, links −), dann y senkrecht (oben +, unten −).",
            "Ziel 3 Kästchen rechts und 2 unten → (3 | −2), also x = 3 und y = −2.")),
        2 to mapOf(null to e("y = m · x + b. b = y-Wert bei x = 0, Steigung m = Δy : Δx = (y₂ − y₁) : (x₂ − x₁).",
            "Ziele (0 | 1) und (2 | 5): b = 1, m = (5 − 1) : (2 − 0) = 2 → y = 2 · x + 1.")),
        3 to mapOf(null to e("y = a · (x − d)² + e. Scheitel S(d | e); a > 0 Tal, a < 0 Hügel.",
            "Scheitel (2 | −1), Ziel (3 | 1): 1 = a · (3 − 2)² − 1 → 1 = a − 1 → a = 2.")),
        4 to mapOf(null to e("Wurfweite auf ebenem Boden: w = v² · sin(2α) : g (g: Erde 9,81, Mars 3,71, Mond 1,62 m/s²).",
            "v = 10 m/s, α = 45°, Erde: w = 10² · sin(90°) : 9,81 = 100 : 9,81 ≈ 10,2 m.")),
        5 to mapOf(
            Kind.SINE to e("y = a · sin(b · x) + d. a = Ausschlag, eine Schwingung ist 2π : b lang, d = Mittellinie.",
                "y = 2 · sin(x) + 1 schwingt zwischen 1 − 2 = −1 und 1 + 2 = 3; eine Schwingung ist 2π ≈ 6,28 lang."),
            Kind.EXP to e("y = a · bˣ. a = Wert bei x = 0, b = Faktor pro Schritt (b > 1 wächst, b < 1 zerfällt).",
                "a = 3, b = 2: bei x = 2 ist y = 3 · 2² = 3 · 4 = 12.")),
        6 to mapOf(null to e("Gravitation: F = G · m · M : r². Doppelte Masse → doppelte Kraft, doppelter Abstand → ¼ der Kraft.",
            "Fliegst du nur halb so weit entfernt vorbei, zieht der Planet 2² = 4-mal so stark – die Bahn knickt viel stärker.")),
        7 to mapOf(null to e("Treffpunkt: beide Gleichungen gleichsetzen (y_A = y_B), nach x auflösen, x einsetzen → y.",
            "A: y = x + 1, B: y = −x + 5 → x + 1 = −x + 5 → 2x = 4 → x = 2, y = 2 + 1 = 3 → Treffpunkt (2 | 3).")),
        8 to mapOf(
            Kind.PROP to e("Proportional: y = k · x mit k = y : x (Quotient bleibt gleich).", "Ziel (4 | 6): k = 6 : 4 = 1,5 → y = 1,5 · x."),
            Kind.ANTI to e("Antiproportional: y = k : x mit k = x · y (Produkt bleibt gleich).", "Ziel (2 | 6): k = 2 · 6 = 12 → y = 12 : x, bei x = 4 also y = 3.")),
        9 to mapOf(null to e("Abstand: r = √(Δx² + Δy²) (Satz des Pythagoras).", "3 rechts, 4 hoch: r = √(3² + 4²) = √(9 + 16) = √25 = 5.")),
        10 to mapOf(null to e("y = log_b(x) + d. log_b(x) = n heißt bⁿ = x; bei x = 1 ist y = d.",
            "Ziel (8 | 3), d = 0: log_b(8) = 3 → b³ = 8 → b = 2.")),
        11 to mapOf(null to e("Reflexion: Einfallswinkel = Ausfallswinkel.",
            "Trifft der Strahl unter 30° auf einen waagerechten Spiegel, prallt er unter 30° wieder ab – wie ein Ball an der Bande.")),
        12 to mapOf(null to e("Energieerhaltung m · g · h = ½ · m · v²; Weite ab der Kante: s = 2 · √((h − h₀) · h₀).",
            "Kante h₀ = 2, Ziel s = 8: 8 = 2 · √((h − 2) · 2) → 16 = (h − 2) · 2 → h − 2 = 8 → h = 10.")),
        13 to mapOf(null to e("s = v · t + s₀. s₀ = Startpunkt, Geschwindigkeit v = Δs : Δt.",
            "Start bei s₀ = 2 m, nach 4 s bei 10 m: v = (10 − 2) : 4 = 2 m/s.")),
        14 to mapOf(
            Kind.LASER to e("tan α = Gegenkathete : Ankathete (Höhe : Weite) → α = tan⁻¹(Höhe : Weite).",
                "Station 4 weiter und 3 höher: tan α = 3 : 4 = 0,75 → α = tan⁻¹(0,75) ≈ 36,9°."),
            Kind.RADIUS to e("sin α = Gegenkathete : Hypotenuse, cos α = Ankathete : Hypotenuse → r = GK : sin α = AK : cos α.",
                "Station 3 hoch unter 30°: r = 3 : sin(30°) = 3 : 0,5 = 6.")),
        15 to mapOf(null to e("Bruch = Zähler : Nenner. ½ = 0,5, ¼ = 0,25, ¾ = 0,75; minus heißt links.",
            "−1¼: ein ganzes Kästchen und ein Viertel nach links → x = −1,25.")),
        16 to mapOf(null to e("Winkelsumme im Dreieck: α + β + γ = 180°. Nebenwinkel: α + β = 180°. Rechter Winkel = 90°.",
            "Dreieck mit 60° und 70°: γ = 180° − 60° − 70° = 50°. Nebenwinkel von 50°: 180° − 50° = 130°.")),
        17 to mapOf(null to e("Brechungsgesetz: sin α₁ = n · sin α₂ (Winkel zum Lot gemessen; Glas n ≈ 1,5, Wasser n ≈ 1,33).",
            "Innen im Glas 20° zum Lot: sin α₁ = 1,5 · sin(20°) ≈ 1,5 · 0,342 = 0,513 → α₁ ≈ 30,9° draußen.")),
        18 to mapOf(null to e("pq-Formel: x = −p/2 ± √((p/2)² − q). Scheitel-x: d = (x₁ + x₂) : 2 = −p/2, e = q − d².",
            "y = x² − 6x + 5: p = −6, q = 5 → x = 3 ± √(9 − 5) = 3 ± 2 → Nullstellen 1 und 5, Scheitel (3 | 5 − 9) = (3 | −4).")),
        19 to mapOf(null to e("Tangentensteigung m = f'(x₀). Ableiten: xⁿ → n · xⁿ⁻¹, Zahlen fallen weg.",
            "f(x) = x², x₀ = 3: f'(x) = 2x → m = f'(3) = 6.")),
        20 to mapOf(null to e("Extremstelle: f'(x) = 0. f''(x) < 0 → Hochpunkt, f''(x) > 0 → Tiefpunkt.",
            "f(x) = x² − 4x: f'(x) = 2x − 4 = 0 → x = 2; f''(x) = 2 > 0 → Tiefpunkt bei x = 2.")),
        21 to mapOf(null to e("Fläche: ∫₀ᵇ f(x) dx = F(b) − F(0). Stammfunktion: xⁿ → xⁿ⁺¹ : (n + 1), k → k · x.",
            "f(x) = 2x bis b = 3: F(x) = x² → F(3) − F(0) = 9 − 0 = 9.")),
        22 to mapOf(null to e("X = P + t · v, also v = (Q − P) : t für jede Koordinate einzeln.",
            "P(1 | 2), Q(7 | 5), t = 3: v₁ = (7 − 1) : 3 = 2, v₂ = (5 − 2) : 3 = 1 → v = (2 | 1).")),
        23 to mapOf(null to e("y = a · bˣ mit b = 1 + p : 100 (Wachstum) bzw. b = 1 − p : 100 (Abnahme); a = Startwert.",
            "100 € mit 5 % Zinsen: b = 1,05; nach 2 Jahren y = 100 · 1,05² = 110,25 €.")),
        24 to mapOf(null to e("s = ½ · a · t² + v₀ · t. a = Beschleunigung (negativ = bremsen), v₀ = Anfangstempo.",
            "a = 2 m/s², v₀ = 1 m/s, nach 3 s: s = ½ · 2 · 3² + 1 · 3 = 9 + 3 = 12 m.")),
        25 to mapOf(null to e("y = a · xⁿ + d. n = Hochzahl, a streckt, d schiebt nach oben/unten.",
            "y = 0,5 · x³ + 1 bei x = 2: 0,5 · 2³ + 1 = 0,5 · 8 + 1 = 5.")),
        26 to mapOf(null to e("y = a · sin(b · (x − c)) + d. c = Verschiebung nach rechts, Wellenberg π : (2b) nach dem Nulldurchgang.",
            "Aufsteigender Nulldurchgang bei x = 1 → c = 1; mit b = 1 liegt der Wellenberg bei 1 + π/2 ≈ 2,57.")),
        27 to mapOf(null to e("Fallzeit t = √(2 · h : g), Weite x = v · t → v = x : t.",
            "h = 19,62 m auf der Erde: t = √(2 · 19,62 : 9,81) = √4 = 2 s; Ziel 30 m weit → v = 30 : 2 = 15 m/s.")),
        28 to mapOf(null to e("y = a · e^(k·x). a = Startwert; aus e^(k·x) = q folgt k = ln(q) : x.",
            "Verdopplung nach x = 5: e^(5k) = 2 → k = ln(2) : 5 ≈ 0,693 : 5 ≈ 0,139.")),
        29 to mapOf(null to e("Kästchen = echte Länge : Maßstab. Osten/Norden plus, Westen/Süden minus.",
            "1 Kästchen = 50 km, Ziel 150 km östlich und 100 km südlich: x = 150 : 50 = 3, y = −(100 : 50) = −2.")),
        31 to mapOf(null to e("Waage: Was du links machst, machst du rechts. Erst + und − wegräumen, dann durch die Zahl vor x teilen.",
            "2x + 5 = 17: beide Seiten − 5 → 2x = 12; beide Seiten : 2 → x = 6. Probe: 2 · 6 + 5 = 17 ✓.")),
        32 to mapOf(null to e("y = a · f(b · (x − c)) + d: d hoch/runter, c rechts (x − c) / links (x + c), a streckt (minus = auf den Kopf), b zur Seite (minus = links/rechts gespiegelt).",
            "y = −2 · (x − 1)² + 5: Gipfel bei (1 | 5), nach unten geöffnet, doppelt so steil. Probe: x = 2 → −2 · 1 + 5 = 3.")),
        30 to mapOf(null to e("P(X = k) = (n über k) · pᵏ · (1 − p)ⁿ⁻ᵏ; kumuliert P(X ≤ k) = Summe von P(X = 0) bis P(X = k).",
            "n = 4, p = 0,5: P(X = 2) = 6 · 0,5² · 0,5² = 6 : 16 = 0,375 – also 37,5 %.")),
    )

    private fun entry(chapter: Int, kind: Kind) = byChapter[chapter]?.let { it[kind] ?: it[null] }
    fun formula(chapter: Int, kind: Kind): String? = entry(chapter, kind)?.let { "Merkformel: ${it.formula}" }
    fun example(chapter: Int, kind: Kind): String? = entry(chapter, kind)?.example
    /** Für den Test: Kapitel mit Formel */
    val chapters: Set<Int> get() = byChapter.keys
}
