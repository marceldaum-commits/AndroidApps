package de.experimentopia.core.model

import de.experimentopia.core.model.Pick.nearest

/** Feuer & Eis, Licht & Klang, Fallturm-Insel */
internal object CatalogC {
    private val H = Island.WAERME
    private val L = Island.LICHTKLANG
    private val F = Island.FALL

    val heat = listOf(
        Experiment("h1", H, "Was schmilzt zuerst?", "🍫", 3..4,
            "Blubb stellt Eis, Schokolade, Butter und Kerzenwachs auf eine Heizplatte, die langsam wärmer wird.",
            quiz("Was schmilzt als Letztes?", listOf(c("Eiswürfel", "🧊"), c("Schokolade", "🍫"), c("Kerzenwachs", "🕯️")),
                listOf(Meltable.EIS, Meltable.SCHOKO, Meltable.WACHS).indexOf(listOf(Meltable.EIS, Meltable.SCHOKO, Meltable.WACHS, Meltable.BUTTER).maxBy { it.meltC })),
            MeltSetup(listOf(Meltable.EIS, Meltable.BUTTER, Meltable.SCHOKO, Meltable.WACHS), 100.0),
            "Dreh die Heizplatte langsam hoch, bis alles geschmolzen ist.",
            "Eis bei 0 °C, Butter bei gut 30 °C, Schokolade bei 34 °C, Wachs erst bei 58 °C.",
            listOf("Jeder Stoff hat seine eigene Schmelztemperatur.", "Was dunkel ist, schmilzt zuerst.", "Große Dinge schmelzen immer zuletzt."),
            "Jeder Stoff schmilzt bei seiner eigenen Temperatur – der Schmelztemperatur.",
            quiz("Warum schmilzt Schokolade im Mund?", listOf(c("Im Mund ist es wärmer (37 °C) als ihre Schmelztemperatur"), c("Spucke löst sie auf"), c("Die Zähne zermahlen sie")),
                if (Meltable.SCHOKO.meltC < 37) 0 else 1)),

        Experiment("h2", H, "Das sture Thermometer", "🌡️", 5..7,
            "100 g Eis (−20 °C) kommen auf den Herd. Blubb beobachtet das Thermometer ganz genau.",
            quiz("Während das Eis schmilzt – was macht das Thermometer?", listOf(c("steigt immer weiter"), c("bleibt bei 0 °C stehen"), c("sinkt")),
                Heat.heat(100.0, -20.0, 100 * Heat.C_ICE * 20 + 100 * Heat.L_MELT / 2).tempC.let { if (Fmt.close(it, 0.0, 1e-9)) 1 else 0 }),
            HeatCurveSetup(100.0, -20.0, 500.0, untilSteam = false),
            "Heiz, bis das Wasser kocht, und beobachte die Kurve.",
            "Bei 0 °C bleibt die Temperatur stehen, bis alles Eis geschmolzen ist – genauso bei 100 °C beim Kochen.",
            listOf("Die Wärme wird gebraucht, um die Teilchen aus dem festen Gitter zu lösen – nicht zum Wärmerwerden.", "Der Herd macht eine Pause.", "Das Thermometer ist eingefroren."),
            "Beim Schmelzen und Sieden bleibt die Temperatur gleich – die Energie löst die Teilchen voneinander.",
            quiz("Kochendes Wasser: Wird es heißer als 100 °C, solange noch Wasser da ist?", listOf(c("ja"), c("nein, es bleibt bei 100 °C")),
                if (Fmt.close(Heat.heat(100.0, -20.0, Heat.energyToSteam(100.0, -20.0) - 100 * Heat.L_BOIL / 2).tempC, 100.0, 1e-9)) 1 else 0)),

        Experiment("h3", H, "Tanzende Teilchen", "💨", 5..6,
            "Blubb hat eine Teilchen-Brille! Damit sieht man, was im Eis, im Wasser und im Dampf passiert.",
            quiz("Was machen die Teilchen, wenn Wasser verdampft?", listOf(c("Sie werden größer"), c("Sie lösen sich voneinander und fliegen frei herum"), c("Sie verschwinden")), 1),
            HeatCurveSetup(30.0, -10.0, 800.0, untilSteam = true),
            "Heiz, bis aus Eis Dampf geworden ist.",
            "Im Eis zittern die Teilchen am Platz, im Wasser rutschen sie aneinander vorbei, im Dampf fliegen sie frei.",
            listOf("Wärme heißt: Teilchen bewegen sich schneller. Schnell genug, lösen sie sich voneinander.", "Die Teilchen schmelzen selbst.", "Die Teilchen werden beim Erhitzen größer."),
            "Teilchenmodell: fest – flüssig – gasförmig unterscheidet sich in Ordnung und Bewegung der Teilchen.",
            quiz("Warum riecht man Kuchen im ganzen Haus?", listOf(c("Duft-Teilchen fliegen durch die Luft"), c("Der Kuchen wird größer"), c("Die Nase ist in der Küche")), 0)),

        Experiment("h4", H, "Heiß + kalt", "🛀", 5..6,
            "100 g Wasser mit 80 °C und 100 g Wasser mit 20 °C – Blubb kippt beides zusammen.",
            quiz("Wie warm wird die Mischung?", listOf(c("100 °C"), c("50 °C"), c("60 °C")), nearest(listOf(100.0, 50.0, 60.0), Heat.mixWater(100.0, 80.0, 100.0, 20.0))),
            MixSetup(Stuff.WASSER, 100.0, 80.0, Stuff.WASSER, 100.0, 20.0),
            "Kipp die beiden Becher zusammen.",
            "Genau 50 °C – die Mitte!",
            listOf("Das heiße Wasser gibt so viel Wärme ab, wie das kalte aufnimmt – bei gleichen Mengen landet man in der Mitte.", "Temperaturen werden addiert.", "Das heiße Wasser gewinnt immer."),
            "Mischtemperatur: Was das Warme abgibt, nimmt das Kalte auf.",
            quiz("200 g Wasser mit 80 °C und 100 g Wasser mit 20 °C – wie warm wird die Mischung?", listOf(c("50 °C"), c("60 °C"), c("70 °C")), nearest(listOf(50.0, 60.0, 70.0), Heat.mixWater(200.0, 80.0, 100.0, 20.0)))),

        Experiment("h5", H, "Das perfekte Badewasser", "🛁", 6..7,
            "Badewasser soll 37 °C haben. Im Eimer: 100 g kaltes Wasser (15 °C). Aus dem Hahn kommt 60 °C heißes Wasser.",
            quiz("Wie viel heißes Wasser brauchst du ungefähr?", listOf(c("50 g"), c("100 g"), c("200 g")),
                nearest(listOf(50.0, 100.0, 200.0), 100.0 * (37 - 15) / (60 - 37))),
            MixSetup(Stuff.WASSER, 50.0, 60.0, Stuff.WASSER, 100.0, 15.0, adjustA = 10.0, targetC = 37.0),
            "Stell die Menge heißes Wasser ein, bis die Mischung 37 °C hat (±1 °C).",
            "Mit 100 g heißem Wasser sind es 37,5 °C – perfekt!",
            listOf("Das heiße Wasser kühlt um 23 °C ab, das kalte wärmt sich um 22 °C auf – fast gleiche Mengen.", "Man braucht immer doppelt so viel heißes Wasser.", "Es kommt nur auf die Temperatur an, nicht auf die Menge."),
            "Richmann: m₁·(T₁ − T) = m₂·(T − T₂).",
            quiz("Warum wird das Badewasser mit der Zeit kühler?", listOf(c("Es gibt Wärme an Luft und Wanne ab"), c("Wasser wird von allein kalt"), c("Der Schaum kühlt")), 0)),

        Experiment("h6", H, "Heißes Eisen ins Wasser", "🔩", 7..8,
            "Ein Eisenklotz (100 g, 100 °C) plumpst in 100 g Wasser (20 °C). Plumps!",
            quiz("Wie warm wird das Wasser?", listOf(c("etwa 28 °C"), c("etwa 60 °C"), c("etwa 90 °C")),
                nearest(listOf(28.0, 60.0, 90.0), Heat.mix(100.0, Stuff.EISEN.c, 100.0, 100.0, Stuff.WASSER.c, 20.0))),
            MixSetup(Stuff.EISEN, 100.0, 100.0, Stuff.WASSER, 100.0, 20.0),
            "Wirf den Eisenklotz ins Wasser.",
            "Nur 27,8 °C! Das Eisen kühlt fast ganz ab, das Wasser wird kaum wärmer.",
            listOf("Wasser speichert pro Gramm etwa neunmal so viel Wärme wie Eisen.", "Eisen ist innen kalt.", "Das Wasser verdampft die Wärme sofort."),
            "Wasser hat eine sehr große spezifische Wärmekapazität – darum kühlt man mit Wasser.",
            quiz("Warum nutzt man Wasser im Auto-Kühler?", listOf(c("Es kann viel Wärme aufnehmen"), c("Es ist billig und blau"), c("Es leitet Strom")), 0)),
    )

