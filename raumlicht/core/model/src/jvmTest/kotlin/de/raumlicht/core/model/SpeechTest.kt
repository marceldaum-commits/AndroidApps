package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

class SpeechTest {
    @Test fun formulaSignsBecomeWords() {
        assertEquals("Genau 25 Prozent, Treffer!", Speech.clean("Genau 25 % – Treffer!"))
        assertEquals("Flieg zum Punkt 4, 2, 0!", Speech.clean("Flieg zum Punkt (4|2|0)!"))
        assertEquals("Fast, der Punkt liegt bei 3.", Speech.clean("Fast (der Punkt liegt bei 3)."))
        assertEquals("f Strich gleich 0 und f zwei Strich 0", Speech.clean("f′ = 0 und f″ < 0"))
        assertEquals("V gleich 16 Pi", Speech.clean("V = 16π")); assertEquals("x hoch 2 minus 2x minus 3", Speech.clean("x² − 2x − 3"))
        assertEquals("P gleich 3 durch 10", Speech.clean("P = 3 : 10")); assertEquals("Tipp gefällig?", Speech.clean("Tipp gefällig? 💡"))
        assertEquals("ein halb mal ein halb gleich ein Viertel", Speech.clean("½ · ½ = ¼"))
        assertEquals("", Speech.clean("🎈✨"))
        // jede Sprechblase der Tour bleibt lesbar: nichts als Buchstaben, Ziffern und Satzzeichen
        Levels.all.forEach { l -> val t = Speech.clean(l.mission); assertTrue(l.id, t.isNotEmpty() && t.all { it.isLetterOrDigit() || it in " .,;:!?-/" }) }
    }
}
