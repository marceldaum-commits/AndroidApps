package de.experimentopia.core.model

import de.experimentopia.core.model.Pick.nearest

/** Blitzinsel und Chemie-Vulkan */
internal object CatalogB {
    private val S = Island.STROM
    private val CH = Island.CHEMIE

    private fun slots(vararg p: Part) = p.map { Slot(it) }
    /** Leuchtet die Lampe (Platz [lamp]), wenn [part] auf Platz [at] liegt? */
    private fun glowsWith(layout: Layout, base: List<Slot>, at: Int, part: Part, lamp: Int, volts: Double = 4.5): Boolean {
        val s = base.toMutableList().also { it[at] = Slot(part) }
        return Circuits.glows(Circuits.solve(layout, s, volts), s, lamp)
    }
    private val tester = Layout.single(2)
    private val testerStart = slots(Part.LEER, Part.LAMPE)
    private val LIGHT = listOf(c("Lampe leuchtet", "💡"), c("Lampe bleibt aus", "🌑"))
    private fun lightIdx(b: Boolean) = if (b) 0 else 1
    private fun brightness(layout: Layout, s: List<Slot>, i: Int, volts: Double = 4.5) = Circuits.brightness(Circuits.solve(layout, s, volts), s, i, volts)
    private val BRIGHT = listOf(c("heller", "🌞"), c("gleich hell", "↔️"), c("dunkler", "🌘"))
    private fun brightIdx(b: Float) = when (Circuits.compareToSingle(b)) { 1 -> 0; 0 -> 1; else -> 2 }
    /** Wahrheitstafel zweier Schalter: Leuchtet die Lampe bei (zu, zu), (zu, auf), (auf, zu), (auf, auf)? */
    private fun table(layout: Layout, lamp: Int): List<Boolean> = listOf(true to true, true to false, false to true, false to false).map { (a, b) ->
        val s = MutableList(3) { Slot(Part.LAMPE) }
        s[0] = Slot(if (a) Part.SCHALTER_ZU else Part.SCHALTER_AUF); s[1] = Slot(if (b) Part.SCHALTER_ZU else Part.SCHALTER_AUF)
        Circuits.glows(Circuits.solve(layout, s, 4.5), s, lamp)
    }
    private val WHEN = listOf(c("wenn mindestens ein Schalter zu ist"), c("nur wenn beide zu sind"), c("nie"))
    private fun whenIdx(t: List<Boolean>) = when { t == listOf(true, true, true, false) -> 0; t == listOf(true, false, false, false) -> 1; else -> 2 }

