package de.experimentopia.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTest {
    @Test fun formelnUndEinheiten() {
        assertEquals("H 2 O ist Wasser.", Speech.clean("H₂O ist Wasser."))
        assertEquals("C O 2 und O H minus", Speech.clean("CO₂ und OH⁻"))
        assertEquals("F e 2 O 3", Speech.clean("Fe₂O₃"))
        assertEquals("N a O H ist ätzend", Speech.clean("NaOH ist ätzend"))
        assertEquals("g ist 9,81 Meter pro Sekunde zum Quadrat", Speech.clean("g ist 9,81 m/s²"))
        assertEquals("Wasser: 1 Gramm pro Kubikzentimeter", Speech.clean("Wasser: 1 g/cm³"))
        assertEquals("1 Sekunde und 3 Sekunden", Speech.clean("1 s und 3 s"))
        assertEquals("0,1 Mol pro Liter", Speech.clean("0,1 mol/L"))
        assertEquals("37 Grad Celsius", Speech.clean("37 °C"))
        assertEquals("4,5 Volt und 0,2 Ampere", Speech.clean("4,5 V und 0,2 A"))
        assertEquals("p H unter 7", Speech.clean("pH unter 7"))
        assertEquals("10 hoch minus 7", Speech.clean("10⁻⁷"))
        assertEquals("F gleich m mal a", Speech.clean("F = m · a"))
        assertEquals("100000 Schläge", Speech.clean("100 000 Schläge"))
        assertEquals("Klasse 3 bis 4", Speech.clean("Kl. 3–4"))
        assertEquals("3 zu 1", Speech.clean("3 : 1"))
        assertEquals("A a gekreuzt mit a a", Speech.clean("Aa × aa"))
        assertEquals("Blutgruppe A B", Speech.clean("Blutgruppe AB"))
        assertEquals("Hallo Blubb!", Speech.clean("👋 Hallo „Blubb“!"))
        assertEquals("2 H 2 plus O 2 ergibt 2 H 2 O", Speech.clean("2 H₂ + O₂ → 2 H₂O"))
        assertEquals("Ein Ei sinkt im Wasser.", Speech.clean("Ein Ei sinkt im Wasser."))
    }
}
