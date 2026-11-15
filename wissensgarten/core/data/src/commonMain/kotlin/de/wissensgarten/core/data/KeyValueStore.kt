package de.wissensgarten.core.data

/**
 * Kleiner Schlüssel-Wert-Speicher mit derselben Bedienung wie Androids SharedPreferences –
 * auf Android genau das, im Web der localStorage des Browsers.
 */
interface KeyValueStore {
    fun getString(key: String, def: String?): String?
    fun getBoolean(key: String, def: Boolean): Boolean
    fun getInt(key: String, def: Int): Int
    fun getLong(key: String, def: Long): Long
    fun getStringSet(key: String, def: Set<String>?): Set<String>?
    fun contains(key: String): Boolean
    fun keys(): Set<String>
    fun edit(): Editor

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putStringSet(key: String, value: Set<String>?): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun apply()
        fun commit(): Boolean { apply(); return true }
    }
}

/** Im Speicher gehaltene Umsetzung (Web und Tests). [onChange] speichert dauerhaft. */
open class MemoryKeyValueStore(initial: Map<String, Any> = emptyMap(), private val onChange: (Map<String, Any>) -> Unit = {}) : KeyValueStore {
    protected val map = initial.toMutableMap()
    fun snapshot(): Map<String, Any> = map.toMap()
    override fun getString(key: String, def: String?) = map[key] as? String ?: def
    override fun getBoolean(key: String, def: Boolean) = map[key] as? Boolean ?: def
    override fun getInt(key: String, def: Int) = (map[key] as? Number)?.toInt() ?: def
    override fun getLong(key: String, def: Long) = (map[key] as? Number)?.toLong() ?: def
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, def: Set<String>?) = (map[key] as? Set<String>) ?: def
    override fun contains(key: String) = key in map
    override fun keys(): Set<String> = map.keys.toSet()
    override fun edit(): KeyValueStore.Editor = object : KeyValueStore.Editor {
        private val puts = LinkedHashMap<String, Any?>()
        private var clearAll = false
        override fun putString(key: String, value: String?) = also { puts[key] = value }
        override fun putBoolean(key: String, value: Boolean) = also { puts[key] = value }
        override fun putInt(key: String, value: Int) = also { puts[key] = value }
        override fun putLong(key: String, value: Long) = also { puts[key] = value }
        override fun putStringSet(key: String, value: Set<String>?) = also { puts[key] = value?.toSet() }
        override fun remove(key: String) = also { puts[key] = null }
        override fun clear() = also { clearAll = true }
        override fun apply() {
            if (clearAll) map.clear()
            puts.forEach { (k, v) -> if (v == null) map.remove(k) else map[k] = v }
            onChange(map.toMap())
        }
    }
}
