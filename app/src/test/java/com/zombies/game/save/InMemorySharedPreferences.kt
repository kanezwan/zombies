package com.zombies.game.save

import android.content.SharedPreferences

/**
 * 纯内存 SharedPreferences，用于单测 [SaveManager]。
 * 注：apply / commit 同步生效。Listener 未实现（测试不需要）。
 */
class InMemorySharedPreferences : SharedPreferences {

    private val data = HashMap<String, Any?>()

    override fun getAll(): Map<String, *> = HashMap(data)

    override fun getString(key: String, defValue: String?): String? =
        (data[key] as? String) ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        (data[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String, defValue: Int): Int =
        (data[key] as? Int) ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        (data[key] as? Long) ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        (data[key] as? Float) ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defValue

    override fun contains(key: String): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        // 测试不需要
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        // 测试不需要
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = HashMap<String, Any?>()
        private var clearFirst = false
        private val removed = HashSet<String>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            pending[key] = value; return this
        }

        override fun putStringSet(
            key: String, values: MutableSet<String>?
        ): SharedPreferences.Editor {
            pending[key] = values; return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pending[key] = value; return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            pending[key] = value; return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            pending[key] = value; return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            pending[key] = value; return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            removed.add(key); return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearFirst = true; return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearFirst) data.clear()
            for (k in removed) data.remove(k)
            data.putAll(pending)
        }
    }
}
