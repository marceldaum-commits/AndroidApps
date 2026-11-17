package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

class SpeechTest {
    private fun says(expected: String, shown: String) = assertEquals(shown, expected, Speech.clean(shown))

    @Test fun formulaSignsBecomeWords() {
        assertEquals("Genau 25 Prozent, Treffer!", Speech.clean("Genau 25 % – Treffer!"))
        assertEquals("Flieg zum Punkt 4, 2, 0!", Speech.clean("Flieg zum Punkt (4|2|0)!"))
        assertEquals("Fast, der Punkt liegt bei 3.", Speech.clean("Fast (der Punkt liegt bei 3)."))
        // früher fiel das Kleiner-Zeichen einfach weg („f zwei Strich 0“) – das war der Fehler A5
        assertEquals("f Strich gleich 0 und f zwei Strich kleiner als 0", Speech.clean("f′ = 0 und f″ < 0"))
        assertEquals("V gleich 16 Pi", Speech.clean("V = 16π")); assertEquals("x hoch 2 minus 2x minus 3", Speech.clean("x² − 2x − 3"))
        assertEquals("P gleich 3 durch 10", Speech.clean("P = 3 : 10")); assertEquals("Tipp gefällig?", Speech.clean("Tipp gefällig? 💡"))
        assertEquals("ein halb mal ein halb gleich ein Viertel", Speech.clean("½ · ½ = ¼"))
        assertEquals("", Speech.clean("🎈✨"))
        assertEquals("1. Zug: pink, 2 von 5. Die Kugel bleibt draußen.", Speech.clean("1. Zug: pink – 2 von 5. Die Kugel bleibt draußen."))
        // jede Sprechblase der Tour bleibt lesbar: nichts als Buchstaben, Ziffern und Satzzeichen
        Levels.all.forEach { l -> val t = Speech.clean(l.mission); assertTrue(l.id, t.isNotEmpty() && t.all { it.isLetterOrDigit() || it in " .,;:!?-/" }) }
    }

    /** A1: Betragsstriche sind keine Bedingung; in Punkten trennt der Strich die Koordinaten. */
    @Test fun absoluteValueBarsAndConditions() {
        says("Betrag von n gleich 3", "|n⃗| = 3")
        says("d gleich Betrag von 9 minus 18, geteilt durch 3, gleich 3", "d = |9 − 18| : 3 = 3")
        says("A gleich ein halb mal Betrag von AB Kreuz AC", "A = ½ · |AB⃗ × AC⃗|")
        says("Abstand gleich Betrag von Klammer auf B minus A Klammer zu mal n, geteilt durch Betrag von n.", "Abstand = |(B − A) · n⃗| : |n⃗|.")
        says("Abstand dann Betrag von 2, 2, 1, gleich 3", "Abstand dann |(2|2|1)| = 3")
        says("P von Band unter der Bedingung VIP gleich 12 durch 40", "P(Band | VIP) = 12 : 40")
        says("P von B unter der Bedingung A gleich 0,3", "P(B | A) = 0,3")
        says("Höhe 0, also P 3, 4, 5", "Höhe 0 → P(3|4|5)")
        says("w gleich B minus A gleich 2 minus 4, 7 minus 2, 4 minus 3", "w⃗ = B − A = (2−4 | 7−2 | 4−3)")
    }

