package de.kurvenflug.app

import androidx.activity.ComponentActivity

/** GitHub-APK: Pro über die Website – Kasse im Browser, Code per E-Mail bzw. Link. */
object ProShops {
    fun create(activity: ComponentActivity): ProShop = CodeShop(BuildConfig.PRO_KEY, BuildConfig.PRO_CHECKOUT) { url ->
        runCatching { activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
    }
}
