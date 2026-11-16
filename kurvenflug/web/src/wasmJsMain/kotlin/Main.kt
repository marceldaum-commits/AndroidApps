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

private fun locationSearch(): String = js("decodeURIComponent(window.location.search || '')")
private fun clearSearch(): Unit = js("{ try { history.replaceState(null, '', window.location.pathname); } catch (e) {} }")

private fun hideLoader(): Unit = js("{ var l = document.getElementById('kf-loader'); if (l) { l.classList.add('weg'); setTimeout(function () { l.remove(); }, 400); } if (window.kfReady) window.kfReady(); }")

/** Kurvenflug im Browser: dieselbe Oberfläche wie auf Android, Spielstand im lokalen Speicher des Browsers. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    WebPlatform.init(BuildInfo.VERSION, BuildInfo.PRO_KEY, BuildInfo.PRO_CHECKOUT)
    val settings = Settings(WebPrefs())
    // Freischalt-Link aus der E-Mail: …/?pro=KF… → Code merken, Adresse wieder aufräumen
    de.kurvenflug.core.model.License.extract(locationSearch())?.let { code ->
        settings.setProCode(code)
        clearSearch()
    }
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
        if (ready) KurvenflugRoot(state, WebPlatform)
    }
}
