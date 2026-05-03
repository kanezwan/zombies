package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 植物被冻结的 debuff（M11：Boss 技能 3 触发）。
 *
 * 冻结期间：
 *  - ShooterSystem 跳过（不射击）
 *  - ProducerSystem 跳过（不产阳光）
 *  - Renderable 被 PlantFreezeSystem 覆盖为冰蓝色
 *
 * [remainingMs] 每帧由 [com.zombies.game.systems.PlantFreezeSystem] 倒计时；<=0 时移除组件并还原渲染色。
 */
data class PlantFreeze(
    var remainingMs: Long
) : Component
