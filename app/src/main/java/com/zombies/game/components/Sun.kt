package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 阳光实体状态。
 * - [value] 被拾取可获得的阳光值
 * - [targetY] 下落的目标 Y（地面 / 生成后的目标位置）；达到后停止下落
 * - [lifetimeMs] 剩余存活时间，归零自动消失
 * - [source] 来源（自然掉落 / 向日葵），仅用于调试
 */
data class Sun(
    val value: Int,
    val targetY: Float,
    var lifetimeMs: Long,
    val source: String = "sky"
) : Component

/**
 * 可点击拾取组件 — 把屏幕点击转换为拾取。
 * [radius] 点击容差（虚拟像素）。
 */
data class Pickable(
    val radius: Float
) : Component
