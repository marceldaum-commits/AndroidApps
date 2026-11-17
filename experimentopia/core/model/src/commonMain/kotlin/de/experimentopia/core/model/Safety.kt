package de.experimentopia.core.model

/**
 * Sicherheitshinweise für Versuche, die Kinder zu Hause nachmachen könnten.
 * Werden im Versuch (beim Ausprobieren und bei der Auflösung) und im Forscherbuch angezeigt.
 * Grundsatz der App: In Experimentopia ist alles sicher – in echt gelten diese Regeln.
 */
object Safety {
    private const val STECKDOSE = "Nur mit einer kleinen Batterie (bis 9 V) ausprobieren – niemals mit Strom aus der Steckdose!"
    private const val SAEURE = "Salzsäure und Natronlauge sind ätzend: nur im Chemieraum mit Schutzbrille und Handschuhen, nie zu Hause."
    private const val HERD = "Herdplatten, heißes Wasser und Dampf verbrennen die Haut – zu Hause nur zusammen mit Erwachsenen."
    private const val TEICH = "Teichwasser nur zusammen mit Erwachsenen holen – nicht allein ans Ufer. Nicht trinken und danach Hände waschen."
    private const val SPEICHEL = "Nur den eigenen Speichel und saubere Gefäße nehmen, nichts davon in den Mund nehmen. Iod-Lösung nicht trinken."
    private const val NUR_PROFIS = "Nur Fachleute dürfen das machen – niemals selbst ausprobieren!"

