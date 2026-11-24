package de.kurvenflug.core.model

/**
 * Inhaltspaket 2.8: „Terme & Gleichungen“ (Kl. 7–8, Zahlenstrahl mit Waage-Bild) und
 * „Funktionen formen“ (Kl. 9–10, y = a · f(b · (x − c)) + d, dazu Gleichungen als Schnittpunkte „sehen“).
 * Jedes Level hat Tipp, eigenen Rechenweg ([Level.walk]) und eigene Rechen-Frage ([Level.quiz]).
 */
object Terms {
    private fun t(x: Double, y: Double, r: Double = 0.4) = Body(x, y, r)
    private fun p(key: String, label: String, min: Double, max: Double, step: Double, start: Double) = Param(key, label, min, max, step, start)
    private fun s(text: String, answer: Double, sets: String? = null, extra: List<Double> = emptyList()) = Walkthrough.step(text, answer, sets, extra)
    private fun f(x: Double) = num(x, 2)

    // ------------------------------------------------------------------ 31 Terme & Gleichungen (Kl. 7–8)

    private val tx = p("x", "x", -9.0, 9.0, 1.0, 0.0)

    /** Gleichung als Waage: Lösung x liegt auf dem Zahlenstrahl, die Station steht dort. */
    private fun eq(n: Int, title: String, left: String, right: String, x: Double, task: String, tip: String, quiz: List<String>, walk: List<Step>) =
        Level("31-$n", 31, n, title, Kind.POINT, "⚖️ $task", listOf(t(x, 0.0, 0.4)), listOf(tx), mapOf("x" to x), fixed = mapOf("y" to 0.0),
            given = "$left = $right", tip = tip, quiz = quiz, walk = walk)

