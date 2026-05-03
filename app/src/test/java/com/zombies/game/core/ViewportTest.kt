package com.zombies.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportTest {

    @Test
    fun `16x9 screen matches virtual exactly`() {
        val vp = Viewport(1920f, 1080f)
        vp.resize(1920, 1080)
        assertEquals(1f, vp.scale, 1e-4f)
        assertEquals(0f, vp.offsetX, 1e-4f)
        assertEquals(0f, vp.offsetY, 1e-4f)
    }

    @Test
    fun `wider screen adds horizontal letterbox`() {
        val vp = Viewport(1920f, 1080f)
        // 2400 x 1080，比 16:9 更宽 → 左右留黑边
        vp.resize(2400, 1080)
        assertEquals(1f, vp.scale, 1e-4f)
        assertTrue(vp.offsetX > 0f)
        assertEquals(0f, vp.offsetY, 1e-4f)
    }

    @Test
    fun `taller screen adds vertical letterbox`() {
        val vp = Viewport(1920f, 1080f)
        vp.resize(1920, 1600)
        assertEquals(1f, vp.scale, 1e-4f)
        assertEquals(0f, vp.offsetX, 1e-4f)
        assertTrue(vp.offsetY > 0f)
    }

    @Test
    fun `screen to virtual round trip`() {
        val vp = Viewport(1920f, 1080f)
        vp.resize(2400, 1080) // 带左右黑边
        val sx = vp.offsetX + 500f * vp.scale
        val sy = vp.offsetY + 300f * vp.scale
        assertEquals(500f, vp.screenToVirtualX(sx), 1e-3f)
        assertEquals(300f, vp.screenToVirtualY(sy), 1e-3f)
    }
}
