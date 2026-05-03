package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 僵尸移动参数。
 * 空闲时以 [baseSpeed] 像素/秒 向左推进；啃食时速度置 0（由 ZombieEatSystem 控制状态机）。
 */
data class ZombieMover(
    var baseSpeed: Float,
    var state: State = State.WALKING
) : Component {
    enum class State { WALKING, EATING }
}

/**
 * 啃食行为：每 [intervalMs] 对当前目标造成 [damage] 伤害。
 * [targetEntityId] = -1 表示无目标，恢复行走。
 */
data class ZombieEater(
    val intervalMs: Long,
    val damage: Int,
    var elapsedMs: Long = 0L,
    var targetEntityId: Long = -1L
) : Component