    /** A2: Das Geteilt-Zeichen wird immer gesprochen – ein Doppelpunkt als Satzzeichen bleibt. */
    @Test fun divisionSign() {
        says("a mal b gleich 25, Betrag von a gleich 5, Betrag von b gleich Wurzel aus 50. Also Kosinus Alpha gleich 25 durch Klammer auf 5 mal Wurzel aus 50 Klammer zu gleich 1 durch Wurzel aus 2.",
            "a⃗ · b⃗ = 25, |a⃗| = 5, |b⃗| = √50. Also cos α = 25 : (5 · √50) = 1 : √2.")
        says("AC gleich r mal AB: aus x1 folgt r gleich 2 durch minus 2 gleich minus 1", "AC⃗ = r · AB⃗: aus x₁ folgt r = 2 : −2 = −1")
        says("Die Ableitung von ln von x ist 1 durch x.", "Die Ableitung von ln(x) ist 1 : x.")
        says("x1 gleich Klammer auf 2 plus 6 Klammer zu durch 2 gleich 4", "x₁ = (2+6):2 = 4")
        says("Probe mit S2 0, 3, 0: 2 mal 3 gleich 6, stimmt", "Probe mit S₂(0|3|0): 2 · 3 = 6 ✓")
        says("n gleich 1, 1, 0, d gleich 0, 1, 1: n mal d gleich 1", "n⃗ = (1|1|0), d⃗ = (0|1|1): n⃗ · d⃗ = 1")
        says("4 über 2 gleich 4 Fakultät durch Klammer auf 2 Fakultät mal 2 Fakultät Klammer zu", "4 über 2 = 4! : (2! · 2!)")
    }

    /** A3: Rechenklammern werden mitgesprochen, Einschübe werden zur Pause – ohne lose Kommas. */
    @Test fun brackets() {
        says("Einsetzen: Klammer auf 1 plus t Klammer zu plus Klammer auf 1 plus 2t Klammer zu plus 1 gleich 9.", "Einsetzen: (1 + t) + (1 + 2t) + 1 = 9.")
        says("Klammer auf x minus 3 Klammer zu mal Klammer auf x plus 1 Klammer zu gleich 0", "(x − 3)(x + 1) = 0")
        says("gleich Klammer auf 4 mal 3 Klammer zu durch Klammer auf 2 mal 1 Klammer zu", "= (4 · 3) : (2 · 1)")
        says("Wie groß ist der Abstand der Drohnen in diesem Moment? Vorgeschrieben sind mindestens 2 Einheiten.",
            "Wie groß ist der Abstand der Drohnen in diesem Moment? (Vorgeschrieben sind mindestens 2 Einheiten.)")
        says("a mal b gleich 2 mal minus 2 plus 1 mal c plus 2 mal 1", "a⃗ · b⃗ = 2·(−2) + 1·c + 2·1")
        says("Sigma gleich Wurzel aus 400 mal 0,5 mal 0,5, gleich Wurzel aus 100 gleich 10", "σ = √(400 · 0,5 · 0,5) = √100 = 10")
        says("x3 gleich 3, nach oben, also P 2, 5, 3", "x₃ = 3 (nach oben) → P(2|5|3)")
    }

    /** A4: Der Strich an Punkten und Größen bleibt hörbar. */
    @Test fun primes() {
        says("P Strich gleich 2 mal F minus P gleich 6, 6, 3", "P′ = 2 · F − P = (6|6|3)")
        says("r Strich hoch 2 gleich r hoch 2 minus d hoch 2 gleich 25 minus 9 gleich 16", "r′² = r² − d² = 25 − 9 = 16")
        says("c Strich gleich 2 mal 10 minus 6 gleich 14, also E Strich: x1 plus 2x2 plus 2x3 gleich 14", "c′ = 2 · 10 − 6 = 14 → E′: x₁ + 2x₂ + 2x₃ = 14")
        says("M Strich 5, 7, 6", "M′(5|7|6)"); says("Z Strich minus 2, 7, 4", "Z′(−2|7|4)")
        says("an der x1x3-Ebene: x2 wechselt, also P zwei Strich 1, minus 4, minus 3", "an der x₁x₃-Ebene: x₂ wechselt → P″(1|−4|−3)")
        says("f Strich von x gleich minus 2x plus 4", "f′(x) = −2x + 4"); says("Zweite Ableitung null setzen: f zwei Strich von x gleich 6x minus 12.", "Zweite Ableitung null setzen: f″(x) = 6x − 12.")
    }

