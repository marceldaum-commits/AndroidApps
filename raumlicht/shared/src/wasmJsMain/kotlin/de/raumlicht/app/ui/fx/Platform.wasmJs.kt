package de.raumlicht.app.ui.fx

private fun jsNow(): Double = js("Date.now()")
private fun jsPerf(): Double = js("performance.now()")
actual fun nowMillis(): Long = jsNow().toLong()
actual fun nowNanos(): Long = (jsPerf() * 1_000_000).toLong() + jsNow().toLong() * 1_000
