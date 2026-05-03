package com.zombies.game.core

import android.graphics.Matrix
import android.graphics.RectF

/**
 * 虚拟分辨率视口。
 *
 * 设计目标：
 *  - 所有游戏逻辑使用统一的虚拟分辨率坐标（默认 1920×1080）
 *  - 真机渲染时按"等比缩放 + 居中"映射到屏幕
 *  - 屏幕触摸点反向映射回虚拟坐标供逻辑使用
 *
 * 缩放策略：FIT —— 保持长宽比，多余部分留黑边（letterbox）。
 */
class Viewport(
    val virtualWidth: Float = 1920f,
    val virtualHeight: Float = 1080f
) {

    var screenWidth: Int = 0
        private set
    var screenHeight: Int = 0
        private set

    /** 虚拟坐标 → 屏幕坐标的变换矩阵（含缩放与居中平移） */
    val matrix: Matrix = Matrix()

    /** 缩放比例（虚拟 → 屏幕） */
    var scale: Float = 1f
        private set

    /** 屏幕画布上虚拟内容的绘制偏移（letterbox 黑边宽度） */
    var offsetX: Float = 0f
        private set
    var offsetY: Float = 0f
        private set

    /** 虚拟内容在屏幕上的实际矩形区域 */
    val contentRect: RectF = RectF()

    /** SurfaceView 尺寸变化时调用 */
    fun resize(screenW: Int, screenH: Int) {
        if (screenW <= 0 || screenH <= 0) return
        screenWidth = screenW
        screenHeight = screenH

        val sx = screenW / virtualWidth
        val sy = screenH / virtualHeight
        scale = minOf(sx, sy)

        val drawW = virtualWidth * scale
        val drawH = virtualHeight * scale
        offsetX = (screenW - drawW) * 0.5f
        offsetY = (screenH - drawH) * 0.5f

        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postTranslate(offsetX, offsetY)

        contentRect.set(offsetX, offsetY, offsetX + drawW, offsetY + drawH)
    }

    /** 屏幕坐标 → 虚拟坐标 */
    fun screenToVirtualX(sx: Float): Float = (sx - offsetX) / scale
    fun screenToVirtualY(sy: Float): Float = (sy - offsetY) / scale

    /** 屏幕点是否落在虚拟内容区域内 */
    fun contains(sx: Float, sy: Float): Boolean = contentRect.contains(sx, sy)
}