    /** A5: Vergleichszeichen fallen nicht mehr weg. */
    @Test fun comparisons() {
        says("Tiefpunkt bei x gleich 1: f Strich von 1 gleich 0 und f zwei Strich von 1 gleich e größer als 0.", "Tiefpunkt bei x = 1: f′(1) = 0 und f″(1) = e > 0.")
        says("gleich lang: Klammer auf k minus 1 Klammer zu hoch 2 gleich 1, also k gleich 1 plus minus 1, wegen k größer als 1 also k gleich 2", "gleich lang: (k − 1)² = 1 → k = 1 ± 1, wegen k > 1 also k = 2")
        says("f zwei Strich von 2 gleich minus 2 kleiner als 0, also Hochpunkt H 2, 4", "f″(2) = −2 < 0 → Hochpunkt H(2|4)")
        says("1 minus 0,5 hoch n größer gleich 0,9, also 0,5 hoch n kleiner gleich 0,1", "1 − 0,5ⁿ ≥ 0,9, also 0,5ⁿ ≤ 0,1")
        says("Probe: n mal A gleich 3 ungleich 16", "Probe: n⃗ · A = 3 ≠ 16"); says("0,828 minus 0,172 gleich 0,656 ungefähr 66 Prozent", "0,828 − 0,172 = 0,656 ≈ 66 %")
    }

    /** A6: Hochzahlen, Integralgrenzen und die Schreibweise mit Dach. */
    @Test fun powersAndIntegrals() {
        says("x minus 2 gleich e hoch 0 gleich 1", "x − 2 = e⁰ = 1"); says("100 mal 2 hoch t gleich 800 heißt 2 hoch t gleich 8.", "100 · 2ᵗ = 800 heißt 2ᵗ = 8.")
        says("A gleich Integral von 0 bis 2 über Klammer auf minus 3x hoch 2 plus 6x Klammer zu dx", "A = ∫₀² (−3x² + 6x) dx")
        says("Integral von 0 bis b über 2x dx gleich x hoch 2, in den Grenzen von 0 bis b, gleich b hoch 2", "∫₀ᵇ 2x dx = [x²]₀ᵇ = b²")
        says("f von x hoch 2 gleich 4. Das Integral von 4 über 0 bis 4 ist 16.", "f(x)² = 4. Das Integral von 4 über [0; 4] ist 16.")
        says("F von x gleich e hoch x", "F(x) = e^x"); says("e hoch 2x minus 4, gleich 1", "e^(2x − 4) = 1")
        says("f Strich von x gleich e hoch minus x, minus x mal e hoch minus x, gleich Klammer auf 1 minus x Klammer zu mal e hoch minus x", "f′(x) = e^(−x) − x · e^(−x) = (1 − x) · e^(−x)")
        says("gleich 4 Quadratmeter", "= 4 m²")
    }

    /** A7: Der Pfeil ist eine Folgerung – oder zwischen zwei Zahlen ein „wird zu“. */
    @Test fun arrows() {
        says("3 plus 3t gleich 9, also t gleich 2", "3 + 3t = 9 → t = 2")
        says("nur x3 wechselt das Vorzeichen: aus 1 wird minus 1", "nur x₃ wechselt das Vorzeichen: 1 → −1")
        says("Betrag von 15 minus c, geteilt durch 3, gleich 3, also Betrag von 15 minus c, gleich 9", "|15 − c| : 3 = 3 → |15 − c| = 9")
    }

    /** A8: Brüche als Zeichen. */
    @Test fun fractions() {
        says("x1 gleich 0: 4 minus 6t gleich 0, also t gleich zwei Drittel", "x₁ = 0: 4 − 6t = 0 → t = ⅔")
        says("Lotfußpunkt F gleich P plus t mal n mit t gleich zwei Drittel: F ungefähr 2,33, 5,33, 3,33", "Lotfußpunkt F = P + t · n⃗ mit t = ⅔: F ≈ (2,33|5,33|3,33)")
        says("V gleich 36 Pi, passt zu vier Drittel mal Pi mal 3 hoch 3", "V = 36π (passt zu ⁴⁄₃ · π · 3³)")
        says("F von x gleich ein Drittel x hoch 3 minus x hoch 2", "F(x) = ⅓x³ − x²"); says("drei Viertel", "¾")
    }