    private val ADD = listOf(c("Braun", color = 0xFF8B5A2B), c("Gelb", color = Rgb.GELB.argb()), c("Schwarz", color = 0xFF222222))
    private fun colorIdx(choices: List<Choice>, col: Rgb) = choices.indexOfFirst { it.text == Colors.name(col) }.let { if (it < 0) 0 else it }
    private val HL = listOf(c("höher", "⬆️"), c("tiefer", "⬇️"))

    val lightSound = listOf(
        Experiment("l1", L, "Rot + Grün = ?", "🔦", 3..5,
            "Blubb richtet eine rote und eine grüne Lampe auf dieselbe weiße Wand.",
            quiz("Welche Farbe siehst du, wo sich die Lichter treffen?", ADD, colorIdx(ADD, Colors.add(listOf(Rgb.ROT, Rgb.GRUEN)))),
            LightSetup(LightMode.ADD, Rgb.GELB),
            "Mach die Wand gelb.",
            "Gelb! Mit Licht ist das anders als im Malkasten.",
            listOf("Lichtfarben addieren sich: Rot und Grün zusammen reizen im Auge dieselben Zellen wie Gelb.", "Rot übermalt Grün.", "Die Wand ist gelb angestrichen."),
            "Licht mischt man additiv: Rot + Grün = Gelb, alle drei = Weiß.",
            quiz("Rot, Grün und Blau gleichzeitig?", listOf(c("Weiß", color = Rgb.WEISS.argb()), c("Schwarz", color = 0xFF222222), c("Braun", color = 0xFF8B5A2B)),
                if (Colors.name(Colors.add(listOf(Rgb.ROT, Rgb.GRUEN, Rgb.BLAU))) == "Weiß") 0 else 1)),

        Experiment("l2", L, "Türkis zaubern", "💎", 4..6,
            "Die Bühne soll türkis leuchten wie ein Schwimmbad.",
            quiz("Welche zwei Lampen ergeben Türkis?", listOf(c("Grün + Blau"), c("Rot + Blau"), c("Rot + Grün")),
                listOf(listOf(Rgb.GRUEN, Rgb.BLAU), listOf(Rgb.ROT, Rgb.BLAU), listOf(Rgb.ROT, Rgb.GRUEN)).indexOfFirst { Colors.add(it).close(Rgb.CYAN) }),
            LightSetup(LightMode.ADD, Rgb.CYAN),
            "Misch Türkis auf die Wand.",
            "Grün und Blau ergeben Türkis (Cyan).",
            listOf("Türkis ist Licht mit Grün- und Blauanteil, aber ohne Rot.", "Türkis ist eine Grundfarbe, die man nicht mischen kann.", "Man braucht dafür alle drei Lampen."),
            "Mit Rot, Grün und Blau kann man fast jede Lichtfarbe mischen – so funktioniert jeder Bildschirm.",
            quiz("Rot + Blau?", listOf(c("Pink (Magenta)", color = Rgb.MAGENTA.argb()), c("Grün", color = Rgb.GRUEN.argb()), c("Weiß", color = Rgb.WEISS.argb())),
                if (Colors.name(Colors.add(listOf(Rgb.ROT, Rgb.BLAU))).startsWith("Pink")) 0 else 1)),

        Experiment("l3", L, "Gelb + Blau = Grün?", "🟡", 5..7,
            "Im Malkasten gibt Gelb mit Blau Grün. Und mit Licht? Für unser Auge wirkt gelbes Licht wie Rot + Grün.",
            quiz("Gelbes und blaues Licht zusammen?", listOf(c("Grün", color = Rgb.GRUEN.argb()), c("Weiß", color = Rgb.WEISS.argb()), c("Braun", color = 0xFF8B5A2B)),
                if (Colors.name(Colors.add(listOf(Rgb.GELB, Rgb.BLAU))) == "Weiß") 1 else 0),
            LightSetup(LightMode.ADD, Rgb.WEISS),
            "Mach die Wand weiß.",
            "Weiß! Gelbes und blaues Licht ergänzen sich.",
            listOf("Gelbes Licht wirkt im Auge wie Rot und Grün zusammen – mit Blau sind alle drei da.", "Blau löscht Gelb aus.", "Weiß entsteht nur aus weißem Licht."),
            "Farben, die zusammen Weiß ergeben, heißen Komplementärfarben.",
            quiz("Und im Malkasten: Gelb + Cyan-Blau?", listOf(c("Grün", color = Rgb.GRUEN.argb()), c("Weiß", color = Rgb.WEISS.argb()), c("Schwarz", color = 0xFF222222)),
                if (Colors.name(Colors.filter(1.0, 0.0, 1.0)) == "Grün") 0 else 1, "Farben im Malkasten schlucken Licht – das ist die subtraktive Mischung.")),

        Experiment("l4", L, "Farbfolien", "🎞️", 6..7,
            "Vor einer weißen Lampe: eine gelbe und eine türkise Folie übereinander.",
            quiz("Welche Farbe kommt durch?", listOf(c("Grün", color = Rgb.GRUEN.argb()), c("Weiß", color = Rgb.WEISS.argb()), c("Gelb", color = Rgb.GELB.argb())),
                listOf("Grün", "Weiß", "Gelb").indexOf(Colors.name(Colors.filter(1.0, 0.0, 1.0)))),
            LightSetup(LightMode.SUB, Rgb.GRUEN),
            "Schieb Folien vor die Lampe, bis Grün herauskommt.",
            "Gelb schluckt Blau, Türkis schluckt Rot – übrig bleibt Grün.",
            listOf("Jede Folie nimmt eine Farbe aus dem weißen Licht weg.", "Folien färben das Licht bunt an.", "Zwei Folien geben immer Schwarz."),
            "Subtraktive Mischung: Filter nehmen Farben weg. So funktioniert der Drucker.",
            quiz("Gelbe, türkise und pinke Folie übereinander?", listOf(c("Weiß", color = Rgb.WEISS.argb()), c("Schwarz", color = 0xFF222222), c("Grün", color = Rgb.GRUEN.argb())),
                if (Colors.name(Colors.filter(1.0, 1.0, 1.0)) == "Schwarz") 1 else 0)),

        Experiment("l5", L, "Regenbogen aus Glas", "🔺", 6..8,
            "Blubb stellt ein Glasprisma in einen Strahl weißes Licht.",
            quiz("Was kommt aus dem Prisma heraus?", listOf(c("weißes Licht"), c("Regenbogenfarben"), c("gar nichts")), 1),
            PrismSetup(),
            "Dreh das Prisma, bis ein breiter Regenbogen erscheint.",
            "Weißes Licht ist eine Mischung aller Farben – das Glas lenkt jede Farbe etwas anders ab.",
            listOf("Glas bricht blaues Licht stärker als rotes – so werden die Farben getrennt.", "Das Prisma ist bunt angemalt.", "Das Glas erzeugt neue Farben."),
            "Weißes Licht besteht aus allen Regenbogenfarben.",
            quiz("Welche Farbe wird am stärksten abgelenkt?", listOf(c("Rot", color = 0xFFFF3030), c("Violett", color = 0xFF8A2BE2)),
                if (Prism.minDeviation(420.0) > Prism.minDeviation(700.0)) 1 else 0)),

        Experiment("l6", L, "Gläser-Xylofon", "🥛", 3..5,
            "Vier Gläser, verschieden voll. Blubb schlägt mit einem Löffel dagegen.",
            quiz("Ein volles Glas klingt im Vergleich zu einem leeren …", HL, if (Klang.glassHz(0.9) < Klang.glassHz(0.0)) 1 else 0),
            SoundSetup(SoundMode.STRIKE),
            "Schlag alle Gläser an und hör genau hin.",
            "Je mehr Wasser, desto tiefer der Ton.",
            listOf("Das Wasser schwingt mit und macht die Glaswand träger – sie schwingt langsamer.", "Wasser ist lauter als Luft.", "Volle Gläser sind kälter."),
            "Angeschlagenes Glas: mehr Wasser = tieferer Ton.",
            quiz("Ein Glas, ganz bis oben voll – im Vergleich zu halb voll?", HL, if (Klang.glassHz(1.0) < Klang.glassHz(0.5)) 1 else 0)),

        Experiment("l7", L, "Röhren-Orgel", "🎵", 4..6,
            "Diesmal pustet Blubb über die Glasröhren – wie über eine Flasche.",
            quiz("Mehr Wasser in der Röhre – der Ton wird …", HL, if (Klang.tubeHz(0.08) > Klang.tubeHz(0.2)) 0 else 1),
            SoundSetup(SoundMode.BLOW),
            "Puste über alle Röhren (antippen) und verändere die Wasserhöhe.",
            "Beim Pusten ist es umgekehrt: mehr Wasser – höherer Ton!",
            listOf("Beim Pusten schwingt die Luftsäule – je kürzer sie ist, desto höher der Ton.", "Das Wasser pfeift mit.", "Die Röhre wird enger."),
            "Pusten: kurze Luftsäule = hoher Ton.",
            quiz("Warum ist es beim Anschlagen umgekehrt?", listOf(c("Dort schwingt das Glas mit dem Wasser, nicht die Luft"), c("Anschlagen ist lauter"), c("Der Löffel ist aus Metall")), 0)),

        Experiment("l8", L, "Stimm den Ton", "🎼", 6..8,
            "Blubb braucht den Ton a″ (880 Hz). Die Röhre ist 25 cm lang. Die Luftsäule ist ein Viertel der Wellenlänge: f = c / (4·L).",
            quiz("Wie hoch muss das Wasser stehen?", listOf(c("etwa 5 cm"), c("etwa 10 cm"), c("etwa 15 cm")), nearest(listOf(0.05, 0.10, 0.15), Klang.waterFor(Note.A5.hz))),
            SoundSetup(SoundMode.TUNE, target = Note.A5),
            "Stell das Wasser so ein, dass der Ton genau a″ ist.",
            "Bei gut 15 cm Wasser bleiben 9,7 cm Luft: 343 / (4 · 0,097) ≈ 880 Hz.",
            listOf("Schall braucht 343 m/s; eine Luftsäule von 9,7 cm schwingt mit 880 Hz.", "Je mehr Wasser, desto tiefer der Ton.", "Die Röhre muss ganz voll sein."),
            "Röhre mit Wasser: f = c / (4·L), L = Länge der Luftsäule.",
            quiz("Welche Luftsäule gibt den Kammerton a′ (440 Hz)?", listOf(c("etwa 10 cm"), c("etwa 19 cm"), c("etwa 40 cm")), nearest(listOf(0.10, 0.19, 0.40), Klang.airFor(440.0)))),

        Experiment("l9", L, "Alle meine Entchen", "🦆", 3..5,
            "Die Röhren sind gestimmt! Blubb pustet über die Röhren und will ein Lied spielen. Die leuchtende Röhre ist dran.",
            quiz("Welche Röhre klingt am höchsten?", listOf(c("die mit am meisten Wasser"), c("die mit am wenigsten Wasser")),
                if (Klang.tubeHz(Klang.TUBE_M - Klang.waterFor(Note.C6.hz)) > Klang.tubeHz(Klang.TUBE_M - Klang.waterFor(Note.C5.hz)) && Klang.waterFor(Note.C6.hz) > Klang.waterFor(Note.C5.hz)) 0 else 1),
            SoundSetup(SoundMode.MELODY, melody = listOf(Note.C5, Note.D5, Note.E5, Note.F5, Note.G5, Note.G5, Note.A5, Note.A5, Note.A5, Note.A5, Note.G5, Note.A5, Note.A5, Note.A5, Note.A5, Note.G5,
                Note.F5, Note.F5, Note.F5, Note.F5, Note.E5, Note.E5, Note.D5, Note.D5, Note.D5, Note.D5, Note.C5)),
            "Tipp immer die leuchtende Röhre an.",
            "Du hast „Alle meine Entchen“ gespielt!",
            listOf("Viel Wasser lässt nur eine kurze Luftsäule übrig – und eine kurze Luftsäule klingt hoch.", "Die Röhren spielen das Lied von allein.", "Die Farbe der Röhre macht den Ton."),
            "Kürzere Luftsäule – höherer Ton.",
            quiz("Aus einer Röhre verdunstet Wasser. Du pustest wieder darüber. Der Ton wird …", HL, 1, "Die Luftsäule wird länger, also tiefer.")),

        Experiment("l10", L, "Die halbe Saite", "🎸", 5..7,
            "Blubb zupft eine gespannte Saite. Dann drückt er sie genau in der Mitte auf das Brett.",
            quiz("Wie klingt die halbe Saite?", listOf(c("eine Oktave höher"), c("eine Oktave tiefer"), c("genauso")),
                when { Fmt.close(Klang.stringHz(0.5) / Klang.stringHz(1.0), 2.0, 1e-9) -> 0; Klang.stringHz(0.5) < Klang.stringHz(1.0) -> 1; else -> 2 }),
            SoundSetup(SoundMode.STRING),
            "Schieb den Steg auf die Hälfte und zupf.",
            "Halbe Länge, doppelte Frequenz – eine Oktave höher.",
            listOf("Eine kürzere Saite schwingt schneller; halbe Länge – doppelt so schnell.", "Die Saite wird lockerer.", "Die Mitte ist immer am lautesten."),
            "Saite: f ist umgekehrt proportional zur Länge.",
            quiz("Und bei zwei Dritteln der Länge?", listOf(c("1,5-fache Frequenz (eine Quinte)"), c("doppelte Frequenz"), c("gleiche Frequenz")),
                nearest(listOf(1.5, 2.0, 1.0), Klang.stringHz(2.0 / 3.0) / Klang.stringHz(1.0)))),
    )

