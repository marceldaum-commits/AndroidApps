package de.wissensgarten.core.model

/**
 * Was der Wissensgarten zeigt (Profil laut Marcel, 19.11.2026): Lernkarten-Kompendium für alle Fächer außer Sprachen, Klasse 1–13.
 * Sprachen (Englisch, Französisch, Latein, Spanisch, der Sprach-Teil von Deutsch und die beiden Plus-Sprachpakete) übernimmt Quasselquest.
 *
 * Marcels Entscheidung (19.11.): Diese Pakete werden auch in bestehenden Gärten AUSGEBLENDET. Sie werden nicht gelöscht:
 * Karten und Lernstand bleiben gespeichert (und in der verschlüsselten Sicherung), damit ein späterer Umzug zu Quasselquest
 * sie übernehmen kann. Statt der Beete zeigt der Garten einen Hinweis auf Quasselquest.
 */
object Angebot {
    /** Freie Sprachpakete: Englisch, Französisch, Latein, Spanisch und der Sprach-Teil von Deutsch (Literatur und Stilmittel bleiben) */
    val SPRACHPAKETE_FREI = setOf(
        "gs-englisch-basics", "gs-englisch-familie-koerper", "gs-englisch-farben-zahlen",
        "s1-englisch-alltag", "s1-englisch-grundwortschatz", "s1-englisch-irregular-verbs", "s1-englisch-tenses",
        "s1-franzoesisch-grundwortschatz", "s1-franzoesisch-verben", "s1-latein-grundwortschatz", "s1-spanisch-grundwortschatz",
        "gs-lesestart", "gs-rechtschreibung", "gs-satzzeichen-satzarten", "gs-wortarten",
        "s1-deutsch-grammatik", "s1-deutsch-zeichensetzung", "s1-deutsch-fremdwoerter", "aw-redewendungen",
    )

    /** Plus-Sprachpakete, die komplett zu Quasselquest gehen (Kaufschutz: Käufer bekommen dort die Sprache gratis) */
    val SPRACHPAKETE_PLUS = setOf("ob-englisch-abi-vokabeln", "aw-experte-sprache")

    val AUSGEBLENDET = SPRACHPAKETE_FREI + SPRACHPAKETE_PLUS

    /** Gehört ein Beet (über seine Paket-ID) zu einem ausgeblendeten Sprachpaket? Eigene Beete (packId = null) nie. */
    fun ausgeblendet(packId: String?) = packId != null && packId in AUSGEBLENDET

    /** Darf angezeigt, neu gepflanzt bzw. vorgeschlagen werden? */
    fun angeboten(p: Pack) = !ausgeblendet(p.id)

    /** Quasselquest auf der gemeinsamen Adresse aller Lern-Apps (Marcel, 19.11.2026); Test-Stufe: lernen-test. */
    fun quasselquestUrl(test: Boolean) = "https://lernen${if (test) "-test" else ""}.mathenachhilfe-online.com/quasselquest/"
    const val SRC = "wissensgarten-sprachen"
    fun quasselquestLink(test: Boolean) = quasselquestUrl(test) + "?src=$SRC"
}