    private val ch31 = Chapter(31, "Terme & Gleichungen", "Klasse 7–8", "⚖️",
        listOf(
            "Eine Gleichung ist wie eine Waage im Gleichgewicht: links und rechts wiegt dasselbe. ⚖️",
            "Die goldene Regel: Was du links machst, machst du auch rechts – dann bleibt die Waage im Gleichgewicht.",
            "So packst du x aus: erst plus und minus wegräumen, dann durch die Zahl vor x teilen.",
            "Terme vereinfachen heißt: gleiche Sorten zusammenfassen, 3x + 2x = 5x. Die Lösung x ist die Stelle auf dem Zahlenstrahl, wo die Station steht.",
        ),
        listOf(
            eq(1, "Gleichgewicht", "x + 3", "8", 5.0, "Links liegen x und 3 Gewichte, rechts 8. Welche Zahl x macht die Waage gerade?",
                "Auf beiden Seiten 3 wegnehmen: x = 8 − 3.",
                listOf("x = 8 − 3", "x = 8 + 3", "x = 3 − 8", "Wie bekommst du x allein?"), listOf(s("Auf beiden Seiten 3 abziehen: x = 8 − 3 = ?", 5.0, "x"))),
            eq(2, "Dreifach schwer", "3x", "12", 4.0, "Drei gleiche Pakete x wiegen zusammen 12. Wie schwer ist eins?",
                "Beide Seiten durch 3 teilen: x = 12 : 3.",
                listOf("x = 12 : 3", "x = 12 − 3", "x = 12 · 3", "Wie wird aus 3x ein einzelnes x?"), listOf(s("Beide Seiten durch 3: x = 12 : 3 = ?", 4.0, "x"))),
            eq(3, "Erst weg, dann teilen", "2x + 5", "17", 6.0, "Zwei Pakete plus 5 Gewichte wiegen 17. Finde x!",
                "Erst 5 abziehen: 2x = 12. Dann durch 2: x = 6.",
                listOf("Erst 17 − 5, dann : 2", "Erst 17 : 2, dann − 5", "17 − 5 − 2", "Was machst du zuerst?"),
                listOf(s("Beide Seiten − 5: 2x = 17 − 5 = ?", 12.0), s("Beide Seiten : 2: x = 12 : 2 = ?", 6.0, "x"))),
            eq(4, "Minus-Gewicht", "5x − 4", "11", 3.0, "Fünf Pakete, davon fehlen 4 Gewichte: 5x − 4 = 11. Finde x!",
                "Erst 4 addieren: 5x = 15. Dann durch 5: x = 3.",
                listOf("Erst 11 + 4, dann : 5", "Erst 11 − 4, dann : 5", "11 : 5 + 4", "Was machst du mit dem − 4?"),
                listOf(s("Beide Seiten + 4: 5x = 11 + 4 = ?", 15.0), s("Beide Seiten : 5: x = 15 : 5 = ?", 3.0, "x"))),
            eq(5, "Klammern", "2 · (x + 3)", "14", 4.0, "Das Doppelte von (x + 3) wiegt 14. Löse die Klammer auf!",
                "Ausmultiplizieren: 2x + 6 = 14. Dann 6 abziehen und durch 2 teilen.",
                listOf("2x + 6 = 14", "2x + 3 = 14", "x + 6 = 14", "Was ergibt 2 · (x + 3)?"),
                listOf(s("2 · (x + 3) = 2x + 6. Beide Seiten − 6: 2x = 14 − 6 = ?", 8.0), s("Beide Seiten : 2: x = 8 : 2 = ?", 4.0, "x"))),
            eq(6, "Zusammenfassen", "3x + 2x − 4", "11", 3.0, "Links stehen zwei Sorten x. Fasse zusammen: 3x + 2x = 5x. Finde x!",
                "5x − 4 = 11 → 5x = 15 → x = 3.",
                listOf("3x + 2x = 5x", "3x + 2x = 5x²", "3x + 2x = 6x", "Was ist 3x + 2x?"),
                listOf(s("3x + 2x = ?x", 5.0), s("5x − 4 = 11. Beide Seiten + 4: 5x = ?", 15.0), s("x = 15 : 5 = ?", 3.0, "x"))),
            eq(7, "x auf beiden Seiten", "4x + 2", "2x + 10", 4.0, "Auf beiden Seiten liegen Pakete. Nimm zuerst 2x von beiden Seiten weg. Finde x!",
                "4x − 2x = 2x: Es bleibt 2x + 2 = 10 → 2x = 8 → x = 4.",
                listOf("Beide Seiten − 2x", "Beide Seiten + 2x", "Beide Seiten : 2", "Wie bekommst du alle x auf eine Seite?"),
                listOf(s("Beide Seiten − 2x: 2x + 2 = 10. Dann − 2: 2x = ?", 8.0), s("x = 8 : 2 = ?", 4.0, "x"))),
            eq(8, "Unter Null", "3x + 7", "−2", -3.0, "Rechts steht eine negative Zahl: 3x + 7 = −2. Die Station liegt links von 0.",
                "7 abziehen: 3x = −9. Durch 3 teilen: x = −3.",
                listOf("−2 − 7 = −9", "−2 + 7 = 5", "7 − 2 = 5", "Was ist −2 − 7?"),
                listOf(s("Beide Seiten − 7: 3x = −2 − 7 = ?", -9.0), s("x = −9 : 3 = ?", -3.0, "x"))),
            eq(9, "Halbe Pakete", "x : 2 + 1", "4", 6.0, "Ein halbes Paket plus 1 Gewicht wiegt 4. Mit x : 2 rechnest du rückwärts mit „mal 2“. Finde x!",
                "Erst 1 abziehen: x : 2 = 3. Dann mal 2: x = 6.",
                listOf("Erst − 1, dann · 2", "Erst : 2, dann − 1", "4 − 1 − 2", "Wie hebst du „: 2“ auf?"),
                listOf(s("Beide Seiten − 1: x : 2 = 4 − 1 = ?", 3.0), s("Beide Seiten · 2: x = 3 · 2 = ?", 6.0, "x"))),
            eq(10, "Großes Finale", "2 · (x − 1) + 3", "x + 7", 6.0, "Klammer auflösen, zusammenfassen, x auf eine Seite – alles, was du gelernt hast!",
                "2x − 2 + 3 = x + 7 → 2x + 1 = x + 7 → x + 1 = 7 → x = 6.",
                listOf("2x + 1 = x + 7", "2x − 5 = x + 7", "2x + 5 = x + 7", "Was ergibt die linke Seite zusammengefasst?"),
                listOf(s("2 · (x − 1) + 3 = 2x − 2 + 3 = 2x + ?", 1.0), s("2x + 1 = x + 7. Beide Seiten − x: x + 1 = 7. Dann − 1: x = ?", 6.0, "x"))),
        ))

