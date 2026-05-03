package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 外挂护甲组件（Newspaper 的报纸、路障/铁桶变体等通用机制）。
 *
 * 伤害结算顺序（由 [com.zombies.game.systems.ProjectileCollisionSystem] 处理）：
 *   先扣护甲 HP，直到 [hp] <= 0 后才进入 [Health]。
 *
 * 破碎时可触发 [onBreak] —— 用于 Newspaper 暴走、读报僵尸特效等。
 *
 * @property hp 当前剩余护甲
 * @property maxHp 初始护甲上限（用于渲染比例、调试）
 * @property broken 是否已破碎（幂等标记，避免 onBreak 多次触发）
 */
data class Armor(
    var hp: Int,
    val maxHp: Int,
    var broken: Boolean = false
) : Component {
    /** 吸收 [incoming] 伤害，返回"穿透到本体"的剩余伤害。 */
    fun absorb(incoming: Int): Int {
        if (broken || hp <= 0) return incoming
        return if (incoming >= hp) {
            val overflow = incoming - hp
            hp = 0
            broken = true
            overflow
        } else {
            hp -= incoming
            0
        }
    }
}

/**
 * 护甲破碎事件标签：由 ProjectileCollisionSystem 打上，
 * ArmorBreakSystem（若存在）或 NewspaperEnrageSystem 下一帧消费。
 * 本期直接在 CollisionSystem 内完成"暴走"逻辑。
 */
class ArmorBrokenTag : Component {
    companion object INSTANCE : Component
}

/**
 * 反弹护盾：携带该组件的僵尸被**非减速、非火焰强化**的子弹命中时，
 * 子弹不会扣血，而是反向飞行（右向）从该行飞出。
 * 命中一次后失效（从组件里移除，或 [active] 置 false）。
 *
 * 设计考虑：只反弹普通豌豆；冰豆/火焰/爆炸仍按原逻辑结算。
 */
data class ReflectShield(
    var active: Boolean = true
) : Component
