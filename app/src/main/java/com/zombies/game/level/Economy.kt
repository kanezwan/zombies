package com.zombies.game.level

/**
 * 经济状态：玩家当前持有的阳光数 + 植物冷却表。
 *
 * 线程模型：仅由 GameLoop 线程访问。UI 线程通过 snapshot 读取 [sun] 用于绘制。
 */
class Economy(initialSun: Int = 50) {

    @Volatile
    var sun: Int = initialSun
        private set

    /** 植物种类 → 距离下次可种植的剩余冷却毫秒数（0 表示就绪） */
    private val cooldownMs = HashMap<String, Long>()

    fun addSun(delta: Int) {
        sun = (sun + delta).coerceAtLeast(0)
    }

    /** 尝试消耗阳光。足够则扣除并返回 true，不足返回 false。 */
    fun trySpend(cost: Int): Boolean {
        if (sun < cost) return false
        sun -= cost
        return true
    }

    // ---------------- 冷却 ----------------

    fun triggerCooldown(type: String, durationMs: Long) {
        cooldownMs[type] = durationMs
    }

    fun cooldownRemainingMs(type: String): Long = cooldownMs[type] ?: 0L

    fun isReady(type: String): Boolean = cooldownRemainingMs(type) <= 0L

    /** 每帧 tick */
    fun tick(dtMs: Long) {
        if (cooldownMs.isEmpty()) return
        val it = cooldownMs.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val left = e.value - dtMs
            if (left <= 0L) it.remove() else e.setValue(left)
        }
    }

    fun reset(initialSun: Int) {
        sun = initialSun
        cooldownMs.clear()
    }
}
