package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 撑杆跳僵尸专属组件：
 *  - 检测同行前方是否存在植物；若存在且在触发距离内 → 触发一次"跳跃"，瞬移到该植物左侧的下一格
 *  - 跳跃后 [used] = true，之后退化为普通僵尸行为
 *
 * @property jumpDistance 触发检测距离（虚拟像素）；当僵尸 x - 植物 x 小于等于该值即触发
 * @property used 是否已经跳过一次
 */
data class PoleVault(
    val jumpDistance: Float = 180f,
    var used: Boolean = false
) : Component
