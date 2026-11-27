package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Theorie-Karten: Jede Aufgabe bekommt die passende Karte, die Texte halten Länge und Schreibweise ein. Schreibt build/theorie.md zum Gegenlesen. */
class TheoryTest {
    private val tour = Levels.all
    private val exams = Exams.all.flatMap { e -> e.parts.map { it.level } }
    private val guides = Guides.all.flatMap { g -> g.parts.map { it.level } }
    private val seeds = 1L..5L
    private val generated: List<Pair<String, Level>> =
        Gen.active.flatMap { k -> seeds.map { "jam-${k.name.lowercase()}" to Gen.level(k, it) } } +
            MoreGen.Kind.entries.flatMap { k -> seeds.map { "mehr-${k.name.lowercase()}" to MoreGen.level(k, it) } } +
            ProGen.Kind.entries.flatMap { k -> seeds.map { "profi-${k.name.lowercase()}" to ProGen.level(k, it) } }
    private val dailies = (20_770L until 20_800L).map { Daily.forDay(it) }

    private fun texts(c: TheoryCard) = listOf(c.title, c.idea, c.trap) + c.steps + c.example

    @Test fun festeAufgabenUeberDieId() {
        assertEquals(107, tour.size); assertEquals(44, exams.size); assertEquals(9, guides.size)
        (tour + exams + guides).forEach { l ->
            assertEquals("${l.id} „${l.title}“ steht nicht in der ID-Tabelle", 1, Theory.stage(l))
            assertTrue(l.id, Theory.forLevel(l) in Theory.cards)
        }
    }

    @Test fun erzeugteAufgabenUeberDenTitel() {
        (generated.map { it.second } + dailies).forEach { l ->
            assertEquals("${l.id} „${l.title}“ fehlt in der Titel-Tabelle", 2, Theory.stage(l))
            val card = Theory.forLevel(l)
            assertTrue(l.id, card in Theory.cards); assertNotSame(l.id, Theory.general, card)
        }
        // Tagesaufgaben verraten ihre Art nicht in der ID – dieselbe Art führt trotzdem zur selben Karte wie beim Üben
        dailies.forEach { d -> assertTrue(d.id, generated.any { (_, l) -> l.title == d.title && Theory.forLevel(l) == Theory.forLevel(d) }) }
        // Jede Art hat ihr eigenes Thema: Nie landen alle Arten auf derselben Karte
        assertTrue(generated.map { Theory.forLevel(it.second).key }.toSet().size >= 15)
    }

    @Test fun rueckfall() {
        fun fake(id: String, chapter: Int, title: String) = Gen.level(Gen.Kind.MIDPOINT, 1).let { l ->
            Level(id, chapter, title, l.mission, l.bounds, l.controls, l.solution, l.tip, l.way, formula = l.formula, objects = l.objects, evaluate = l.evaluate) }
        assertEquals("bewegung", Theory.forLevel(fake("neu-1", 11, "Unbekannt")).key); assertEquals(3, Theory.stage(fake("neu-1", 11, "Unbekannt")))
        Levels.chapters.map { it.number }.forEach { ch -> assertEquals(3, Theory.stage(fake("neu-$ch", ch, "Unbekannt"))) }
        assertSame(Theory.general, Theory.forLevel(fake("neu-2", 0, "Unbekannt"))); assertEquals(4, Theory.stage(fake("neu-2", 0, "Unbekannt")))
        // ID geht vor Titel, Titel vor Kapitel
        assertEquals("spiegeln-koordinaten", Theory.forLevel(tour.first { it.id == "1-5" }).key)      // heißt „Spiegelbild“ wie die Profi-Übung
        assertEquals("spiegeln-ebene", Theory.forLevel(fake("tag-1", 3, "Spiegelbild")).key)
        // Der Kapitel-Rückfall trifft das Hauptthema: Mindestens eine Aufgabe des Kapitels benutzt genau diese Karte
        Levels.chapters.forEach { c -> val key = Theory.forLevel(fake("neu", c.number, "Unbekannt")).key
            assertTrue("Kapitel ${c.number}: $key", c.levels.any { Theory.forLevel(it).key == key }) }
    }

