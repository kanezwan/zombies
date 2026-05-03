package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 状态效果容器。
 *
 * 挂在僵尸（或将来其他目标）上，由 StatusEffectSystem 统一推进时间、到期清理。
 * 其他系统（MovementSystem 等）读取本容器里"当前最强"的效果生效。
 *
 * 当前支持：
 *  - slow: 减速 debuff（multiplier < 1），多个来源叠加时取"最强"（系数最小），剩余时长各自倒计时。
 *
 * 设计约束：本组件是 data class + 可变列表容器，避免每帧创建对象。
 */
class StatusEffects : Component {
    val slows: ArrayList<SlowEffect> = ArrayList(2)

    /** 返回当前最强的减速系数；若无任何减速效果，返回 1.0f（不减速）。 */
    fun currentSlowMultiplier(): Float {
        if (slows.isEmpty()) return 1f
        var min = 1f
        for (s in slows) if (s.multiplier < min) min = s.multiplier
        return min
    }

    /**
     * 添加/刷新一个减速效果：
     *  - 若已有相同 [source] 的效果，则覆盖其系数和剩余时长（刷新）
     *  - 否则追加新条目
     */
    fun addOrRefreshSlow(source: String, multiplier: Float, durationMs: Long) {
        for (i in slows.indices) {
            if (slows[i].source == source) {
                slows[i] = SlowEffect(source, multiplier, durationMs)
                return
            }
        }
        slows.add(SlowEffect(source, multiplier, durationMs))
    }

    /**
     * 添加/刷新一个"完全冻结"效果（multiplier=0）。本质上复用 [addOrRefreshSlow] 机制，
     * 但语义上用 source="freeze" 独立管理，方便被火炬豆等解冻手段定向移除。
     *
     * 冻结期间：currentSlowMultiplier() 返回 0 → ZombieMoveSystem 速度归零；
     *           StatusEffectSystem 将渲染切到 [Renderable.ZOMBIE_FROZEN]。
     */
    fun addOrRefreshFreeze(durationMs: Long) {
        addOrRefreshSlow(source = "freeze", multiplier = 0f, durationMs = durationMs)
    }

    /** 立即移除指定来源的效果（如火炬豆解冻）。返回是否移除成功。 */
    fun removeBySource(source: String): Boolean {
        val it = slows.iterator()
        while (it.hasNext()) {
            if (it.next().source == source) {
                it.remove()
                return true
            }
        }
        return false
    }

    /** 推进时间，移除到期的效果。返回是否有移除发生（供外部做视觉还原）。 */
    fun tick(dtMs: Long): Boolean {
        if (slows.isEmpty()) return false
        var removed = false
        val it = slows.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.remainingMs -= dtMs
            if (s.remainingMs <= 0L) {
                it.remove()
                removed = true
            }
        }
        return removed
    }
}

/**
 * 单条减速效果。
 * @property source 唯一来源键（如 "snowpea"），用于同源刷新
 * @property multiplier 速度倍率，范围 (0, 1]，越小减速越强
 * @property remainingMs 剩余持续时间（毫秒）
 */
data class SlowEffect(
    val source: String,
    val multiplier: Float,
    var remainingMs: Long
)