    /** A9: Ein einfaches Minus vor einer Zahl ist ein Minus – der Bindestrich im Wort bleibt. */
    @Test fun asciiMinusAndHyphens() {
        says("n mal M gleich minus 2, Betrag von n gleich 3", "n⃗ · M = -2, |n⃗| = 3")
        says("d gleich Betrag von minus 1 minus 5, geteilt durch 3, gleich 2", "d = |-1 − 5| : 3 = 2")
        says("f Strich von minus 2 gleich minus 0,41. Am Tiefpunkt ist die Steigung 0.", "f′(-2) = −0,41. Am Tiefpunkt ist die Steigung 0.")
        says("Beim Spiegeln an der x1x3-Ebene wechselt nur x2 das Vorzeichen.", "Beim Spiegeln an der x₁x₃-Ebene wechselt nur x₂ das Vorzeichen.")
        says("Zweimal ableiten, Produkt- und Kettenregel", "Zweimal ableiten (Produkt- und Kettenregel)"); says("Abi-Pass", "Abi-Pass")
    }

    /** A10: Häkchen, Kreuzchen, parallel und senkrecht – die Aussage kippt nicht. */
    @Test fun checkMarks() {
        says("x1 stimmt, x2 und x3 noch nicht.", "x₁ stimmt ✓ – x₂ und x₃ noch nicht.")
        says("Dein Punkt liegt in E, aber noch nicht auf g.", "Dein Punkt liegt in E ✓, aber noch nicht auf g ✗.")
        says("Dein Punkt liegt am Boden, x3 gleich 0, aber liegt auf g nicht.", "Dein Punkt liegt am Boden (x₃ = 0) ✓, aber liegt auf g nicht ✗.")
        says("B stimmt noch nicht, setz die Punkte ein und prüfe die Gleichung.", "B ✗ – setz die Punkte ein und prüfe die Gleichung.")
        says("S stimmt. A, B stimmen noch nicht, setz die Punkte ein und prüfe die Gleichung.", "S ✓  A, B ✗ – setz die Punkte ein und prüfe die Gleichung.")
        says("A stimmt, B, C stimmen noch nicht: Setz die Punkte ein und prüfe die Gleichung.", "A ✓ – B, C ✗: Setz die Punkte ein und prüfe die Gleichung.")
        says("A stimmt", "A ✓"); says("B stimmt noch nicht", "B ✗")
        says("Probe x2: 3 gleich 6 minus 3, stimmt, x3: 2 gleich 5 minus 3, stimmt", "Probe x₂: 3 = 6 − 3 ✓, x₃: 2 = 5 − 3 ✓")
        says("Probe: 4, 0, 1 plus 2 mal 2, 3, 1 gleich 8, 6, 3, stimmt", "Probe: (4|0|1) + 2 · (2|3|1) = (8|6|3) ✓")
        says("h parallel zu g, also gleiche Richtung", "h ∥ g → gleiche Richtung"); says("n senkrecht zu u", "n⃗ ⊥ u⃗")
    }

    /** A11: Einheiten und griechische Buchstaben. */
    @Test fun unitsAndGreek() {
        says("2 Euro, so viel gewinnt man im Schnitt pro Spiel.", "2 € – so viel gewinnt man im Schnitt pro Spiel.")
        says("Drei blaue Felder bringen 2 Euro: 2 mal 0,3 gleich 0,60 Euro.", "Drei blaue Felder bringen 2 €: 2 · 0,3 = 0,60 €.")
        says("Kosinus Alpha gleich 1 durch Wurzel aus 2, also Alpha gleich 45 Grad", "cos α = 1 : √2 → α = 45°")
        says("Delta t gleich 5 minus 1 gleich 4 Sekunden", "Δt = 5 − 1 = 4 Sekunden")
        says("mü gleich 100 mal 0,5 gleich 50, Sigma gleich Wurzel aus 25 gleich 5", "μ = 100 · 0,5 = 50, σ = √25 = 5")
        says("mü minus 2 Sigma gleich 200 minus 20 gleich 180, obere Grenze 220", "μ − 2σ = 200 − 20 = 180 (obere Grenze 220)")
        says("n gleich d1 Kreuz d2 gleich 2, 0, minus 1 Kreuz 0, minus 1, 1 gleich minus 1, minus 2, minus 2", "n⃗ = d⃗₁ × d⃗₂ = (2|0|−1) × (0|−1|1) = (−1|−2|−2)")
    }

