package de.experimentopia.core.model

import java.io.File
import java.text.Normalizer

/** Beim Bauen der WebApp: schreibt /vorlesen/texte.json (Liste aller festen Vorlese-Texte für den Stimmen-Server). */
fun main(args: Array<String>) {
    val bad = Nova.catalog().filterNot { Normalizer.isNormalized(it, Normalizer.Form.NFC) }
    require(bad.isEmpty()) { "Texte nicht in Unicode-NFC (Hash würde nicht passen): ${bad.take(3)}" }
    File(args[0]).apply { parentFile.mkdirs() }.writeText(Nova.json())
}
