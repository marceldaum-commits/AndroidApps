package de.experimentopia.core.model

import de.experimentopia.core.model.Pick.nearest

/**
 * Erweiterung (Version 2.16.0): Motor, Dynamo & Trafo – Elektromagnet, Kraft auf Strom, Elektromotor, Induktion, Generator, Transformator (Lehrplan Kl. 8–10).
 * Alle Zahlen kommen aus [Emag]; Antworten werden berechnet, nicht hingeschrieben.
 */
internal object CatalogI {
    private val S = Island.STROM
    private fun mt(v: Double) = "${Fmt.num(v, 1)} mT"
    private fun volt(v: Double) = "${Fmt.num(v, 1)} V"
    private fun near(q: String, opts: List<Double>, value: Double, fmt: (Double) -> String) = quiz(q, opts.map { c(fmt(it)) }, nearest(opts, value))
    private val b100 = Emag.coilField(100, 1.5, false) * 1000      // 100 Windungen, 3 V, Luftspule → 1,885 mT

    private val raw = listOf(
        // ── Elektromagnet ──
        Experiment("y1", S, "Strom macht Magnet", "🧲", 5..7,
            "Blubb wickelt Draht um einen Nagel und hängt ihn an eine Batterie. Daneben liegen Büroklammern. Ob die wohl hüpfen?",
            quiz("Der Strom wird eingeschaltet. Was passiert mit den Büroklammern neben der Spule?", listOf(c("Sie werden angezogen", "📎"), c("Nichts", "😴"), c("Sie werden weggeschleudert", "💨")),
                Pick.index(Emag.coilField(100, 1.5, false) > 0)),
            CoilSetup(100, 0.0, false, adjustVolts = true, minMilliT = 1.0),
            "Dreh die Spannung auf, bis das Feld der Spule mindestens 1 mT stark ist.",
            "Fließt Strom durch den Draht, entsteht um ihn ein Magnetfeld – die Spule wird zum Magneten. Mit 100 Windungen und 3 V (1,5 A) sind es ${Fmt.num(b100, 1)} mT. Ohne Strom ist die Spule kein Magnet.",
            listOf("Elektrischer Strom erzeugt ein Magnetfeld. Fließt der Strom, ist die Spule ein Magnet; ohne Strom nicht.",
                "Die Büroklammern werden von der Batterie angezogen.", "Der Draht wird heiß und zieht darum Metall an."),
            "Elektromagnet: Fließt Strom durch eine Spule, entsteht ein Magnetfeld. Schaltet man den Strom aus, verschwindet es.",
            quiz("Warum nimmt ein Schrottplatz-Kran einen Elektromagneten und keinen Dauermagneten?",
                listOf(c("Man kann ihn ausschalten, damit das Eisen wieder herunterfällt"), c("Er ist billiger"), c("Er zieht auch Holz an")), 0)),

        Experiment("y2", S, "Mehr Windungen, stärkeres Feld", "🌀", 6..8,
            "Blubbs Spule mit 100 Windungen hebt gerade eine Büroklammer. Blubb will mehr: Er wickelt einfach mehr Draht auf – und schaut, was das Feld macht.",
            near("Bei 3 V (1,5 A) hat die Spule mit 100 Windungen ${Fmt.num(b100, 1)} mT. Wie stark ist sie mit 300 Windungen (gleicher Strom)?", listOf(1.9, 3.8, 5.7, 19.0), Emag.coilField(300, 1.5, false) * 1000, ::mt),
            CoilSetup(100, 3.0, false, adjustWindings = true, targetMilliT = Emag.coilField(300, 1.5, false) * 1000),
            "Stell die Windungszahl so ein, dass das Feld ${Fmt.num(Emag.coilField(300, 1.5, false) * 1000, 1)} mT stark ist (± 3 %).",
            "Feld und Windungszahl sind proportional: B = μ₀ · N · I ÷ l. Dreimal so viele Windungen, dreimal so starkes Feld: 3 · ${Fmt.num(b100, 1)} mT ≈ ${Fmt.num(Emag.coilField(300, 1.5, false) * 1000, 1)} mT.",
            listOf("Jede Windung trägt ihr Magnetfeld bei. Viele Windungen ergeben ein stärkeres Feld – B ist proportional zu N.",
                "Mehr Windungen verbrauchen mehr Strom, darum wird das Feld schwächer.", "Die Windungszahl ändert nichts, nur die Drahtfarbe."),
            "Feld einer langen Spule: B = μ₀ · N · I ÷ l. Doppelte Windungszahl, doppelt so starkes Feld.",
            quiz("Du verdoppelst die Windungszahl UND den Strom. Wie ändert sich das Feld?", listOf(c("Es wird viermal so stark"), c("Es wird doppelt so stark"), c("Es wird halb so stark")), 0)),

        Experiment("y3", S, "Der Eisenkern", "🔩", 6..8,
            "Blubb schiebt einen Eisenstab in die Spule. Plötzlich springen die Büroklammern an wie die Flöhe! Was macht das Eisen?",
            quiz("Du steckst einen Eisenstab in die Spule. Das Feld wird …", listOf(c("etwa 200-mal so stark", "🚀"), c("gleich stark", "🟰"), c("schwächer", "🔻")),
                Pick.index(Emag.coilField(100, 0.5, true) > 100 * Emag.coilField(100, 0.5, false))),
            CoilSetup(100, 1.0, false, adjustCore = true, minMilliT = 50.0),
            "Schieb den Eisenkern in die Spule: Das Feld soll mindestens 50 mT erreichen (100 Windungen, 1 V).",
            "Das Eisen wird selbst zum Magneten und verstärkt das Feld der Spule stark. Im Modell (Faktor 200) werden aus ${Fmt.num(Emag.coilField(100, 0.5, false) * 1000, 2)} mT genau ${Fmt.num(Emag.coilField(100, 0.5, true) * 1000, 0)} mT.",
            listOf("Im Eisen richten sich winzige Elementarmagnete aus und verstärken das Feld der Spule.", "Das Eisen leitet den Strom besser als Luft.", "Eisen ist schwer und drückt das Feld zusammen."),
            "Ein Eisenkern verstärkt das Feld einer Spule um ein Vielfaches (im Modell etwa 200-fach).",
            quiz("Warum wickelt man den Draht eines starken Elektromagneten um einen Eisenkern?", listOf(c("Das Feld wird viel stärker"), c("Der Magnet wird billiger"), c("Der Magnet wird leichter")), 0)),

        Experiment("y4", S, "Das Feld ausrechnen", "🧮", 8..10,
            "Blubb will die Stärke seines Magneten nicht schätzen, sondern ausrechnen. Dafür gibt es eine Formel für die lange Spule.",
            near("Luftspule: N = 250 Windungen, l = 10 cm, I = 1,6 A. Wie groß ist B?", listOf(0.5, 1.6, 5.0, 16.0), Emag.coilField(250, 1.6, false) * 1000, ::mt),
            CoilSetup(250, 3.0, false, adjustVolts = true, targetMilliT = 5.0),
            "Stell die Spannung so ein, dass das Feld 5,0 mT stark ist (± 3 %). Die Spule hat 250 Windungen und 2 Ω.",
            "B = μ₀ · N · I ÷ l = 1,2566·10⁻⁶ T·m/A · 250 · 1,6 A ÷ 0,10 m ≈ 5,03·10⁻³ T ≈ 5,0 mT. Dafür brauchst du I = 1,6 A, also U = I · R = 1,6 A · 2 Ω = 3,2 V.",
            listOf("B wächst mit Windungszahl und Strom und sinkt mit der Spulenlänge: B = μ₀ · N · I ÷ l.", "B hängt nur von der Batteriespannung ab, nicht vom Strom.", "B wird größer, wenn die Spule länger wird."),
            "B = μ₀ · N · I ÷ l (B in Tesla, μ₀ = 1,2566·10⁻⁶ T·m/A, N Windungen, I in Ampere, l in Meter).",
            quiz("Dieselbe Spule wird auf die halbe Länge zusammengeschoben (gleiche Windungen, gleicher Strom). B wird …", listOf(c("doppelt so groß"), c("halb so groß"), c("gleich")), 0)),

        // ── Motor ──
        Experiment("y5", S, "Die Leiterschaukel", "🪁", 8..9,
            "Blubb hängt einen Draht wie eine Schaukel zwischen die Pole eines Hufeisenmagneten. Strom an – und die Schaukel schwingt von selbst aus!",
            quiz("Du polst den Strom um. Die Schaukel schlägt …", listOf(c("in die Gegenrichtung aus", "↔️"), c("gar nicht aus", "😴"), c("in dieselbe Richtung, nur stärker", "➡️")), 0),
            MotorSetup(MotorMode.DIRECTION, wantRight = true),
            "Pole den Strom so, dass die Schaukel nach rechts ausschlägt.",
            "Auf einen Leiter, durch den Strom fließt, wirkt im Magnetfeld eine Kraft. Kehrt man den Strom um, kehrt sich auch die Kraft um – die Schaukel schlägt zur anderen Seite aus.",
            listOf("Die Kraft auf den Draht hängt von der Stromrichtung ab: andere Stromrichtung, andere Kraftrichtung.", "Beim Umpolen wird der Magnet schwächer.", "Die Schaukel schwingt von selbst hin und her, egal wie der Strom fließt."),
            "Auf einen stromdurchflossenen Leiter im Magnetfeld wirkt eine Kraft. Kehrt man den Strom um, kehrt sich die Kraft um.",
            quiz("Wie kehrst du die Drehrichtung eines Gleichstrommotors um?", listOf(c("Die Batterie umpolen"), c("Den Motor schütteln"), c("Die Kabel verlängern")), 0)),

        Experiment("y6", S, "Wie stark ist die Kraft?", "💪", 9..10,
            "Die Leiterschaukel schlägt mal stärker, mal schwächer aus. Blubb misst nach: Wovon hängt die Kraft ab – und wie viel Newton sind es?",
            near("Magnet B = 0,1 T, Leiter l = 4 cm, Strom I = 5 A (senkrecht zum Feld). Wie groß ist die Kraft?", listOf(0.002, 0.02, 0.2, 2.0), Emag.conductorForce(0.1, 5.0, 0.04), { "${Fmt.num(it, 3)} N" }),
            MotorSetup(MotorMode.FORCE, 0.1, 0.06, targetN = 0.03),
            "Stell den Strom so ein, dass die Kraft 0,03 N beträgt (B = 0,1 T, Leiter 6 cm lang, ± 3 %).",
            "F = B · I · l. Für 0,03 N: I = F ÷ (B · l) = 0,03 N ÷ (0,1 T · 0,06 m) = 5 A. Probe: 0,1 T · 5 A · 0,06 m = 0,03 N.",
            listOf("Die Kraft ist proportional zu Feld, Strom und Leiterlänge: F = B · I · l.", "Die Kraft hängt nur von der Länge des Kabels zur Steckdose ab.", "Je kleiner der Strom, desto größer die Kraft."),
            "F = B · I · l (Leiter senkrecht zum Feld; B in Tesla, I in Ampere, l in Meter, F in Newton).",
            quiz("Du verdoppelst den Strom und halbierst die Leiterlänge. Die Kraft wird …", listOf(c("gleich groß"), c("doppelt so groß"), c("halb so groß")), 0)),

        Experiment("y7", S, "Der Elektromotor", "⚙️", 8..10,
            "Blubb baut den Motor aus der Leiterschaukel: Der Draht dreht sich im Kreis, und mit einem Polwender wechselt der Strom bei jeder halben Umdrehung. Wie schnell dreht er sich?",
            near("Der Motor dreht im Leerlauf bei 2 V mit 1 200 Umdrehungen pro Minute (Drehzahl ∝ Spannung). Wie schnell bei 4 V?", listOf(600.0, 1200.0, 2400.0, 4800.0), Emag.motorRpm(4.0), { "${Fmt.num(it, 0)} min⁻¹" }),
            MotorSetup(MotorMode.SPEED, targetRpm = 3000.0),
            "Stell die Spannung so ein, dass der Motor 3 000 Umdrehungen pro Minute macht (± 3 %).",
            "Im Leerlauf ist die Drehzahl proportional zur Spannung: n = 600 min⁻¹ je Volt · U. Für 3 000 min⁻¹ brauchst du U = 3 000 ÷ 600 = 5 V.",
            listOf("Mehr Spannung heißt mehr Strom in den Spulen, damit mehr Kraft – der Motor dreht schneller.", "Mehr Spannung bremst den Motor.", "Die Drehzahl hängt nur von der Farbe der Kabel ab."),
            "Gleichstrommotor: Mehr Spannung, mehr Drehzahl. Strom umpolen, Drehrichtung umkehren.",
            quiz("Du verdoppelst die Spannung am Motor. Die Leerlaufdrehzahl wird …", listOf(c("doppelt so groß"), c("gleich"), c("halb so groß")), 0)),

        // ── Induktion ──
        Experiment("y8", S, "Magnet in der Spule", "🧲", 7..9,
            "Blubb schließt ein Messgerät an eine Spule an. Dann schiebt er einen Stabmagneten hinein – der Zeiger zuckt! Und wenn der Magnet still liegt?",
            quiz("Ein Stabmagnet liegt ruhig in der Spule. Das Messgerät zeigt …", listOf(c("nichts – nur wenn sich der Magnet bewegt", "0️⃣"), c("immer Spannung", "⚡"), c("Spannung, aber nur beim Nordpol", "🧭")), 0),
            DynamoSetup(DynamoMode.MAGNET, 500, minV = 0.05),
            "Bewege den Magneten so schnell durch die Spule, dass die Spannung mindestens 50 mV erreicht (500 Windungen).",
            "Spannung entsteht nur, wenn sich das Magnetfeld in der Spule ändert (Induktion). Ein ruhender Magnet ergibt 0 V. Bei 0,5 m/s sind es ${Fmt.num(Emag.inducedByMagnet(500, 0.5) * 1000, 1)} mV.",
            listOf("Spannung wird nur induziert, wenn sich der magnetische Fluss durch die Spule ändert. Ein ruhender Magnet ändert nichts.", "Magnete erzeugen immer Spannung, das Messgerät ist nur zu ungenau.", "Die Spule muss sich erst aufladen."),
            "Induktion: Ändert sich das Magnetfeld in einer Spule, entsteht eine Spannung – bei Ruhe nicht.",
            quiz("Wie entsteht der Strom für die Lampe am Fahrraddynamo?", listOf(c("Ein Magnet dreht sich an einer Spule vorbei"), c("Im Rad steckt eine Batterie"), c("Die Reibung heizt den Draht auf")), 0)),

        Experiment("y9", S, "Die Induktionsspannung", "📈", 9..10,
            "Blubb schiebt den Magneten mal langsam, mal schnell durch Spulen mit verschiedenen Windungszahlen. Es gibt eine Formel, die alles erklärt.",
            near("Magnet mit 0,5 m/s durch 500 Windungen: ${Fmt.num(Emag.inducedByMagnet(500, 0.5) * 1000, 1)} mV. Wie viel bei 1,0 m/s?", listOf(31.0, 62.5, 125.0, 250.0), Emag.inducedByMagnet(500, 1.0) * 1000, { "${Fmt.num(it, 1)} mV" }),
            DynamoSetup(DynamoMode.MAGNET, 500, adjustWindings = true, targetV = 0.1),
            "Erzeuge 100 mV (± 3 %): Such dir Windungszahl und Geschwindigkeit aus. Es gibt mehrere Lösungen!",
            "U = N · ΔΦ ÷ Δt mit Δt = s ÷ v. Beispiel 500 Windungen, v = 0,8 m/s: Δt = 0,10 m ÷ 0,8 m/s = 0,125 s; U = 500 · 2,5·10⁻⁵ Wb ÷ 0,125 s = 0,10 V. Auch 1 000 Windungen mit 0,4 m/s ergeben 0,10 V.",
            listOf("Schneller bewegt, ändert sich der Fluss in kürzerer Zeit – die Spannung steigt. Mehr Windungen: Jede Windung liefert ihren Teil.", "Je langsamer, desto größer die Spannung.", "Die Spannung hängt nur vom Material der Spule ab."),
            "U = N · ΔΦ ÷ Δt: Mehr Windungen oder schnellere Änderung, größere Spannung.",
            quiz("Du verdoppelst die Windungszahl und die Geschwindigkeit. Die Spannung wird …", listOf(c("viermal so groß"), c("doppelt so groß"), c("gleich")), 0)),

        Experiment("y10", S, "Der Generator", "🚲", 9..10,
            "Im Kraftwerk dreht eine Turbine riesige Spulen im Magnetfeld. Blubb dreht im Labor eine kleine Spule mit der Kurbel – und auf dem Schirm wellt sich die Spannung.",
            near("Eine Spule (N = 100, A = 20 cm², B = 0,1 T) dreht sich 10-mal pro Sekunde. Wie groß ist die Scheitelspannung Û?", listOf(0.6, 1.3, 2.5, 6.3), Emag.generatorPeak(100, 0.1, 0.002, 10.0), ::volt),
            DynamoSetup(DynamoMode.GENERATOR, 100, teslas = 0.1, areaM2 = 0.002, targetV = 2.5),
            "Dreh den Generator so schnell, dass die Scheitelspannung 2,5 V erreicht (± 3 %).",
            "Û = N · B · A · 2π · f = 100 · 0,1 T · 0,002 m² · 2π · 20 s⁻¹ ≈ ${Fmt.num(Emag.generatorPeak(100, 0.1, 0.002, 20.0), 2)} V. Effektivwert: Û ÷ √2 ≈ ${Fmt.num(Emag.effective(Emag.generatorPeak(100, 0.1, 0.002, 20.0)), 2)} V.",
            listOf("Die drehende Spule ändert ständig ihren Fluss: Je schneller sie dreht, desto größer die Spannung. Sie wechselt ihr Vorzeichen – das ist Wechselspannung.", "Beim schnelleren Drehen wird die Spannung kleiner.", "Ein Generator erzeugt immer gleichmäßige Gleichspannung."),
            "Generator: Û = N · B · A · 2π · f. Die Spannung wechselt ihr Vorzeichen – Wechselspannung.",
            quiz("Wie erhöhst du die Spannung eines Fahrraddynamos?", listOf(c("Schneller fahren (höhere Drehfrequenz)"), c("Das Licht ausschalten"), c("Die Reifen aufpumpen")), 0)),

        // ── Trafo ──
        Experiment("y11", S, "Spannung umspannen", "🔌", 9..10,
            "Das Handy-Ladegerät macht aus 230 V nur wenige Volt. Drinnen steckt ein Transformator: zwei Spulen auf einem gemeinsamen Eisenkern, ohne Draht-Verbindung.",
            near("Primärspule: 600 Windungen an 12 V. Sekundärspule: 300 Windungen. Wie groß ist U₂?", listOf(3.0, 6.0, 12.0, 24.0), Emag.secondaryVolts(12.0, 600, 300), ::volt),
            TrafoSetup(TrafoMode.RATIO, 12.0, 600, 6.0, targetU2 = 4.0),
            "Wähle die Sekundärwindungen so, dass U₂ = 4 V herauskommt (± 3 %).",
            "U₂ ÷ U₁ = N₂ ÷ N₁, also U₂ = U₁ · N₂ ÷ N₁. Für 4 V: N₂ = 600 · 4 V ÷ 12 V = 200 Windungen.",
            listOf("Die Spannungen verhalten sich wie die Windungszahlen: U₁ ÷ U₂ = N₁ ÷ N₂.", "Die Spannung bleibt immer gleich, nur der Strom ändert sich.", "Je weniger Windungen, desto höher die Spannung."),
            "Transformator: U₁ ÷ U₂ = N₁ ÷ N₂ (funktioniert nur mit Wechselspannung).",
            near("Ein Trafo hat primär 1 000 und sekundär 50 Windungen an 230 V. Wie groß ist U₂ (ideal)?", listOf(4.6, 11.5, 23.0, 46.0), Emag.secondaryVolts(230.0, 1000, 50), ::volt)),

        Experiment("y12", S, "Trafo: Strom und Leistung", "⚡", 9..10,
            "Der Trafo macht aus 12 V zum Beispiel 6 V. Aber Energie lässt sich nicht aus dem Nichts erzeugen. Was passiert mit der Stromstärke?",
            near("Ein idealer Trafo macht aus 12 V 6 V (N₂ = N₁ ÷ 2). Primär fließt 1,0 A. Wie groß ist die Stromstärke sekundär?", listOf(0.5, 1.0, 2.0, 6.0), 12.0 * 1.0 / 6.0, { "${Fmt.num(it, 1)} A" }),
            TrafoSetup(TrafoMode.RATIO, 12.0, 600, 6.0, targetI2 = 3.0),
            "Stell die Sekundärstromstärke auf 3 A ein (Last 6 Ω, ± 3 %).",
            "I₂ = U₂ ÷ R. Für 3 A braucht es U₂ = 3 A · 6 Ω = 18 V, also N₂ = 600 · 18 ÷ 12 = 900 Windungen. Leistung: P = 18 V · 3 A = 54 W. Primär: I₁ = P ÷ U₁ = 54 W ÷ 12 V = 4,5 A. Beim idealen Trafo ist P₁ = P₂.",
            listOf("Ein idealer Trafo verliert keine Leistung: U₁ · I₁ = U₂ · I₂. Wird die Spannung kleiner, wird der Strom größer.", "Der Trafo erzeugt zusätzliche Leistung.", "Strom und Spannung sind beim Trafo immer gleich."),
            "Idealer Trafo: P₁ = P₂, also I₁ ÷ I₂ = N₂ ÷ N₁.",
            near("Ein Trafo senkt 230 V auf 23 V. Sekundär fließen 5,0 A. Wie groß ist I₁ (ideal)?", listOf(0.5, 1.0, 5.0, 50.0), 23.0 * 5.0 / 230.0, { "${Fmt.num(it, 1)} A" })),

        Experiment("y13", S, "Warum Hochspannung?", "🗼", 9..10,
            "Hohe Strommasten tragen Leitungen mit 110 000 oder 380 000 Volt – lebensgefährlich! Warum macht man das? Blubb rechnet nach, wie viel Strom unterwegs verloren geht.",
            near("Ein Kraftwerk schickt 2 MW durch eine Leitung (5 Ω). Bei 10 kV gehen 200 kW verloren. Wie viel bei 100 kV?", listOf(0.2, 2.0, 20.0, 200.0), Emag.lineLoss(2_000_000.0, 100_000.0, 5.0) / 1000, { "${Fmt.num(it, 1)} kW" }),
            TrafoSetup(TrafoMode.LINE, powerW = 2_000_000.0, lineOhm = 5.0, maxLossShare = 0.01),
            "Wähle eine Leitungsspannung, bei der höchstens 1 % der Leistung verloren geht.",
            "I = P ÷ U und P_V = I² · R. Bei 10 kV: I = 2 000 000 W ÷ 10 000 V = 200 A, P_V = (200 A)² · 5 Ω = 200 000 W = 10 %. Bei 50 kV: I = 40 A, P_V = (40 A)² · 5 Ω = 8 000 W = 0,4 %.",
            listOf("Bei hoher Spannung fließt für dieselbe Leistung weniger Strom (I = P ÷ U). Die Verluste wachsen mit dem Quadrat des Stroms (P_V = I² · R) – sie sinken stark.", "Bei hoher Spannung wird der Draht dicker.", "Bei höherer Spannung wird die Leitung kälter und leitet besser."),
            "Fernleitungen nutzen Hochspannung: P = U · I und P_V = I² · R. Hohe Spannung, kleiner Strom, kleine Verluste.",
            quiz("Du verdoppelst die Spannung der Leitung (gleiche Leistung). Die Verluste werden …", listOf(c("viermal kleiner"), c("halb so groß"), c("doppelt so groß")), 0)),
    )

    /** Antworten, die keine Zahlen sind, werden reihum verschoben, damit die richtige nicht immer oben steht (Zahlen bleiben der Größe nach sortiert) */
    private fun mixed(q: Quiz, shift: Int): Quiz {
        val numeric = q.choices.all { ch -> ch.text.firstOrNull()?.let { it.isDigit() || it == '−' } == true }
        if (numeric || q.choices.size < 2) return q
        val k = shift % q.choices.size
        val rotated = q.choices.drop(k) + q.choices.take(k)
        return q.copy(choices = rotated, answer = (q.answer - k + q.choices.size) % q.choices.size)
    }

    val emag: List<Experiment> = raw.mapIndexed { i, e -> e.copy(predict = mixed(e.predict, i), transfer = mixed(e.transfer, i + 1)) }
}