    @Test fun kartenSindVollstaendigUndKurz() {
        val all = Theory.cards + Theory.erleben + Theory.general
        assertEquals(all.size, all.map { it.key }.toSet().size)
        assertEquals(all.size, all.map { it.title }.toSet().size)
        assertEquals(all.size, all.map { it.emoji }.toSet().size)
        val used = (tour + exams + guides + generated.map { it.second } + dailies).map { Theory.forLevel(it).key }.toSet()
        Theory.cards.forEach { c -> assertTrue("${c.key} wird von keiner Aufgabe benutzt", c.key in used) }
        assertFalse(Theory.general.key in used)
        all.forEach { c ->
            assertTrue(c.key, Regex("[a-z]+(-[a-z]+)*").matches(c.key)); assertTrue(c.key, c.emoji.isNotBlank() && c.title.isNotBlank())
            assertTrue("${c.key}: idea ${c.idea.length}", c.idea.length in 40..180)
            assertTrue("${c.key}: steps", c.steps.size in 2..4); assertTrue("${c.key}: example", c.example.size in 2..4)
            (c.steps + c.example).forEach { assertTrue("${c.key}: ${it.length} Zeichen: $it", it.length in 3..110) }
            assertTrue("${c.key}: trap ${c.trap.length}", c.trap.length in 20..140)
            assertFalse(c.key, c.trap.startsWith("Achtung")); assertTrue(c.key, c.trap.first().isUpperCase() || c.trap.first() in "„(") ; assertTrue(c.key, c.trap.last() in ".!“")
            assertTrue("${c.key}: sheet", c.sheet == null || c.sheet in Sheet.cards.indices)
            texts(c).forEach { t -> assertEquals(c.key, t.trim(), t); assertFalse("${c.key}: $t", "  " in t) }
        }
        // Formelsammlung: Geometrie-, Analysis- und Stochastik-Karten zeigen auf die passende Seite
        assertEquals("Kugeln", Sheet.cards[Theory.cards.first { it.key == "schnittkreis" }.sheet!!].title)
        assertEquals("Wahrscheinlichkeit", Sheet.cards[Theory.card("pfadregeln")!!.sheet!!].title)
        assertEquals("Rotationskörper", Sheet.cards[Theory.card("rotation")!!.sheet!!].title)
        assertEquals("Bewegung", Sheet.cards[Theory.cards.first { it.key == "treffpunkt" }.sheet!!].title)
        assertTrue("${Theory.cards.size} Karten zu Aufgaben", Theory.cards.size in 40..75); assertTrue(Theory.cards.none { it in Theory.erleben })
    }

    @Test fun schreibweise() {
        val bad = listOf(
            Regex("-\\d") to "ASCII-Minus vor einer Ziffer", Regex("\\d\\.\\d") to "Dezimalpunkt", Regex(" [+−·:] [−+] ") to "doppeltes Rechenzeichen",
            Regex(" [+−·:] −\\d") to "negative Zahl ohne Klammer hinter einem Rechenzeichen", Regex("(?<![\\d,])1 Einheiten") to "1 Einheiten",
            Regex(" - ") to "Bindestrich statt Gedankenstrich", Regex("\"|'") to "gerade Anführungszeichen", Regex("\\*|/") to "Mal- oder Geteiltzeichen der Tastatur",
            Regex("\\d%") to "Prozent ohne Abstand", Regex("\\.\\.\\.") to "drei Punkte statt …", Regex("(?i)\\bachtung\\b") to "„Achtung“ setzt die Oberfläche",
            Regex("[a-zA-Z]->|=>|<=|>=|!=") to "ASCII-Pfeil oder -Vergleich", Regex("\\bx[123]\\b") to "Index nicht tiefgestellt", Regex("\\^[0-9]") to "Hochzahl mit ^",
        )
        (Theory.cards + Theory.general).forEach { c -> texts(c).forEach { t -> bad.forEach { (re, what) -> assertFalse("${c.key}: $what in „$t“", re.containsMatchIn(t)) } } }
        // Vektorpfeil steht immer hinter einem Buchstaben oder Index, Anführungszeichen sind paarig
        (Theory.cards + Theory.general).forEach { c -> texts(c).forEach { t ->
            assertFalse("${c.key}: $t", Regex("(?<![A-Za-z₁₂′])\u20D7").containsMatchIn(t))
            assertEquals("${c.key}: $t", t.count { it == '„' }, t.count { it == '“' })
            assertEquals("${c.key}: $t", t.count { it == '(' }, t.count { it == ')' })
        } }
    }

