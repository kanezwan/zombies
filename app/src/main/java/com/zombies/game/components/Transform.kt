package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 虚拟坐标系下的位置（锚点：脚踩点 / 底部中心）。
 */
data class Transform(
    var x: Float,
    var y: Float
) : Component

/**
 * 网格所属（植物/僵尸行列）。col=-1 表示僵尸跨格移动中，以 [Transform] 为准。
 */
data class GridCell(
    var row: Int,
    var col: Int
) : Component
