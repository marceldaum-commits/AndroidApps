package de.kurvenflug.core.model

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Keine Kaufaufforderung an Kinder (UWG Anhang Nr. 28, Korrektur AppZentrale 19.11.):
 * In der Oberfläche darf nirgends „Frag deine Eltern“, „Kauf dir …“ o. Ä. stehen; Pro-Schlösser zeigen nur den sachlichen Hinweis.
 */
class KidsNoPurchaseAppealTest {
    private val sources = File("../../shared/src/commonMain/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()

    @Test fun noPurchaseAppealToChildren() {
        val bad = Regex("(?i)frag\\s+deine\\s+eltern|kauf\\s+dir|jetzt\\s+kaufen|bitte\\s+deine\\s+eltern|sag\\s+deinen\\s+eltern")
        val hits = sources.flatMap { f -> f.readLines().mapIndexedNotNull { i, l -> if (bad.containsMatchIn(l)) "${f.name}:${i + 1}" else null } }
        assertTrue(hits.isEmpty(), "Kaufaufforderung an Kinder: $hits")
    }

    @Test fun lockedLevelShowsNoticeNotShop() {
        val root = sources.first { it.name == "Root.kt" }.readText()
        assertTrue("ProLockedNotice(" in root)
        assertTrue("locked = true" !in root, "gesperrtes Level darf nicht die Pro-Seite mit Preis zeigen")
        val pro = sources.first { it.name == "ProScreen.kt" }.readText()
        assertTrue("\"Das gehört zu KurvenFlug Pro. Deine Eltern können im Elternbereich nachsehen.\"" in pro)
    }
}
