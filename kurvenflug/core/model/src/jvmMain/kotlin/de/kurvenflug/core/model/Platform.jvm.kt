package de.kurvenflug.core.model

actual fun nowMillis(): Long = System.currentTimeMillis()
actual fun utcOffsetMinutes(millis: Long): Int = java.util.TimeZone.getDefault().getOffset(millis) / 60_000
