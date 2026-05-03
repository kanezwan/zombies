package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 雪碧图渲染组件（M8）。
 *
 * 与 [Renderable] 并存：当 SpriteRenderSystem 找到 [spriteKey] 对应的 SpriteSheet 时，
 * 优先使用雪碧图渲染；否则由 ShapeRenderSystem 用 Renderable 色块降级渲染。
 *
 * @property spriteKey 资源键，对应 assets/<spriteKey>.png + .json
 * @property animation 当前播放的动画名（SpriteSheet.animation(name)）
 * @property playedMs 已播放时长（AnimationSystem 每帧累加）
 * @property zOrder 渲染顺序（越大越靠前）
 */
data class SpriteRenderable(
    val spriteKey: String,
    var animation: String = "idle",
    var playedMs: Long = 0L,
    var zOrder: Int = 0
) : Component
