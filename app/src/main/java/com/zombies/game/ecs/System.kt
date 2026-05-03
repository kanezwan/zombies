package com.zombies.game.ecs

import android.graphics.Canvas

/**
 * 逻辑系统：每帧固定步长被 [World.update] 调用。
 *
 * 实现示例：MovementSystem / CollisionSystem / SpawnSystem / SunSystem。
 *
 * @property priority 越小越先执行（默认 0）
 */
abstract class GameSystem(val priority: Int = 0) {
    /** 每个固定步长调用一次 */
    open fun update(world: World, dtMs: Long) {}
}

/**
 * 渲染系统：每帧由 [World.render] 调用。
 *
 * 与 [GameSystem] 分离的原因：
 *  - 逻辑可能跑多次（追帧），渲染只跑一次
 *  - 便于关闭/替换渲染层（例如服务器侧无渲染）
 */
abstract class RenderSystem(val priority: Int = 0) {
    /** 每帧调用一次。Canvas 已被 SurfaceView 锁定。 */
    abstract fun render(world: World, canvas: Canvas)
}
