package de.experimentopia.core.model

/**
 * Direkt-Links in Experimentopia (…/#thema=<name>) und Querverweise zu den anderen Lern-Apps.
 * Abgestimmt in der Wochenrunde (18./19.11.2026, experimentopia/WOCHENRUNDE.md): feste Namen je Thema, unbekannte Namen öffnen die Startseite;
 * höchstens EIN Querverweis je Thema, nur wo ein Kind den Zusammenhang sofort sieht, immer mit erklärendem Satz, nie zu Kaufseiten.
 */
object Links {
    /** Fester Link-Name je Thema – bleibt stabil, auch wenn Titel oder Reihenfolge sich ändern */
    fun name(t: Topic) = when (t) {
        Topic.AUFTRIEB -> "schwimmen"; Topic.HEBEL -> "hebel"; Topic.MASCHINEN -> "rollen"; Topic.NEWTON -> "kraft"
        Topic.FALL -> "fallen"; Topic.SCHWINGUNG -> "pendel"; Topic.STROM -> "strom"; Topic.MAGNET -> "magnet"
        Topic.WAERME -> "waerme"; Topic.LICHT -> "licht"; Topic.OPTIK -> "optik"; Topic.SCHALL -> "schall"
        Topic.TRENNEN -> "trennen"; Topic.SAEURE -> "saeure"; Topic.REAKTION -> "reaktion"
        Topic.VERERBUNG -> "vererbung"; Topic.ZELLEN -> "zellen"; Topic.ERNAEHRUNG -> "ernaehrung"; Topic.OEKOLOGIE -> "oekologie"; Topic.KOERPER -> "koerper"
    }
    /** Angekündigte Namen für Themen, die noch gebaut werden – führen bis dahin auf das Fach */
    val PLANNED = mapOf("energie" to Subject.PHYSIK, "elektromagnetismus" to Subject.PHYSIK)

    /** Ziel eines Direkt-Links: Fach und (wenn bekannt) Thema */
    data class Target(val subject: Subject, val topic: Topic?)

    /** Liest „thema=<name>“ aus Adresse (Hash oder Suchteil); feine Namen, grobe Kennungen (physik-optik …) und angekündigte Namen; sonst null */
    fun target(address: String): Target? {
        val v = Regex("thema=([a-z0-9-]+)", RegexOption.IGNORE_CASE).find(address)?.groupValues?.get(1)?.lowercase() ?: return null
        Topic.entries.firstOrNull { name(it) == v }?.let { return Target(it.subject, it) }
        PLANNED[v]?.let { return Target(it, null) }
        // grobe Kennung: erstes Thema dieser Gruppe in Schulreihenfolge
        Subject.entries.flatMap { Curriculum.topics(it) }.firstOrNull { ParentReport.slug(it) == v }?.let { return Target(it.subject, it) }
        return null
    }

    fun url(t: Topic, test: Boolean) = (if (test) "https://experimentopia-test.mathenachhilfe-online.com" else "https://experimentopia.mathenachhilfe-online.com") + "/#thema=" + name(t)

    /** Querverweis zu einer anderen Lern-App */
    data class Cross(val app: String, val emoji: String, val sentence: String, private val liveUrl: String, private val testUrl: String) {
        fun url(test: Boolean) = if (test) testUrl else liveUrl
        /** „Weiter in KurvenFlug“, „Weiter im Mathe-Trainer“, „Weiter im Wissensgarten“ */
        val heading get() = "Weiter " + (if (app == "KurvenFlug") "in " else "im ") + app
    }
    private fun kurvenflug(kapitel: String, s: String) = Cross("KurvenFlug", "🚀", s, "https://kurvenflug.mathenachhilfe-online.com/#kapitel=$kapitel", "https://kurvenflug-test.mathenachhilfe-online.com/#kapitel=$kapitel")
    private fun matheTrainer(unit: String, s: String) = Cross("Mathe-Trainer", "🧮", s, "https://trainer.mathenachhilfe-online.com/#/u/$unit", "https://trainer-test.mathenachhilfe-online.com/#/u/$unit")
    private fun wissensgarten(paket: String, s: String) = Cross("Wissensgarten", "🌼", s, "https://wissensgarten.mathenachhilfe-online.com/#paket=$paket", "https://wissensgarten-test.mathenachhilfe-online.com/#paket=$paket")

    /** Höchstens ein Querverweis je Thema – nur, wo der Zusammenhang sofort sichtbar ist */
    fun cross(t: Topic): Cross? = when (t) {
        Topic.OPTIK -> matheTrainer("k9-aehnlich", "Wie groß ein Schatten wird, sagt der Strahlensatz. Im Mathe-Trainer rechnest du solche Aufgaben Schritt für Schritt nach.")
        Topic.LICHT -> kurvenflug("lichtbrechung", "Wie stark Licht an Glas oder Wasser abknickt, rechnest du in KurvenFlug im Kapitel „Lichtbrechung“ aus.")
        Topic.NEWTON -> kurvenflug("beschleunigung", "Wie schnell ein Wagen wird und wie weit er kommt, rechnest du in KurvenFlug im Kapitel „Beschleunigte Bewegung“ nach.")
        Topic.FALL -> kurvenflug("wurf", "Fallzeit und Flugbahn rechnest du in KurvenFlug im Kapitel „Wurf & Schwerkraft“ aus.")
        Topic.STROM -> matheTrainer("k7-zuordnung", "Beim Ohmschen Gesetz ist die Stromstärke proportional zur Spannung. Proportionale Zuordnungen übst du im Mathe-Trainer.")
        Topic.MAGNET -> wissensgarten("s1-physik-strom", "Die wichtigsten Begriffe zu Strom und Magnetismus wiederholst du mit den Lernkarten im Wissensgarten.")
        Topic.SAEURE -> wissensgarten("s1-chemie-saeuren-basen", "Säure, Lauge, Indikator, pH: Die Begriffe wiederholst du mit den Lernkarten im Wissensgarten.")
        Topic.OEKOLOGIE -> wissensgarten("s1-bio-pflanzen", "Pflanzenteile und Fachwörter zur Fotosynthese wiederholst du mit den Lernkarten im Wissensgarten.")
        Topic.VERERBUNG -> matheTrainer("k8-zufall", "Im Kreuzungsquadrat rechnest du mit Wahrscheinlichkeiten. Wie das allgemein geht, übst du im Mathe-Trainer.")
        else -> null
    }
}
