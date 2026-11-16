package de.raumlicht.app.ui.fx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb

actual fun solidShader(color: Color): Shader = android.graphics.LinearGradient(0f, 0f, 0f, 1f, color.toArgb(), color.toArgb(), android.graphics.Shader.TileMode.CLAMP)
actual fun drawNote(text: String) {}
