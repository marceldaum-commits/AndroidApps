package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test

/** Jeder Versuch: eindeutige Antwort, Ziel erreichbar, Texte vollständig. */
class CatalogTest {
    private val all = Catalog.all

    @Test fun grundsaetzliches() {
        assertTrue("umfangreich: mindestens doppelt so viele wie in 0.2.0 (71)", all.size >= 142)
        assertEquals("IDs eindeutig", all.size, all.map { it.id }.toSet().size)
        Island.entries.forEach { assertTrue("$it hat Versuche", Catalog.of(it).size >= 6) }
        all.forEach { e ->
            assertEquals("${e.id}: drei Begründungen", 3, e.why.toSet().size)
            assertTrue("${e.id}: Auswahl", e.predict.choices.size in 2..4 && e.transfer.choices.size in 2..4)
            assertEquals("${e.id}: Antworten verschieden", e.predict.choices.size, e.predict.choices.map { it.text }.toSet().size)
            assertTrue("${e.id}: Klassen", e.grades.first >= 3 && e.grades.last <= 10)
            listOf(e.story, e.task, e.reveal, e.fact, e.predict.question, e.transfer.question).forEach { assertTrue(e.id, it.isNotBlank()) }
        }
        // Für Grundschüler gibt es auf jeder Insel etwas
        Island.entries.forEach { i -> assertTrue("$i für Klasse 3", Catalog.of(i).any { 3 in it.grades }) }
        // Inseln sind nach Thema, innerhalb des Themas nach Klassenstufe sortiert
        Island.entries.forEach { i -> val l = Catalog.of(i); assertEquals("$i sortiert", l.sortedWith(compareBy({ it.topic.ordinal }, { it.grades.first })), l) }
    }

