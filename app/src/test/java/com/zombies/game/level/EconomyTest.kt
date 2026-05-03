package com.zombies.game.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyTest {

    @Test
    fun addAndSpend() {
        val eco = Economy(initialSun = 50)
        eco.addSun(30)
        assertEquals(80, eco.sun)
        assertTrue(eco.trySpend(50))
        assertEquals(30, eco.sun)
        assertFalse(eco.trySpend(100)) // 不够
        assertEquals(30, eco.sun)
    }

    @Test
    fun cooldownTicksDown() {
        val eco = Economy()
        eco.triggerCooldown("sunflower", 1000L)
        assertFalse(eco.isReady("sunflower"))
        eco.tick(300)
        assertEquals(700L, eco.cooldownRemainingMs("sunflower"))
        eco.tick(1000) // 超过
        assertTrue(eco.isReady("sunflower"))
        assertEquals(0L, eco.cooldownRemainingMs("sunflower"))
    }

    @Test
    fun readyByDefault() {
        val eco = Economy()
        assertTrue(eco.isReady("anything"))
    }

    @Test
    fun resetRestoresSun() {
        val eco = Economy(initialSun = 50)
        eco.addSun(200)
        eco.triggerCooldown("x", 500L)
        eco.reset(initialSun = 75)
        assertEquals(75, eco.sun)
        assertTrue(eco.isReady("x"))
    }

    @Test
    fun addSunClampsToZero() {
        val eco = Economy(initialSun = 10)
        eco.addSun(-100)
        assertEquals(0, eco.sun)
    }
}
