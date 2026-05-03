package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 速度组件（虚拟像素 / 秒）。
 * 用于子弹、阳光下落、未来僵尸移动。
 */
data class Velocity(
    var vx: Float,
    var vy: Float
) : Component
