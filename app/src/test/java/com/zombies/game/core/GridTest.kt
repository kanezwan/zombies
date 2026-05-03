package com.zombies.game.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridTest {

    @Test
    fun `cell center maps back to same cell`() {
        for (row in 0 until Grid.ROWS) {
            for (col in 0 until Grid.COLS) {
                val x = Grid.cellCenterX(col)
                val y = Grid.cellCenterY(row)
                val cell = Grid.pointToCell(x, y)
                assertArrayEquals(
                    "center of (row=$row,col=$col) should map back",
                    intArrayOf(row, col), cell
                )
            }
        }
    }

    @Test
    fun `point outside lawn returns -1 -1`() {
        val outside = Grid.pointToCell(10f, 10f)
        assertArrayEquals(intArrayOf(-1, -1), outside)

        val far = Grid.pointToCell(5000f, 5000f)
        assertArrayEquals(intArrayOf(-1, -1), far)
    }

    @Test
    fun `left top corner of cell maps correctly`() {
        val x = Grid.cellLeft(3) + 1f
        val y = Grid.cellTop(2) + 1f
        val cell = Grid.pointToCell(x, y)
        assertArrayEquals(intArrayOf(2, 3), cell)
    }

    @Test
    fun `isValidCell boundary`() {
        assertTrue(Grid.isValidCell(0, 0))
        assertTrue(Grid.isValidCell(Grid.ROWS - 1, Grid.COLS - 1))
        assertFalse(Grid.isValidCell(-1, 0))
        assertFalse(Grid.isValidCell(0, Grid.COLS))
        assertFalse(Grid.isValidCell(Grid.ROWS, 0))
    }

    @Test
    fun `grid dimensions are as designed`() {
        assertEquals(5, Grid.ROWS)
        assertEquals(9, Grid.COLS)
    }
}
