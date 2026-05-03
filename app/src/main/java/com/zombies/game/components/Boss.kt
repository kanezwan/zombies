package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * Boss 组件（M11：僵王博士雏形）。
 *
 * 承载"阶段 + 多个技能的计时器 + 已触发的一次性技能幂等标记"。
 *
 * 阶段说明（按 HP 百分比触发，只触发一次）：
 *  - PHASE_1 → PHASE_2：HP <= 66%
 *  - PHASE_2 → PHASE_3：HP <= 33%（此处触发一次性"冻结全场植物"技能）
 *
 * 技能：
 *  - [summonCooldownMs] 每 [summonIntervalMs] 召唤 2 只小怪
 *  - [throwCarCooldownMs] 每 [throwCarIntervalMs] 选一行扔车横扫
 *  - [freezeAllTriggered] 一次性：HP <= 33% 时冻结所有植物
 *
 * @property phase 当前阶段（1/2/3），随 HP 下降推进
 * @property summonIntervalMs 召唤间隔，默认 15000
 * @property summonCooldownMs 召唤剩余冷却，0 时触发
 * @property throwCarIntervalMs 扔车间隔，默认 25000
 * @property throwCarCooldownMs 扔车剩余冷却
 * @property freezeAllTriggered 阶段 3 的冻结全场植物是否已触发过
 * @property slowImmune Boss 是否免疫 slow/freeze（默认 true）
 */
data class Boss(
    var phase: Int = 1,
    val summonIntervalMs: Long = 15_000L,
    var summonCooldownMs: Long = 15_000L,          // 首次冷却 = 完整 interval
    val throwCarIntervalMs: Long = 25_000L,
    var throwCarCooldownMs: Long = 25_000L,
    var freezeAllTriggered: Boolean = false,
    val slowImmune: Boolean = true
) : Component
