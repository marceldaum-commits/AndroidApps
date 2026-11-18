package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/** Regeln für Raumlicht Pro: Was frei bleibt, was dazugehört, welche Codes gelten. */
class ProTest {
    /** Seit 2.18.7 ist die Kasse eingetragen (Produkt „Raumlicht Pro“ im Shop) – damit sind die Pro-Teile gesperrt, bis ein Code freischaltet. */
    @Test fun kasseIstEingetragen() {
        assertTrue(Pro.CHECKOUT_URL.startsWith("https://www.mathenachhilfe-online.com/kasse/")); assertTrue(Pro.ENABLED); assertEquals(64, Pro.PUBLIC_KEY.length)
        assertEquals("7,99 €", Pro.PRICE); assertEquals("Raumlicht Pro", Pro.NAME)
    }

    // Schlüssel nur für Tests (Seed 0…31) – mit dem echten Schlüssel kann hier niemand Codes erzeugen
    private val pk = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val pass = "RL-04202-0000F-MGKKT-CDRGB-BYPG5-8XEVQ-M0F02-94P76-M1MXZ-28QYX-86PN1-NEY0T-JNXA5-8S0GJ-HA3X2-C0GQW-AW2K1-BQN3M-4PV5F-ZHQBG-ZKMPE-KJSDT-6VARJ-0W"
    private val kurvenflug = "RL-040G2-0000F-MGKKV-QD10E-XXXJN-05Q3B-DPG8M-0VWZR-KNQ5R-DR4ND-G4XD5-BKQZV-8V162-V9YKD-2DF1D-3MTYY-ZRFHM-74P0G-PVYQF-V8ZE0-CQJ6C-SNV78-NR1SN-GG"
    private val anderesProdukt = "RL-04204-0000F-MGKKV-FSHJX-JTBS8-VZG6K-DCNPG-7TDQC-W9MJX-SNY83-5TXZB-T5GGM-7M7PS-VH08E-40M4F-78TS6-SCG47-35ACT-NGGPF-EJB4G-T6YGD-XY73H-GX3GE-04"

    @Test fun gueltigerCode() {
        val i = License.check(pass, pk)!!
        assertEquals(License.APP_ID, i.app); assertEquals(License.PRODUCT_PASS, i.product); assertEquals(1001L, i.serial)
        assertTrue(Pro.active(pass, pk))
        // mitten in einer E-Mail, als Link ohne Bindestriche, klein geschrieben
        assertTrue(Pro.active(License.extract("Hallo! Die URL und dein Code:\n$pass\nViel Erfolg im Abi!"), pk))
        assertTrue(Pro.active(License.extract("https://raumlicht.mathenachhilfe-online.com/?pro=" + pass.replace("-", "")), pk))
        assertTrue(Pro.active(pass.lowercase(), pk))
        assertEquals(pass, License.extract(pass.replace("-", " ")))
    }

    /** Der Link aus der E-Mail: Code ohne Bindestriche, mit Vorsilbe (so verschickt ihn der Shop), ohne Vorsilbe, mit Bindestrichen – und Unsinn fällt durch */
    @Test fun linkAusDerMail() {
        val plain = pass.replace("-", "")
        assertEquals(pass, Pro.fromLink(plain, pk)); assertEquals(pass, Pro.fromLink(plain.removePrefix("RL"), pk)); assertEquals(pass, Pro.fromLink(pass, pk)); assertEquals(pass, Pro.fromLink(plain.lowercase(), pk))
        assertNull(Pro.fromLink(plain.substring(0, 60) + (if (plain[60] == 'A') 'B' else 'A') + plain.substring(61), pk)); assertNull(Pro.fromLink("", pk)); assertNull(Pro.fromLink("RL12345", pk)); assertNull(Pro.fromLink(plain))
    }

    @Test fun falscheCodesFallenDurch() {
        val bad = pass.substring(0, 20) + (if (pass[20] == 'A') 'B' else 'A') + pass.substring(21)
        assertFalse("ein Zeichen verändert", Pro.active(bad, pk))
        assertFalse("anderer Schlüssel", Pro.active(pass, "1" + pk.drop(1)))
        assertFalse("mit dem Test-Schlüssel erzeugt – der echte nimmt ihn nicht", Pro.active(pass))
        assertFalse("Code einer anderen App", Pro.active(kurvenflug, pk)); assertFalse("anderes Produkt", Pro.active(anderesProdukt, pk))
        assertFalse(Pro.active(pass, "")); assertFalse(Pro.active("RL-12345", pk)); assertFalse(Pro.active(null, pk)); assertFalse(Pro.active("", pk))
        assertNull(License.extract("kein Code, nur Kerle und Perlen"))
    }

    @Test fun kodierungHinUndZurueck() { val bytes = ByteArray(73) { (it * 37 + 5).toByte() }; assertArrayEquals(bytes, License.decode(License.encode(bytes))) }

    @Test fun grenzeFreiPro() {
        // Kapitel 1–6 komplett frei, danach je Kapitel genau die ersten zwei
        Levels.chapters.forEach { c -> val free = c.levels.count { Pro.freeLevel(it.id) }
            assertEquals("Kapitel ${c.number}", if (c.number <= 6) c.levels.size else 2, free)
            if (c.number > 6) assertEquals(listOf(true, true, false), c.levels.take(3).map { Pro.freeLevel(it.id) }) }
        assertEquals(Levels.chapters.filter { it.number <= 6 }.sumOf { it.levels.size } + 20 * 2, Levels.all.count { Pro.freeLevel(it.id) })
        assertFalse(Pro.locked("level/1-6")); assertFalse(Pro.locked("level/7-2")); assertTrue(Pro.locked("level/7-3")); assertTrue(Pro.locked("level/25-7"))
        // Tagesaufgabe, Spiele und Grundfunktionen bleiben frei
        listOf("home", "tagesaufgabe", "jam", "spickzettel", "abzeichen", "backstage", "abi", "achterbahn/huegel", "raumflug", "galton", "ballerbude", "zufall", "einstellungen").forEach { assertFalse(it, Pro.locked(it)) }
        // Üben: nur „Einstieg“ frei
        assertFalse(Pro.locked("jam/MIX/EINSTIEG")); assertTrue(Pro.locked("jam/MIX/ABI")); assertTrue(Pro.locked("jam/PUNKTE/PROFI")); assertTrue(Pro.locked("jam/MIX"))
        // Prüfungsaufgaben: die erste frei
        assertEquals(1, Exams.all.count { Pro.freeExam(it.id) }); assertFalse(Pro.locked("abi/${Exams.all[0].id}/0")); assertTrue(Pro.locked("abi/${Exams.all[3].id}/1"))
        Pro.areas.keys.forEach { assertTrue(it, Pro.locked(it)) }
    }

    @Test fun texte() {
        assertEquals("Aufgabe 7-3 „${Levels.byId("7-3")!!.title}“", Pro.what("level/7-3")); assertEquals("Üben in der Stufe „Profi“", Pro.what("jam/MIX/PROFI")); assertEquals("Binomial-Labor", Pro.what("binomial"))
        assertTrue(Pro.benefits.first().contains("${Levels.all.size} Aufgaben")); assertTrue(Pro.free.isNotEmpty())
    }
}
