package com.zombies.game.systems

import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.RectF
import com.zombies.game.components.StaticSpriteRenderable
import com.zombies.game.components.Transform
import com.zombies.game.components.WalkBob
import com.zombies.game.ecs.Entity
import com.zombies.game.ecs.RenderSystem
import com.zombies.game.ecs.World
import com.zombies.game.resource.BitmapProvider
import kotlin.math.sin

/**
 * 静态单图渲染系统（M9）。
 *
 * 渲染规则：
 *  - 锚点：**底部居中**贴到 [Transform]（与 [ShapeRenderSystem] 的 RECT 行为一致：
 *    `top = t.y - heightPx`，`left = t.x - widthPx/2`）
 *  - 按 [StaticSpriteRenderable.zOrder] 升序绘制
 *  - 资源缺失时静默跳过，[com.zombies.game.components.Renderable] 色块作为兜底继续显示
 *  - 若实体挂有 [WalkBob]，会在绘制位置叠加上下抖动 + 轻微左右摇摆（模拟走路）
 *
 * priority = 110（晚于 CardHudRenderSystem=100，这样世界内实体如僵尸/阳光贴图
 * 会盖在 HUD 之上——符合 PvZ 原版"僵尸走到卡牌下时压过卡牌"的视觉）。
 * 色块 ShapeRenderSystem=10 仍在 HUD 之下作兜底；StaticSprite 失败时色块不会被看到，
 * 属可接受的渲染回退。
 */
class StaticSpriteRenderSystem(
    private val bitmaps: BitmapProvider
) : RenderSystem(priority = 110) {

    private val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
    private val sorted = ArrayList<Entity>(128)
    private val dst = RectF()

    override fun render(world: World, canvas: Canvas) {
        sorted.clear()
        world.forEachWith2<StaticSpriteRenderable, Transform> { e, _, _ -> sorted.add(e) }
        sorted.sortBy { it.get<StaticSpriteRenderable>()!!.zOrder }

        for (e in sorted) {
            val sr = e.get<StaticSpriteRenderable>() ?: continue
            val t = e.get<Transform>() ?: continue
            val bmp = bitmaps.getStandalone(sr.spriteKey) ?: continue

            // WalkBob：
            //   - 行走（active=true）：小幅垂直抖动 + 身体"压缩-拉伸"（squash & stretch）
            //   - 啃食（active=false）：不抖，但若挂了 eatLean 属性可前后探头
            //   去掉左右旋转，避免"醉汉摇晃"的不自然感
            val bob = e.get<WalkBob>()
            var yOffset = 0f
            var squashX = 1f
            var squashY = 1f
            var leanX = 0f
            if (bob != null) {
                val phase = 2.0 * Math.PI * (bob.playedMs + bob.phaseMs) / 1000.0 * bob.freqHz
                if (bob.active) {
                    val s = sin(phase).toFloat()
                    // 垂直抬起（脚离地）= |sin|
                    yOffset = -kotlin.math.abs(s) * bob.amplitudePx
                    // 身体压缩：落地瞬间（|sin|~0）略微压扁（x 拉宽 / y 压低）
                    //   用 (1 - |sin|) 作为"落地程度"：落地=1, 腾空=0
                    val landing = 1f - kotlin.math.abs(s)
                    squashX = 1f + landing * 0.06f
                    squashY = 1f - landing * 0.06f
                }
                // 啃食前后冲：sin(phase) 产生 -1..+1，乘上 leanPx
                if (bob.eatLeanPx != 0f) {
                    leanX = sin(phase).toFloat() * bob.eatLeanPx
                }
            }

            val drawW = sr.widthPx * squashX
            val drawH = sr.heightPx * squashY
            val drawHalf = drawW / 2f
            // 底部锚点保持在 t.y（脚踩在同一高度），squashY 让顶部位置变化
            dst.set(
                t.x - drawHalf + leanX,
                t.y - drawH + yOffset,
                t.x + drawHalf + leanX,
                t.y + yOffset
            )
            canvas.drawBitmap(bmp, null, dst, paint)
        }
    }
}
