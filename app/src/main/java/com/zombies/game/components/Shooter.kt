package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 射手（豌豆射手 / 双发豌豆 / 寒冰射手 / 西瓜投手）。
 * - [intervalMs] 射击间隔
 * - [damage] 单发伤害
 * - [projectileSpeed] 子弹速度（虚拟像素 / 秒）
 * - [elapsedMs] 自上次射击的累计时间
 * - [onlyWhenZombieInRow] 仅在本行有僵尸时才攒 CD 并射击（经典 PVZ 规则）
 * - [shotsPerFire] 单次射击发出的子弹数（repeater=2）
 * - [shotGapMs] 多发连射时每发的时间间隔（由 ShooterSystem 使用排队实现）
 * - [slowMultiplier] / [slowDurationMs] / [slowSource] 子弹附加减速参数（snowpea / wintermelon）
 * - [splashRadius] / [splashDamageRatio] 子弹溅射参数（wintermelon）
 * - [bulletKind] "pea" | "icepea" | "melon"；决定 ShooterSystem 调用哪个工厂方法
 */
data class Shooter(
    val intervalMs: Long,
    val damage: Int,
    val projectileSpeed: Float,
    var elapsedMs: Long = 0L,
    val onlyWhenZombieInRow: Boolean = true,
    val shotsPerFire: Int = 1,
    val shotGapMs: Long = 120L,
    val slowMultiplier: Float = 1f,
    val slowDurationMs: Long = 0L,
    val slowSource: String = "",
    val splashRadius: Float = 0f,
    val splashDamageRatio: Float = 0f,
    val bulletKind: String = "pea",
    /** 运行时字段：待补发的子弹数（剩余连射次数） */
    var pendingShots: Int = 0,
    /** 运行时字段：距离下一次补发剩余毫秒 */
    var nextShotGapMs: Long = 0L
) : Component
