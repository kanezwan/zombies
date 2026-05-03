package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 子弹配置。
 * - [row] 所在行（仅与同行僵尸碰撞）
 * - [damage] 命中伤害（可被 Torchwood 穿透强化而递增，因此可变）
 * - [maxX] 飞行超过此 X 坐标视为脱靶消失
 * - [slowMultiplier] 命中时附加的减速倍率；<= 0 或 >= 1 视为不减速
 * - [slowDurationMs] 减速持续时间（毫秒）
 * - [slowSource] 减速来源键（用于同源刷新去重）
 * - [splashRadius] 溅射半径（虚拟像素），>0 时命中后对同行前方该距离内的其他僵尸造成 [splashDamageRatio]×damage
 * - [splashDamageRatio] 溅射伤害系数（0~1），仅传递伤害，不传递 slow
 * - [fireEnhanced] 是否已被火炬树桩强化（渲染用；同时也作为幂等检查标志，同子弹穿越多个火炬取第一个）
 * - [lastTorchedCol] 最后一次被强化时的火炬列号，防止同一火炬重复加成（不同火炬可再加）
 */
data class Projectile(
    val row: Int,
    var damage: Int,
    val maxX: Float,
    val slowMultiplier: Float = 1f,
    val slowDurationMs: Long = 0L,
    val slowSource: String = "",
    val splashRadius: Float = 0f,
    val splashDamageRatio: Float = 0f,
    var fireEnhanced: Boolean = false,
    var lastTorchedCol: Int = -1
) : Component {
    /** 是否携带减速 debuff */
    fun hasSlow(): Boolean =
        slowMultiplier > 0f && slowMultiplier < 1f && slowDurationMs > 0L

    /** 是否携带溅射效果 */
    fun hasSplash(): Boolean =
        splashRadius > 0f && splashDamageRatio > 0f
}
