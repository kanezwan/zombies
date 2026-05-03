package com.zombies.game.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridOccupancyTest {

    @Test
    fun emptyByDefault() {
        val g = GridOccupancy()
        assertTrue(g.isEmpty(0, 0))
        assertEquals(-1L, g.get(0, 0))
    }

    @Test
    fun setAndClear() {
        val g = GridOccupancy()
        g.set(2, 3, 42L)
        assertFalse(g.isEmpty(2, 3))
        assertEquals(42L, g.get(2, 3))
        g.clear(2, 3)
        assertTrue(g.isEmpty(2, 3))
    }

    @Test
    fun invalidCellIgnored() {
        val g = GridOccupancy()
        g.set(-1, 0, 99L)          // 越界写入应被忽略
        g.set(10, 10, 99L)
        assertEquals(-1L, g.get(-1, 0))
        assertEquals(-1L, g.get(10, 10))
        // 越界格 isEmpty 返回 false（表示"非有效格"，不可种植）
        assertFalse(g.isEmpty(-1, 0))
        assertFalse(g.isEmpty(10, 10))
    }

    @Test
    fun resetClearsAll() {
        val g = GridOccupancy()
        g.set(0, 0, 1L)
        g.set(4, 8, 2L)
        g.reset()
        assertTrue(g.isEmpty(0, 0))
        assertTrue(g.isEmpty(4, 8))
    }
}
