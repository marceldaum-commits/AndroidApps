package de.raumlicht.app.ui.fx

actual fun nowMillis(): Long = android.os.SystemClock.uptimeMillis()
actual fun nowNanos(): Long = System.nanoTime()
