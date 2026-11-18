package de.kurvenflug.core.model

actual fun zipDeflate(data: ByteArray): ByteArray = PureZip.deflate(data)
actual fun zipInflate(data: ByteArray, limit: Int): ByteArray = PureZip.inflate(data, limit)
