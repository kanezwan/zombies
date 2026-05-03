package com.zombies.game.systems

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.zombies.game.components.FloatingText
import com.zombies.game.components.Transform
import com.zombies.game.ecs.RenderSystem
import com.zombies.game.ecs.World

/**
 * 飘字渲染：以 [Transform] 为基准点绘制文字，alpha 随 elapsedMs/totalMs 线性递减。
 *
 * priority = 120（晚于 CardHudRenderSystem=100、StaticSpriteRenderSystem=110，
 * 保证飘字永远显示在最上层，不会被 HUD 或僵尸贴图遮挡）。
 */
class FloatingTextRenderSystem : RenderSystem(priority = 120) {

    private val paint = Paint().apply {
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    override fun render(world: World, canvas: Canvas) {
        world.forEachWith2<FloatingText, Transform> { _, ft, t ->
            val lifeRatio = (ft.elapsedMs.toFloat() / ft.totalMs).coerceIn(0f, 1f)
            // 前 20% 完全不透明，后续线性渐隐
            val alphaRatio = if (lifeRatio < 0.2f) 1f else (1f - (lifeRatio - 0.2f) / 0.8f)
            val alpha = (alphaRatio * 255f).toInt().coerceIn(0, 255)
            paint.color = (ft.color and 0x00FFFFFF) or (alpha shl 24)
            paint.textSize = ft.textSize
            canvas.drawText(ft.text, t.x, t.y, paint)
        }
    }
}
