package de.kurvenflug.core.data

import android.content.Context
import android.content.SharedPreferences

/** [Prefs] über die SharedPreferences „kurvenflug“ – dieselbe Datei wie seit Version 0.1, nichts geht verloren. */
class AndroidPrefs(ctx: Context) : Prefs {
    private val p: SharedPreferences = ctx.getSharedPreferences("kurvenflug", Context.MODE_PRIVATE)
    override fun getBoolean(key: String, default: Boolean) = p.getBoolean(key, default)
    override fun getInt(key: String, default: Int) = p.getInt(key, default)
    override fun getLong(key: String, default: Long) = p.getLong(key, default)
    override fun getString(key: String, default: String?): String? = p.getString(key, default)
    override fun getStringSet(key: String, default: Set<String>): Set<String> = p.getStringSet(key, default).orEmpty().toSet()
    override fun keys(): Set<String> = p.all.keys
    override fun edit(): Prefs.Editor = object : Prefs.Editor {
        val e = p.edit()
        override fun putBoolean(key: String, v: Boolean) = apply { e.putBoolean(key, v) }
        override fun putInt(key: String, v: Int) = apply { e.putInt(key, v) }
        override fun putLong(key: String, v: Long) = apply { e.putLong(key, v) }
        override fun putString(key: String, v: String?) = apply { e.putString(key, v) }
        override fun putStringSet(key: String, v: Set<String>) = apply { e.putStringSet(key, v) }
        override fun remove(key: String) = apply { e.remove(key) }
        override fun apply() = e.apply()
    }
}

/** Einstellungen auf Android */
fun Settings(ctx: Context) = Settings(AndroidPrefs(ctx))

/** Alles, was die App braucht, an einer Stelle. */
class AppContainer(ctx: Context) {
    val db = FlightDatabase.create(ctx)
    val settings = Settings(ctx)
    val progress = ProgressRepository(db.progress(), settings.profile)
}
