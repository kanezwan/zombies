package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 走路抖动组件（零资产的"伪动画"）。
 *
 * 两种模式：
 *  - **行走**（[active] = true, [eatLeanPx] = 0）：垂直抖动 + 身体 squash&stretch（不旋转）
 *  - **啃食**（[active] = false, [eatLeanPx] > 0）：垂直不动，前后探头（x 方向来回 leanPx）
 *
 * 效果由 [com.zombies.game.systems.StaticSpriteRenderSystem] 消费，
 * 时间由 [com.zombies.game.systems.AnimationSystem] 推进 [playedMs]。
 *
 * @param amplitudePx 垂直抖动幅度，像素。建议 4~10f
 * @param freqHz 行走节奏，每秒几次。建议 2~4f
 * @param tiltDeg 兼容旧字段，**当前渲染系统已不使用旋转**，保留避免迁移破坏
 * @param phaseMs 初始相位偏移，给同一批僵尸不同节奏避免"同步齐步走"
 * @param playedMs 累计播放时长（AnimationSystem 推进）
 * @param active 行走开关；false 时停止上下弹跳
 * @param eatLeanPx 啃食时的前后探头幅度（像素）。0 = 不探头；建议 3~5f
 */
data class WalkBob(
    val amplitudePx: Float = 6f,
    val freqHz: Float = 3f,
    val tiltDeg: Float = 0f,
    val phaseMs: Long = 0L,
    var playedMs: Long = 0L,
    var active: Boolean = true,
    var eatLeanPx: Float = 0f
) : Component
