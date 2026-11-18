package de.experimentopia.core.model

import org.junit.Test

/** Inhaltsliste für die Wochenrunde (Klassenstufen × Themen) – nur mit INHALTSLISTE=<Datei> */
class InhaltslisteExport {
    @Test fun export() {
        val out = System.getenv("INHALTSLISTE") ?: return
        val sb = StringBuilder("| Fach | Thema (Kennung) | Versuche | Kl. 3–4 | Kl. 5–6 | Kl. 7–8 | Kl. 9–10 |\n|---|---|---|---|---|---|---|\n")
        Topic.entries.forEach { t ->
            val l = t.experiments()
            fun n(r: IntRange) = l.count { e -> r.any { it in e.grades } }
            sb.append("| ${t.subject.title} | ${t.title} (`${ParentReport.slug(t)}`) | ${l.size} | ${n(3..4)} | ${n(5..6)} | ${n(7..8)} | ${n(9..10)} |\n")
        }
        sb.append("| **Summe** | ${Topic.entries.size} Themen | **${Catalog.all.size}** | ${Catalog.all.count { e -> (3..4).any { it in e.grades } }} | ${Catalog.all.count { e -> (5..6).any { it in e.grades } }} | ${Catalog.all.count { e -> (7..8).any { it in e.grades } }} | ${Catalog.all.count { e -> (9..10).any { it in e.grades } }} |\n")
        java.io.File(out).writeText(sb.toString())
    }
}
