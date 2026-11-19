package de.wissensgarten.core.model

/**
 * Abgestimmte Querverweise zu unseren anderen Lern-Apps (Wochenrunde 18.11.2026, siehe WOCHENRUNDE.md).
 * Sie stehen im Theorie-Blatt unter Brummis Erklärung: Merken im Wissensgarten, Üben, Tempo, Erleben und Ausprobieren in der passenden App.
 * Jeder Link trägt `src=wissensgarten-theorie`, damit die Ziel-App anonym zählen kann, woher Besuche kommen. Weitere Daten gehen nicht mit.
 */
object Querverweise {
    const val SRC = "wissensgarten-theorie"

    /** [mitTor]: Ziel-App zeigt in der Oberstufe Preise ohne Eltern-Tor (Mathe-Trainer, Raumlicht) → Link nur über das Tor (Recht V10b) */
    data class Link(val app: String, val emoji: String, val text: String, val url: String, val mitTor: Boolean = false)

    private fun host(name: String, test: Boolean) = "https://$name${if (test) "-test" else ""}.mathenachhilfe-online.com"

    /** Mathe-Trainer: Üben mit vollständigen Rechenwegen (Einheit `#/u/<id>`, Gebiet `#/g/<id>`) */
    private val matheTrainer = mapOf(
        "s1-mathe-algebra" to "u/k7-terme",
        "s1-mathe-brueche-prozent" to "u/k6-bruch",
        "s1-mathe-geometrie-formeln" to "u/k8-flaechen",
        "s1-mathe-gleichungen" to "u/k7-gleichungen",
        "s1-mathe-kopfrechnen" to "u/k5-rechnen",
        "s1-mathe-negative-zahlen" to "u/k5-ganz",
        "s1-mathe-pythagoras-trigo" to "u/k9-pythagoras",
        "s1-mathe-wahrscheinlichkeit" to "u/k6-zufall",
        "ob-mathe-analysis" to "g/analysis",
    )

    /** KurvenFlug: Funktionen und Bewegungen erleben (`#kapitel=<name>`, nur Kapitel bis Klasse 10) */
    private val kurvenFlug = mapOf(
        "s1-mathe-algebra" to "geraden",
        "s1-mathe-gleichungen" to "terme",
        "s1-physik-einheiten-formeln" to "weg-zeit",
    )

    /** Rechenblitz: Tempo trainieren (`?thema=&stufe=&modus=`), Kennungen vom Rechenblitz bestätigt (19.11.: pow = Quadratzahlen, Stufe 3 bis 20²) */
    private val rechenblitz = mapOf(
        "gs-einmaleins" to "thema=mul&stufe=2&modus=training",
        "gs-plus-minus-20" to "thema=add&stufe=2&modus=training",
        "gs-plus-minus-100" to "thema=add&stufe=3&modus=training",
        "gs-zahlenraum-1000" to "thema=add&stufe=5&modus=training",
        "gs-geld" to "thema=geld&modus=training",
        "gs-uhrzeit" to "thema=zeit&modus=training",
        "gs-mathe-groessen" to "thema=unit&modus=training",
        "s1-mathe-kopfrechnen" to "thema=pow&stufe=3&modus=training",
    )

    /** Experimentopia: Ausprobieren im Versuch (`#thema=<name>`) */
    private val experimentopia = mapOf(
        "s1-physik-strom" to "strom",
        "s1-physik-einheiten-formeln" to "kraft",
        "gs-wetter-jahreszeiten" to "waerme",
        "s1-chemie-saeuren-basen" to "saeure",
        "s1-chemie-grundbegriffe" to "trennen",
        "s1-bio-pflanzen" to "oekologie",
        "s1-bio-oekologie" to "oekologie",
        "gs-pflanzen-wald" to "oekologie",
        "s1-bio-zelle-mensch" to "zellen",
        "s1-bio-organe" to "koerper",
        "gs-koerper-natur" to "koerper",
        "gs-gesund-ernaehrung" to "ernaehrung",
    )

    /** Raumlicht: Zufall und Raum erleben (`?ziel=…`) */
    private val raumlicht = mapOf(
        "s1-mathe-wahrscheinlichkeit" to "galton",
    )

    /** Alle Querverweise eines Pakets in fester Reihenfolge; [test] = Test-Stufe der Ziel-Apps */
    fun fuer(packId: String, test: Boolean = false): List<Link> = buildList {
        matheTrainer[packId]?.let { add(Link("Mathe-Trainer", "🧮", "Üben mit Rechenwegen im Mathe-Trainer", "${host("trainer", test)}/?src=$SRC#/$it")) }
        rechenblitz[packId]?.let { q -> add(Link("Rechenblitz", "⚡", "Tempo trainieren im Rechenblitz",
            "${host("rechenblitz", test)}/?" + (if (q.isEmpty()) "" else "$q&") + "src=$SRC")) }
        kurvenFlug[packId]?.let { add(Link("KurvenFlug", "🚀", "Erleben in KurvenFlug", "${host("kurvenflug", test)}/?src=$SRC#kapitel=$it")) }
        experimentopia[packId]?.let { add(Link("Experimentopia", "🔬", "Ausprobieren in Experimentopia", "${host("experimentopia", test)}/?src=$SRC#thema=$it")) }
        raumlicht[packId]?.let { add(Link("Raumlicht", "🔦", "Erleben in Raumlicht", "${host("raumlicht", test)}/?ziel=$it&src=$SRC")) }
    }

    /** Alle Paket-IDs mit mindestens einem Querverweis (für Tests) */
    val pakete: Set<String> get() = matheTrainer.keys + kurvenFlug.keys + rechenblitz.keys + experimentopia.keys + raumlicht.keys

    /** Eigener Deeplink, mit dem andere Apps auf ein Paket verweisen */
    fun paketLink(packId: String, test: Boolean = false) =
        "https://lernen${if (test) "-test" else ""}.mathenachhilfe-online.com/wissensgarten/#paket=$packId"
}
