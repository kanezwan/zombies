package com.zombies.game.components

import com.zombies.game.ecs.Component

/**
 * 静态单图渲染组件（M9）。
 *
 * 与 [SpriteRenderable]（雪碧图 + 动画）、[Renderable]（色块兜底）并存：
 *  - 单张 PNG（`assets/sprites/xxx/yyy.png`），无 JSON、无动画、无多帧
 *  - 渲染时按 [widthPx] × [heightPx] 绘制；锚点语义与 [Renderable] 矩形一致：
 *    图像底部中心对齐到 [com.zombies.game.components.Transform]，即"脚踩在 (t.x, t.y)"
 *  - 资源缺失时 [com.zombies.game.systems.StaticSpriteRenderSystem] 静默跳过，
 *    [Renderable] 色块继续显示作为兜底
 *
 * @property spriteKey 资源路径（不含扩展名），例如 "sprites/plants/sunflower"
 * @property widthPx   目标绘制宽（虚拟像素）
 * @property heightPx  目标绘制高（虚拟像素）
 * @property zOrder    渲染顺序，越大越靠前（同 [Renderable]）
 */
data class StaticSpriteRenderable(
    val spriteKey: String,
    val widthPx: Float,
    val heightPx: Float,
    var zOrder: Int = 0
) : Component
