package de.kurvenflug.app

// Brücke zu kf.js (liegt neben index.html): Ton, Speicher, Teilen, Zurück-Taste. Alles bleibt im Browser.

internal fun jsStoreGet(key: String): String? = js("window.localStorage.getItem(key)")
internal fun jsStoreSet(key: String, value: String): Unit = js("{ try { window.localStorage.setItem(key, value) } catch (e) {} }")
internal fun jsStoreRemove(key: String): Unit = js("window.localStorage.removeItem(key)")
internal fun jsStoreKeys(): String = js("Object.keys(window.localStorage).join('\\n')")

internal fun jsShare(text: String, title: String): Unit = js("window.KF.share(text, title)")
internal fun jsOpen(url: String): Unit = js("window.open(url, '_blank', 'noopener')")
internal fun jsVibrate(ms: Int): Unit = js("{ try { if (navigator.vibrate) navigator.vibrate(ms) } catch (e) {} }")
internal fun jsPhoto(spec: String, title: String): Unit = js("window.KF.photo(spec, title)")

internal fun jsBufNew(n: Int, rate: Int): Int = js("window.KF.bufNew(n, rate)")
internal fun jsBufSet(id: Int, i: Int, v: Double): Unit = js("window.KF.bufData[id][i] = v")
internal fun jsBufDone(id: Int): Unit = js("window.KF.bufDone(id)")
internal fun jsPlay(id: Int, rate: Double, volume: Double): Unit = js("window.KF.play(id, rate, volume)")
internal fun jsMusic(id: Int, volume: Double): Unit = js("window.KF.music(id, volume)")
internal fun jsOnBack(handler: () -> Boolean): Unit = js("window.KF.onBack(handler)")
internal fun jsOnUnlock(handler: () -> Unit): Unit = js("window.KF.onUnlock(handler)")

// Vorlesen (kf.js, Abschnitt „Vorlesen“)
internal fun jsVoices(): String = js("window.KF.voices()")
internal fun jsOnVoices(f: () -> Unit): Unit = js("window.KF.onVoices(f)")
internal fun jsOnSpeaking(f: (Boolean) -> Unit): Unit = js("window.KF.onSpeaking(f)")
internal fun jsSay(text: String, name: String, rate: Double): Unit = js("window.KF.say(text, name, rate)")
internal fun jsHush(): Unit = js("window.KF.hush()")
internal fun jsPackHas(cb: (Boolean) -> Unit): Unit = js("window.KF.packHas(cb)")
internal fun jsPackLoad(progress: (Int) -> Unit, done: (String) -> Unit): Unit = js("window.KF.packLoad(progress, done)")
internal fun jsPackCancel(): Unit = js("window.KF.packCancel()")
internal fun jsPackRemove(): Unit = js("window.KF.packRemove()")
internal fun jsNatWarm(): Unit = js("window.KF.natWarm()")
internal fun jsNatSay(ids: String, rate: Double, fail: () -> Unit): Unit = js("window.KF.natSay(ids, rate, fail)")
internal fun jsText(url: String, cb: (String) -> Unit): Unit = js("window.KF.text(url, cb)")
