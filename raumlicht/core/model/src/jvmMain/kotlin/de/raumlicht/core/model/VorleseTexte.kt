package de.raumlicht.core.model

import java.io.File

/** Schreibt die Liste der festen Vorlese-Texte (Aufruf beim Bauen der WebApp: `./gradlew :core:model:vorleseTexte`). */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "build/vorlesen/texte.json")
    out.parentFile.mkdirs(); out.writeText(Vorlesen.json())
    println("${Vorlesen.texts().size} feste Vorlese-Texte → $out")
}