    // ------------------------------------------------------------------ 32 Funktionen formen (Kl. 9–10)

    private val fa = p("a", "a", -3.0, 3.0, 0.5, 1.0)
    private val fb = p("b", "b", -3.0, 3.0, 0.5, 1.0)
    private val fc = p("c", "c", -8.0, 8.0, 1.0, 0.0)
    private val fd = p("d", "d", -8.0, 8.0, 1.0, 0.0)
    private val allParams = mapOf("a" to fa, "b" to fb, "c" to fc, "d" to fd)

    /** Ein Formen-Level: y = a · f(b · (x − c)) + d; [free] = die einstellbaren Werte, die übrigen stehen fest. */
    private fun form(n: Int, title: String, base: Int, a: Double, b: Double, c: Double, d: Double, free: List<String>, xs: List<Double>,
                     task: String, tip: String, quiz: List<String>, walk: List<Step>): Level {
        val all = mapOf("a" to a, "b" to b, "c" to c, "d" to d)
        val cv = Curve(base, a, b, c, d)
        return Level("32-$n", 32, n, title, Kind.FORM, task, xs.map { t(it, cv.y(it), 0.3) }, free.map { allParams.getValue(it) },
            all.filterKeys { it in free }, fixed = all.filterKeys { it !in free } + ("base" to base.toDouble()), tip = tip, quiz = quiz, walk = walk)
    }

    /** Gleichung als zwei Graphen: Die Lösung liegt dort, wo sich beide schneiden – Station getarnt, du liest sie am Bild ab. */
    private fun see(n: Int, title: String, left: Curve, right: Curve, sx: Double, equation: String, task: String, tip: String,
                    quiz: List<String>, walk: List<Step>): Level {
        val y = left.y(sx)
        return Level("32-$n", 32, n, title, Kind.POINT, task, listOf(t(sx, y, 0.4)), listOf(tx, p("y", "y", -9.0, 9.0, 1.0, 0.0)), mapOf("x" to sx, "y" to y),
            hidden = true, given = equation, tip = tip, quiz = quiz, walk = walk, curves = listOf(left, right))
    }

