package de.wissensgarten.app.data

/** Fertige Beete, damit der Garten beim ersten Start nicht leer ist. */
object StarterBeds {
    private fun bed(id: String, name: String, emoji: String, hue: Float, pairs: List<Pair<String, String>>) =
        Bed(id, name, emoji, hue, pairs.mapIndexed { i, (f, b) -> Card("$id-$i", f, b) })

    fun all(): List<Bed> = listOf(
        bed(
            "hauptstaedte", "Hauptstädte Europas", "🏰", 330f, listOf(
                "Frankreich" to "Paris", "Spanien" to "Madrid", "Italien" to "Rom",
                "Polen" to "Warschau", "Schweden" to "Stockholm", "Norwegen" to "Oslo",
                "Portugal" to "Lissabon", "Ungarn" to "Budapest", "Tschechien" to "Prag",
                "Österreich" to "Wien", "Schweiz" to "Bern", "Irland" to "Dublin",
                "Finnland" to "Helsinki", "Griechenland" to "Athen", "Kroatien" to "Zagreb",
            )
        ),
        bed(
            "englisch", "Englisch: Alltag", "💬", 200f, listOf(
                "die Erfahrung" to "experience", "zuverlässig" to "reliable", "vermeiden" to "to avoid",
                "die Umwelt" to "environment", "sich beschweren" to "to complain", "ausreichend" to "sufficient",
                "die Gelegenheit" to "opportunity", "überzeugen" to "to convince", "der Nachteil" to "disadvantage",
                "ehrlich" to "honest", "die Bedingung" to "condition", "vorschlagen" to "to suggest",
            )
        ),
        bed(
            "chemie", "Chemie: Elemente", "⚗️", 45f, listOf(
                "H" to "Wasserstoff", "He" to "Helium", "C" to "Kohlenstoff", "N" to "Stickstoff",
                "O" to "Sauerstoff", "Na" to "Natrium", "Mg" to "Magnesium", "Cl" to "Chlor",
                "K" to "Kalium", "Ca" to "Calcium", "Fe" to "Eisen", "Cu" to "Kupfer",
                "Ag" to "Silber", "Au" to "Gold", "Hg" to "Quecksilber",
            )
        ),
    )
}