    /** A12: Prozent und Grad mitten im zusammengesetzten Wort. */
    @Test fun percentInsideWords() {
        says("mü plus 2 Sigma gleich 60 gehört zum zweiseitigen 95-Prozent-Bereich.", "μ + 2σ = 60 gehört zum zweiseitigen 95-%-Bereich.")
        says("Stell den Winkel in 15-Grad-Schritten ein.", "Stell den Winkel in 15°-Schritten ein.")
    }

    /** A13: Funktionen heißen „f von x“, Punkte bleiben Zahlenfolgen. */
    @Test fun functionNotation() {
        says("f von x gleich minus Klammer auf x minus 2 Klammer zu hoch 2 plus 4", "f(x) = −(x − 2)² + 4")
        says("F von 3 minus F von 0 gleich 9 minus 9", "F(3) − F(0) = 9 − 9")
        says("k gleich 7: P von X kleiner gleich 7 gleich 94,5 Prozent, das kleinste k über 90 Prozent.", "k = 7: P(X ≤ 7) = 94,5 % – das kleinste k über 90 %.")
        says("t von x gleich 2x minus 1 berührt die Bahn genau in 1, 1.", "t(x) = 2x − 1 berührt die Bahn genau in (1|1).")
        says("x von 3 gleich 1, 1, 0 plus 3 mal 1, 2, 1", "x⃗(3) = (1|1|0) + 3 · (1|2|1)")
        says("Genau 4, denn ln von e hoch 2 gleich 2 und ln von 1 gleich 0.", "Genau 4 – denn ln(e²) = 2 und ln(1) = 0.")
        says("Mindestens eins rechnet man über das Gegenereignis: 1 minus P von keins.", "„Mindestens eins“ rechnet man über das Gegenereignis: 1 − P(keins).")
        says("Berührpunkt: 1, f von 1 gleich 1, 1", "Berührpunkt: (1 | f(1)) = (1|1)")
    }

    /** A14: saubere Satzzeichen – kein Leerzeichen davor, nichts doppelt. */
    @Test fun punctuation() {
        says("Erst x1, nach vorn, dann x2; fertig.", "Erst x₁ (nach vorn) , dann x₂ ; fertig .")
        says("S1 4, 0, 0; genauso S2 0, 3, 0 und S3 0, 0, 2", "S₁(4|0|0); genauso S₂(0|3|0) und S₃(0|0|2)")
        says("a gleich 2; A liegt nicht in E, 2 plus 2 plus 10 gleich 14, also echt parallel", "a = 2; A liegt nicht in E (2 + 2 + 10 = 14), also echt parallel")
        says("Bei unter der Bedingung zählt nur noch ein Teil der Gäste.", "Bei „unter der Bedingung …“ zählt nur noch ein Teil der Gäste.")
        says("Um 12:30 Uhr geht es los, z. B. hier.", "Um 12:30 Uhr geht es los (z. B. hier).")
        val odd = Regex(""" [,.;:!?]|,,|  |[.;:!?],|,[.;:!?]|^[ ,]|[ ,]$""")
        (Levels.all.flatMap { listOf(it.mission, it.tip) + it.way } + Levels.chapters.flatMap { it.intro }).forEach { shown ->
            val t = Speech.clean(shown)
            assertTrue("Sonderzeichen übrig: $shown → $t", t.all { it.isLetterOrDigit() && it.code < 0x250 || it in " .,;:!?-" })
            assertNull("Satzzeichen unsauber: $shown → $t", odd.find(t))
        }
    }
}