    private val ch32 = Chapter(32, "Funktionen formen", "Klasse 9–10", "🎛️",
        listOf(
            "Jede Kurve lässt sich formen: y = a · f(b · (x − c)) + d. Die Grundform f ist zum Beispiel x², |x| oder √x.",
            "d schiebt nach oben (plus) oder unten (minus). c schiebt nach rechts (x − c): Achtung, bei x − 3 geht es nach rechts!",
            "a streckt nach oben, ist a negativ, kippt die Kurve um (Spiegelung an der x-Achse). b staucht oder streckt zur Seite, ist b negativ, spiegelt es an der y-Achse.",
            "Am Ende „siehst“ du Gleichungen: Die Lösung ist dort, wo sich zwei Graphen schneiden – und manchmal liegt eine errechnete Lösung gar nicht auf beiden (Scheinlösung)!",
        ),
        listOf(
            form(1, "Nach oben", 1, 1.0, 1.0, 0.0, 2.0, listOf("d"), listOf(0.0, 2.0), "y = x² + d. Die Parabel soll durch (0 | 2) und (2 | 6) fliegen. Wie weit musst du sie nach oben schieben?",
                "Bei x = 0 ist x² = 0, also y = d. Die Station bei (0 | 2) verrät d = 2.",
                listOf("d = y bei x = 0", "d = x bei y = 0", "d = −y", "Wo liest du d ab?"), listOf(s("Bei x = 0 ist x² = 0. y(0) = d = ?", 2.0, "d"))),
            form(2, "Nach rechts", 1, 1.0, 1.0, 3.0, 0.0, listOf("c"), listOf(3.0, 5.0), "y = (x − c)². Der Scheitel soll bei der Station (3 | 0) liegen.",
                "Der Scheitel liegt bei x = c. Bei (x − 3)² ist c = 3 – nach rechts, auch wenn da ein Minus steht.",
                listOf("c = x des Scheitels", "c = −x des Scheitels", "c = y des Scheitels", "Wo liegt der Scheitel von (x − c)²?"),
                listOf(s("Der Scheitel von (x − c)² liegt bei x = c. Station bei x = 3: c = ?", 3.0, "c"))),
            form(3, "Strecken", 1, 2.0, 1.0, 0.0, 0.0, listOf("a"), listOf(1.0, 2.0), "y = a · x². Die Parabel muss durch (1 | 2) und (2 | 8). Wie stark streckst du sie?",
                "Bei x = 1 ist x² = 1, also y = a. Die Station bei (1 | 2) sagt a = 2. Probe: 2 · 2² = 8 ✓.",
                listOf("a = y : x² bei der Station", "a = y − x²", "a = y · x²", "Wie findest du a?"), listOf(s("Bei x = 1: a · 1² = 2, also a = ?", 2.0, "a"), s("Probe bei x = 2: a · 4 = ?", 8.0))),
            form(4, "Kopfstand", 2, -1.0, 1.0, 0.0, 4.0, listOf("a", "d"), listOf(0.0, 3.0, -3.0), "y = a · |x| + d. Der Betrag macht ein V. Du brauchst ein umgedrehtes V mit Spitze bei (0 | 4): Flieg durch die Station!",
                "Die Spitze liegt bei (0 | d) → d = 4. Ein Minus bei a kippt das V um. Bei x = 3: −1 · 3 + 4 = 1 ✓.",
                listOf("a = −1 und d = 4", "a = 1 und d = 4", "a = −1 und d = −4", "Was kippt das V um?"),
                listOf(s("Die Spitze bei (0 | 4): d = ?", 4.0, "d"), s("Bei x = 3 liegt y = 1. a · 3 + 4 = 1, also a = ?", -1.0, "a"))),
            form(5, "Schmal und breit", 2, 1.0, 0.5, 0.0, 0.0, listOf("b"), listOf(4.0, -4.0), "y = |b · x|. Die Station (4 | 2) liegt auf dem V. Wie weit ziehst du es zur Seite auseinander?",
                "Bei x = 4 ist y = |b · 4| = 2 → b = 2 : 4 = 0,5. Kleines b zieht die Kurve breit.",
                listOf("b = y : x", "b = x : y", "b = y − x", "Wie findest du b aus einem Punkt?"), listOf(s("b · 4 = 2, also b = 2 : 4 = ?", 0.5, "b"))),
            form(6, "Alles zusammen", 1, -2.0, 1.0, 1.0, 5.0, listOf("a", "c", "d"), listOf(1.0, 2.0, 3.0), "y = a · (x − c)² + d. Die Parabel ist ein Hügel mit Gipfel (1 | 5). Wie steil fällt sie?",
                "Der Gipfel (1 | 5) gibt c = 1 und d = 5. Bei x = 2 liegt y = 3: a · 1 + 5 = 3 → a = −2.",
                listOf("Gipfel → c, d; Punkt → a", "Punkt → c, d; Gipfel → a", "a = d : c", "Womit fängst du an?"),
                listOf(s("Der Gipfel liegt bei x = c = ?", 1.0, "c"), s("Der Gipfel liegt auf y = d = ?", 5.0, "d"), s("Bei x = 2: a · (2 − 1)² + 5 = 3, also a = 3 − 5 = ?", -2.0, "a"))),
            form(7, "Wurzel-Rakete", 4, 1.0, 1.0, 2.0, 1.0, listOf("c", "d"), listOf(2.0, 6.0), "y = √(x − c) + d. Die Wurzelkurve startet am Anfangspunkt (2 | 1) und geht durch (6 | 3).",
                "Die Wurzelkurve beginnt bei (c | d) = (2 | 1). Probe bei x = 6: √(6 − 2) + 1 = 2 + 1 = 3 ✓.",
                listOf("Anfangspunkt = (c | d)", "Anfangspunkt = (−c | d)", "Anfangspunkt = (c | −d)", "Wo beginnt eine Wurzelkurve?"),
                listOf(s("Der Anfang (2 | 1) hat x = c = ?", 2.0, "c"), s("… und y = d = ?", 1.0, "d"), s("Probe bei x = 6: √4 + 1 = ?", 3.0))),
            form(8, "Spiegelwelt", 4, 1.0, -1.0, 0.0, 0.0, listOf("b"), listOf(-4.0, -1.0), "y = √(b · x). Mit b = 1 geht die Kurve nach rechts. Du brauchst sie nach links durch (−4 | 2).",
                "Minus bei b spiegelt an der y-Achse: √(−x). Probe bei x = −4: √4 = 2 ✓.",
                listOf("b = −1", "b = 1", "b = 0,5", "Wie klappst du die Kurve nach links?"),
                listOf(s("Bei x = −4 muss b · (−4) = 4 sein, also b = 4 : (−4) = ?", -1.0, "b"))),
            form(9, "Kubik-Welle", 3, 0.5, 1.0, 1.0, -2.0, listOf("a", "c", "d"), listOf(1.0, 3.0, -1.0), "y = a · (x − c)³ + d. Der Wendepunkt liegt bei (1 | −2), dann geht es durch (3 | 2).",
                "Wendepunkt (c | d) = (1 | −2). Bei x = 3: a · 2³ − 2 = 2 → 8a = 4 → a = 0,5.",
                listOf("Wendepunkt → c, d; Punkt → a", "a = 4", "a = 2", "Wie kommst du auf a?"),
                listOf(s("Wendepunkt: c = ?", 1.0, "c"), s("Wendepunkt: d = ?", -2.0, "d"), s("Bei x = 3: a · 8 − 2 = 2 → 8a = ?", 4.0), s("a = 4 : 8 = ?", 0.5, "a"))),
            see(10, "Gleichung sehen", Curve(1), Curve(5, d = 2.0), 2.0, "x² = x + 2",
                "Zwei Graphen: die Parabel y = x² und die Gerade y = x + 2. Die Gleichung x² = x + 2 hat Lösungen dort, wo sie sich schneiden. Fliege zum Schnittpunkt rechts der y-Achse!",
                "Die Graphen schneiden sich bei x = −1 und x = 2. Rechts der y-Achse: (2 | 4).",
                listOf("Schnittpunkt der Graphen ablesen", "Nur die Parabel anschauen", "x² + x + 2 rechnen", "Wo liegt die Lösung einer Gleichung im Bild?"),
                listOf(s("Bei x = 2 ist x² = ?", 4.0), s("… und x + 2 = ?", 4.0), s("Beide gleich → Schnittpunkt (2 | ?)", 4.0, "y"))),
            see(11, "Nur eine Lösung", Curve(2), Curve(5, d = 2.0), -1.0, "|x| = x + 2",
                "Der Betrag |x| ist ein V, y = x + 2 eine Gerade. Wie oft schneiden sie sich? Fliege zum Schnittpunkt!",
                "Rechts der y-Achse läuft die Gerade parallel über dem V – kein Schnitt. Links: −x = x + 2 → x = −1, y = 1.",
                listOf("Schnittpunkt (−1 | 1)", "Schnittpunkt (2 | 2)", "Gar keiner", "Wie viele Lösungen zeigt das Bild?"),
                listOf(s("Links der y-Achse ist |x| = −x. Dann −x = x + 2 → x = ?", -1.0, "x"), s("y = −(−1) = ?", 1.0, "y"))),
            see(12, "Scheinlösung", Curve(4, c = -2.0), Curve(5), 2.0, "√(x + 2) = x",
                "Rechnen (Quadrieren) ergibt x = 2 und x = −1. Aber stimmen beide? Zeichne √(x + 2) und y = x: Fliege zum echten Schnittpunkt!",
                "x = −1 ist eine Scheinlösung: √(−1 + 2) = 1, aber rechts steht −1. Echt ist nur x = 2: √4 = 2 ✓.",
                listOf("Probe: x = 2 passt, x = −1 nicht", "Beide passen", "Nur x = −1 passt", "Warum prüfst du nach dem Quadrieren?"),
                listOf(s("Probe x = −1: √(−1 + 2) = ?", 1.0), s("… aber x = −1 steht auf der anderen Seite. Echt ist x = ?", 2.0, "x"), s("y = x = ?", 2.0, "y"))),
        ))

    val chapters = listOf(ch31, ch32)
}
