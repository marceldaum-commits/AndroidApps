package de.experimentopia.core.model

import de.experimentopia.core.model.Pick.nearest

/** Erweiterung (Version 2.7.0): Optik – Schatten, Spiegel, Brechung, Linsen (Lehrplan Kl. 5–10 in allen Ländern) */
internal object CatalogH {
    private val L = Island.LICHTKLANG
    private fun cm(v: Double) = "${Fmt.num(v, 1)} cm"
    private fun deg(v: Double) = "${Fmt.num(v, 0)}°"
    private fun near(q: String, opts: List<Double>, value: Double, fmt: (Double) -> String) = quiz(q, opts.map { c(fmt(it)) }, nearest(opts, value))

    val optics = listOf(
        // ── Schatten ──
        Experiment("x1", L, "Riesen-Schattenhase", "🐰", 3..5,
            "Blubb spielt Schattentheater: Eine Hasenfigur steht vor der Taschenlampe, an der Wand hüpft ihr Schatten. Blubb will einen RIESEN-Hasen!",
            quiz("Wohin muss die Lampe, damit der Schatten größer wird?", listOf(c("näher an die Figur", "🔦"), c("weiter weg von der Figur", "↔️"), c("egal – der Schatten bleibt gleich", "🤷")),
                if (Optics.shadow(10.0, 20.0, 100.0) > Optics.shadow(10.0, 60.0, 100.0)) 0 else 1),
            ShadowSetup(10.0, 100.0, 50.0),
            "Schieb die Lampe so, dass der Schatten 50 cm groß wird – fünfmal so groß wie der Hase.",
            "Je näher die Lampe an der Figur steht, desto größer wird der Schatten. Bei 20 cm Abstand ist er 50 cm groß.",
            listOf("Licht läuft geradeaus. Steht die Lampe nah, gehen die Strahlen hinter der Figur weit auseinander – der Schatten wird groß.",
                "Eine nahe Lampe ist heller und malt darum größere Schatten.", "Die Figur wird größer, wenn die Lampe nah ist."),
            "Licht breitet sich geradlinig aus. Wo es nicht hinkommt, entsteht Schatten – je näher die Lampe, desto größer der Schatten.",
            quiz("Warum ist dein Schatten am Abend so lang?", listOf(c("Die Sonne steht tief und scheint flach"), c("Abends bist du größer"), c("Abends ist die Sonne heller")), 0)),

        Experiment("x2", L, "Schatten vorausberechnen", "📐", 7..8,
            "Der Hase ist 10 cm hoch, die Wand steht 100 cm von der Lampe weg. Blubb will die Schattengröße vorher ausrechnen – mit dem Strahlensatz.",
            near("Die Lampe steht 40 cm vor dem Hasen. Wie groß wird der Schatten?", listOf(10.0, 25.0, 40.0, 60.0), Optics.shadow(10.0, 40.0, 100.0), ::cm),
            ShadowSetup(10.0, 100.0, 20.0),
            "Stell die Lampe so, dass der Schatten genau 20 cm groß ist.",
            "Schatten ÷ Figur = Wandabstand ÷ Lampenabstand. Für 20 cm Schatten muss die Lampe 50 cm vor dem Hasen stehen: 10 cm · 100 ÷ 50 = 20 cm.",
            listOf("Lichtstrahlen von der Lampe bilden zwei ähnliche Dreiecke – darum gilt der Strahlensatz: S = G · Wandabstand ÷ Lampenabstand.",
                "Der Schatten ist immer doppelt so groß wie die Figur.", "Man muss nur die Figurgröße und den Wandabstand addieren."),
            "Strahlensatz: Schattengröße = Figurgröße · (Lampe bis Wand) ÷ (Lampe bis Figur).",
            near("Lampe 25 cm vor dem Hasen, Wand 100 cm von der Lampe – wie groß ist der Schatten?", listOf(25.0, 40.0, 75.0), Optics.shadow(10.0, 25.0, 100.0), ::cm)),

        // ── Spiegel ──
        Experiment("x3", L, "Laser ins Ziel", "🎯", 3..6,
            "Im Spiegelsaal blinken drei Sterne. Blubbs Laser zeigt schräg nach unten auf einen Spiegel. Kann er alle Sterne treffen?",
            quiz("Der Laser trifft schräg auf den Spiegel. Wie läuft er weiter?", listOf(c("genauso schräg zur anderen Seite", "↗️"), c("genau zurück zum Laser", "↩️"), c("durch den Spiegel hindurch", "⬇️")),
                Pick.index(Optics.reflectAngle(30.0) == 30.0)),
            MirrorSetup(-40.0, listOf(40.0, 70.0, 130.0)),
            "Dreh den Spiegel so, dass der Laserstrahl nacheinander alle drei Sterne trifft.",
            "Der Strahl prallt ab wie ein Ball von der Wand – so schräg, wie er ankommt, fliegt er auch weiter. Wer den Spiegel dreht, lenkt den Strahl.",
            listOf("Ein Spiegel wirft Licht im gleichen Winkel zurück, in dem es ankommt.", "Spiegel schicken Licht immer senkrecht nach oben.", "Der Laser wird vom Spiegel angezogen."),
            "Am Spiegel gilt: Einfallswinkel = Reflexionswinkel.",
            quiz("Wie sieht ein Fahrer um eine enge Ecke, ohne hinzugehen?", listOf(c("mit einem gewölbten Spiegel an der Ecke"), c("mit einer Lupe"), c("mit einer Taschenlampe")), 0)),

        Experiment("x4", L, "Das Reflexionsgesetz", "📐", 7..8,
            "Blubb zeichnet das Lot ein – eine Linie senkrecht auf dem Spiegel. Von dort aus misst er die Winkel: Wie schräg kommt der Strahl an, wie schräg geht er?",
            near("Der Laser trifft unter 35° zum Lot. Unter welchem Winkel zum Lot wird er zurückgeworfen?", listOf(35.0, 55.0, 70.0), Optics.reflectAngle(Optics.incidence(-55.0, 0.0)), ::deg),
            MirrorSetup(-55.0, listOf(35.0, 95.0, 145.0), showAngles = true),
            "Triff alle drei Sterne und beobachte dabei die beiden Winkel am Lot.",
            "Einfallswinkel und Reflexionswinkel sind immer gleich groß. Dreht man den Spiegel, ändern sich beide gemeinsam.",
            listOf("Das Lot halbiert den Winkel zwischen ankommendem und zurückgeworfenem Strahl – das ist das Reflexionsgesetz.",
                "Der Reflexionswinkel ist immer 90° minus Einfallswinkel.", "Der Reflexionswinkel ist immer doppelt so groß."),
            "Reflexionsgesetz: α = β, beide Winkel werden vom Lot aus gemessen. Strahl, Lot und reflektierter Strahl liegen in einer Ebene.",
            near("Blubb dreht den Spiegel um 10°. Um wie viel Grad dreht sich der zurückgeworfene Strahl?", listOf(5.0, 10.0, 20.0),
                Optics.angleDiff(Optics.reflectDir(-55.0, 10.0), Optics.reflectDir(-55.0, 0.0)), ::deg)),

        // ── Brechung ──
        Experiment("x5", L, "Die geknickte Münze", "🐠", 5..7,
            "Auf dem Boden eines Aquariums liegt eine Münze. Blubb leuchtet mit dem Laser schräg ins Wasser – aber der Strahl landet nicht da, wo er hinzielt!",
            quiz("Licht geht schräg von Luft in Wasser. Wie läuft es weiter?", listOf(c("es knickt zum Lot hin (steiler)"), c("es knickt vom Lot weg (flacher)"), c("es läuft gerade weiter")),
                Optics.refract(40.0, Medium.LUFT.n, Medium.WASSER.n)!!.let { if (it < 40.0) 0 else if (it > 40.0) 1 else 2 }),
            RefractSetup(Medium.LUFT, Medium.WASSER, RefractGoal.HIT, 30.0),
            "Stell den Einfallswinkel so ein, dass der Strahl im Wasser unter 30° zum Lot läuft und die Münze trifft.",
            "Im Wasser läuft der Strahl steiler. Für 30° im Wasser braucht man etwa 42° in der Luft.",
            listOf("Licht ist im Wasser langsamer als in Luft – beim schrägen Eintritt knickt es darum zum Lot hin.", "Das Wasser drückt den Strahl nach unten.", "Wasser zieht Licht an wie ein Magnet."),
            "Brechung: Geht Licht schräg in einen dichteren Stoff (Luft → Wasser, Luft → Glas), knickt es zum Lot hin.",
            quiz("Warum sieht ein Strohhalm im Wasserglas geknickt aus?", listOf(c("Licht knickt an der Wasseroberfläche"), c("Das Wasser biegt den Strohhalm"), c("Das Glas ist gewölbt und schief")), 0)),

        Experiment("x6", L, "Licht bleibt gefangen", "🔦", 8..10,
            "Jetzt leuchtet Blubb von innen aus einem Glasblock heraus in die Luft – und macht den Winkel immer flacher. Plötzlich passiert etwas Seltsames.",
            quiz("Was passiert, wenn der Winkel immer größer wird?", listOf(c("Ab einem Winkel kommt gar kein Licht mehr heraus"), c("Es kommt immer Licht heraus"), c("Das Licht wird farbig")),
                if (Optics.critical(Medium.GLAS.n, Medium.LUFT.n) != null) 0 else 1),
            RefractSetup(Medium.GLAS, Medium.LUFT, RefractGoal.TOTAL),
            "Vergrößere den Einfallswinkel, bis kein Licht mehr aus dem Glas herauskommt.",
            "Ab etwa 42° wird das ganze Licht zurück ins Glas gespiegelt – Totalreflexion. Darunter kommt ein Teil heraus und knickt vom Lot weg.",
            listOf("Von Glas nach Luft knickt Licht vom Lot weg. Ab dem Grenzwinkel müsste es über 90° hinaus – also bleibt es im Glas.",
                "Das Glas wird bei flachem Licht undurchsichtig.", "Der Laser ist zu schwach für flache Winkel."),
            "Totalreflexion: Von dicht nach dünn kommt ab dem Grenzwinkel kein Licht mehr heraus. Glasfasern leiten so Licht um jede Kurve.",
            near("Wie groß ist der Grenzwinkel von Glas (n = 1,5) zu Luft?", listOf(30.0, 42.0, 60.0), Optics.critical(Medium.GLAS.n, Medium.LUFT.n)!!, ::deg)),

        Experiment("x7", L, "Warum Diamanten funkeln", "💎", 9..10,
            "Ein Diamant hat die riesige Brechzahl 2,42. Blubb vermutet: Darum funkelt er so sehr.",
            quiz("Ist der Grenzwinkel bei Diamant größer oder kleiner als bei Glas (42°)?", listOf(c("kleiner"), c("größer"), c("gleich groß")),
                Optics.critical(Medium.DIAMANT.n, 1.0)!!.let { d -> val g = Optics.critical(Medium.GLAS.n, 1.0)!!; if (d < g) 0 else if (d > g) 1 else 2 }),
            RefractSetup(Medium.DIAMANT, Medium.LUFT, RefractGoal.TOTAL),
            "Finde den Grenzwinkel im Diamanten: Ab wann bleibt das Licht drin?",
            "Schon ab etwa 24° bleibt das Licht im Diamanten gefangen. Es wird viele Male hin und her gespiegelt, bevor es oben herauskommt – darum funkelt er.",
            listOf("Grenzwinkel: sin α = 1 ÷ n. Je größer die Brechzahl, desto kleiner der Grenzwinkel – und desto mehr Licht bleibt gefangen.",
                "Diamanten leuchten von selbst.", "Diamanten sind hohl und voller Licht."),
            "Grenzwinkel: sin α_G = n₂ ÷ n₁. Große Brechzahl → kleiner Grenzwinkel → viel Totalreflexion.",
            near("Wie groß ist der Grenzwinkel von Wasser (n = 1,33) zu Luft?", listOf(24.0, 42.0, 49.0), Optics.critical(Medium.WASSER.n, 1.0)!!, ::deg)),

        // ── Linsen ──
        Experiment("x8", L, "Die Kerze steht Kopf", "🕯️", 5..7,
            "Blubb stellt eine Kerze vor eine Lupe und hält ein Blatt Papier dahinter. Auf dem Blatt erscheint ein Bild der Flamme!",
            quiz("Wie sieht die Kerze auf dem Papier aus?", listOf(c("Sie steht auf dem Kopf", "🙃"), c("Sie steht aufrecht", "🕯️"), c("Es gibt gar kein Bild", "⬜")),
                if (Optics.imageDist(10.0, 30.0) != null) 0 else 2),
            LensSetup(10.0, 30.0, LensGoal.SHARP),
            "Schieb den Schirm, bis die Kerze scharf zu sehen ist.",
            "Bei 15 cm hinter der Linse ist das Bild scharf – kleiner als die Kerze und auf dem Kopf.",
            listOf("Die Lichtstrahlen kreuzen sich in der Linse: Was oben war, landet unten.", "Die Linse dreht das Licht mit Absicht um.", "Der Schirm hängt verkehrt herum."),
            "Eine Sammellinse macht auf dem Schirm ein umgedrehtes Bild – scharf nur in einem bestimmten Abstand.",
            quiz("Wie ist das Bild in deinem Auge auf der Netzhaut?", listOf(c("auf dem Kopf – das Gehirn dreht es um"), c("aufrecht"), c("es gibt keins")), 0)),

        Experiment("x9", L, "Die Linsengleichung", "🧮", 8..10,
            "Die Linse hat 10 cm Brennweite, die Kerze steht 25 cm davor. Blubb will die Bildweite ausrechnen, bevor er den Schirm schiebt.",
            near("Wie weit hinter der Linse wird das Bild scharf?", listOf(10.0, 16.7, 25.0), Optics.imageDist(10.0, 25.0)!!) { "etwa ${Fmt.num(it, 0)} cm" },
            LensSetup(10.0, 25.0, LensGoal.SHARP),
            "Schieb den Schirm an die Stelle, an der das Bild scharf ist.",
            "1/10 = 1/25 + 1/b ergibt b ≈ 16,7 cm. Genau dort wird das Bild scharf.",
            listOf("Linsengleichung: 1/f = 1/g + 1/b. Mit f = 10 cm und g = 25 cm folgt 1/b = 0,1 − 0,04 = 0,06, also b ≈ 16,7 cm.",
                "Das Bild ist immer genau bei der Brennweite.", "Bildweite = Gegenstandsweite − Brennweite."),
            "Linsengleichung: 1/f = 1/g + 1/b (f Brennweite, g Gegenstandsweite, b Bildweite).",
            near("Die Kerze rückt auf 15 cm an die Linse. Wo ist jetzt das scharfe Bild?", listOf(7.5, 15.0, 30.0), Optics.imageDist(10.0, 15.0)!!) { "${Fmt.num(it, 1)} cm" }),

        Experiment("x10", L, "Doppelt so groß", "🔎", 8..10,
            "Für ein Dia-Bild soll die Kerze auf dem Schirm genau doppelt so groß erscheinen. Blubb darf Kerze und Schirm verschieben.",
            near("Wie weit muss die Kerze von der Linse (f = 10 cm) weg?", listOf(5.0, 15.0, 20.0, 30.0), Optics.objectFor(10.0, 2.0), ::cm),
            LensSetup(10.0, null, LensGoal.SIZE, 2.0),
            "Mach das Bild scharf und genau doppelt so groß wie die Kerze.",
            "Bei 15 cm Gegenstandsweite liegt das scharfe Bild bei 30 cm – doppelt so weit weg, also doppelt so groß.",
            listOf("Abbildungsmaßstab: B/G = b/g. Doppelt so groß heißt b = 2g; mit der Linsengleichung folgt g = 15 cm und b = 30 cm.",
                "Doppelte Brennweite gibt immer doppelte Größe.", "Man braucht dafür eine zweite Linse."),
            "Abbildungsmaßstab: B ÷ G = b ÷ g. Je näher die Kerze an den Brennpunkt rückt, desto größer und weiter weg das Bild.",
            near("Wo muss die Kerze stehen, damit das Bild genau gleich groß ist?", listOf(10.0, 20.0, 40.0), Optics.objectFor(10.0, 1.0), ::cm)),

        Experiment("x11", L, "Die Lupe", "🐞", 5..7,
            "Blubb schiebt die Kerze ganz nah an die Linse heran – näher als die Brennweite. Was zeigt jetzt der Schirm?",
            quiz("Kerze näher als die Brennweite: Was zeigt der Schirm?", listOf(c("gar kein scharfes Bild"), c("ein riesiges scharfes Bild"), c("ein winziges scharfes Bild")),
                if (Optics.imageDist(10.0, 5.0) == null) 0 else 1),
            LensSetup(10.0, null, LensGoal.MAGNIFIER),
            "Schieb die Kerze so nah an die Linse, dass die Linse zur Lupe wird.",
            "Innerhalb der Brennweite gibt es kein Bild auf dem Schirm. Schaut man aber durch die Linse, sieht man die Kerze groß und aufrecht – eine Lupe!",
            listOf("Die Strahlen laufen hinter der Linse auseinander und treffen sich nie. Das Auge verlängert sie rückwärts und sieht ein großes, aufrechtes Bild.",
                "Die Linse ist zu nah und darum kaputt.", "Die Kerze ist zu klein für die Linse."),
            "Lupe: Gegenstand innerhalb der Brennweite → vergrößertes, aufrechtes Bild, das man nur durch die Linse sieht.",
            quiz("Wie hältst du eine Lupe über eine Ameise?", listOf(c("näher als die Brennweite"), c("genau doppelt so weit wie die Brennweite"), c("so weit weg wie möglich")), 0)),
    )
}
