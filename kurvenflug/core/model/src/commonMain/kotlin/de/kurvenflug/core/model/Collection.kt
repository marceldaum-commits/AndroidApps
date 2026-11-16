package de.kurvenflug.core.model

/** Eintrag im Planeten-Lexikon – echte Fakten, freigeschaltet über Sterne. */
data class CodexEntry(val id: String, val emoji: String, val name: String, val needStars: Int, val facts: List<String>)

object Codex {
    val all = listOf(
        CodexEntry("mond", "🌕", "Mond", 0, listOf(
            "Schwerkraft: nur 1,62 m/s² – du wiegst dort ein Sechstel.",
            "384 400 km von der Erde entfernt. Licht braucht 1,3 Sekunden.",
            "Er zeigt uns immer dieselbe Seite.")),
        CodexEntry("erde", "🌍", "Erde", 5, listOf(
            "g = 9,81 m/s². Ein Stein fällt in 1 s etwa 4,9 m.",
            "Sie rast mit rund 30 km/s um die Sonne.",
            "Einziger bekannter Planet mit flüssigem Wasser an der Oberfläche.")),
        CodexEntry("mars", "🔴", "Mars", 10, listOf(
            "g = 3,71 m/s² – du wiegst dort nur 38 %.",
            "Olympus Mons ist fast dreimal so hoch wie der Mount Everest.",
            "Ein Marstag dauert 24 h 37 min.")),
        CodexEntry("sonne", "☀️", "Sonne", 15, listOf(
            "Licht braucht 8 Minuten 20 Sekunden bis zur Erde.",
            "Sie enthält 99,86 % der Masse des Sonnensystems.",
            "Im Kern: etwa 15 Millionen °C.")),
        CodexEntry("merkur", "🌑", "Merkur", 20, listOf(
            "Ein Jahr dauert nur 88 Tage.",
            "Tagsüber 430 °C, nachts −180 °C.",
            "Kleinster Planet – kaum größer als unser Mond.")),
        CodexEntry("venus", "🟡", "Venus", 25, listOf(
            "Heißester Planet: rund 465 °C – heißer als Merkur!",
            "Ein Tag ist länger als ein Jahr.",
            "Sie dreht sich rückwärts.")),
        CodexEntry("jupiter", "🟠", "Jupiter", 30, listOf(
            "Über 300-mal so schwer wie die Erde.",
            "Der Große Rote Fleck ist ein Sturm, größer als die Erde.",
            "Raumsonden holen sich bei ihm Schwung (Swing-by).")),
        CodexEntry("saturn", "🪐", "Saturn", 40, listOf(
            "Seine Dichte ist geringer als die von Wasser – er würde schwimmen.",
            "Die Ringe sind bis zu 280 000 km breit, aber oft nur 10 m dick.",
            "Über 140 Monde sind bekannt.")),
        CodexEntry("uranus", "🧊", "Uranus", 50, listOf(
            "Er rollt quasi auf der Seite um die Sonne (98° gekippt).",
            "Ein Sommer am Pol dauert 21 Jahre.",
            "Entdeckt 1781 mit einem Teleskop – erster neu gefundener Planet.")),
        CodexEntry("neptun", "🔵", "Neptun", 60, listOf(
            "Winde bis 2 100 km/h – die schnellsten im Sonnensystem.",
            "Erst berechnet, dann gefunden: Mathe hat ihn 1846 vorhergesagt!",
            "Ein Neptunjahr dauert 165 Erdenjahre.")),
        CodexEntry("iss", "🛰️", "ISS", 75, listOf(
            "Fliegt mit 28 000 km/h – einmal um die Erde in 92 Minuten.",
            "Die Astronauten sehen 16 Sonnenaufgänge am Tag.",
            "Sie „fällt“ ständig um die Erde herum – deshalb Schwerelosigkeit.")),
        CodexEntry("voyager", "📡", "Voyager 1", 90, listOf(
            "Gestartet 1977, heute das am weitesten entfernte Menschenwerk.",
            "Sie nutzte Swing-bys an Jupiter und Saturn – wie deine Schleuder-Level!",
            "Ihre Funksignale brauchen über 22 Stunden zur Erde.")),
        CodexEntry("komet", "☄️", "Halleyscher Komet", 110, listOf(
            "Kommt etwa alle 76 Jahre wieder – das nächste Mal 2061.",
            "Seine Bahn ist eine lang gezogene Ellipse.",
            "Der Schweif zeigt immer von der Sonne weg.")),
        CodexEntry("loch", "🕳️", "Schwarzes Loch", 140, listOf(
            "So viel Masse auf so kleinem Raum, dass nicht mal Licht entkommt.",
            "Im Zentrum unserer Milchstraße sitzt eins: Sagittarius A* – 4 Millionen Sonnenmassen.",
            "2019 wurde zum ersten Mal ein Foto von einem gemacht.")),
    )
    fun unlocked(stars: Int) = all.filter { stars >= it.needStars }
    fun next(stars: Int) = all.firstOrNull { stars < it.needStars }
}

/** Werkstatt: Aussehen von Sonde, Spur und Volt – rein kosmetisch, freigeschaltet über Energiezellen. */
data class Cosmetic(val name: String, val emoji: String, val needCells: Int, val pro: Boolean = false)

object Workshop {
    val shapes = listOf(Cosmetic("Pfeil", "➤", 0), Cosmetic("Rakete", "🚀", 5), Cosmetic("UFO", "🛸", 15), Cosmetic("Komet", "☄️", 30),
        Cosmetic("Sternschnuppe", "🌠", 0, pro = true))
    val trails = listOf(Cosmetic("Neon", "〰️", 0), Cosmetic("Funken", "✨", 8), Cosmetic("Regenbogen", "🌈", 20), Cosmetic("Doppel", "⚌", 35),
        Cosmetic("Galaxie", "🌌", 0, pro = true))
    val hats = listOf(Cosmetic("Ohne", "🤖", 0), Cosmetic("Helm", "⛑️", 3), Cosmetic("Partyhut", "🎉", 12), Cosmetic("Kopfhörer", "🎧", 25), Cosmetic("Krone", "👑", 45),
        Cosmetic("Zauberhut", "🎩", 0, pro = true), Cosmetic("Saturnring", "🪐", 0, pro = true))
}
