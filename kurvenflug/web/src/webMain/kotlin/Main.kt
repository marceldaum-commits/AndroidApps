import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import de.kurvenflug.app.AppState
import de.kurvenflug.app.KurvenflugRoot
import de.kurvenflug.app.WebPlatform
import de.kurvenflug.app.WebPrefs
import de.kurvenflug.app.WebProgressStore
import de.kurvenflug.core.data.Settings
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import de.kurvenflug.web.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Freischalt-Link: neu „#pro=CODE“ (landet nicht in Server-Logs), weiter auch „?pro=CODE“ für alte Karten und Mails */
private fun locationSearch(): String = js("(function () { try { return decodeURIComponent((window.location.search || '') + ' ' + (window.location.hash || '')); } catch (e) { return (window.location.search || '') + ' ' + (window.location.hash || ''); } })()")
private fun clearSearch(): Unit = js("(function () { try { history.replaceState(null, '', window.location.pathname); } catch (e) {} })()")

/** Start-Protokoll (index.html): das Programm läuft – bricht der Start danach ab, weiß der nächste Start, wo */
private fun stage(s: String): Unit = js("(function () { if (window.kfStage) window.kfStage(s) })()")

/** Kurzer Hinweis nach dem Start (Bestätigung des Einlöse-Links) */
private fun confirm(msg: String): Unit = js("(function () { setTimeout(function () { if (window.KF && window.KF.toast) window.KF.toast(msg); }, 3500); })()")

private fun hideLoader(): Unit = js("(function () { var l = document.getElementById('kf-loader'); if (l) { l.classList.add('weg'); setTimeout(function () { l.remove(); }, 600); } if (window.kfReady) window.kfReady(); })()")

/** Kurvenflug im Browser: dieselbe Oberfläche wie auf Android, Spielstand im lokalen Speicher des Browsers. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    stage("läuft")
    WebPlatform.init(BuildInfo.VERSION, BuildInfo.PRO_KEY, BuildInfo.PRO_CHECKOUT)
    val settings = Settings(WebPrefs())
    // Freischalt-Link aus der E-Mail: …/#pro=KF… (neu) oder …/?pro=KF… → Code merken, Adresse sofort aufräumen (history.replaceState)
    de.kurvenflug.core.model.License.extract(locationSearch())?.let { code ->
        settings.setProCode(code)
        clearSearch()
        // Bestätigung (B4): gültig → Freude, sonst freundlich sagen, was zu tun ist
        val ok = de.kurvenflug.core.model.License.check(code, BuildInfo.PRO_KEY, de.kurvenflug.core.model.CalDate.today().toEpochDay()) != null
        if (ok) WebPlatform.countAt("code_eingeloest", "link")
        confirm(if (ok) "🎉 Kurvenflug Pro ist freigeschaltet – viel Spaß!" else "Der Freischalt-Link hat nicht gepasst. Bitte den Code unter Einstellungen → Kurvenflug Pro eingeben.")
    }
    // Direkt-Link anderer Lern-Apps: …/#kapitel=<name> (oder Nummer) → erstes offenes Level dieses Kapitels; Unbekanntes → Startseite
    val chapterLink = de.kurvenflug.core.model.ChapterLinks.extract(locationSearch())
        ?.takeIf { de.kurvenflug.core.model.ChapterLinks.chapter(it) != null }
    if (de.kurvenflug.core.model.ChapterLinks.extract(locationSearch()) != null) clearSearch()
    val state = AppState(settings, WebProgressStore(settings.profile), MainScope())
    // Bildschirmleser-Spiegel aus: Er meldete bei Levelende gelegentlich „Node … not found“ (Compose 1.9) –
    // das grafische Flugbahn-Spiel ist mit Screenreader ohnehin kaum spielbar.
    ComposeViewport(document.body!!, configure = { isA11YEnabled = false }) {
        val fonts = LocalFontFamilyResolver.current
        var ready by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            // Emojis und Mathe-Zeichen: eigene Schriften, damit alles wie auf dem Handy aussieht
            runCatching { fonts.preload(FontFamily(Font("NotoColorEmoji", Res.readBytes("files/emoji.ttf")))) }
            runCatching { fonts.preload(FontFamily(Font("DejaVuSymbols", Res.readBytes("files/symbols.ttf")))) }
            ready = true
            hideLoader()
        }
        val linkTarget = remember { mutableStateOf(chapterLink?.let { de.kurvenflug.app.CHAPTER_LINK + it }) }
        if (ready) KurvenflugRoot(state, WebPlatform, widgetLevel = linkTarget)
    }
}