    private fun who(a: String, b: String) = listOf(c(a), c(b), c("gleichzeitig", "🤝"))
    private fun whoIdx(w: Int) = when (w) { -1 -> 0; 1 -> 1; else -> 2 }

    val fall = listOf(
        Experiment("f1", F, "Kugel gegen Feder", "🍃", 3..4,
            "Blubb lässt im Fallturm eine Stahlkugel und eine Feder gleichzeitig los – aus 2 m Höhe.",
            quiz("Wer ist zuerst unten?", who("Kugel", "Feder"), whoIdx(Fall.winner(Faller.KUGEL, Faller.FEDER, 2.0, Planet.ERDE))),
            FallSetup(Faller.KUGEL, Faller.FEDER, listOf(Planet.ERDE), 2.0),
            "Lass beide fallen.",
            "Die Kugel ist nach 0,6 s unten, die Feder schwebt mehrere Sekunden.",
            listOf("Die Luft bremst die leichte, breite Feder viel stärker.", "Schwere Dinge fallen immer schneller.", "Die Feder ist magisch."),
            "In Luft bremst der Luftwiderstand leichte, breite Dinge stark.",
            quiz("Und in einer Röhre ohne Luft?", who("Kugel", "Feder"), whoIdx(Fall.winner(Faller.KUGEL, Faller.FEDER, 2.0, Planet.VAKUUM)))),

        Experiment("f2", F, "Der Papier-Trick", "🗞️", 3..5,
            "Zwei gleiche Blätter Papier. Blubb knüllt eins zu einer Kugel zusammen.",
            quiz("Wer landet zuerst?", who("flaches Blatt", "Papierkugel"), whoIdx(Fall.winner(Faller.PAPIER, Faller.KNAEUEL, 2.0, Planet.ERDE))),
            FallSetup(Faller.PAPIER, Faller.KNAEUEL, listOf(Planet.ERDE), 2.0),
            "Lass beide fallen.",
            "Die Papierkugel ist viel schneller – obwohl beide gleich schwer sind!",
            listOf("Gleiches Gewicht, aber die Kugel bietet der Luft viel weniger Fläche.", "Zerknülltes Papier ist schwerer.", "Das flache Blatt hält sich an der Luft fest."),
            "Die Form entscheidet über den Luftwiderstand.",
            quiz("Warum fällt ein Fallschirmspringer mit offenem Schirm langsam?", listOf(c("Großer Schirm – großer Luftwiderstand"), c("Der Schirm ist leicht"), c("Oben ist weniger Schwerkraft")), 0)),

        Experiment("f3", F, "Die Vakuumröhre", "🍶", 5..7,
            "Blubb pumpt alle Luft aus einer hohen Glasröhre. Darin: Feder und Kugel.",
            quiz("Wer ist zuerst unten?", who("Kugel", "Feder"), whoIdx(Fall.winner(Faller.KUGEL, Faller.FEDER, 2.0, Planet.VAKUUM))),
            FallSetup(Faller.KUGEL, Faller.FEDER, listOf(Planet.VAKUUM, Planet.ERDE), 2.0),
            "Lass beide fallen – einmal ohne, einmal mit Luft.",
            "Ohne Luft kommen Feder und Kugel genau gleichzeitig an!",
            listOf("Ohne Luftwiderstand fallen alle Körper gleich schnell – die Masse spielt keine Rolle.", "In der Röhre ist die Feder schwerer.", "Die Röhre zieht alles gleich an."),
            "Ohne Luft fallen alle Körper gleich schnell (Galilei).",
            quiz("1971 ließ ein Astronaut auf dem Mond einen Hammer und eine Feder fallen. Was geschah?", who("Hammer zuerst", "Feder zuerst"), whoIdx(Fall.winner(Faller.HAMMER, Faller.FEDER, 1.5, Planet.MOND)))),

        Experiment("f4", F, "Schwer gegen leicht", "🔨", 5..7,
            "Ein Hammer (1,3 kg) und ein Tennisball (58 g) aus 2 m Höhe – an der Luft.",
            quiz("Wer ist zuerst unten – oder kommen beide praktisch gleichzeitig an?", who("Hammer", "Tennisball"), whoIdx(Fall.winner(Faller.HAMMER, Faller.TENNIS, 2.0, Planet.ERDE))),
            FallSetup(Faller.HAMMER, Faller.TENNIS, listOf(Planet.ERDE), 2.0),
            "Lass beide fallen und vergleiche die Zeiten.",
            "Praktisch gleichzeitig – der Ball ist nur wenige Tausendstelsekunden langsamer.",
            listOf("Auf kurzer Strecke bremst die Luft beide kaum – also fallen sie gleich schnell.", "Der Tennisball ist innen schwer.", "Der Hammer bremst sich selbst."),
            "Wenn der Luftwiderstand klein ist, fallen schwere und leichte Dinge gleich schnell.",
            quiz("Aus 100 m Höhe?", who("Hammer", "Tennisball"), whoIdx(Fall.winner(Faller.HAMMER, Faller.TENNIS, 100.0, Planet.ERDE)), "Auf langer Strecke wird der Ball so schnell, dass die Luft ihn merklich bremst.")),

        Experiment("f5", F, "Fallzeit rechnen", "⏱️", 8..10,
            "Eine Stahlkugel fällt 20 m tief. Ohne Luftwiderstand gilt: h = ½·g·t².",
            quiz("Wie lange fällt sie?", listOf(c("etwa 1 s"), c("etwa 2 s"), c("etwa 4 s")), nearest(listOf(1.0, 2.0, 4.0), Fall.timeNoAir(20.0, Planet.ERDE.g))),
            FallSetup(Faller.KUGEL, null, listOf(Planet.VAKUUM), 20.0),
            "Lass die Kugel fallen und stopp die Zeit.",
            "2,02 s: t = √(2·20 m / 9,81 m/s²).",
            listOf("t = √(2h/g) = √(40/9,81) ≈ 2 s.", "1 m pro 0,1 s, also 2 s.", "Je schwerer, desto schneller."),
            "Freier Fall: h = ½·g·t², also t = √(2h/g).",
            quiz("Aus 80 m (viermal so hoch)?", listOf(c("etwa 4 s"), c("etwa 8 s"), c("etwa 16 s")), nearest(listOf(4.0, 8.0, 16.0), Fall.timeNoAir(80.0, Planet.ERDE.g)), "Vierfache Höhe – doppelte Zeit.")),

        Experiment("f6", F, "Fallen auf dem Mond", "🌕", 8..10,
            "Auf dem Mond ist g = 1,62 m/s² – nur ein Sechstel so groß wie auf der Erde.",
            quiz("Wievielmal so lange fällt ein Hammer dort 20 m?", listOf(c("sechsmal so lang"), c("etwa 2,5-mal so lang"), c("gleich lang")),
                nearest(listOf(6.0, 2.5, 1.0), Fall.timeNoAir(20.0, Planet.MOND.g) / Fall.timeNoAir(20.0, Planet.ERDE.g))),
            FallSetup(Faller.HAMMER, null, listOf(Planet.VAKUUM, Planet.MOND, Planet.JUPITER), 20.0),
            "Lass den Hammer auf der Erde (ohne Luft), auf dem Mond und auf dem Jupiter fallen.",
            "Erde: 2,0 s, Mond: 5,0 s – 2,46-mal so lang.",
            listOf("t hängt von der Wurzel aus g ab: √(9,81 ÷ 1,62) ≈ √6,06 ≈ 2,46.", "Auf dem Mond gibt es keine Schwerkraft.", "Sechsfach schwächer heißt sechsfach länger."),
            "Fallzeit ∼ 1/√g.",
            quiz("Und auf dem Jupiter (g = 24,8 m/s²): Ist der Hammer dort schneller oder langsamer unten als auf der Erde?", listOf(c("schneller unten"), c("langsamer unten")), if (Fall.timeNoAir(20.0, Planet.JUPITER.g) < Fall.timeNoAir(20.0, Planet.ERDE.g)) 0 else 1)),

        Experiment("f7", F, "Schweres Pendel", "🕰️", 5..7,
            "Zwei Pendel, beide 1 m lang. An einem hängt 100 g, am anderen 1 kg.",
            quiz("Welches schwingt schneller hin und her?", listOf(c("das schwere"), c("das leichte"), c("beide gleich")),
                if (Fmt.close(Pendulum.period(1.0, 9.81, 10.0), Pendulum.period(1.0, 9.81, 10.0), 1e-9)) 2 else 0),
            PendulumSetup(listOf(1.0), listOf(0.1, 1.0)),
            "Starte beide Pendel und vergleiche.",
            "Beide brauchen genau 2,0 s für eine Schwingung.",
            listOf("Die Schwingungsdauer hängt nur von Länge und g ab, nicht von der Masse.", "Schwere Pendel sind träge und langsamer.", "Leichte Pendel werden vom Wind beschleunigt."),
            "Pendel: T = 2π·√(l/g) – ohne Masse!",
            quiz("Ein kürzeres Pendel schwingt …", listOf(c("schneller"), c("langsamer"), c("gleich")), if (Pendulum.periodSmall(0.5, 9.81) < Pendulum.periodSmall(1.0, 9.81)) 0 else 1)),

        Experiment("f8", F, "Viermal so lang", "📏", 8..9,
            "Blubb hat ein 25-cm-Pendel und ein 1-m-Pendel.",
            quiz("Wie verändert sich die Schwingungsdauer bei vierfacher Länge?", listOf(c("doppelt so lang"), c("viermal so lang"), c("gleich")),
                nearest(listOf(2.0, 4.0, 1.0), Pendulum.periodSmall(1.0, 9.81) / Pendulum.periodSmall(0.25, 9.81))),
            PendulumSetup(listOf(0.25, 1.0), listOf(0.2)),
            "Starte beide Pendel und miss die Zeiten.",
            "1,0 s gegen 2,0 s – doppelt so lang.",
            listOf("Die Länge steht unter der Wurzel: √4 = 2.", "Doppelt so lang, doppelt so schwer.", "Die Länge spielt keine Rolle."),
            "T ∼ √l.",
            quiz("Neunfache Länge?", listOf(c("dreifache Dauer"), c("neunfache Dauer"), c("gleich")), nearest(listOf(3.0, 9.0, 1.0), Pendulum.periodSmall(9.0, 9.81) / Pendulum.periodSmall(1.0, 9.81)))),

        Experiment("f9", F, "Das Sekundenpendel", "⏲️", 9..10,
            "Alte Standuhren ticken mit einem Pendel, das für jedes Hin oder Her genau 1 s braucht (T = 2 s).",
            quiz("Wie lang ist so ein Pendel?", listOf(c("etwa 25 cm"), c("etwa 1 m"), c("etwa 2 m")), nearest(listOf(0.25, 1.0, 2.0), Pendulum.lengthFor(2.0, Planet.ERDE.g))),
            PendulumSetup(listOf(0.5), listOf(0.5), targetT = 2.0),
            "Stell die Länge ein, bis T = 2,00 s (±1 %).",
            "Bei 99 cm schwingt das Pendel mit 2,00 s.",
            listOf("l = g·(T/2π)² = 9,81 · (2/6,28)² ≈ 0,99 m.", "Ein Meter pro Sekunde.", "Jedes Pendel braucht 2 s."),
            "Aus T = 2π·√(l/g) folgt l = g·(T/2π)².",
            quiz("Wie geht dieselbe Pendeluhr auf dem Mond?", listOf(c("geht schneller"), c("geht langsamer")), if (Pendulum.periodSmall(0.994, Planet.MOND.g) > 2.0) 1 else 0)),
    )
}

