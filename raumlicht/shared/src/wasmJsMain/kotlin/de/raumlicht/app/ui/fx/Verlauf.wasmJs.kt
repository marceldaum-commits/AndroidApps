package de.raumlicht.app.ui.fx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb

actual fun solidShader(color: Color): Shader = org.jetbrains.skia.Shader.makeColor(color.toArgb())
private fun note(text: String): Unit = js("{ if (window.__rlNote) window.__rlNote(text); }")
actual fun drawNote(text: String) = note(text)