    private val notes: Map<String, String> = buildMap {
        // Strom
        listOf("s1", "s2", "s3", "s4", "s5", "s6", "s12", "s13", "s14", "s15", "s16", "s17", "s18").forEach { put(it, STECKDOSE) }
        put("s7", "Nicht nachmachen: Bei einem Kurzschluss werden Kabel und Batterie heiß, Akkus können sogar brennen. Und niemals an der Steckdose!")
        // Wasser
        put("w7", "Spiritus brennt sehr leicht und ist giftig – nur mit Erwachsenen, weit weg von Flammen, nicht trinken.")
        put("w8", "Spiritus brennt sehr leicht und ist giftig – nur mit Erwachsenen, weit weg von Flammen, nicht trinken. Honig, Öl und Spülmittel danach nicht mehr essen.")
        put("w10", "Quecksilber ist sehr giftig – seine Dämpfe schaden schon in kleinen Mengen. Darum gibt es diesen Versuch nur hier in der App.")
        // Chemie
        put("c1", "Zum Rotkohlkochen braucht man den Herd – nur zusammen mit Erwachsenen.")
        put("c2", "Waschsoda und Rohrreiniger sind stark basisch: Schutzbrille und Handschuhe, und nur mit Erwachsenen.")
        put("c15", "Waschsoda ist stark basisch: Schutzbrille und Handschuhe, und nur mit Erwachsenen.")
        listOf("c3", "c4", "c16", "c17").forEach { put(it, SAEURE) }
        put("c5", "$SAEURE Beim Verdünnen gilt: Erst das Wasser, dann die Säure!")
        put("c18", "$SAEURE Beim Verdünnen gilt: Erst das Wasser, dann die Säure!")
        put("c24", "$SAEURE Dabei entsteht Wasserstoff – der ist brennbar.")
        put("c6", "Essig und Natron sind harmlos – aber nie die Flasche fest verschließen, sonst kann sie platzen.")
        put("c26", "Essig und Natron sind harmlos – aber nie die Flasche fest verschließen, sonst kann sie platzen.")
        put("c7", "Nur die Lehrkraft macht diesen Versuch – Knallgas kann Gefäße zerreißen. $NUR_PROFIS")
        put("c8", "Brennendes Magnesium ist so hell, dass es den Augen schadet: nicht hineinschauen, nie mit Wasser löschen – nur im Chemieraum.")
        put("c9", "Gas und Flammen nur zusammen mit Erwachsenen. Ohne genug Luft entsteht beim Verbrennen giftiges Kohlenmonoxid.")
        put("c13", "Gas und Flammen nur zusammen mit Erwachsenen. Ohne genug Luft entsteht beim Verbrennen giftiges Kohlenmonoxid.")
        put("c21", "Feuerzeuge sind kein Spielzeug – Feuer nur zusammen mit Erwachsenen.")
        put("c11", "Nur mit Erwachsenen, Schutzbrille und Handschuhen – Wasserstoffperoxid reizt Haut und Augen, der Schaum wird warm. Nie hochprozentiges Wasserstoffperoxid benutzen.")
        put("c19", "Nie Strom aus der Steckdose nehmen! Kein Kochsalz ins Wasser – sonst entsteht giftiges Chlor. Die Gase nicht anzünden (Knallgas).")
        put("c20", "Achtung: Die Spiritusflamme ist fast unsichtbar! Nie Spiritus in eine brennende oder heiße Flamme nachgießen – Stichflamme. Nur mit Erwachsenen.")
        put("c22", "Thermit wird über 2000 °C heiß und spritzt flüssiges Eisen. $NUR_PROFIS")
        put("c25", "Kupferblech nur mit der Tiegelzange in die Flamme halten – es wird sehr heiß. Nur im Chemieraum.")
        put("t2", HERD)
        put("t3", "Eisen und Schwefel nicht erhitzen – dabei entstehen giftige, stechend riechende Gase. Nur im Chemieraum.")
        put("t4", "Öl gehört nicht in den Ausguss – im Restmüll entsorgen.")
        // Wärme
        listOf("h2", "h3", "h9", "h13", "h14").forEach { put(it, HERD) }
        put("h1", "$HERD Heißes Wachs kann brennen.")
        put("h5", "Vorsicht, Wasser mit 60 °C verbrüht die Haut in wenigen Sekunden – immer erst kalt, dann warm einlassen.")
        put("h6", "Heißes Metall verbrennt die Haut, und sehr heißes Metall lässt Wasser explosionsartig verdampfen und spritzen. Nicht zu Hause nachmachen!")
        put("h7", "Geschmolzener Zucker ist fast 190 °C heiß und klebt auf der Haut – nur zusammen mit Erwachsenen.")
        put("h8", "Achtung: Karamell ist fast 190 °C heiß und klebt auf der Haut – nur zusammen mit Erwachsenen kochen!")
        put("h10", "Heißes Metall verbrennt die Haut – nur zusammen mit Erwachsenen.")
        put("h12", "Heißes Metall verbrennt die Haut – nur zusammen mit Erwachsenen.")
        // Licht und Fall
        listOf("l5", "l15").forEach { put(it, "Mit Sonnenlicht nachmachen? Nie direkt in die Sonne schauen – auch nicht durch das Prisma!") }
        listOf("f17").forEach { put(it, "Nie etwas von Balkonen, Brücken oder Türmen werfen – es kann Menschen schwer verletzen.") }
        // Biologie
        put("e1", "Nur mit Erwachsenen! Iod-Lösung nicht trinken, sie färbt Haut und Kleidung. Getestete Lebensmittel nicht mehr essen.")
        put("e2", "Getestete Lebensmittel danach nicht mehr essen.")
        put("e3", "Getestete Lebensmittel danach nicht mehr essen.")
        put("e4", "Die Biuret-Probe enthält ätzende Natronlauge und Kupfersulfat – nur im Schullabor mit Schutzbrille und Handschuhen.")
        listOf("e5", "e6", "e7", "e8").forEach { put(it, SPEICHEL) }
        listOf("u1", "u2", "u3", "u4", "u5").forEach { put(it, TEICH) }
        put("v2", "Am Hals nur sanft und nur auf einer Seite fühlen. Nicht mit dem Daumen fühlen – der hat einen eigenen Puls.")
        put("v1", "Wenn dir beim Rennen schwindlig wird oder die Brust wehtut: sofort aufhören und einem Erwachsenen Bescheid sagen.")
    }

    fun note(id: String): String? = notes[id]
    /** Alle Versuche mit Hinweis (für den Test) */
    val ids: Set<String> get() = notes.keys
}
