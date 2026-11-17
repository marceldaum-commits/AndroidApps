package de.kurvenflug.core.model

/**
 * Die gemeinsame Stimme „Thorsten“ (Piper, de_DE-thorsten-medium) auf unserem eigenen Server – für alle Apps gleich (Vorgabe Leitstand).
 * Android und WebApp laden dieselbe Datei, prüfen dieselbe Prüfsumme und rechnen mit ONNX Runtime.
 */
object VoicePack {
    const val URL = "https://stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx"
    const val SIZE = 63_201_294L
    const val SHA256 = "7e64762d8e5118bb578f2eea6207e1a35a8e0c30595010b666f983fc87bb7819"
    const val HOST = "stimmen.mathenachhilfe-online.com"
    /** Hinweis vor dem Laden (Stimme + Rechenkern im Browser, Vorgabe Leitstand) */
    const val MB_HINT = 75
    const val SAMPLE_RATE = 22050
    /** aus de_DE-thorsten-medium.onnx.json („inference“) */
    const val NOISE = 0.667f
    const val NOISE_W = 0.8f
}
