package com.zombies.game.systems

import android.graphics.Canvas
import android.graphics.Paint
import com.zombies.game.components.Renderable
import com.zombies.game.components.StaticSpriteRenderable
import com.zombies.game.components.Transform
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.RenderSystem
import com.zombies.game.ecs.World
import com.zombies.game.resource.BitmapProvider

/**
 * 占位渲染系统：按 zOrder 升序绘制 [Renderable]。
 * M4 起可替换为 SpriteRenderSystem（Bitmap 雪碧图版），或二者共存。
 *
 * M9 增强：若实体同时持有 [StaticSpriteRenderable]，且 [bitmapProvider] 能成功加载该 PNG，
 *          则**跳过色块绘制**，避免色块从图边缘露出。资源缺失时回退到色块兜底。
 *
 * @param bitmapProvider 可空；用于查询静态图是否存在以决定是否跳过。传 null 时永远画色块。
 */
class ShapeRenderSystem(
    private val bitmapProvider: BitmapProvider? = null
) : RenderSystem(priority = 10) {

    private val paint = Paint().apply { isAntiAlias = true }
    private val sorted = ArrayList<Entity>(128)

    override fun render(world: World, canvas: Canvas) {
        sorted.clear()
        world.forEachWith2<Renderable, Transform> { e, _, _ -> sorted.add(e) }
        sorted.sortBy { it.get<Renderable>()!!.zOrder }

        for (e in sorted) {
            val r = e.get<Renderable>() ?: continue
            val t = e.get<Transform>() ?: continue

            // 若实体挂了可用的静态图，色块退场（否则会从图边缘漏出一圈底色）。
            // 缺图时 getStandalone 返回 null，此判断为 false，色块继续作为兜底。
            if (bitmapProvider != null) {
                val sprite = e.get<StaticSpriteRenderable>()
                if (sprite != null && bitmapProvider.getStandalone(sprite.spriteKey) != null) {
                    continue
                }
            }

            paint.color = r.color
            when (r.shape) {
                Renderable.Shape.CIRCLE -> {
                    canvas.drawCircle(t.x, t.y - r.height / 2f, r.width / 2f, paint)
                }
                Renderable.Shape.RECT -> {
                    val half = r.width / 2f
                    canvas.drawRect(
                        t.x - half,
                        t.y - r.height,
                        t.x + half,
                        t.y,
                        paint
                    )
                }
            }
        }
    }
}
