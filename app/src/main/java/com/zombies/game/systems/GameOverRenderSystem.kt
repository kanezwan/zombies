package com.zombies.game.systems

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.zombies.game.core.Grid
import com.zombies.game.ecs.RenderSystem
import com.zombies.game.ecs.World
import com.zombies.game.level.GameContext

/**
 * 胜/负横幅 + 关卡进度条 + Retry / Menu 按钮。
 * RUNNING 只画底部进度；VICTORY/DEFEAT 画全屏半透明遮罩 + 文本 + 两个按钮。
 *
 * 按钮命中检测由 [GameSurfaceView] 直接在 UI 线程处理（绕过 ECS 输入队列），
 * 使用 [GameOverRenderSystem.retryRect] / [GameOverRenderSystem.menuRect] 的同一坐标。
 */
class GameOverRenderSystem(private val ctx: GameContext) : RenderSystem(priority = 200) {

    private val progressBgPaint = Paint().apply { color = Color.argb(120, 0, 0, 0) }
    private val progressFgPaint = Paint().apply { color = Color.argb(220, 255, 193, 7) }
    private val maskPaint = Paint().apply { color = Color.argb(160, 0, 0, 0) }
    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 96f
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val subPaint = Paint().apply {
        color = Color.argb(220, 255, 255, 255)
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val btnPaint = Paint().apply { color = Color.argb(230, 56, 142, 60); isAntiAlias = true }
    private val btnBorderPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true
    }
    private val btnTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    override fun render(world: World, canvas: Canvas) {
        val screenW = SCREEN_W
        val screenH = SCREEN_H

        // 进度条（底部）
        val barY = Grid.ORIGIN_Y + Grid.HEIGHT + 30f
        val barLeft = Grid.ORIGIN_X
        val barRight = barLeft + Grid.WIDTH
        val barH = 18f
        canvas.drawRect(barLeft, barY, barRight, barY + barH, progressBgPaint)
        val total = ctx.wave.totalDurationMs.coerceAtLeast(1L)
        val ratio = (ctx.levelElapsedMs.toFloat() / total).coerceIn(0f, 1f)
        canvas.drawRect(barLeft, barY, barLeft + Grid.WIDTH * ratio, barY + barH, progressFgPaint)

        when (ctx.state) {
            GameContext.State.VICTORY -> {
                canvas.drawRect(0f, 0f, screenW, screenH, maskPaint)
                canvas.drawText("VICTORY", screenW / 2f, screenH / 2f - 40f, titlePaint)
                canvas.drawText("All zombies defeated.", screenW / 2f, screenH / 2f + 20f, subPaint)
                drawButtons(canvas)
            }
            GameContext.State.DEFEAT -> {
                canvas.drawRect(0f, 0f, screenW, screenH, maskPaint)
                canvas.drawText("THE ZOMBIES ATE YOUR BRAINS", screenW / 2f, screenH / 2f - 40f, titlePaint)
                canvas.drawText("A zombie reached your house.", screenW / 2f, screenH / 2f + 20f, subPaint)
                drawButtons(canvas)
            }
            GameContext.State.RUNNING -> { /* no-op */ }
        }
    }

    private fun drawButtons(canvas: Canvas) {
        val rr = retryRect()
        canvas.drawRoundRect(rr, 16f, 16f, btnPaint)
        canvas.drawRoundRect(rr, 16f, 16f, btnBorderPaint)
        canvas.drawText("RETRY", rr.centerX(), rr.centerY() + 14f, btnTextPaint)

        val mr = menuRect()
        canvas.drawRoundRect(mr, 16f, 16f, btnPaint)
        canvas.drawRoundRect(mr, 16f, 16f, btnBorderPaint)
        canvas.drawText("MENU", mr.centerX(), mr.centerY() + 14f, btnTextPaint)
    }

    companion object {
        /** 虚拟屏幕宽高（用于居中布局） */
        const val SCREEN_W = 1920f
        const val SCREEN_H = 1080f

        private const val BTN_W = 360f
        private const val BTN_H = 110f
        private const val BTN_GAP = 40f

        fun retryRect(): RectF {
            val totalW = BTN_W * 2 + BTN_GAP
            val left = (SCREEN_W - totalW) / 2f
            val top = SCREEN_H / 2f + 110f
            return RectF(left, top, left + BTN_W, top + BTN_H)
        }

        fun menuRect(): RectF {
            val totalW = BTN_W * 2 + BTN_GAP
            val left = (SCREEN_W - totalW) / 2f + BTN_W + BTN_GAP
            val top = SCREEN_H / 2f + 110f
            return RectF(left, top, left + BTN_W, top + BTN_H)
        }
    }
}
