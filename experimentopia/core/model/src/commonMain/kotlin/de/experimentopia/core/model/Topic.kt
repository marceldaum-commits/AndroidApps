package de.experimentopia.core.model

enum class Subject(val title: String, val emoji: String) { PHYSIK("Physik", "🔭"), CHEMIE("Chemie", "⚗️"), BIOLOGIE("Biologie", "🧬") }

/** Fachbereiche – zum gezielten Suchen. Die Reihenfolge ist auch die Reihenfolge auf den Inseln. */
enum class Topic(val title: String, val emoji: String, val subject: Subject, val words: String) {
    AUFTRIEB("Schwimmen, Dichte & Verdrängung", "🏊", Subject.PHYSIK, "Auftrieb Archimedes Dichte Verdrängung schwimmen sinken schweben Wasser Salzwasser"),
    HEBEL("Hebel & Drehmoment", "⚖️", Subject.PHYSIK, "Mechanik Hebelgesetz Wippe Drehmoment Gleichgewicht Kraftarm Lastarm"),
    MASCHINEN("Rollen & schiefe Ebene", "🏗️", Subject.PHYSIK, "Mechanik Flaschenzug Rolle Rampe schiefe Ebene goldene Regel Arbeit Kraft Weg"),
    NEWTON("Kraft & Bewegung (Newton)", "🛹", Subject.PHYSIK, "Newton Gesetze Mechanik Kraft Masse Beschleunigung Trägheit actio reactio Wechselwirkung Rückstoß Impuls"),
    FALL("Fallen & Schwerkraft", "🪂", Subject.PHYSIK, "Mechanik freier Fall Luftwiderstand Gravitation Schwerkraft Mond Planeten Fallbeschleunigung Galilei"),
    SCHWINGUNG("Pendel & Schwingungen", "🕰️", Subject.PHYSIK, "Mechanik Pendel Schwingung Periode Schwingungsdauer Uhr"),
    STROM("Stromkreise", "💡", Subject.PHYSIK, "Elektrizität Strom Spannung Widerstand Ohm Leiter Isolator Schalter Reihe parallel Kurzschluss"),
    MAGNET("Magnetismus", "🧲", Subject.PHYSIK, "Magnet Pole Nordpol Südpol Eisen anziehen abstoßen"),
    WAERME("Wärme & Teilchen", "🌡️", Subject.PHYSIK, "Wärmelehre Temperatur schmelzen sieden verdampfen Teilchenmodell Aggregatzustand Mischtemperatur Wärmekapazität"),
    LICHT("Licht & Farben", "🌈", Subject.PHYSIK, "Optik Licht Farben Malkasten Farben mischen Tuschkasten additiv subtraktiv Prisma Brechung Regenbogen Spektrum Filter"),
    SCHALL("Schall & Töne", "🎵", Subject.PHYSIK, "Akustik Schall Ton Frequenz Hertz Oktave Saite Luftsäule Musik"),
    TRENNEN("Stoffe trennen", "🧺", Subject.CHEMIE, "Gemisch Stoffgemisch trennen Filtern Sieben Eindampfen Magnet Scheidetrichter Chromatografie Farbstoffe Salz Sand"),
    SAEURE("Säuren & Laugen", "🧪", Subject.CHEMIE, "Säure Lauge Base pH-Wert Indikator Rotkohl neutralisieren Titration verdünnen"),
    REAKTION("Chemische Reaktionen", "💥", Subject.CHEMIE, "Reaktion Reaktionsgleichung Atome Moleküle ausgleichen Verbrennung Gas Oxidation Rost"),
    VERERBUNG("Vererbung & Genetik", "🧬", Subject.BIOLOGIE, "Genetik Vererbung Mendel Erbse Kreuzung Kreuzungsquadrat dominant rezessiv Allel Genotyp Phänotyp Blutgruppe Gene"),
    ZELLEN("Zellen & Osmose", "🔬", Subject.BIOLOGIE, "Zelle Osmose Diffusion Plasmolyse Salz Kartoffel Zwiebel Mikroskop Konzentration"),
    ERNAEHRUNG("Nährstoffe & Enzyme", "🍞", Subject.BIOLOGIE, "Ernährung Nährstoffe Stärke Zucker Glukose Fett Eiweiß Nachweis Iod Enzym Amylase Verdauung Speichel"),
    OEKOLOGIE("Pflanzen & Ökosysteme", "🌲", Subject.BIOLOGIE, "Ökologie Fotosynthese Photosynthese Pflanze Sauerstoff Licht Wasserpest Räuber Beute Nahrungskette Population Luchs Hase"),
    ;
    fun experiments() = Catalog.all.filter { it.topic == this }
}

/** Suche nach Stichwort: Titel, Geschichte, Merksatz, Thema und Fachwörter */
object Search {
    fun find(query: String, subject: Subject? = null, topic: Topic? = null): List<Experiment> {
        val words = query.lowercase().split(' ', ',').filter { it.length >= 2 }
        return Catalog.all.filter { e ->
            (subject == null || e.topic.subject == subject) && (topic == null || e.topic == topic) &&
                words.all { w -> listOf(e.title, e.story, e.fact, e.topic.title, e.topic.words, e.predict.question, e.island.title).any { it.lowercase().contains(w) } }
        }
    }
}
