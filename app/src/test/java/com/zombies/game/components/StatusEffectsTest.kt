package com.zombies.game.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusEffectsTest {

    @Test
    fun noEffectReturnsOne() {
        val e = StatusEffects()
        assertEquals(1f, e.currentSlowMultiplier(), 0.0001f)
    }

    @Test
    fun addAndStrongestSlowWins() {
        val e = StatusEffects()
        e.addOrRefreshSlow("snowpea", 0.5f, 3000L)
        e.addOrRefreshSlow("ice", 0.3f, 2000L)
        assertEquals(0.3f, e.currentSlowMultiplier(), 0.0001f)
    }

    @Test
    fun sameSourceRefreshesInsteadOfDuplicating() {
        val e = StatusEffects()
        e.addOrRefreshSlow("snowpea", 0.5f, 1000L)
        e.addOrRefreshSlow("snowpea", 0.5f, 3000L) // 刷新时长
        assertEquals(1, e.slows.size)
        assertEquals(3000L, e.slows[0].remainingMs)
    }

    @Test
    fun tickExpiresEffects() {
        val e = StatusEffects()
        e.addOrRefreshSlow("snowpea", 0.5f, 1000L)
        val removedA = e.tick(500L)
        assertFalse(removedA)
        assertEquals(0.5f, e.currentSlowMultiplier(), 0.0001f)
        val removedB = e.tick(600L)
        assertTrue(removedB)
        assertEquals(1f, e.currentSlowMultiplier(), 0.0001f)
    }
}
