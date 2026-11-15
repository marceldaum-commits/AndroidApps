@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.wissensgarten.app.ui.klasse

@JsModule("qrcode-generator")
private external fun qrcode(typeNumber: Int, level: String): QrJs

private external interface QrJs : JsAny {
    fun addData(data: String)
    fun make()
    fun getModuleCount(): Int
    fun isDark(row: Int, col: Int): Boolean
}

actual fun qrModules(text: String): Array<BooleanArray>? = runCatching {
    val q = qrcode(0, "L"); q.addData(text); q.make()
    val n = q.getModuleCount()
    // 1 Modul Rand wie bei zxing
    Array(n + 2) { y -> BooleanArray(n + 2) { x -> x in 1..n && y in 1..n && q.isDark(y - 1, x - 1) } }
}.getOrNull()