    /** Strenge Schreibweise mit Ortsvektoren: darf Analysis und Stochastik nicht anfassen und muss in der Geometrie aus „B − A“ „OB⃗ − OA⃗“ machen */
    @Test fun strengeSchreibweise() {
        // Karten ohne Vektoren (Analysis und Zufall zum Erleben) bleiben in strenger Schreibweise unverändert
        assertTrue(Theory.erleben.size >= 25)
        Theory.erleben.forEach { c -> texts(c).forEach { t -> assertEquals(c.key, t, Notation.strict(t)) } }
        fun card(key: String) = Theory.cards.first { it.key == key }
        assertEquals("AB⃗ = OB⃗ − OA⃗ (Spitze minus Fuß)", Notation.strict(card("verbindungsvektor").steps[0]))
        assertEquals("OM⃗ = ½ · (OA⃗ + OB⃗)", Notation.strict(card("mittelpunkt").steps[0]))
        assertEquals("g: x⃗ = OA⃗ + t · d⃗", Notation.strict(card("richtungsvektor").steps[0]))
        assertEquals("OP′⃗ = 2 · OF⃗ − OP⃗ (dasselbe wie OP⃗ + 2 · PF⃗)", Notation.strict(card("spiegeln-ebene").steps[2]))
    }

    @Test fun uebersichtSchreiben() {
        val users = LinkedHashMap<String, MutableList<String>>()
        fun add(l: Level, label: String) { val list = users.getOrPut(Theory.forLevel(l).key) { ArrayList() }; if (label !in list) list += label }
        (tour + exams + guides).forEach { add(it, it.id) }
        generated.forEach { (kind, l) -> add(l, "$kind-… („${l.title}“)") }
        dailies.forEach { add(it, "tag-… („${it.title}“)") }
        val md = buildString {
            append("# Theorie-Karten (${Theory.cards.size} + allgemeine Karte)\n\n")
            (Theory.cards + Theory.general).forEach { c ->
                append("## ${c.emoji} ${c.title} · `${c.key}`\n\n")
                append("Formelsammlung: ${c.sheet?.let { "${it} „${Sheet.cards[it].title}“" } ?: "–"}\n\n")
                append("**Idee:** ${c.idea}\n\n**So gehst du vor:**\n"); c.steps.forEachIndexed { i, s -> append("${i + 1}. $s\n") }
                append("\n**Beispiel:**\n"); c.example.forEach { append("- $it\n") }
                append("\n**Achtung:** ${c.trap}\n\n")
                val strict = texts(c).filter { Notation.strict(it) != it }
                if (strict.isNotEmpty()) { append("**Streng (Ortsvektoren):**\n"); strict.forEach { append("- ${Notation.strict(it)}\n") }; append("\n") }
                append("**Aufgaben (${users[c.key]?.size ?: 0}):** ${users[c.key]?.joinToString(", ") ?: "–"}\n\n")
            }
        }
        File("build").mkdirs(); File("build/theorie.md").writeText(md)
        assertTrue(md.length > 20_000)
    }
}
