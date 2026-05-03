package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 火炬树桩组件：子弹穿过植物所在格时，伤害乘以 [multiplier]。
 *
 * 实现方式：[com.zombies.game.systems.ProjectileSystem] 推进子弹坐标时检测同行占格是否是火炬；
 * 若是且本次尚未被该火炬强化，则把子弹 [Projectile.damage] ×= [multiplier]，并记录已处理的火炬 id。
 */
data class Torchwood(
    val multiplier: Float = 2f
) : Component

/**
 * 撑杆跳免疫组件：附加此组件的植物在 [com.zombies.game.systems.PoleVaultSystem] 中被跳过。
 * 例如高坚果 tallnut / 南瓜头 pumpkin。
 */
class PoleVaultImmune : Component {
    companion object INSTANCE : Component
}

/**
 * 夜间专属植物（蘑菇类）的"睡眠"状态：白天进入睡眠 → 不射击、不产出，渲染灰色；
 * 夜间醒来 → 正常工作。由 [com.zombies.game.systems.NightPlantSleepSystem] 管理。
 */
data class Sleep(var asleep: Boolean = false) : Component
