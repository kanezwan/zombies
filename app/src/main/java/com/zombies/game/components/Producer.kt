package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 阳光生产者（向日葵）。
 * - [intervalMs] 每次吐阳光间隔
 * - [amount] 每次产出阳光数量
 * - [elapsedMs] 累计经过时间，达到 interval 时触发产出
 * - [startupDelayMs] 初次产出的额外延迟（避免刚种下立刻产出）
 */
data class Producer(
    val intervalMs: Long,
    val amount: Int,
    var elapsedMs: Long = 0L,
    var startupDelayMs: Long = 0L
) : Component
