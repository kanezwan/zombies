package com.zombies.game.systems

import android.graphics.Canvas
import android.graphics.Paint
import com.zombies.game.components.SpriteRenderable
import com.zombies.game.components.Transform
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.RenderSystem
import com.zombies.game.ecs.World
import com.zombies.game.resource.BitmapProvider

/**
 * 雪碧图渲染系统（M8）：
 *  - 按 [SpriteRenderable.zOrder] 升序遍历
 *  - 通过 [BitmapProvider.get] 拿到对应 SpriteSheet；若不存在则**跳过**，交由 ShapeRenderSystem 做色块降级
 *  - 动画名若不存在，回落到 "idle"；若仍不存在则跳过
 *
 * priority = 12（紧接 ShapeRenderSystem = 10 之后，盖在色块上；
 * 这样色块继续作为"兜底"存在，哪怕雪碧图缺帧一帧也不会黑屏）。
 */
class SpriteRenderSystem(
    private val bitmaps: BitmapProvider
) : RenderSystem(priority = 12) {

    private val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
    private val sorted = ArrayList<Entity>(128)

    override fun render(world: World, canvas: Canvas) {
        sorted.clear()
        world.forEachWith2<SpriteRenderable, Transform> { e, _, _ -> sorted.add(e) }
        sorted.sortBy { it.get<SpriteRenderable>()!!.zOrder }

        for (e in sorted) {
            val sr = e.get<SpriteRenderable>() ?: continue
            val t = e.get<Transform>() ?: continue
            val sheet = bitmaps.get(sr.spriteKey) ?: continue
            val animName = when {
                sheet.hasAnimation(sr.animation) -> sr.animation
                sheet.hasAnimation("idle") -> "idle"
                else -> continue
            }
            sheet.drawAnimation(canvas, animName, sr.playedMs, t.x, t.y, paint)
        }
    }
}