    val power = listOf(
        Experiment("s1", S, "Leiter oder nicht?", "🔌", 3..4,
            "Blubb hat eine Lücke in den Stromkreis gebaut. Welche Dinge lassen den Strom durch?",
            quiz("Womit leuchtet die Lampe?", listOf(c("Holzstab", "🌲"), c("Eisennagel", "📌"), c("Radiergummi", "🧽")),
                listOf(Part.HOLZ, Part.NAGEL, Part.GUMMI).indexOfFirst { glowsWith(tester, testerStart, 0, it, 1) }),
            CircuitSetup(tester, testerStart, listOf(Part.NAGEL, Part.HOLZ, Part.MUENZE, Part.GUMMI, Part.ALUFOLIE, Part.GLAS, Part.PLASTIK), setOf(1), 4.5, CircuitGoal.TEST_ALL),
            "Leg jedes Ding einmal in die Lücke.",
            "Nagel, Münze und Alufolie lassen die Lampe leuchten – Holz, Gummi, Glas und Plastik nicht.",
            listOf("Metalle leiten Strom, Holz, Gummi, Glas und Plastik sind Isolatoren.", "Nur harte Dinge leiten Strom.", "Strom fließt nur durch Dinge, die glänzen."),
            "Metalle sind Leiter. Holz, Gummi, Glas und Kunststoff sind Isolatoren.",
            quiz("Warum sind Kabel mit Plastik umhüllt?", listOf(c("Plastik isoliert – so bekommt man keinen Schlag"), c("Damit sie bunt sind"), c("Plastik leitet den Strom schneller")), 0)),

        Experiment("s2", S, "Die Bleistift-Überraschung", "✏️", 4..6,
            "Blubb spitzt einen Bleistift und holt die Mine heraus. Sie ist schwarz und gar nicht aus Metall …",
            quiz("Leuchtet die Lampe mit der Bleistiftmine?", LIGHT, lightIdx(glowsWith(tester, testerStart, 0, Part.BLEISTIFT, 1))),
            CircuitSetup(tester, testerStart, listOf(Part.BLEISTIFT, Part.SALZWASSER, Part.LEITUNGSWASSER, Part.HOLZ), setOf(1), 4.5, CircuitGoal.TEST_ALL),
            "Teste Bleistiftmine, Salzwasser, Leitungswasser und Holz.",
            "Die Mine leitet! Salzwasser lässt die Lampe schwach glimmen, bei Leitungswasser bleibt sie aus.",
            listOf("Die Mine ist aus Graphit, einer Form von Kohlenstoff – Graphit leitet Strom.", "Bleistifte enthalten Blei, und Blei ist ein Metall.", "Schwarze Dinge leiten immer."),
            "Auch Graphit leitet Strom. Salzwasser leitet etwas, Leitungswasser nur sehr schwach.",
            quiz("Leuchtet die Lampe mit Leitungswasser in der Lücke?", LIGHT, lightIdx(glowsWith(tester, testerStart, 0, Part.LEITUNGSWASSER, 1)),
                "Leitungswasser leitet viel zu schwach für das Lämpchen. Mit Netzstrom aus der Steckdose ist Wasser trotzdem lebensgefährlich!")),

        Experiment("s3", S, "Zwei Schalter hintereinander", "🔒", 5..6,
            "Blubb baut zwei Schalter hintereinander in den Stromkreis.",
            quiz("Wann leuchtet die Lampe?", WHEN, whenIdx(table(Layout.single(3), 2))),
            CircuitSetup(Layout.single(3), slots(Part.SCHALTER_AUF, Part.SCHALTER_AUF, Part.LAMPE), emptyList(), setOf(2), 4.5, CircuitGoal.BOTH_WAYS),
            "Tipp die Schalter an und probier alle Kombinationen.",
            "Nur wenn beide Schalter zu sind, leuchtet die Lampe – eine UND-Schaltung.",
            listOf("Der Strom muss durch beide Schalter hindurch – ist einer offen, ist der Kreis unterbrochen.", "Zwei Schalter geben doppelt so viel Strom.", "Der erste Schalter reicht immer."),
            "Reihenschaltung von Schaltern = UND.",
            quiz("Wo nützt eine UND-Schaltung?", listOf(c("Maschine läuft nur, wenn beide Hände auf den Knöpfen sind"), c("Klingel an zwei Türen"), c("Licht im Flur von zwei Seiten")), 0)),

        Experiment("s4", S, "Zwei Schalter nebeneinander", "🔓", 5..6,
            "Jetzt liegen die zwei Schalter nebeneinander – parallel.",
            quiz("Wann leuchtet die Lampe?", WHEN, whenIdx(table(Layout.ladder(0, listOf(1, 1), 1), 2))),
            CircuitSetup(Layout.ladder(0, listOf(1, 1), 1), slots(Part.SCHALTER_AUF, Part.SCHALTER_AUF, Part.LAMPE), emptyList(), setOf(2), 4.5, CircuitGoal.BOTH_WAYS),
            "Probier alle Kombinationen der Schalter.",
            "Ein einziger geschlossener Schalter reicht – eine ODER-Schaltung.",
            listOf("Der Strom hat zwei Wege – einer reicht.", "Parallele Schalter sind stärker.", "Die Lampe merkt sich den letzten Schalter."),
            "Parallelschaltung von Schaltern = ODER.",
            quiz("Die Klingel soll läuten, egal ob jemand an der Haustür oder am Gartentor drückt. Welche Schaltung?", listOf(c("Schalter in Reihe"), c("Schalter parallel")), 1)),

        Experiment("s5", S, "Lampen in Reihe", "💡", 5..7,
            "Zwei Lämpchen hintereinander an derselben Batterie.",
            quiz("Wie hell leuchten sie – verglichen mit einem Lämpchen allein?", BRIGHT, brightIdx(brightness(Layout.single(2), slots(Part.LAMPE, Part.LAMPE), 0))),
            CircuitSetup(Layout.single(2), slots(Part.LAMPE, Part.LAMPE), emptyList(), emptySet(), 4.5, CircuitGoal.REMOVE_ONE),
            "Schau, wie hell sie sind. Dann tipp eine Lampe an, um sie herauszudrehen.",
            "In Reihe sind beide dunkler als ein Lämpchen allein. Und ohne die eine geht auch die andere aus!",
            listOf("Die Spannung teilt sich auf beide Lampen auf, und es fließt weniger Strom.", "Die zweite Lampe klaut der ersten das Licht.", "Lampen leuchten nur allein richtig."),
            "Reihenschaltung: Ein Weg für den Strom – Lampen teilen sich die Spannung.",
            quiz("Eine Lampe herausdrehen – was macht die andere?", listOf(c("geht aus"), c("leuchtet weiter")),
                if (Circuits.glows(Circuits.solve(Layout.single(2), slots(Part.LEER, Part.LAMPE), 4.5), slots(Part.LEER, Part.LAMPE), 1)) 1 else 0)),

        Experiment("s6", S, "Lampen parallel", "🎄", 5..7,
            "Jetzt hat jede Lampe ihren eigenen Weg zur Batterie.",
            quiz("Wie hell leuchten sie – verglichen mit einem Lämpchen allein?", BRIGHT, brightIdx(brightness(Layout.ladder(0, listOf(1, 1)), slots(Part.LAMPE, Part.LAMPE), 0))),
            CircuitSetup(Layout.ladder(0, listOf(1, 1)), slots(Part.LAMPE, Part.LAMPE), emptyList(), emptySet(), 4.5, CircuitGoal.REMOVE_ONE),
            "Vergleich die Helligkeit, dann dreh eine Lampe heraus.",
            "Parallel leuchten beide so hell wie eine allein – und die andere bleibt an.",
            listOf("Jede Lampe bekommt die volle Batteriespannung über ihren eigenen Weg.", "Parallel ist die Batterie stärker.", "Die Lampen helfen sich gegenseitig."),
            "Parallelschaltung: Jede Lampe hat ihren eigenen Stromweg – so ist es im Haus.",
            quiz("Eine Lampe herausdrehen – was macht die andere?", listOf(c("geht aus"), c("leuchtet weiter")),
                if (Circuits.glows(Circuits.solve(Layout.ladder(0, listOf(1, 1)), slots(Part.LEER, Part.LAMPE), 4.5), slots(Part.LEER, Part.LAMPE), 1)) 1 else 0)),

        Experiment("s7", S, "Kurzschluss!", "⚠️", 6..7,
            "Blubb will ein Kabel direkt neben die Lampe legen. Ob das eine gute Idee ist?",
            quiz("Was passiert?", listOf(c("Lampe leuchtet heller"), c("Lampe geht aus – Kurzschluss!"), c("gar nichts")),
                Circuits.solve(Layout.ladder(0, listOf(1, 1)), slots(Part.LAMPE, Part.KABEL), 4.5).let { r -> if (r.short && !Circuits.glows(r, slots(Part.LAMPE, Part.KABEL), 0)) 1 else 0 }),
            CircuitSetup(Layout.ladder(0, listOf(1, 1)), slots(Part.LAMPE, Part.LEER), listOf(Part.KABEL), setOf(0), 4.5, CircuitGoal.SHORT),
            "Leg das Kabel in die freie Lücke neben der Lampe.",
            "Der Strom nimmt den bequemen Weg durchs Kabel. Die Lampe geht aus, die Batterie wird heiß!",
            listOf("Das Kabel hat fast keinen Widerstand – fast aller Strom fließt daran vorbei, und zwar sehr viel.", "Das Kabel klaut der Lampe das Licht und leuchtet selbst.", "Die Lampe ist kaputtgegangen."),
            "Kurzschluss: Der Strom umgeht den Verbraucher – es fließt sehr viel Strom. Gefährlich!",
            quiz("Was schützt im Haus vor Kurzschlüssen?", listOf(c("Die Sicherung schaltet den Strom ab"), c("Dickere Lampen"), c("Mehr Steckdosen")), 0)),

        Experiment("s8", S, "Ohmsches Gesetz", "Ω", 8..9,
            "An einer 4,5-V-Batterie hängt ein einstellbarer Widerstand. Blubb braucht genau 0,2 A.",
            quiz("Welcher Widerstand?", listOf(c("9 Ω"), c("22,5 Ω"), c("45 Ω")), nearest(listOf(9.0, 22.5, 45.0), Circuits.ohmFor(4.5, 0.2))),
            CircuitSetup(Layout.single(1), listOf(Slot(Part.WIDERSTAND, 50.0)), emptyList(), setOf(0), 4.5, CircuitGoal.AMPS, targetA = 0.2, adjustable = 0),
            "Stell den Widerstand so ein, dass das Messgerät 0,2 A zeigt.",
            "Bei 22,5 Ω fließen genau 0,2 A.",
            listOf("I = U / R, also R = U / I = 4,5 V / 0,2 A = 22,5 Ω.", "Je größer der Widerstand, desto mehr Strom.", "Der Widerstand muss so groß sein wie die Spannung."),
            "Ohmsches Gesetz: U = R · I.",
            quiz("Widerstand verdoppelt (45 Ω). Was passiert mit der Stromstärke?", listOf(c("verdoppelt sich"), c("halbiert sich"), c("bleibt gleich")),
                nearest(listOf(0.4, 0.1, 0.2), Circuits.amps(4.5, 45.0)))),

        Experiment("s9", S, "Widerstände in Reihe", "🔗", 8..9,
            "6 V Batterie, dahinter 10 Ω und 20 Ω hintereinander.",
            quiz("Welche Stromstärke misst Blubb?", listOf(c("0,2 A"), c("0,6 A"), c("0,9 A")), nearest(listOf(0.2, 0.6, 0.9),
                Circuits.solve(Layout.single(2), listOf(Slot(Part.WIDERSTAND, 10.0), Slot(Part.WIDERSTAND, 20.0)), 6.0).totalA)),
            CircuitSetup(Layout.single(2), listOf(Slot(Part.WIDERSTAND, 10.0), Slot(Part.WIDERSTAND, 20.0)), emptyList(), setOf(0, 1), 6.0, CircuitGoal.MEASURE),
            "Tipp auf „Strom messen“ und lies die Stromstärke ab.",
            "In Reihe addieren sich die Widerstände: 30 Ω, also 0,2 A.",
            listOf("R gesamt = 10 Ω + 20 Ω = 30 Ω, I = 6 V / 30 Ω = 0,2 A.", "Es zählt nur der größere Widerstand.", "Jeder Widerstand bekommt 6 V und eigenen Strom."),
            "Reihenschaltung: R = R₁ + R₂.",
            quiz("Die beiden Widerstände parallel geschaltet – welche Stromstärke?", listOf(c("0,2 A"), c("0,6 A"), c("0,9 A")), nearest(listOf(0.2, 0.6, 0.9),
                Circuits.solve(Layout.ladder(0, listOf(1, 1)), listOf(Slot(Part.WIDERSTAND, 10.0), Slot(Part.WIDERSTAND, 20.0)), 6.0).totalA), "Parallel: 0,6 A + 0,3 A = 0,9 A.")),

        Experiment("s10", S, "Magnet-Detektiv", "🧲", 3..4,
            "Blubb hat einen starken Magneten. Zieht er alles aus Metall an?",
            quiz("Zieht ein Magnet alle Metalle an?", listOf(c("Ja, alle Metalle"), c("Nein, nur manche"), c("Nur Gold")),
                if (MagnetItem.entries.any { it in setOf(MagnetItem.ALUDOSE, MagnetItem.KUPFER, MagnetItem.GOLDRING) && !it.magnetic }) 1 else 0),
            MagnetSetup(listOf(MagnetItem.BUEROKLAMMER, MagnetItem.ALUDOSE, MagnetItem.CENT, MagnetItem.HOLZ, MagnetItem.GOLDRING, MagnetItem.SCHERE, MagnetItem.KUPFER, MagnetItem.WEISSBLECH)),
            "Fahr mit dem Magneten über jedes Ding.",
            "Büroklammer, Schere, Konservendose und sogar die 1-Cent-Münze springen an den Magneten. Alu, Kupfer und Gold nicht!",
            listOf("Magnete ziehen Eisen und die meisten Stahlsorten, Nickel und Kobalt an – nicht jedes Metall.", "Magnete ziehen nur Dinge an, die glänzen.", "Schwere Dinge kann der Magnet nicht hochheben."),
            "Magnete ziehen Eisen, Nickel und Kobalt an (manches Edelstahl-Besteck übrigens kaum).",
            quiz("Wird eine Alu-Getränkedose angezogen?", listOf(c("ja"), c("nein")), if (MagnetItem.ALUDOSE.magnetic) 0 else 1, "Darum trennt die Sortieranlage Stahl-Dosen (Weißblech) mit Magneten von den Alu-Dosen.")),

        Experiment("s11", S, "Nord trifft Nord", "🧭", 3..5,
            "Zwei Magnete, beide mit dem roten Nordpol voran.",
            quiz("Was passiert, wenn sie sich nähern?", listOf(c("Sie ziehen sich an"), c("Sie stoßen sich ab")), if (Magnets.attract('N', 'N')) 0 else 1),
            MagnetSetup(emptyList(), poles = true),
            "Schieb die Magnete zusammen – einmal Nord-Nord, einmal Nord-Süd.",
            "Gleiche Pole stoßen sich ab, verschiedene ziehen sich an.",
            listOf("Gleichnamige Pole stoßen sich ab, ungleichnamige ziehen sich an.", "Magnete mögen keine Farbe Rot.", "Der eine Magnet ist kaputt."),
            "Gleiche Pole stoßen sich ab, verschiedene Pole ziehen sich an.",
            quiz("Nordpol trifft Südpol?", listOf(c("Sie ziehen sich an"), c("Sie stoßen sich ab")), if (Magnets.attract('N', 'S')) 0 else 1)),
    )

