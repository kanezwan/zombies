package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 炸弹引信组件：种下 [fuseMs] 毫秒后触发范围爆炸。
 *
 * - [radius]: 爆炸半径（虚拟像素，按欧氏距离 vs 僵尸 Transform）
 * - [damage]: 范围内所有僵尸受到的单次伤害
 * - [triggered]: 防止重复触发；BombSystem 设置后同帧销毁实体
 *
 * 樱桃炸弹：fuseMs = 1200, radius = Grid.CELL_W * 1.5, damage = 1800（一击必杀任何 M5 僵尸）
 */
data class BombFuse(
    val fuseMs: Long,
    val radius: Float,
    val damage: Int,
    var elapsedMs: Long = 0L,
    var triggered: Boolean = false
) : Component