/** Alle Versuche */
object Catalog {
    // muss vor [all] stehen (Reihenfolge beim Anlegen des Objekts)
    /** Fachbereich je Versuch */
    private val topics: Map<String, Topic> = buildMap {
        (1..17).forEach { put("w$it", Topic.AUFTRIEB) }
        ((1..5) + (10..14) + 19).forEach { put("k$it", Topic.HEBEL) }; ((6..9) + (15..18)).forEach { put("k$it", Topic.MASCHINEN) }
        (1..9).forEach { put("n$it", Topic.NEWTON) }
        ((1..9) + (12..18)).forEach { put("s$it", Topic.STROM) }; listOf(10, 11, 19, 20).forEach { put("s$it", Topic.MAGNET) }
        ((1..5) + (14..18)).forEach { put("c$it", Topic.SAEURE) }; ((6..13) + (19..26)).forEach { put("c$it", Topic.REAKTION) }
        (1..14).forEach { put("h$it", Topic.WAERME) }
        ((1..5) + (11..15)).forEach { put("l$it", Topic.LICHT) }; ((6..10) + (16..20)).forEach { put("l$it", Topic.SCHALL) }
        ((1..6) + (10..13) + 17).forEach { put("f$it", Topic.FALL) }; ((7..9) + (14..16)).forEach { put("f$it", Topic.SCHWINGUNG) }
        (1..8).forEach { put("g$it", Topic.VERERBUNG) }; (1..4).forEach { put("o$it", Topic.ZELLEN) }
        (1..8).forEach { put("e$it", Topic.ERNAEHRUNG) }; (1..7).forEach { put("p$it", Topic.OEKOLOGIE) }
        (1..7).forEach { put("t$it", Topic.TRENNEN) }; (1..4).forEach { put("m$it", Topic.LICHT) }
        (1..5).forEach { put("u$it", Topic.ZELLEN); put("q$it", Topic.OEKOLOGIE); put("j$it", Topic.OEKOLOGIE); put("v$it", Topic.KOERPER) }
        (1..11).forEach { put("x$it", Topic.OPTIK) }; (1..13).forEach { put("y$it", Topic.ELEKTROMAGNETISMUS) }
    }
    fun topicOf(id: String): Topic = topics[id] ?: error("Versuch $id hat kein Thema")

    /** Je Insel nach Klassenstufe sortiert (stabil: gleiche Stufe bleibt in der geschriebenen Reihenfolge) */
    val all: List<Experiment> = (CatalogA.water + CatalogD.water + CatalogA.force + CatalogD.force + CatalogB.power + CatalogD.power + CatalogB.chem + CatalogE.chem +
        CatalogC.heat + CatalogE.heat + CatalogC.lightSound + CatalogE.lightSound + CatalogC.fall + CatalogE.fall + CatalogF.bio + CatalogF.chem + CatalogF.light + CatalogG.all + CatalogH.optics + CatalogI.emag)
        .let { all -> Island.entries.flatMap { i -> all.filter { it.island == i }.sortedWith(compareBy({ topicOf(it.id).ordinal }, { it.grades.first })) } }
        .map { with(Typo) { it.typeset() } }

    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun of(island: Island) = all.filter { it.island == island }
    /** Nächster Versuch derselben Insel */
    fun next(id: String): Experiment? { val e = byId(id) ?: return null; val l = of(e.island); return l.getOrNull(l.indexOf(e) + 1) }
    /** Versuche passend zur Klassenstufe (für den Filter) */
    fun forGrade(g: Int) = all.filter { g in it.grades }
}
