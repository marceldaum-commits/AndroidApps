package de.wissensgarten.app.ui.fx

/**
 * Nur für Messungen (Ursachensuche bei Ruckeln): `?aus=himmel,brummi,pflanzen,freunde,wind,deko,karten` schaltet Teile des Gartens ab,
 * damit man sieht, welcher Teil die Zeit kostet. Ohne den Parameter ändert sich nichts.
 */
object Experiment {
    var aus: Set<String> = emptySet()
    fun aus(name: String) = name in aus
}
