package de.kurvenflug.core.model

@JsFun("() => Date.now()")
private external fun jsNow(): Double

@JsFun("(t) => -new Date(t).getTimezoneOffset()")
private external fun jsOffset(t: Double): Int

actual fun nowMillis(): Long = jsNow().toLong()
actual fun utcOffsetMinutes(millis: Long): Int = jsOffset(millis.toDouble())
