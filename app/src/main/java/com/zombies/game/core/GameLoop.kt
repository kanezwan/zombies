package com.zombies.game.core

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * 固定步长游戏循环。
 *
 * 设计：
 *  - 独立渲染线程，避免阻塞 UI
 *  - 逻辑步长固定 [STEP_MS]，渲染按"能跑多快跑多快"
 *  - 通过累加 dt 决定调用多少次 [onUpdate]，避免帧率波动导致逻辑漂移
 *  - 每秒回调一次 [onFps] 上报实时帧率
 */
class GameLoop(
    private val holder: SurfaceHolder,
    private val onUpdate: (dtMs: Long) -> Unit,
    private val onRender: (canvas: Canvas) -> Unit,
    private val onFps: (fps: Int) -> Unit
) {

    @Volatile
    var isRunning: Boolean = false
        private set

    private var thread: Thread? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        thread = Thread(::loop, "GameLoop").apply { start() }
    }

    fun stop() {
        isRunning = false
        thread?.let {
            try {
                it.join(500)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        thread = null
    }

    private fun loop() {
        var lastNs = System.nanoTime()
        var accumulatorMs = 0L
        var fpsCounter = 0
        var fpsTimerMs = 0L

        while (isRunning) {
            val nowNs = System.nanoTime()
            val frameMs = ((nowNs - lastNs) / 1_000_000L).coerceAtMost(MAX_FRAME_MS)
            lastNs = nowNs

            // 1) 逻辑更新（固定步长）
            accumulatorMs += frameMs
            while (accumulatorMs >= STEP_MS) {
                onUpdate(STEP_MS)
                accumulatorMs -= STEP_MS
            }

            // 2) 渲染
            val canvas: Canvas? = try {
                holder.lockCanvas()
            } catch (_: IllegalStateException) {
                null
            }
            if (canvas != null) {
                try {
                    synchronized(holder) { onRender(canvas) }
                } finally {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: IllegalStateException) {
                        // surface 已销毁
                    }
                }
            }

            // 3) FPS 统计
            fpsCounter++
            fpsTimerMs += frameMs
            if (fpsTimerMs >= 1000L) {
                onFps(fpsCounter)
                fpsCounter = 0
                fpsTimerMs = 0L
            }

            // 4) 简单节流，避免无意义满 CPU
            val targetFrameMs = 16L
            val sleep = targetFrameMs - ((System.nanoTime() - nowNs) / 1_000_000L)
            if (sleep > 1) {
                try {
                    Thread.sleep(sleep)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    companion object {
        /** 逻辑步长：约 60Hz */
        private const val STEP_MS = 16L

        /** 单帧最大处理时间，避免后台切回时一次性追帧过多 */
        private const val MAX_FRAME_MS = 100L
    }
}