    /** Die erwarteten richtigen Antworten – so steht es in jedem Physik-/Chemiebuch. Berechnet werden sie aus den Modellen. */
    @Test fun antwortenStimmen() {
        fun ans(id: String) = Catalog.byId(id)!!.predict.let { it.choices[it.answer].text }
        fun tr(id: String) = Catalog.byId(id)!!.transfer.let { it.choices[it.answer].text }
        assertEquals("der kleine Nagel", ans("w1")); assertEquals("die kleine Münze", ans("w2")); assertEquals("Apfel", ans("w3"))
        assertEquals("geht unter", ans("w4")); assertEquals("schwimmt", ans("w5")); assertEquals("Ja, als Boot geformt", ans("w6"))
        assertEquals("sinkt", ans("w7")); assertEquals("schwebt mittendrin", tr("w7")); assertEquals("Spiritus", ans("w8"))
        assertEquals("mischt sich mit dem Wasser", tr("w8")); assertEquals("etwa die Hälfte", ans("w9")); assertEquals("fast neun Zehntel", tr("w9"))
        assertEquals("Mias Seite geht runter", ans("k1")); assertEquals("Papas Seite geht runter", tr("k1")); assertEquals("Feld 2", ans("k2")); assertEquals("Feld 1", tr("k2"))
        assertEquals("Feld 4", ans("k3")); assertEquals("Geht nicht – Feld 8 gibt es nicht", tr("k3")); assertEquals("Feld 2", ans("k4")); assertEquals("14 kg", tr("k4"))
        assertEquals("118 N", ans("k5")); assertEquals("294 N", tr("k5"))
        assertEquals("Nein, sie ändert nur die Richtung", ans("k6")); assertEquals("196 N", ans("k7")); assertEquals("2 m", tr("k7"))
        assertEquals("4", ans("k8")); assertEquals("12 m", tr("k8")); assertEquals("lange Rampe", ans("k9")); assertEquals("123 N", tr("k9"))
        assertEquals("Eisennagel", ans("s1")); assertEquals("Lampe leuchtet", ans("s2")); assertEquals("Lampe bleibt aus", tr("s2"))
        assertEquals("nur wenn beide zu sind", ans("s3")); assertEquals("wenn mindestens ein Schalter zu ist", ans("s4"))
        assertEquals("dunkler", ans("s5")); assertEquals("geht aus", tr("s5")); assertEquals("gleich hell", ans("s6")); assertEquals("leuchtet weiter", tr("s6"))
        assertEquals("Lampe geht aus – Kurzschluss!", ans("s7")); assertEquals("22,5 Ω", ans("s8")); assertEquals("halbiert sich", tr("s8"))
        assertEquals("0,2 A", ans("s9")); assertEquals("0,9 A", tr("s9")); assertEquals("Nein, nur manche", ans("s10")); assertEquals("nein", tr("s10"))
        assertEquals("Sie stoßen sich ab", ans("s11")); assertEquals("Sie ziehen sich an", tr("s11"))
        assertEquals("rot", ans("c1")); assertEquals("grün", tr("c1")); assertEquals("sauer", ans("c2")); assertEquals("basisch", tr("c2"))
        assertEquals("10 mL", ans("c3")); assertEquals("5 mL", tr("c3")); assertEquals("fast 12", ans("c4")); assertEquals("2", ans("c5")); assertEquals("3", tr("c5"))
        assertEquals("Er bläst sich auf", ans("c6")); assertEquals("Nur bis der Essig verbraucht ist", tr("c6")); assertEquals("Wasser", ans("c7")); assertEquals("2", tr("c7"))
        assertEquals("2", ans("c8")); assertEquals("2", ans("c9")); assertEquals("2", tr("c9")); assertEquals("3", ans("c10")); assertEquals("1", ans("c11"))
        assertEquals("6", ans("c12")); assertEquals("5", ans("c13")); assertEquals("3", tr("c13"))
        assertEquals("Kerzenwachs", ans("h1")); assertEquals("bleibt bei 0 °C stehen", ans("h2")); assertEquals("nein, es bleibt bei 100 °C", tr("h2"))
        assertEquals("50 °C", ans("h4")); assertEquals("60 °C", tr("h4")); assertEquals("100 g", ans("h5")); assertEquals("etwa 28 °C", ans("h6"))
        assertEquals("Gelb", ans("l1")); assertEquals("Weiß", tr("l1")); assertEquals("Grün + Blau", ans("l2")); assertEquals("Pink (Magenta)", tr("l2"))
        assertEquals("Weiß", ans("l3")); assertEquals("Grün", tr("l3")); assertEquals("Grün", ans("l4")); assertEquals("Schwarz", tr("l4")); assertEquals("Violett", tr("l5"))
        assertEquals("tiefer", ans("l6")); assertEquals("tiefer", tr("l6")); assertEquals("höher", ans("l7")); assertEquals("etwa 15 cm", ans("l8")); assertEquals("etwa 19 cm", tr("l8"))
        assertEquals("die mit am meisten Wasser", ans("l9")); assertEquals("eine Oktave höher", ans("l10")); assertEquals("1,5-fache Frequenz (eine Quinte)", tr("l10"))
        assertEquals("Kugel", ans("f1")); assertEquals("gleichzeitig", tr("f1")); assertEquals("Papierkugel", ans("f2")); assertEquals("gleichzeitig", ans("f3"))
        assertEquals("gleichzeitig", tr("f3")); assertEquals("gleichzeitig", ans("f4")); assertEquals("Hammer", tr("f4"))
        assertEquals("etwa 2 s", ans("f5")); assertEquals("etwa 4 s", tr("f5")); assertEquals("etwa 2,5-mal so lang", ans("f6")); assertEquals("schneller unten", tr("f6"))
        assertEquals("beide gleich", ans("f7")); assertEquals("schneller", tr("f7")); assertEquals("doppelt so lang", ans("f8")); assertEquals("dreifache Dauer", tr("f8"))
        assertEquals("beide gleich schnell", ans("n1")); assertEquals("Mia", ans("n2")); assertEquals("dreimal so schnell", tr("n2"))
        assertEquals("nur halb so schnell", ans("n3")); assertEquals("ein Drittel der Beschleunigung", tr("n3")); assertEquals("sie verdoppelt sich", ans("n4")); assertEquals("10 m/s²", tr("n4"))
        assertEquals("etwa 1 m", ans("f9")); assertEquals("geht langsamer", tr("f9"))
    }

