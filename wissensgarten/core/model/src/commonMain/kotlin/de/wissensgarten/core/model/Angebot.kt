package de.wissensgarten.core.model

/**
 * Was der Wissensgarten neu zum Pflanzen anbietet (Profil laut Marcel, 19.11.2026): Lernkarten-Kompendium für alle Fächer
 * außer Sprachen, Klasse 1–13. Sprachen (Englisch, Französisch, Latein, Spanisch und der Sprach-Teil von Deutsch)
 * übernimmt Quasselquest.
 *
 * Wichtig: Bereits gepflanzte Sprach-Beete bleiben mit allen Karten und dem ganzen Lernstand erhalten und werden weiter
 * aktualisiert – sie lassen sich nur nicht mehr neu pflanzen. Plus-Pakete bleiben unberührt (Entscheidung bei Marcel).
 */
object Angebot {
    /** Fächer, die an Quasselquest gehen */
    val SPRACH_FAECHER = setOf("englisch", "franzoesisch", "latein", "spanisch")

    /** Deutsch: nur der Sprach-Teil (Rechtschreibung, Grammatik, Wortschatz) geht an Quasselquest; Literatur und Stilmittel bleiben */
    val DEUTSCH_SPRACHE = setOf(
        "gs-lesestart", "gs-rechtschreibung", "gs-satzzeichen-satzarten", "gs-wortarten",
        "s1-deutsch-grammatik", "s1-deutsch-zeichensetzung", "s1-deutsch-fremdwoerter", "aw-redewendungen",
    )

    fun istSprachpaket(p: Pack) = p.subject in SPRACH_FAECHER || p.id in DEUTSCH_SPRACHE

    /** Darf neu gepflanzt bzw. vorgeschlagen werden? */
    fun angeboten(p: Pack) = p.isPlus || !istSprachpaket(p)
}
