package com.zombies.game.level

import com.zombies.game.core.Grid

/**
 * 草坪格子占用表：记录每个格子上种植的植物实体 id。
 * -1 表示空。
 */
class GridOccupancy {
    private val cells = LongArray(Grid.ROWS * Grid.COLS) { -1L }

    fun isEmpty(row: Int, col: Int): Boolean =
        Grid.isValidCell(row, col) && cells[index(row, col)] == -1L

    fun set(row: Int, col: Int, entityId: Long) {
        if (Grid.isValidCell(row, col)) cells[index(row, col)] = entityId
    }

    fun clear(row: Int, col: Int) {
        if (Grid.isValidCell(row, col)) cells[index(row, col)] = -1L
    }

    fun get(row: Int, col: Int): Long =
        if (Grid.isValidCell(row, col)) cells[index(row, col)] else -1L

    fun reset() { for (i in cells.indices) cells[i] = -1L }

    private fun index(row: Int, col: Int) = row * Grid.COLS + col
}
