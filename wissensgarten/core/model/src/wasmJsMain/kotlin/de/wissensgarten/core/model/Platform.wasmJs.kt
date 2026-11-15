@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.wissensgarten.core.model

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

@JsModule("pako")
private external object Pako : JsAny {
    fun deflateRaw(data: Uint8Array, options: JsAny?): Uint8Array
    fun inflateRaw(data: Uint8Array): Uint8Array
    fun gzip(data: Uint8Array): Uint8Array
    fun ungzip(data: Uint8Array): Uint8Array
}

private fun jsNow(): Double = js("Date.now()")
private fun jsOffsetMin(ms: Double): Int = js("new Date(ms).getTimezoneOffset()")
private fun level9(): JsAny = js("({ level: 9 })")

private fun ByteArray.toU8(): Uint8Array { val a = Uint8Array(size); for (i in indices) a[i] = this[i]; return a }
private fun Uint8Array.toBytes(): ByteArray { val i8 = Int8Array(buffer, byteOffset, length); return ByteArray(length) { i8[it] } }

actual object Platform {
    actual fun nowMs(): Long = jsNow().toLong()
    actual fun tzOffsetMs(ms: Long): Int = -jsOffsetMin(ms.toDouble()) * 60_000
    actual fun deflateRaw(bytes: ByteArray): ByteArray = Pako.deflateRaw(bytes.toU8(), level9()).toBytes()
    actual fun inflateRaw(bytes: ByteArray): ByteArray = Pako.inflateRaw(bytes.toU8()).toBytes()
    actual fun gzip(bytes: ByteArray): ByteArray = Pako.gzip(bytes.toU8()).toBytes()
    actual fun gunzip(bytes: ByteArray): ByteArray = Pako.ungzip(bytes.toU8()).toBytes()
}
