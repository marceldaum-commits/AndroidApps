package de.raumlicht.core.model

import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/**
 * Kapitel 27: Lineare Gleichungssysteme und Gauß-Verfahren – als drei Ebenen im Raum.
 * Jede Gleichung ist eine Scheibe; die Lösung ist der Punkt (oder die Gerade), in dem sich alle drei treffen.
 * Jede Lösung wird im Test unabhängig nachgerechnet (TourTenTest).
 */
internal object TourTen {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)

    /** Eine Gleichung des Systems: Koeffizienten und rechte Seite */
    class Eq(val n: V3, val c: Int)

    private fun text(e: Eq, name: String): List<Part> = listOf(Txt("$name: ${Geo.planeText(e.n, e.c)}"))
    private fun system(vararg e: Eq) = e.mapIndexed { i, q -> text(q, listOf("I", "II", "III")[i]) }
    private fun planes(vararg e: Eq) = e.mapIndexed { i, q -> Stage.plane("E${i + 1}", q.n, q.c.toDouble(), listOf("cyan", "violet", "pink")[i], listOf("I", "II", "III")[i]) }

    // Die Systeme – öffentlich für den Test
    val s1 = listOf(Eq(p(1, 0, 0), 2), Eq(p(1, 1, 0), 5), Eq(p(1, 1, 1), 9));          val p1 = p(2, 3, 4)
    val s2 = listOf(Eq(p(1, 1, 1), 9), Eq(p(1, 1, -1), 3), Eq(p(1, -1, 0), -2));       val p2 = p(2, 4, 3)
    val s3 = listOf(Eq(p(1, 2, 1), 8), Eq(p(2, 1, -1), 1), Eq(p(1, -1, 2), 5));        val p3 = p(1, 2, 3)
    /** III = I + II: unendlich viele Lösungen auf einer Geraden (4 − t | 2 | t) */
    val s5 = listOf(Eq(p(1, 1, 1), 6), Eq(p(1, -1, 1), 2), Eq(p(2, 0, 2), 8));         val p5 = p(3, 2, 1)
    /** I und II sind parallel, aber verschieden: keine Lösung */
    val s6 = listOf(Eq(p(1, 1, 1), 3), Eq(p(1, 1, 1), 5), Eq(p(1, -1, 0), 0));         val n6 = 0
    val s7 = listOf(Eq(p(2, 1, -1), 3), Eq(p(1, -1, 2), 5), Eq(p(1, 1, 1), 5));        val p7 = p(2, 1, 2)
    /** Rechenschritt in Aufgabe 27-4: II − k · I lässt x₁ verschwinden */
    val s4 = listOf(Eq(p(1, 2, 1), 4), Eq(p(3, 1, -1), 7));                            const val K4 = 3

    private val ch27 = listOf(
        Families.point("27-1", 27, "Die Treppe", "Drei Scheiben treffen sich in einem Punkt. Sie sind schon wie eine Treppe sortiert: Aus Scheibe I liest du x₁ ab, dann kommt x₂, zuletzt x₃. Wo landet Lux?",
            p1, hidden = true, tip = "I: x₁ = 2. In II einsetzen: 2 + x₂ = 5. Dann beide in III.",
            way = listOf("I: x₁ = 2", "II: 2 + x₂ = 5, also x₂ = 3", "III: 2 + 3 + x₃ = 9, also x₃ = 4"),
            given = system(*s1.toTypedArray()), name = "P", extras = { planes(*s1.toTypedArray()) },
            mistakes = listOf(p(2, 5, 9) to "Das sind die rechten Seiten. Setze die schon gefundenen Werte ein und rechne die Reste aus: x₂ = 5 − 2.")),
        Families.point("27-2", 27, "Einmal eliminieren", "Die dritte Scheibe kennt nur x₁ und x₂. Mit einem Trick verschwindet x₃: I und II zusammenzählen. Wo treffen sich alle drei?",
            p2, hidden = true, tip = "I + II: 2x₁ + 2x₂ = 12, also x₁ + x₂ = 6. Zusammen mit III (x₁ − x₂ = −2) bekommst du x₁ und x₂.",
            way = listOf("I + II: 2x₁ + 2x₂ = 12, also x₁ + x₂ = 6", "mit III: x₁ − x₂ = −2 → addiert 2x₁ = 4, also x₁ = 2 und x₂ = 4", "in I: 2 + 4 + x₃ = 9, also x₃ = 3"),
            given = system(*s2.toTypedArray()), extras = { planes(*s2.toTypedArray()) },
            mistakes = listOf(p(4, 2, 3) to "x₁ und x₂ sind vertauscht: Aus x₁ + x₂ = 6 und x₁ − x₂ = −2 folgt x₁ = 2 (die Differenz ist negativ, also ist x₁ der kleinere Wert).")),
        Families.point("27-3", 27, "Gauß in drei Schritten", "Jetzt das echte Verfahren: Erst x₁ aus den unteren Zeilen entfernen, dann x₂ aus der letzten. So wird aus dem Gleichungssystem eine Treppe. Wo treffen sich die Scheiben?",
            p3, hidden = true, tip = "II − 2·I: −3x₂ − 3x₃ = −15. III − I: −3x₂ + x₃ = −3. Diese beiden voneinander abziehen, dann hast du x₃.",
            way = listOf("II − 2·I: −3x₂ − 3x₃ = −15, also x₂ + x₃ = 5", "III − I: −3x₂ + x₃ = −3", "(III−I) − (II−2·I): 4x₃ = 12, also x₃ = 3; dann x₂ = 2 und x₁ = 8 − 4 − 3 = 1"),
            given = system(*s3.toTypedArray()), extras = { planes(*s3.toTypedArray()) },
            mistakes = listOf(p(3, 2, 1) to "x₁ und x₃ sind vertauscht. Setze zur Probe in I ein: 3 + 4 + 1 = 8 stimmt, aber II: 6 + 2 − 1 = 7 ≠ 1.")),
        Families.scalar("27-4", 27, "Der Rechenschritt", "Zeile II soll x₁ verlieren: Du ziehst k-mal Zeile I ab (II − k · I). Welches k brauchst du?", "k", 0..6, K4, 0,
            tip = "Vor x₁ steht in I eine 1 und in II eine 3. 3 − k · 1 soll 0 werden.",
            way = listOf("x₁-Faktor in II: 3, in I: 1", "3 − k · 1 = 0, also k = 3", "II − 3·I: −5x₂ − 4x₃ = −5"), color = "violet",
            formula = { a -> system(*s4.toTypedArray()).take(2) + listOf(listOf(Txt("II − "), Num(a.toString(), "a"), Txt(" · I"))) },
            objects = { _, _ -> planes(*s4.toTypedArray()) },
            check = { a -> Families.compare(a, K4, "Der Faktor", "k = 3 – x₁ fällt weg, übrig bleibt eine Gleichung mit nur zwei Unbekannten.", mapOf(1 to "Mit k = 1 steht in II noch 3 − 1 = 2 · x₁. Es muss 0 werden.")) }),
        Families.point("27-5", 27, "Unendlich viele Treffer", "Hier ist III einfach I + II – die dritte Scheibe bringt nichts Neues, die drei treffen sich in einer ganzen Geraden. Such dir den Punkt auf dieser Geraden mit x₃ = 1.",
            p5, hidden = true, tip = "I − II: 2x₂ = 4, also x₂ = 2 immer. Dann bleibt x₁ + x₃ = 4 – mit x₃ = 1 ist x₁ = 3.",
            way = listOf("III (2x₁ + 2x₃ = 8) ist I + II – keine neue Information", "I − II: 2x₂ = 4, also x₂ = 2; bleibt x₁ + x₃ = 4", "Gerade: (4 − t | 2 | t). Mit x₃ = 1: P(3|2|1)"),
            given = system(*s5.toTypedArray()), extras = { planes(*s5.toTypedArray()) },
            mistakes = listOf(p(2, 2, 2) to "Der Punkt (2|2|2) liegt auf der Geraden, hat aber x₃ = 2. Gesucht ist x₃ = 1.")),
        Families.scalar("27-6", 27, "Gibt es einen Treffer?", "Wie viele Lösungen hat dieses System? Schau auf die Scheiben: 0 = keine, 1 = genau eine, 2 = unendlich viele.", "n", 0..2, 0, 1,
            tip = "Vergleiche I und II: gleiche linke Seite, aber 3 ≠ 5. Zwei parallele Scheiben berühren sich nie.",
            way = listOf("I − II: 0 = −2 – ein Widerspruch", "Die Scheiben I und II sind parallel und verschieden", "keine Lösung"), color = "pink",
            formula = { a -> system(*s6.toTypedArray()) + listOf(listOf(Txt("Lösungen: "), Num(listOf("keine", "genau eine", "unendlich viele")[a], "a"))) },
            objects = { _, _ -> planes(*s6.toTypedArray()) },
            check = { a -> Families.compare(a, 0, "Die Anzahl", "Keine Lösung – ein Widerspruch wie 0 = −2 heißt: Die Scheiben treffen sich nie.",
                mapOf(1 to "Eine Lösung gäbe es, wenn I − II etwas Brauchbares ergäbe. Es kommt aber 0 = −2 heraus.", 2 to "Unendlich viele gäbe es bei 0 = 0. Hier steht 0 = −2 – ein Widerspruch.")) }),
        Families.point("27-7", 27, "Abi-Check: Drei Laser", "Drei Laserebenen schneiden sich in genau einem Punkt – dort steht der Sensor. Löse das Gleichungssystem und bringe Lux hin.",
            p7, hidden = true, abi = true, tip = "Aus III folgt x₃ = 5 − x₁ − x₂. In I und II einsetzen – dann hast du ein System mit nur zwei Unbekannten.",
            way = listOf("III: x₃ = 5 − x₁ − x₂", "in I: 2x₁ + x₂ − 5 + x₁ + x₂ = 3, also 3x₁ + 2x₂ = 8; in II: x₁ − x₂ + 10 − 2x₁ − 2x₂ = 5, also −x₁ − 3x₂ = −5", "x₁ = 5 − 3x₂ eingesetzt: 15 − 9x₂ + 2x₂ = 8, also x₂ = 1, x₁ = 2, x₃ = 2"),
            given = system(*s7.toTypedArray()), extras = { planes(*s7.toTypedArray()) },
            mistakes = listOf(p(1, 2, 2) to "Zur Probe in I: 2 + 2 − 2 = 2 ≠ 3. Ein Wert ist vertauscht.")),
    )

    val chapters = listOf(
        Chapter(27, "Gleichungssysteme & Gauß", "Lineare Gleichungssysteme mit drei Unbekannten und das Gauß-Verfahren – als drei Scheiben im Raum", "Schaltzentrale", "🧩", "cyan", listOf(
            "In der Schaltzentrale hängen drei Scheiben im Raum. Jede ist eine Gleichung – die Lösung ist der Punkt, in dem sich alle treffen.",
            "Das Gauß-Verfahren räumt auf: Gleichungen so zusammenzählen, dass Unbekannte verschwinden, bis eine Treppe entsteht.",
            "Manchmal treffen sich die Scheiben in einer Geraden – oder nie: Das erkennst du am Ergebnis (0 = 0 oder 0 = −2)."), ch27),
    )
}
