package com.zombies.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTest {

    @Test
    fun `tick fires exactly once per interval`() {
        val t = Timer(1000L)
        assertFalse(t.tick(500L))
        assertFalse(t.tick(499L))
        assertTrue(t.tick(1L)) // 正好达到 1000
        assertFalse(t.tick(500L))
    }

    @Test
    fun `tick does not double-fire on large dt`() {
        val t = Timer(100L)
        // 一次 500ms 的大跳步只触发一次（剩余 400ms 被保留）
        assertTrue(t.tick(500L))
        assertEquals(400L, t.elapsedMs)
    }

    @Test
    fun `reset restarts accumulator`() {
        val t = Timer(100L)
        t.tick(50L)
        t.reset()
        assertEquals(0L, t.elapsedMs)
    }

    @Test
    fun `progress in range 0-1`() {
        val t = Timer(200L)
        assertEquals(0f, t.progress(), 1e-4f)
        t.tick(100L)
        assertEquals(0.5f, t.progress(), 1e-4f)
    }
}