    private fun colorChoice(name: String, pH: Double) = c(name, color = Indicator.color(pH))
    private val COLORS3 = listOf("rot" to 2.0, "violett" to 6.0, "grün" to 10.0)
    private val KIND = listOf(c("sauer", "🍋"), c("neutral", "💧"), c("basisch", "🧼"))
    private fun kindIdx(pH: Double) = listOf("sauer", "neutral", "basisch").indexOf(Indicator.kind(pH))
    private fun coeffQuiz(r: Reaction, k: Int, options: List<Int>, q: String) = quiz(q, options.map { c("$it") }, options.indexOf(r.coeffs[k]))

    val chem = listOf(
        Experiment("c1", CH, "Zauber-Saft", "🟣", 3..4,
            "Blubb hat Rotkohl gekocht. Der Saft ist violett. Angeblich kann er zaubern …",
            quiz("Welche Farbe bekommt der Saft mit Zitronensaft?", COLORS3.map { (n, p) -> colorChoice(n, p) }, COLORS3.indexOfFirst { it.first == Indicator.name(Household.ZITRONE.pH) }),
            PhSetup(listOf(Household.ZITRONE, Household.WASSER, Household.SEIFE)),
            "Gib jeden Stoff in ein eigenes Glas mit Rotkohlsaft.",
            "Zitrone: rot. Wasser: bleibt violett. Seife: grün!",
            listOf("Rotkohlsaft ist ein Anzeiger: Säuren färben ihn rot, Laugen grün.", "Die Zitrone ist gelb und färbt den Saft.", "Seife wäscht die Farbe heraus."),
            "Rotkohlsaft zeigt an: sauer = rot, neutral = violett, basisch = blau, grün oder gelb.",
            quiz("Welche Farbe bekommt der Saft mit Seifenlauge?", COLORS3.map { (n, p) -> colorChoice(n, p) }, COLORS3.indexOfFirst { it.first == Indicator.name(Household.SEIFE.pH) })),

        Experiment("c2", CH, "Sauer oder basisch?", "🧫", 4..6,
            "Blubb hat die ganze Küche geplündert. Was davon ist sauer, was basisch?",
            quiz("Ist Cola sauer oder basisch?", KIND, kindIdx(Household.COLA.pH)),
            PhSetup(listOf(Household.ESSIG, Household.COLA, Household.MILCH, Household.NATRON, Household.WASCHSODA, Household.APFELSAFT)),
            "Teste alle sechs Stoffe.",
            "Essig, Cola und Apfelsaft sind sauer, Milch fast neutral, Natron und Soda basisch.",
            listOf("Der pH-Wert sagt es: unter 7 sauer, 7 neutral, über 7 basisch – der Saft zeigt ihn als Farbe.", "Alles Süße ist basisch.", "Flüssigkeiten sind immer sauer."),
            "pH-Wert: unter 7 sauer, genau 7 neutral, über 7 basisch.",
            quiz("Und Natron?", KIND, kindIdx(Household.NATRON.pH))),

        Experiment("c3", CH, "Säure trifft Lauge", "🧪", 7..8,
            "Im Becherglas: 10 mL Salzsäure (0,1 mol/L) mit Rotkohlsaft. Blubb tropft Natronlauge (0,1 mol/L) dazu.",
            quiz("Wie viel Lauge bis neutral?", listOf(c("5 mL"), c("10 mL"), c("20 mL")), nearest(listOf(5.0, 10.0, 20.0), Acid.neutralMl(0.1, 10.0, 0.1))),
            TitrationSetup(0.1, 10.0, 0.1, 1.0),
            "Tropf Lauge dazu, bis der Saft neutral ist.",
            "Bei genau 10 mL ist alles neutral: gleich viel Säure- wie Lauge-Teilchen.",
            listOf("Gleiche Konzentration und gleiche Menge – die Teilchen heben sich genau auf.", "Lauge ist konzentrierter als Säure, man braucht weniger.", "Neutral wird es nie."),
            "Neutralisation: n(Säure) = n(Lauge).",
            quiz("Mit doppelt so konzentrierter Lauge (0,2 mol/L) – wie viel?", listOf(c("5 mL"), c("10 mL"), c("20 mL")), nearest(listOf(5.0, 10.0, 20.0), Acid.neutralMl(0.1, 10.0, 0.2)))),

        Experiment("c4", CH, "Der pH-Sprung", "📈", 8..10,
            "Kurz vor dem Neutralpunkt (9 mL Lauge) zeigt das Messgerät pH 2,3. Blubb gibt noch 2 mL dazu.",
            quiz("Was zeigt das Messgerät bei 11 mL?", listOf(c("etwa 3"), c("etwa 7"), c("fast 12")),
                nearest(listOf(3.0, 7.0, 12.0), Beaker().addAcid(0.1, 10.0).addBase(0.1, 11.0).pH)),
            TitrationSetup(0.1, 10.0, 0.1, 1.0),
            "Tropf langsam bis über den Neutralpunkt und beobachte die Kurve.",
            "Um den Neutralpunkt springt der pH-Wert von gut 2 auf fast 12 – mit nur 2 mL!",
            listOf("Kurz vorm Ende ist kaum Säure übrig – schon ein Tropfen Lauge zu viel macht die Lösung basisch.", "Das Messgerät ist kaputt.", "Die Lauge wird plötzlich stärker."),
            "Am Neutralpunkt ändert sich der pH-Wert sprunghaft.",
            quiz("Jede pH-Stufe bedeutet zehnmal weniger sauer. Wie viel weniger sauer ist pH 2,3 als pH 1 (Unterschied 1,3 Stufen)?",
                listOf(c("etwa 20-mal weniger sauer"), c("etwa 1,3-mal weniger sauer"), c("genauso sauer")), 0,
                "1 Stufe = 10-mal, 1,3 Stufen ≈ 20-mal (10 hoch 1,3).")),

        Experiment("c5", CH, "Verdünnen", "🚰", 8..10,
            "Blubb hat 10 mL Salzsäure mit pH 1 und verdünnt sie mit Wasser.",
            quiz("Zehnmal so viel Volumen (100 mL) – welcher pH?", listOf(c("0,1"), c("2"), c("7")), nearest(listOf(0.1, 2.0, 7.0), Beaker().addAcid(0.1, 10.0).addWater(90.0).pH)),
            TitrationSetup(0.1, 10.0, null, 10.0, targetPh = 2.0, tolPh = 0.05),
            "Gib Wasser dazu, bis der pH-Wert 2 ist.",
            "Bei 100 mL ist der pH 2 – zehnfach verdünnt, eine pH-Stufe höher.",
            listOf("Zehnfach verdünnt heißt: zehnmal weniger H⁺ pro Liter, also eine pH-Stufe mehr.", "Wasser ist basisch und neutralisiert.", "Mit Wasser wird jede Säure sofort neutral."),
            "Verdünnen um den Faktor 10 erhöht den pH einer starken Säure um 1.",
            quiz("Noch einmal zehnfach verdünnt (1 L)?", listOf(c("3"), c("7"), c("20")), nearest(listOf(3.0, 7.0, 20.0), Beaker().addAcid(0.1, 10.0).addWater(990.0).pH))),

        Experiment("c6", CH, "Der Natron-Ballon", "🎈", 3..6,
            "Essig in der Flasche, Natron im Luftballon. Blubb stülpt den Ballon über den Flaschenhals …",
            quiz("Was passiert mit dem Ballon?", listOf(c("Er bläst sich auf", "🎈"), c("Er wird in die Flasche gesaugt", "🌀"), c("Nichts", "😐")),
                if (Fizz.gasL(1.0, 100.0) > 0) 0 else 2),
            FizzSetup(100.0, 6, 1.0),
            "Schütte Natron in die Flasche, bis der Ballon mindestens 1 Liter groß ist.",
            "Es sprudelt! Das Gas Kohlenstoffdioxid füllt den Ballon.",
            listOf("Natron und Essig reagieren – dabei entsteht ein Gas, das Platz braucht.", "Der Essig verdunstet.", "Natron ist Luft in Pulverform."),
            "Bei Reaktionen können neue Stoffe entstehen – hier ein Gas.",
            quiz("Immer mehr Natron, immer größerer Ballon?", listOf(c("Ja, ohne Ende"), c("Nur bis der Essig verbraucht ist")),
                if (Fizz.gasL(6.0, 100.0) > Fizz.gasL(3.0, 100.0) + 1e-9) 0 else 1, "Der Stoff, der zuerst aufgebraucht ist, begrenzt die Gasmenge.")),

        Experiment("c7", CH, "Knallgas", "💥", 7..9,
            "Wasserstoff und Sauerstoff – zusammen mit einem Funken. Blubb hält sich die Ohren zu.",
            quiz("Was entsteht?", listOf(c("Wasser", "💧"), c("Rauch", "💨"), c("Kohlenstoffdioxid", "💨")), Reactions.byId("knallgas").right.indexOfFirst { it.name == "Wasser" }.let { if (it >= 0) 0 else 1 }),
            ReactionSetup("knallgas"),
            "Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus: Links und rechts gleich viele Atome!",
            "2 H₂ + O₂ → 2 H₂O – mit einem lauten Knall!",
            listOf("Atome gehen nie verloren: 4 H und 2 O links, 4 H und 2 O rechts.", "Bei Reaktionen verschwinden Atome.", "Der Knall macht neue Atome."),
            "Bei Reaktionen werden Atome nur neu verbunden – keins geht verloren.",
            quiz("Wie viele H₂ braucht ein O₂?", listOf(c("1"), c("2"), c("4")), listOf(1, 2, 4).indexOf(Reactions.byId("knallgas").coeffs[0] / Reactions.byId("knallgas").coeffs[1]))),

        Experiment("c8", CH, "Magnesium-Blitz", "⚡", 7..9,
            "Ein Streifen Magnesium in der Flamme – Blubb setzt die Schutzbrille auf.",
            coeffQuiz(Reactions.byId("magnesium"), 0, listOf(1, 2, 3), "Wie viele Mg-Atome reagieren mit einem O₂?"),
            ReactionSetup("magnesium"),
            "Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus.",
            "2 Mg + O₂ → 2 MgO – ein gleißend weißer Blitz.",
            listOf("Ein O₂ hat zwei O-Atome, jedes MgO braucht eins – also zwei Mg.", "Magnesium braucht immer gleich viel Sauerstoff wie sich selbst.", "Der Blitz verbraucht Atome."),
            "Atome zählen: links und rechts gleich viele von jeder Sorte.",
            quiz("Ist das weiße Pulver nachher leichter, schwerer oder gleich schwer wie der Magnesiumstreifen vorher?", listOf(c("leichter"), c("schwerer"), c("gleich schwer")), 1,
                "Sauerstoff aus der Luft wird eingebaut – das Oxid wiegt mehr.")),

        Experiment("c9", CH, "Die Gasflamme", "🔵", 8..10,
            "Am Gasherd verbrennt Methan (CH₄).",
            coeffQuiz(Reactions.byId("erdgas"), 1, listOf(1, 2, 4), "Wie viele O₂ braucht ein CH₄?"),
            ReactionSetup("erdgas"),
            "Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus.",
            "CH₄ + 2 O₂ → CO₂ + 2 H₂O.",
            listOf("Die 4 H werden zu 2 H₂O, das C zu CO₂ – dafür braucht es 4 O, also 2 O₂.", "Ein O₂ reicht immer.", "Methan enthält schon Sauerstoff."),
            "Verbrennen Kohlenwasserstoffe mit genug Sauerstoff, entstehen CO₂ und Wasser.",
            coeffQuiz(Reactions.byId("erdgas"), 3, listOf(1, 2, 4), "Wie viele Wasser-Moleküle entstehen?")),

        Experiment("c10", CH, "Warum rostet Eisen?", "🟤", 8..10,
            "Blubbs Fahrradkette ist rot geworden. Rost ist Eisen plus Sauerstoff.",
            coeffQuiz(Reactions.byId("rost"), 1, listOf(2, 3, 4), "4 Fe + ? O₂ → 2 Fe₂O₃"),
            ReactionSetup("rost"),
            "Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus.",
            "4 Fe + 3 O₂ → 2 Fe₂O₃.",
            listOf("Rechts sind 6 O-Atome – dafür braucht es 3 O₂.", "Rost entsteht aus Wasser allein.", "Eisen wird zu Sauerstoff."),
            "Rosten ist eine langsame Reaktion mit Sauerstoff (und Wasser hilft mit).",
            quiz("Warum rostet ein Nagel in trockener Luft kaum?", listOf(c("Zum Rosten braucht es auch Feuchtigkeit"), c("Trockene Luft hat keinen Sauerstoff"), c("Nägel rosten nie")), 0)),

        Experiment("c11", CH, "Elefantenzahnpasta", "🐘", 8..10,
            "Wasserstoffperoxid zerfällt mit einem Helfer blitzschnell. Blubb hat Spülmittel dazugegeben …",
            coeffQuiz(Reactions.byId("zahnpasta"), 2, listOf(1, 2, 4), "2 H₂O₂ → 2 H₂O + ? O₂"),
            ReactionSetup("zahnpasta"),
            "Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus.",
            "2 H₂O₂ → 2 H₂O + O₂ – ein riesiger Schaumberg!",
            listOf("Links 4 O, rechts in 2 H₂O schon 2 O – bleiben 2 O, also ein O₂.", "Der Schaum ist geschmolzene Zahnpasta.", "Es entsteht Wasserstoff."),
            "Bei einer Zersetzung zerfällt ein Stoff in mehrere neue.",
            quiz("Was macht den Schaum so groß?", listOf(c("Sauerstoffgas wird im Spülmittel eingefangen"), c("Wasser kocht"), c("Das Glas wird kleiner")), 0)),

        Experiment("c12", CH, "Fotosynthese", "🌿", 9..10,
            "Ein Blatt im Sonnenlicht baut aus Luft und Wasser Zucker.",
            coeffQuiz(Reactions.byId("fotosynthese"), 3, listOf(3, 6, 12), "6 CO₂ + 6 H₂O → C₆H₁₂O₆ + ? O₂"),
            ReactionSetup("fotosynthese"),
            "Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus.",
            "6 CO₂ + 6 H₂O → C₆H₁₂O₆ + 6 O₂.",
            listOf("Links 18 O, im Zucker 6 – bleiben 12 O, also 6 O₂.", "Pflanzen machen Sauerstoff aus nichts.", "Der Zucker enthält keinen Sauerstoff."),
            "Pflanzen speichern Sonnenenergie in Zucker und geben Sauerstoff ab.",
            quiz("Woher kommt der Sauerstoff, den wir atmen, ursprünglich?", listOf(c("Von Pflanzen und Algen"), c("Aus Vulkanen"), c("Aus dem Weltall")), 0)),

        Experiment("c13", CH, "Der Gasgrill", "🍖", 9..10,
            "Propan (C₃H₈) aus der Gasflasche verbrennt.",
            coeffQuiz(Reactions.byId("grill"), 1, listOf(3, 5, 8), "C₃H₈ + ? O₂ → 3 CO₂ + 4 H₂O"),
            ReactionSetup("grill"),
            "Gleiche die Gleichung mit den kleinsten ganzen Zahlen aus.",
            "C₃H₈ + 5 O₂ → 3 CO₂ + 4 H₂O.",
            listOf("Rechts 6 + 4 = 10 O, also 5 O₂.", "Ein Propan braucht genau ein O₂.", "Propan verbrennt ohne Sauerstoff."),
            "Erst C und H ausgleichen, dann den Sauerstoff zählen.",
            coeffQuiz(Reactions.byId("grill"), 2, listOf(1, 3, 5), "Wie viele CO₂ entstehen aus einem Propan?")),
    )
}
