package de.kurvenflug.core.model

// Gilt für beide Browser-Fassungen (Kotlin/Wasm und Kotlin/JS): js() mit einfachem Ausdruck verstehen beide.
private fun jsNow(): Double = js("Date.now()")
private fun jsOffset(t: Double): Int = js("-new Date(t).getTimezoneOffset()")

actual fun nowMillis(): Long = jsNow().toLong()
actual fun utcOffsetMinutes(millis: Long): Int = jsOffset(millis.toDouble())
