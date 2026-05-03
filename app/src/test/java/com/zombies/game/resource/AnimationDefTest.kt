package com.zombies.game.resource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationDefTest {

    @Test
    fun `looping animation wraps around`() {
        val anim = AnimationDef(frames = intArrayOf(0, 1, 2, 3), fps = 10, loop = true)
        // frameDuration = 100ms
        assertEquals(0, anim.frameAt(0L))
        assertEquals(1, anim.frameAt(100L))
        assertEquals(3, anim.frameAt(300L))
        assertEquals(0, anim.frameAt(400L)) // wrap
        assertEquals(1, anim.frameAt(500L))
    }

    @Test
    fun `non-looping animation stops at last frame`() {
        val anim = AnimationDef(frames = intArrayOf(10, 11, 12), fps = 10, loop = false)
        assertEquals(10, anim.frameAt(0L))
        assertEquals(12, anim.frameAt(200L))
        assertEquals(12, anim.frameAt(9999L))
        assertFalse(anim.isFinished(100L))
        assertTrue(anim.isFinished(300L))
    }

    @Test
    fun `duration calculation`() {
        val anim = AnimationDef(frames = intArrayOf(0, 1, 2, 3, 4), fps = 10, loop = true)
        assertEquals(500L, anim.durationMs)
        assertEquals(100L, anim.frameDurationMs)
    }

    @Test
    fun `empty frames safe`() {
        val anim = AnimationDef(frames = IntArray(0), fps = 10, loop = true)
        assertEquals(0, anim.frameAt(0L))
    }
}
