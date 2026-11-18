package de.wissensgarten.app.ui.fx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb

actual fun solidShader(color: Color): Shader = org.jetbrains.skia.Shader.makeColor(color.toArgb())

private fun consoleWarn(text: String): Unit = js("console.warn('[Wissensgarten] ' + text)")
actual fun drawNote(text: String) = consoleWarn(text)
