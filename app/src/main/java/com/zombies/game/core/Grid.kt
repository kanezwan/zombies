package com.zombies.game.core

/**
 * 草坪网格（5 行 × 9 列）虚拟坐标映射。
 *
 * 虚拟分辨率：1920 × 1080
 * 顶部 UI 高度约 160（卡牌槽 + 阳光），底部留 80（进度条）
 * 草坪绘制区：x ∈ [180, 1820]（宽 1640），y ∈ [180, 1020]（高 840）
 *  - 单元宽：1640 / 9 ≈ 182
 *  - 单元高： 840 / 5 = 168
 */
object Grid {
    const val ROWS = 5
    const val COLS = 9

    const val ORIGIN_X = 180f
    const val ORIGIN_Y = 180f
    const val WIDTH    = 1640f
    const val HEIGHT   = 840f

    const val CELL_W = WIDTH / COLS  // 182.22
    const val CELL_H = HEIGHT / ROWS // 168.0

    /** 网格坐标 → 单元中心（虚拟坐标） */
    fun cellCenterX(col: Int): Float = ORIGIN_X + (col + 0.5f) * CELL_W
    fun cellCenterY(row: Int): Float = ORIGIN_Y + (row + 0.5f) * CELL_H

    /** 网格坐标 → 单元左上角（虚拟坐标） */
    fun cellLeft(col: Int): Float = ORIGIN_X + col * CELL_W
    fun cellTop(row: Int): Float = ORIGIN_Y + row * CELL_H

    /**
     * 虚拟坐标 → 网格坐标。
     * 落在草坪外返回 (-1, -1)。
     */
    fun pointToCell(x: Float, y: Float): IntArray {
        if (x < ORIGIN_X || x >= ORIGIN_X + WIDTH || y < ORIGIN_Y || y >= ORIGIN_Y + HEIGHT) {
            return intArrayOf(-1, -1)
        }
        val col = ((x - ORIGIN_X) / CELL_W).toInt().coerceIn(0, COLS - 1)
        val row = ((y - ORIGIN_Y) / CELL_H).toInt().coerceIn(0, ROWS - 1)
        return intArrayOf(row, col)
    }

    fun isValidCell(row: Int, col: Int): Boolean =
        row in 0 until ROWS && col in 0 until COLS
}