    /** Ziele, die man einstellen muss, sind mit den Reglern auch erreichbar. */
    @Test fun zieleErreichbar() {
        all.forEach { e ->
            when (val s = e.setup) {
                is LeverSetup -> if (s.goal == LeverGoal.BALANCE) assertTrue("${e.id}: Gleichgewicht möglich", s.tray.any { Lever.balancePos(s.fixed, it.massKg) != null })
                is PulleySetup -> s.maxN?.let { assertNotNull(e.id, Pulley.minRopes(s.massKg, it, s.ropes)) }
                is CircuitSetup -> {
                    if (s.goal == CircuitGoal.AMPS) {
                        val i = s.adjustable!!
                        val ok = Lab.STROM.let { (2..200).map { it * 0.5 }.any { r -> val sl = s.start.toMutableList().also { it[i] = Slot(Part.WIDERSTAND, r) }; Fmt.close(Circuits.solve(s.layout, sl, s.volts).totalA, s.targetA!!, 0.005) } }
                        assertTrue("${e.id}: Stromstärke einstellbar", ok)
                    }
                    if (s.goal == CircuitGoal.TEST_ALL) assertTrue(s.tray.all { it.isMaterial })
                    assertEquals(e.id, s.layout.slots.size, s.start.size)
                }
                is TitrationSetup -> {
                    var b = Beaker().addAcid(s.acidC, s.acidMl); var steps = 0
                    while (kotlin.math.abs(b.pH - s.targetPh) > s.tolPh && steps < 500) { b = if (s.baseC != null) b.addBase(s.baseC, s.stepMl) else b.addWater(s.stepMl); steps++ }
                    assertTrue("${e.id}: Ziel-pH erreichbar", steps < 500)
                }
                is FizzSetup -> assertTrue(e.id, (1..s.maxSpoons).any { Fizz.gasL(it.toDouble(), s.vinegarMl) >= s.targetL })
                is MixSetup -> if (s.targetC != null) assertTrue(e.id, (1..50).any { Fmt.close(Heat.mix(it * s.adjustA!!, s.a.c, s.tA, s.massB, s.b.c, s.tB), s.targetC, 1.0) })
                is SoundSetup -> s.target?.let { n -> assertTrue(e.id, (0..250).any { mm -> Klang.inTune(Klang.tubeHz(Klang.TUBE_M - mm / 1000.0), n.hz) }) }
                is PendulumSetup -> s.targetT?.let { t -> assertTrue(e.id, (10..200).any { cm -> Fmt.close(Pendulum.period(cm / 100.0, Planet.ERDE.g, s.amplitudeDeg), t, t * 0.01) }) }
                is LightSetup -> s.target?.let { t ->
                    val steps = listOf(0.0, 0.25, 0.5, 0.75, 1.0)
                    val ok = steps.any { a -> steps.any { b -> steps.any { c -> (if (s.mode == LightMode.ADD) Rgb(a, b, c) else Colors.filter(a, b, c)).close(t) } } }
                    assertTrue(e.id, ok)
                }
                is ReactionSetup -> assertNotNull(Reactions.byId(s.reactionId))
                is FallSetup -> assertTrue(s.planets.isNotEmpty())
                else -> {}
            }
        }
    }

    @Test fun themenUndSuche() {
        Topic.entries.forEach { assertTrue("$it hat Versuche", it.experiments().isNotEmpty()) }
        all.forEach { it.topic }   // jeder Versuch hat ein Thema
        assertTrue(Search.find("Newton").containsAll(Topic.NEWTON.experiments()))
        assertTrue(Search.find("magnet").any { it.id == "s10" })
        assertTrue(Search.find("", Subject.CHEMIE).all { it.topic.subject == Subject.CHEMIE })
        assertEquals(Topic.REAKTION.experiments().size, Search.find("", topic = Topic.REAKTION).size)
    }

    @Test fun freiesLaborHatJedesGeraet() { Lab.entries.forEach { assertEquals(it, FreeLab.setup(it).lab) } }

    @Test fun proIstAusgeschaltetUndFair() {
        assertFalse("Pro erst einschalten, wenn man es kaufen kann", Pro.ENABLED)
        all.forEach { assertFalse(Pro.locked(it, proOwned = false)) }
        // Eingeschaltet: jede Insel hat freie Versuche, gekauft ist alles offen
        Island.entries.forEach { i -> assertTrue(Catalog.of(i).count { !Pro.locked(it, false, enabled = true) } >= Pro.FREE_PER_ISLAND) }
        assertTrue(all.none { Pro.locked(it, true, enabled = true) })
        assertTrue(Pro.freeCount in 20 until all.size)
    }

    @Test fun raetselDesTagesWechselt() {
        val days = (0L until all.size.toLong()).map { Daily.forDay(20_000 + it).id }
        assertEquals("jeder Versuch kommt einmal dran", all.size, days.toSet().size)
        assertEquals(Daily.forDay(5), Daily.forDay(5))
    }

    @Test fun sterneUndEinschaetzung() {
        assertEquals(1, Score.stars(false, false)); assertEquals(3, Score.stars(true, true))
        assertTrue(Calibration.matches(Sure.SICHER, true)); assertFalse(Calibration.matches(Sure.SICHER, false))
        val rows = Calibration.rows(List(4) { Sure.SICHER to true } + (Sure.UNSICHER to false))
        assertTrue(Calibration.advice(rows).contains("meistens richtig"))
        assertTrue(Badges.earned(Stats(done = 1, ahas = 1)).map { it.id }.containsAll(listOf("erster", "aha1")))
    }
}

class IslandGradeTest {
    @Test fun klassenangabeStimmt() = Island.entries.forEach { i ->
        val l = Catalog.of(i); val text = "Kl. ${l.minOf { it.grades.first }}–${l.maxOf { it.grades.last }}"
        org.junit.Assert.assertEquals("$i", text, i.grades)
    }
}
