package com.zombies.game.save

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * 存档管理：使用 [SharedPreferences] 持久化。
 *
 * 字段：
 *  - progress_<levelId> → JSON { unlocked, cleared, bestTimeMs, bestSunLeft }
 *  - stat_kills / stat_planted → 全局累计
 *  - setting_muted → 静音开关
 *
 * 规则：
 *  - level_1 始终视为解锁（即使没有任何存档）
 *  - 通关某关后，自动解锁下一关（levelOrder 内顺延）
 *  - onLevelCleared 只在"更好"的纪录时覆盖 bestTimeMs / bestSunLeft
 *
 * 线程模型：SharedPreferences.Editor.apply 异步；本类仅由 UI 线程或 GameOverSystem 回调调用。
 *
 * 单测可通过直接传入自定义 [SharedPreferences] 实现来避免依赖 Android 运行时。
 */
class SaveManager private constructor(
    private val prefs: SharedPreferences
) {

    // ---- 关卡顺序（默认解锁规则用） ----
    private val levelOrder: List<String> = listOf("level_1", "level_2", "level_3")

    // ---- 查询 ----

    fun getProgress(levelId: String): LevelProgress {
        val raw = prefs.getString(progressKey(levelId), null)
        val defaultUnlocked = levelId == levelOrder.firstOrNull()
        if (raw.isNullOrBlank()) {
            return LevelProgress(levelId = levelId, unlocked = defaultUnlocked)
        }
        return try {
            val obj = JSONObject(raw)
            LevelProgress(
                levelId = levelId,
                unlocked = obj.optBoolean("unlocked", defaultUnlocked),
                cleared = obj.optBoolean("cleared", false),
                bestTimeMs = if (obj.has("bestTimeMs") && !obj.isNull("bestTimeMs"))
                    obj.optLong("bestTimeMs") else null,
                bestSunLeft = if (obj.has("bestSunLeft") && !obj.isNull("bestSunLeft"))
                    obj.optInt("bestSunLeft") else null
            )
        } catch (e: Exception) {
            LevelProgress(levelId = levelId, unlocked = defaultUnlocked)
        }
    }

    fun getAllProgress(): List<LevelProgress> = levelOrder.map { getProgress(it) }

    fun isUnlocked(levelId: String): Boolean = getProgress(levelId).unlocked

    fun getTotalKills(): Long = prefs.getLong(KEY_KILLS, 0L)
    fun getTotalPlanted(): Long = prefs.getLong(KEY_PLANTED, 0L)

    fun isMuted(): Boolean = prefs.getBoolean(KEY_MUTED, false)

    // ---- 写入 ----

    /**
     * 通关时调用：保存更优纪录、解锁下一关。
     * @return 下一关 id（若无则 null）
     */
    fun onLevelCleared(levelId: String, stats: ClearStats): String? {
        val prev = getProgress(levelId)
        val betterTime = prev.bestTimeMs?.let { stats.timeMs < it } ?: true
        val betterSun = prev.bestSunLeft?.let { stats.sunLeft > it } ?: true
        val newProgress = prev.copy(
            unlocked = true,
            cleared = true,
            bestTimeMs = if (betterTime) stats.timeMs else prev.bestTimeMs,
            bestSunLeft = if (betterSun) stats.sunLeft else prev.bestSunLeft
        )
        writeProgress(newProgress)

        // 解锁下一关
        val idx = levelOrder.indexOf(levelId)
        val next = if (idx in 0 until levelOrder.size - 1) levelOrder[idx + 1] else null
        if (next != null) {
            val np = getProgress(next)
            if (!np.unlocked) writeProgress(np.copy(unlocked = true))
        }
        return next
    }

    fun addKills(count: Int) {
        if (count <= 0) return
        prefs.edit().putLong(KEY_KILLS, getTotalKills() + count).apply()
    }

    fun addPlanted(count: Int) {
        if (count <= 0) return
        prefs.edit().putLong(KEY_PLANTED, getTotalPlanted() + count).apply()
    }

    fun setMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
    }

    /** 清空所有存档（用于设置页"重置"按钮） */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ---- 内部 ----

    private fun writeProgress(p: LevelProgress) {
        val obj = JSONObject().apply {
            put("unlocked", p.unlocked)
            put("cleared", p.cleared)
            if (p.bestTimeMs != null) put("bestTimeMs", p.bestTimeMs) else put("bestTimeMs", JSONObject.NULL)
            if (p.bestSunLeft != null) put("bestSunLeft", p.bestSunLeft) else put("bestSunLeft", JSONObject.NULL)
        }
        prefs.edit().putString(progressKey(p.levelId), obj.toString()).apply()
    }

    private fun progressKey(levelId: String) = "progress_$levelId"

    companion object {
        private const val PREF_NAME = "zombies_save"
        private const val KEY_KILLS = "stat_kills"
        private const val KEY_PLANTED = "stat_planted"
        private const val KEY_MUTED = "setting_muted"

        /** Android 端便捷构造：使用应用默认 SharedPreferences。 */
        operator fun invoke(context: Context): SaveManager {
            val prefs = context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return SaveManager(prefs)
        }

        /** 测试端：传入任意 SharedPreferences 实现。 */
        fun from(prefs: SharedPreferences): SaveManager = SaveManager(prefs)
    }
}
